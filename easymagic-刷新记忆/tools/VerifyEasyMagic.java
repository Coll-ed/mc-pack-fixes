import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 离线校验打补丁后的 Easy Magic jar。不启动游戏。
 *
 * 重点校验**注入点真的存在**（这是本改动最大的风险点）：
 * mixin 的注解值写错一个字符，在 NeoForge 里表现为启动时报错或静默不生效。
 * 这里直接读目标类的字节码来确认。
 */
public final class VerifyEasyMagic {

    private static int pass;
    private static int fail;

    private static final String MIXIN_CONFIG = "easymagic.rerollmemory.mixins.json";
    private static final String MIXIN_CLASS =
            "fuzs/easymagic/mixin/rerollmemory/EnchantmentMenuRerollMemoryMixin.class";
    /** v2 新增：记录"界面真正展示的附魔"的 mixin。 */
    private static final String CLUE_MIXIN_CLASS =
            "fuzs/easymagic/mixin/rerollmemory/ModEnchantmentMenuClueMixin.class";
    /** EasyMagic 自己的附魔台菜单（新挂钩点所在类，在原版 Easy Magic jar 里）。 */
    private static final String MOD_ENCHANTMENT_MENU =
            "fuzs/easymagic/world/inventory/ModEnchantmentMenu.class";
    /** EasyMagic 自带的 accessor（空池子保护要用它的 @Invoker）。 */
    private static final String ENCHANTMENT_MENU_ACCESSOR =
            "fuzs/easymagic/mixin/accessor/EnchantmentMenuAccessor.class";
    private static final String ACCESSOR_INVOKER = "callGetEnchantmentList";
    private static final String DESC_UPDATE_LEVELS_AND_CLUES =
            "(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/ItemStack;I)V";
    private static final String FIELD_CLUES = "clues";
    private static final String DESC_CLUES = "Ljava/util/List;";
    private static final String FIELD_PLAYER = "player";
    private static final String DESC_PLAYER = "Lnet/minecraft/world/entity/player/Player;";
    private static final String MODS_TOML = "META-INF/neoforge.mods.toml";
    private static final String ENCHANTMENT_MENU = "net/minecraft/world/inventory/EnchantmentMenu.class";

