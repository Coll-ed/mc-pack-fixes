import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 修 EasyMagic「附魔刷新记忆」的缺陷（v3）。
 *
 * ── 缺陷（原版）：空池保护会绕过过滤 ──
 * {@code protectEmptyPool} 在槽位凑不出附魔时置 retrying=true 重算，
 * 而 {@code filterCandidates} 见 retrying=true 就**原样返回未过滤的流** →
 * 把刷过的附魔又摆出来（实测 508 次）。
 *
 * ── v1/v2 修法的问题（用户实测反馈）──
 * · v1：**任一**槽位凑不出就清池。实测 pool=55 kinds 就被清空，而当时全集约 98 种
 *       （现在装了新模组已近 200+）—— 还剩一大半没见过的被误判成"刷完了"。
 * · v2：改成"连续 3 个槽位失败才清池"—— 3 是硬编码的槽位数，与池子实际状态无关。
 *
 * ── v3 修法（不硬编码，只看筛选的实际产出）──
 * 1) {@code rerollMemory$filterCandidates}：不再有"retrying 就不过滤"的分支，
 *    永远走正常过滤（于是凑不出时该槽位就是**空**，绝不用见过的顶替）。
 * 2) {@code rerollMemory$protectEmptyPool}：每槽位把结果报告给
 *    {@code DupFilterState.beginSlot(slot, filled)}；该判定用**槽位序号回绕**识别
 *    "上一轮刷新结束"，若那一轮**一个附魔都没出来**才返回 true → 此刻重洗池子。
 *    不数槽位、不假设池子大小。
 * 3) 修正随之不再准确的日志文案。
 *
 * 用法: PatchEasyMagicDupFilter <原jar> <输出jar> <DupFilterState.class路径>
 */
public final class PatchEasyMagicDupFilter {

    private static final String MIXIN_CLASS =
            "fuzs/easymagic/mixin/rerollmemory/EnchantmentMenuRerollMemoryMixin";
    private static final String MEMORY_CLASS = "fuzs/easymagic/rerollmemory/RerollMemory";
    private static final String FILTER_OUTCOME = MEMORY_CLASS + "$FilterOutcome";

    /** 被注入的决策状态类（本工具编译后塞进补丁 jar）。 */
    private static final String STATE_CLASS = "compatpatch/easymagic/DupFilterState";
    private static final String DESC_BEGIN_SLOT = "(IZ)Z";

    private static final String METHOD_FILTER_CANDIDATES = "rerollMemory$filterCandidates";
    private static final String DESC_FILTER_CANDIDATES =
            "(Ljava/util/stream/Stream;)Ljava/util/stream/Stream;";
    private static final String METHOD_PROTECT_EMPTY_POOL = "rerollMemory$protectEmptyPool";
    private static final String DESC_PROTECT_EMPTY_POOL =
            "(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/ItemStack;II"
                    + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V";
    private static final String METHOD_PLAYER = "rerollMemory$player";
    private static final String DESC_PLAYER = "()Lnet/minecraft/world/entity/player/Player;";
    private static final String FIELD_FILTER_APPLIED = "rerollMemory$filterApplied";
    private static final String FIELD_RETRYING = "rerollMemory$retrying";
    private static final String FIELD_ROLLING_ITEM = "rerollMemory$rollingItem";
    private static final String DESC_ITEM_STACK = "Lnet/minecraft/world/item/ItemStack;";
    private static final String DESC_CLEAR =
            "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V";
    private static final String DESC_LOG_EMPTY =
            "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V";
    private static final String DESC_FILTER_CANDIDATES_STATIC =
            "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;"
                    + "Ljava/util/stream/Stream;)L" + FILTER_OUTCOME + ";";
    private static final String DESC_REMOVED_ANY = "()Z";
    private static final String DESC_STREAM = "()Ljava/util/stream/Stream;";
    private static final String DESC_CIR = "Lorg/spongepowered/asm/mixin/injection/callback/"
            + "CallbackInfoReturnable;";
    private static final String CALLBACK_INFO_RETURNABLE =
            "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";