    private static final String DESC_GET_ENCHANTMENT_LIST =
            "(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/ItemStack;II)Ljava/util/List;";
    private static final String DESC_SELECT_ENCHANTMENT =
            "(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;"
                    + "ILjava/util/stream/Stream;)Ljava/util/List;";
    private static final String DESC_CLICK_MENU_BUTTON =
            "(Lnet/minecraft/world/entity/player/Player;I)Z";
    private static final String FIELD_ENCHANT_SLOTS = "enchantSlots";
    private static final String DESC_ENCHANT_SLOTS = "Lnet/minecraft/world/Container;";

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("用法: VerifyEasyMagic <打好补丁的jar> <原版jar> <编译输出目录> <Minecraft jar>");
            System.exit(2);
        }
        Path patched = Path.of(args[0]);
        Path original = Path.of(args[1]);
        Path classes = Path.of(args[2]);
        Path minecraftJar = Path.of(args[3]);

        System.out.println("=========== A. jar 内容 ===========");
        Map<String, byte[]> patchedEntries = readZip(patched);
        Map<String, byte[]> originalEntries = readZip(original);

        List<String> expectedNew = List.of(
                MIXIN_CONFIG,
                MIXIN_CLASS,
                "fuzs/easymagic/rerollmemory/RerollMemory.class",
                "fuzs/easymagic/rerollmemory/RerollMemory$PoolKey.class",
                "fuzs/easymagic/rerollmemory/RerollMemoryConfig.class",
                "fuzs/easymagic/rerollmemory/RerollMemoryTargets.class",
                "fuzs/easymagic/rerollmemory/RerollMemory$FilterOutcome.class",
                CLUE_MIXIN_CLASS);

        check("补丁 jar 可以打开，条目 " + patchedEntries.size() + " 个"
                        + "（原版 " + originalEntries.size() + " + 新增 " + expectedNew.size() + "）",
                patchedEntries.size() == originalEntries.size() + expectedNew.size());
        for (String name : expectedNew) {
            check("包含 " + name, patchedEntries.containsKey(name));
        }
        // 原有内容一个都不能少
        Set<String> lost = new LinkedHashSet<>(originalEntries.keySet());
        lost.removeAll(patchedEntries.keySet());
        check("原版条目无丢失（丢失 " + lost.size() + " 个）", lost.isEmpty());
        if (!lost.isEmpty()) {
            System.out.println("       " + lost);
        }

        System.out.println();
        System.out.println("=========== B. mixin 配置 ===========");
        String mixinJson = new String(patchedEntries.get(MIXIN_CONFIG), StandardCharsets.UTF_8);
        check("required=false（注入失败只记日志，不崩游戏）",
                mixinJson.contains("\"required\": false"));
        check("package 正确", mixinJson.contains("\"package\": \"fuzs.easymagic.mixin.rerollmemory\""));
        check("列出了 mixin 类", mixinJson.contains("EnchantmentMenuRerollMemoryMixin"));
        check("列出了记录展示附魔的 mixin 类", mixinJson.contains("ModEnchantmentMenuClueMixin"));

        System.out.println();
        System.out.println("=========== C. mods.toml 挂载 ===========");
        String toml = new String(patchedEntries.get(MODS_TOML), StandardCharsets.UTF_8);
        int mixinEntries = countOccurrences(toml, "[[mixins]]");
        check("[[mixins]] 条目 = 3（原 2 + 新增 1），实际 " + mixinEntries, mixinEntries == 3);
        check("新 mixin 配置已挂载", toml.contains("config=\"" + MIXIN_CONFIG + "\""));
        check("已标注改版", toml.contains("Easy Magic (Shin 改版)"));
        check("原版两个 mixin 配置仍在",
                toml.contains("easymagic.common.mixins.json")
                        && toml.contains("easymagic.neoforge.mixins.json"));

        System.out.println();
        System.out.println("=========== D. mixin 注解常量已内联成字面量 ===========");
        // javac 会把 static final String 常量内联进注解。若没内联，
        // 注解值就会是字段引用，运行期直接错 —— 这里用字节搜索确认字面量在常量池里。
        byte[] mixinBytes = patchedEntries.get(MIXIN_CLASS);
        check("getEnchantmentList 描述符在类常量池里",
                containsBytes(mixinBytes, DESC_GET_ENCHANTMENT_LIST));
        check("selectEnchantment 描述符在类常量池里",
                containsBytes(mixinBytes, DESC_SELECT_ENCHANTMENT));
        check("clickMenuButton 描述符在类常量池里",
                containsBytes(mixinBytes, DESC_CLICK_MENU_BUTTON));

        System.out.println();
        System.out.println("=========== D2. mixin 类结构（@Shadow / @Unique 字段） ===========");
        ClassNode mixin = new ClassNode();
        new ClassReader(mixinBytes).accept(mixin, 0);
        check("@Shadow 注解存在（注解常量池里有描述符）",
                containsBytes(mixinBytes,
                        "Lorg/spongepowered/asm/mixin/Shadow;"));
        check("@Unique 注解存在",
                containsBytes(mixinBytes,
                        "Lorg/spongepowered/asm/mixin/Unique;"));
        check("shadow 字段 enchantSlots 已声明（类型 " + DESC_ENCHANT_SLOTS + "）",
                findField(mixin, FIELD_ENCHANT_SLOTS, DESC_ENCHANT_SLOTS) != null);
        check("字段 rerollMemory$rollingItem 已声明",
                findField(mixin, "rerollMemory$rollingItem",
                        "Lnet/minecraft/world/item/ItemStack;") != null);
        check("字段 rerollMemory$enchantTarget 已声明",
                findField(mixin, "rerollMemory$enchantTarget",
                        "Lnet/minecraft/world/item/ItemStack;") != null);

        String configClass = new String(
                patchedEntries.get("fuzs/easymagic/rerollmemory/RerollMemoryConfig.class"),
                StandardCharsets.UTF_8);
        check("配置项 clearAllItemPools 在配置类里", configClass.contains("clearAllItemPools"));
        check("配置项 resetWhenExhausted 在配置类里",
                configClass.contains("resetWhenExhausted"));

        System.out.println();
        System.out.println("=========== D3. v2 新挂钩点（EasyMagic 侧）与 accessor 依赖 ===========");
        // 新挂钩点与 accessor 都是 Easy Magic 自己的类，就在"原版 Easy Magic jar"里，
        // 不必额外传参 —— 直接对着将要运行的那个版本校验，名字/描述符写错立刻暴露。
        byte[] modMenuBytes = originalEntries.get(MOD_ENCHANTMENT_MENU);
        check("原版 Easy Magic jar 里有 " + MOD_ENCHANTMENT_MENU, modMenuBytes != null);
        if (modMenuBytes != null) {
            ClassNode modMenu = new ClassNode();
            new ClassReader(modMenuBytes).accept(modMenu, 0);
            check("ModEnchantmentMenu.updateLevelsAndClues 存在（记录展示附魔的落点）",
                    findMethod(modMenu, "updateLevelsAndClues", DESC_UPDATE_LEVELS_AND_CLUES) != null);
            check("ModEnchantmentMenu.clues 字段存在（界面提示列表）",
                    findField(modMenu, FIELD_CLUES, DESC_CLUES) != null);
            check("ModEnchantmentMenu.player 字段存在",
                    findField(modMenu, FIELD_PLAYER, DESC_PLAYER) != null);
        }
        byte[] accessorBytes = originalEntries.get(ENCHANTMENT_MENU_ACCESSOR);
        check("原版 Easy Magic jar 里有 " + ENCHANTMENT_MENU_ACCESSOR, accessorBytes != null);
        if (accessorBytes != null) {
            ClassNode accessor = new ClassNode();
            new ClassReader(accessorBytes).accept(accessor, 0);
            check("accessor 提供 @Invoker " + ACCESSOR_INVOKER + "（空池子保护靠它重跑原版方法）",
                    findMethod(accessor, ACCESSOR_INVOKER, DESC_GET_ENCHANTMENT_LIST) != null);
        }
        byte[] clueMixinBytes = patchedEntries.get(CLUE_MIXIN_CLASS);
        check("补丁 jar 里有 " + CLUE_MIXIN_CLASS, clueMixinBytes != null);
        if (clueMixinBytes != null) {
            check("updateLevelsAndClues 描述符已内联进新 mixin 常量池",
                    containsBytes(clueMixinBytes, DESC_UPDATE_LEVELS_AND_CLUES));
            check("新 mixin 目标类写的是 ModEnchantmentMenu",
                    containsBytes(clueMixinBytes, "fuzs/easymagic/world/inventory/ModEnchantmentMenu"));
            check("新 mixin 有 HEAD 注入（提交上一轮展示的附魔 ⇒ 提示与落子一致）",
                    containsBytes(clueMixinBytes, "beginRefresh"));
        }
        check("空池子保护字段 rerollMemory$filterApplied 已声明",
                findField(mixin, "rerollMemory$filterApplied", "Z") != null);
        check("空池子保护字段 rerollMemory$retrying 已声明（防递归）",
                findField(mixin, "rerollMemory$retrying", "Z") != null);
        check("空池子保护确实调用了 accessor 的 " + ACCESSOR_INVOKER,
                containsBytes(mixinBytes, ACCESSOR_INVOKER));

        System.out.println();
        System.out.println("=========== E. 注入点在目标类字节码里确实存在 ===========");
        // 目标类是 Minecraft 的类，要从游戏 jar 里读，不在 mod jar 里
        byte[] menuBytes = readZip(minecraftJar).get(ENCHANTMENT_MENU);
        check("能从游戏 jar 里读到 " + ENCHANTMENT_MENU, menuBytes != null);
        if (menuBytes == null) {
            System.out.println("       游戏 jar: " + minecraftJar);
            System.out.println(fail == 0 ? "[FAIL]" : "[FAIL]");
            System.exit(1);
        }
        ClassNode menu = new ClassNode();
        new ClassReader(menuBytes).accept(menu, 0);

        MethodNode getList = findMethod(menu, "getEnchantmentList", DESC_GET_ENCHANTMENT_LIST);
        check("EnchantmentMenu.getEnchantmentList 存在", getList != null);
        MethodNode clickButton = findMethod(menu, "clickMenuButton", DESC_CLICK_MENU_BUTTON);
        check("EnchantmentMenu.clickMenuButton 存在", clickButton != null);

        if (getList != null) {
            // 找 selectEnchantment 调用，并确认它的前一条指令是把 Stream 压栈
            MethodInsnNode call = null;
            AbstractInsnNode before = null;
            for (AbstractInsnNode insn = getList.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode mi
                        && mi.getOpcode() == org.objectweb.asm.Opcodes.INVOKESTATIC
                        && mi.owner.equals("net/minecraft/world/item/enchantment/EnchantmentHelper")
                        && mi.name.equals("selectEnchantment")
                        && mi.desc.equals(DESC_SELECT_ENCHANTMENT)) {
                    call = mi;
                    before = insn.getPrevious();
                    break;
                }
            }
            check("getEnchantmentList 里调用了 selectEnchantment（@ModifyArg 的落点）", call != null);
            if (call != null) {
                boolean streamPushedLast = before instanceof MethodInsnNode mi
                        && mi.name.equals("stream")
                        && mi.desc.equals("()Ljava/util/stream/Stream;");
                check("selectEnchantment 前一条指令压入 Stream ⇒ 候选流就是 index 3",
                        streamPushedLast);
                if (!streamPushedLast) {
                    System.out.println("       实际前一条: " + describe(before));
                }
                int params = countParams(DESC_SELECT_ENCHANTMENT);
                check("selectEnchantment 共 4 个参数（index 3 有效），实际 " + params, params == 4);
            }
        }

        if (clickButton != null) {
            boolean returnsBoolean = clickButton.desc.endsWith(")Z");
            check("clickMenuButton 返回 boolean ⇒ 可用 true 判定是否真的附魔成功", returnsBoolean);
        }

        // @Shadow 的字段必须在目标类里真实存在（名字+描述符都要对），否则整个 mixin 会不生效
        check("@Shadow 目标字段 enchantSlots 存在于 EnchantmentMenu",
                findField(menu, FIELD_ENCHANT_SLOTS, DESC_ENCHANT_SLOTS) != null);
        if (findField(menu, FIELD_ENCHANT_SLOTS, DESC_ENCHANT_SLOTS) == null) {
            System.out.println("       期望: " + FIELD_ENCHANT_SLOTS + " " + DESC_ENCHANT_SLOTS);
            for (org.objectweb.asm.tree.FieldNode field : menu.fields) {
                if (field.name.toLowerCase().contains("enchant")) {
                    System.out.println("       实际有: " + field.name + " " + field.desc);
                }
            }
        }

        System.out.println();
        System.out.println("=========== F. 纯逻辑单元测试（removeSeen） ===========");
        testRemoveSeen(classes);

        System.out.println();
        System.out.println(fail == 0
                ? "[ALL PASS] " + pass + " 项校验全部通过"
                : "[FAIL] " + fail + " 项失败 / 共 " + (pass + fail) + " 项");
        if (fail > 0) {
            System.exit(1);
        }
    }

    /** removeSeen 是过滤逻辑的核心，且刻意做成了纯函数，这里直接测。 */
    @SuppressWarnings("unchecked")
    private static void testRemoveSeen(Path classes) throws Exception {
        java.net.URL[] urls = {classes.toUri().toURL()};
        try (java.net.URLClassLoader loader =
                     new java.net.URLClassLoader(urls, VerifyEasyMagic.class.getClassLoader())) {
            Class<?> memory = Class.forName("fuzs.easymagic.rerollmemory.RerollMemory", true, loader);
            Method removeSeen = memory.getMethod("removeSeen", List.class, Function.class, Set.class);
            Function<String, String> identity = s -> s;

            // 全部没见过 -> 原样返回
            List<String> all = new ArrayList<>(List.of("sharpness", "unbreaking", "fortune"));
            Object result = removeSeen.invoke(null, all, identity, Set.of());
            check("没见过时全部保留", result.equals(all));

            // 部分见过 -> 只留没见过的
            result = removeSeen.invoke(null, all, identity, Set.of("unbreaking"));
            check("部分见过时只保留没见过的",
                    result.equals(List.of("sharpness", "fortune")));

            // 全部见过 -> 空（由调用方据此判定"池子刷完"）
            result = removeSeen.invoke(null, all, identity, Set.of("sharpness", "unbreaking", "fortune"));
            check("全部见过时返回空（触发池子刷完处理）", ((List<?>) result).isEmpty());

            // 取不到 key 的项按"没见过"处理（宁多不少，不能误排除）
            result = removeSeen.invoke(null, all, (Function<String, String>) s -> null, Set.of("sharpness"));
            check("取不到 key 的项不会被误排除", result.equals(all));

            // seen 为 null 视为空集
            result = removeSeen.invoke(null, all, identity, null);
            check("seen 为 null 时视为空集", result.equals(all));

            // 空候选
            result = removeSeen.invoke(null, List.of(), identity, Set.of("x"));
            check("空候选返回空", ((List<?>) result).isEmpty());

            // 池子标识：(玩家, 物品类别) —— 决定"给剑刷会不会消耗书的池子"
            Class<?> poolKeyClass =
                    Class.forName("fuzs.easymagic.rerollmemory.RerollMemory$PoolKey", true, loader);
            var ctor = poolKeyClass.getDeclaredConstructor(java.util.UUID.class, String.class);
            java.util.UUID playerA = java.util.UUID.randomUUID();
            java.util.UUID playerB = java.util.UUID.randomUUID();
            Object swordA = ctor.newInstance(playerA, "net.minecraft.world.item.SwordItem");
            Object swordA2 = ctor.newInstance(playerA, "net.minecraft.world.item.SwordItem");
            Object bookA = ctor.newInstance(playerA, "net.minecraft.world.item.BookItem");
            Object swordB = ctor.newInstance(playerB, "net.minecraft.world.item.SwordItem");
            check("同玩家同类别 -> 同一个池子", swordA.equals(swordA2));
            check("同玩家同类别 -> hashCode 一致（可作 Map 键）", swordA.hashCode() == swordA2.hashCode());
            check("同玩家不同类别 -> 不同池子（剑不消耗书的池子）", !swordA.equals(bookA));
            check("不同玩家同类别 -> 不同池子", !swordA.equals(swordB));
        }
    }

    private static org.objectweb.asm.tree.FieldNode findField(ClassNode node, String name, String desc) {
        for (org.objectweb.asm.tree.FieldNode field : node.fields) {
            if (field.name.equals(name) && field.desc.equals(desc)) {
                return field;
            }
        }
        return null;
    }

    private static String describe(AbstractInsnNode insn) {
        return insn == null ? "(null)" : insn.getClass().getSimpleName() + " " + insn;
    }

    private static int countParams(String desc) {
        String params = desc.substring(1, desc.indexOf(')'));
        int count = 0;
        for (int i = 0; i < params.length(); i++) {
            char c = params.charAt(i);
            if (c == 'L') {
                i = params.indexOf(';', i);
                count++;
            } else if (c == '[') {
                // 数组：跳过标记，下一轮计入
            } else {
                count++;
            }
        }
        return count;
    }

    private static MethodNode findMethod(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }
        return null;
    }

    private static boolean containsBytes(byte[] haystack, String needle) {
        byte[] target = needle.getBytes(StandardCharsets.UTF_8);
        outer:
        for (int i = 0; i <= haystack.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (haystack[i + j] != target[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static Map<String, byte[]> readZip(Path jar) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var it = zip.entries();
            while (it.hasMoreElements()) {
                ZipEntry entry = it.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] chunk = new byte[8192];
                    int read;
                    while ((read = in.read(chunk)) > 0) {
                        buffer.write(chunk, 0, read);
                    }
                    entries.put(entry.getName(), buffer.toByteArray());
                }
            }
        }
        return entries;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (ok) {
            pass++;
        } else {
            fail++;
        }
    }

    private VerifyEasyMagic() {
    }
}