    // 日志文案（旧 -> 新）。旧文案可能有两种（原始版 / v1 版），替换时accept 任一。
    private static final String[] OLD_MSGS = {
            "[{}] filtering left nothing usable for slot {} (item={}, pool={} kinds) "
                    + "=> pool saturated: starting a new round",
            "[{}] filtering left nothing usable for slot {} (item={}, pool={} kinds) "
                    + "=> empty-pool protection: rerunning this lookup unfiltered (vanilla result)",
    };
    private static final String NEW_MSG =
            "[{}] filtering left nothing usable for slot {} (item={}, pool={} kinds) "
                    + "=> slot left empty (no unseen enchantment fits; "
                    + "a fully-empty refresh re-rolls the pool next time)";

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("用法: PatchEasyMagicDupFilter <原jar> <输出jar> <DupFilterState.class>");
            System.exit(2);
        }
        Path inJar = Path.of(args[0]);
        Path outJar = Path.of(args[1]);
        Path stateClass = Path.of(args[2]);
        if (!Files.isRegularFile(stateClass)) {
            throw new IllegalStateException("找不到决策状态类: " + stateClass);
        }

        Map<String, byte[]> entries = readZip(inJar);

        // ── 1) filterCandidates：去掉「retrying 就不过滤/清池」的分支，永远走正常过滤 ──
        ClassNode mixin = readClass(entries, MIXIN_CLASS);
        MethodNode filter = require(mixin, METHOD_FILTER_CANDIDATES, DESC_FILTER_CANDIDATES);
        rebuildFilterCandidates(filter, MIXIN_CLASS);
        entries.put(MIXIN_CLASS + ".class", writeClass(mixin));
        System.out.println("[OK] 重写 " + METHOD_FILTER_CANDIDATES + "：永远走正常过滤（凑不出时该槽位留空）");

        // ── 2) protectEmptyPool：不再重算，改为计数 + 只有真饱和才清池 ──
        MethodNode protect = require(mixin, METHOD_PROTECT_EMPTY_POOL, DESC_PROTECT_EMPTY_POOL);
        rebuildProtectEmptyPool(protect, MIXIN_CLASS);
        entries.put(MIXIN_CLASS + ".class", writeClass(mixin));
        System.out.println("[OK] 重写 " + METHOD_PROTECT_EMPTY_POOL
                + "：槽位留空；整轮全空才在下一轮重洗池子");

        // ── 3) 日志文案 ──
        ClassNode memory = readClass(entries, MEMORY_CLASS);
        int changed = 0;
        for (String oldMsg : OLD_MSGS) {
            changed += replaceString(memory, "logEmptyPoolProtection", oldMsg, NEW_MSG);
        }
        if (changed != 1) {
            throw new IllegalStateException("预期改 1 处日志文案，实际 " + changed + " 处，中止");
        }
        entries.put(MEMORY_CLASS + ".class", writeClass(memory));
        System.out.println("[OK] 修正日志文案 1 处（改为「留空」而非「清池」）");

        // ── 4) 塞入决策状态类 ──
        entries.put(STATE_CLASS + ".class", Files.readAllBytes(stateClass));
        System.out.println("[OK] 注入 " + STATE_CLASS);

        writeZip(outJar, entries);
        System.out.println("[OK] 输出 " + outJar.toAbsolutePath() + "  (" + Files.size(outJar) + " bytes)");
    }

    /**
     * 重建 filterCandidates 为**只有正常路径**：
     * <pre>
     *   FilterOutcome outcome = RerollMemory.filterCandidates(player, rollingItem, candidates);
     *   this.rerollMemory$filterApplied = outcome.removedAny();
     *   return outcome.stream();
     * </pre>
     */
    private static void rebuildFilterCandidates(MethodNode m, String owner) {
        InsnList ins = new InsnList();
        ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, METHOD_PLAYER, DESC_PLAYER, false));
        ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ins.add(new FieldInsnNode(Opcodes.GETFIELD, owner, FIELD_ROLLING_ITEM, DESC_ITEM_STACK));
        ins.add(new VarInsnNode(Opcodes.ALOAD, 1));
        ins.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MEMORY_CLASS,
                "filterCandidates", DESC_FILTER_CANDIDATES_STATIC, false));
        ins.add(new VarInsnNode(Opcodes.ASTORE, 2));
        ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ins.add(new VarInsnNode(Opcodes.ALOAD, 2));
        ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, FILTER_OUTCOME,
                "removedAny", DESC_REMOVED_ANY, false));
        ins.add(new FieldInsnNode(Opcodes.PUTFIELD, owner, FIELD_FILTER_APPLIED, "Z"));
        ins.add(new VarInsnNode(Opcodes.ALOAD, 2));
        ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, FILTER_OUTCOME,
                "stream", DESC_STREAM, false));
        ins.add(new InsnNode(Opcodes.ARETURN));
        apply(m, ins, 3);
    }

    /**
     * 重建 protectEmptyPool（v3）：
     * <pre>
     *   List result = cir.getReturnValue();
     *   boolean filled = (result != null &amp;&amp; !result.isEmpty());
     *   // 把本槽位结果报告给判定器；它用「槽位序号回绕」识别上一轮结束，
     *   // 若上一轮一个附魔都没出来 → 重洗池子（开新一轮）
     *   if (DupFilterState.beginSlot(slot, filled)) {
     *       RerollMemory.clear(player(), rollingItem);
     *   }
     *   if (filled) return;                                     // 有结果：什么都不用做
     *   if (!this.filterApplied || this.retrying) return;
     *   RerollMemory.logEmptyPoolProtection(player(), rollingItem, slot);   // 记录：该槽位留空
     *   // 不再就地清池：等整轮都为空时由上面的判定器在下一轮开头重洗
     * </pre>
     */
    private static void rebuildProtectEmptyPool(MethodNode m, String owner) {
        LabelNode lNotFilled = new LabelNode();
        LabelNode lStoreFilled = new LabelNode();
        LabelNode lNoReset = new LabelNode();
        LabelNode lReturn = new LabelNode();

        InsnList ins = new InsnList();
        // List result = cir.getReturnValue();
        ins.add(new VarInsnNode(Opcodes.ALOAD, 5));
        ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CALLBACK_INFO_RETURNABLE,
                "getReturnValue", "()Ljava/lang/Object;", false));
        ins.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.CHECKCAST, "java/util/List"));
        ins.add(new VarInsnNode(Opcodes.ASTORE, 6));
        // boolean filled = result != null && !result.isEmpty();
        ins.add(new VarInsnNode(Opcodes.ALOAD, 6));
        ins.add(new JumpInsnNode(Opcodes.IFNULL, lNotFilled));
        ins.add(new VarInsnNode(Opcodes.ALOAD, 6));
        ins.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "isEmpty", "()Z", true));
        ins.add(new JumpInsnNode(Opcodes.IFNE, lNotFilled));
        ins.add(new InsnNode(Opcodes.ICONST_1));
        ins.add(new JumpInsnNode(Opcodes.GOTO, lStoreFilled));
        ins.add(lNotFilled);
        ins.add(new InsnNode(Opcodes.ICONST_0));
        ins.add(lStoreFilled);
        ins.add(new VarInsnNode(Opcodes.ISTORE, 7));
        // if (DupFilterState.beginSlot(slot, filled)) { RerollMemory.clear(player(), rollingItem); }
        ins.add(new VarInsnNode(Opcodes.ILOAD, 3));
        ins.add(new VarInsnNode(Opcodes.ILOAD, 7));
        ins.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STATE_CLASS,
                "beginSlot", DESC_BEGIN_SLOT, false));
        ins.add(new JumpInsnNode(Opcodes.IFEQ, lNoReset));
        ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, METHOD_PLAYER, DESC_PLAYER, false));
        ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ins.add(new FieldInsnNode(Opcodes.GETFIELD, owner, FIELD_ROLLING_ITEM, DESC_ITEM_STACK));
        ins.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MEMORY_CLASS, "clear", DESC_CLEAR, false));
        ins.add(lNoReset);
        // if (filled) return;
        ins.add(new VarInsnNode(Opcodes.ILOAD, 7));
        ins.add(new JumpInsnNode(Opcodes.IFNE, lReturn));
        // if (!filterApplied || retrying) return;
        ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ins.add(new FieldInsnNode(Opcodes.GETFIELD, owner, FIELD_FILTER_APPLIED, "Z"));
        ins.add(new JumpInsnNode(Opcodes.IFEQ, lReturn));
        ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ins.add(new FieldInsnNode(Opcodes.GETFIELD, owner, FIELD_RETRYING, "Z"));
        ins.add(new JumpInsnNode(Opcodes.IFNE, lReturn));
        // RerollMemory.logEmptyPoolProtection(player(), rollingItem, slot);
        ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, METHOD_PLAYER, DESC_PLAYER, false));
        ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ins.add(new FieldInsnNode(Opcodes.GETFIELD, owner, FIELD_ROLLING_ITEM, DESC_ITEM_STACK));
        ins.add(new VarInsnNode(Opcodes.ILOAD, 3));
        ins.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MEMORY_CLASS,
                "logEmptyPoolProtection", DESC_LOG_EMPTY, false));
        ins.add(lReturn);
        ins.add(new InsnNode(Opcodes.RETURN));
        apply(m, ins, 8);
    }

    private static void apply(MethodNode m, InsnList ins, int maxLocals) {
        m.instructions = ins;
        m.tryCatchBlocks.clear();
        m.localVariables = null;
        m.visibleLocalVariableAnnotations = null;
        m.maxStack = 8;
        if (m.maxLocals < maxLocals) {
            m.maxLocals = maxLocals;
        }
    }

    private static int replaceString(ClassNode node, String methodName, String oldMsg, String newMsg) {
        int changed = 0;
        for (MethodNode m : node.methods) {
            if (!m.name.equals(methodName)) {
                continue;
            }
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LdcInsnNode ldc && oldMsg.equals(ldc.cst)) {
                    ldc.cst = newMsg;
                    changed++;
                }
            }
        }
        return changed;
    }

    private static ClassNode readClass(Map<String, byte[]> entries, String name) {
        byte[] bytes = entries.get(name + ".class");
        if (bytes == null) {
            throw new IllegalStateException("jar 里没有 " + name);
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static MethodNode require(ClassNode node, String name, String desc) {
        for (MethodNode m : node.methods) {
            if (m.name.equals(name) && m.desc.equals(desc)) {
                return m;
            }
        }
        throw new IllegalStateException("找不到方法 " + name + desc);
    }

    private static byte[] writeClass(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static Map<String, byte[]> readZip(Path jar) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (e.isDirectory()) {
                    entries.put(e.getName(), new byte[0]);
                    continue;
                }
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int read;
                while ((read = in.read(chunk)) > 0) {
                    buf.write(chunk, 0, read);
                }
                entries.put(e.getName(), buf.toByteArray());
            }
        }
        return entries;
    }

    private static void writeZip(Path jar, Map<String, byte[]> entries) throws Exception {
        Files.createDirectories(jar.toAbsolutePath().getParent());
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                ZipEntry entry = new ZipEntry(e.getKey());
                entry.setTime(0L);
                out.putNextEntry(entry);
                out.write(e.getValue());
                out.closeEntry();
            }
        }
    }

    private PatchEasyMagicDupFilter() {
    }
}
