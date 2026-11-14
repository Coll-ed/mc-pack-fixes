import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 给 Easy Magic 打补丁：加入「刷新记忆」机制。
 *
 * ── 这个打包器**不改任何已有字节码** ──
 * 只做三件事：
 *   1. 把新编译的类加进 jar；
 *   2. 加入一个独立的 mixin 配置 easymagic.rerollmemory.mixins.json；
 *   3. 在 neoforge.mods.toml 里挂上那个 mixin 配置，并标注改版。
 * 相比直接改写字节码，风险低得多，也容易回滚（保留原版 .orig 即可）。
 *
 * 找不到挂载锚点就直接报错退出 —— 宁可失败，也不能产出一个"看起来打了补丁、其实没挂上"的 jar。
 */
public final class PatchEasyMagic {

    private static final String MODS_TOML = "META-INF/neoforge.mods.toml";
    private static final String MIXIN_CONFIG_NAME = "easymagic.rerollmemory.mixins.json";

    /** 挂载点：现有第二个 mixin 配置那一行之后插入，保持 TOML 结构清晰。 */
    private static final String MIXIN_ANCHOR = "config=\"easymagic.neoforge.mixins.json\"";
    private static final String MIXIN_ENTRY = "[[mixins]]\nconfig=\"" + MIXIN_CONFIG_NAME + "\"\n";

    private static final String DISPLAY_NAME_ORIGINAL = "displayName = \"Easy Magic\"";
    private static final String DISPLAY_NAME_PATCHED = "displayName = \"Easy Magic (Shin 改版)\"";

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("用法: PatchEasyMagic <原版jar> <编译输出目录> <资源目录> <输出jar>");
            System.exit(2);
        }
        Path inJar = Path.of(args[0]);
        Path classDir = Path.of(args[1]);
        Path resDir = Path.of(args[2]);
        Path outJar = Path.of(args[3]);

        Map<String, byte[]> entries = readZip(inJar);

        // ── 1. 新类 ──
        List<String> addedClasses = new ArrayList<>();
        if (!Files.isDirectory(classDir)) {
            throw new IOException("编译输出目录不存在: " + classDir);
        }
        try (var stream = Files.walk(classDir)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                String name = classDir.relativize(file).toString().replace('\\', '/');
                if (!name.endsWith(".class")) {
                    continue;
                }
                entries.put(name, Files.readAllBytes(file));
                addedClasses.add(name);
            }
        }
        if (addedClasses.isEmpty()) {
            throw new IOException("编译输出目录里没有 .class: " + classDir);
        }

        // ── 2. mixin 配置 ──
        Path mixinJson = resDir.resolve(MIXIN_CONFIG_NAME);
        if (!Files.isRegularFile(mixinJson)) {
            throw new IOException("找不到 mixin 配置: " + mixinJson);
        }
        entries.put(MIXIN_CONFIG_NAME, Files.readAllBytes(mixinJson));

        // ── 3. 挂载 mixin 配置 + 改版标注 ──
        byte[] tomlBytes = entries.get(MODS_TOML);
        if (tomlBytes == null) {
            throw new IOException("jar 里没有 " + MODS_TOML);
        }
        String toml = new String(tomlBytes, StandardCharsets.UTF_8);

        if (!toml.contains(MIXIN_CONFIG_NAME)) {
            int anchor = toml.indexOf(MIXIN_ANCHOR);
            if (anchor < 0) {
                throw new IOException("在 " + MODS_TOML + " 里找不到挂载锚点: " + MIXIN_ANCHOR
                        + " —— 该模组版本可能已变化，需要重新确认结构");
            }
            int insertAt = anchor + MIXIN_ANCHOR.length();
            // 吃掉锚点行末尾的换行，保证新段落从行首开始
            while (insertAt < toml.length()
                    && (toml.charAt(insertAt) == '\r' || toml.charAt(insertAt) == '\n')) {
                insertAt++;
            }
            toml = toml.substring(0, insertAt) + "\n" + MIXIN_ENTRY + "\n"
                    + toml.substring(insertAt);
            System.out.println("[OK] 已挂载 mixin 配置: " + MIXIN_CONFIG_NAME);
        } else {
            System.out.println("[SKIP] mixin 配置已挂载过，不重复添加");
        }

        if (toml.contains(DISPLAY_NAME_ORIGINAL)) {
            toml = toml.replace(DISPLAY_NAME_ORIGINAL, DISPLAY_NAME_PATCHED);
            System.out.println("[OK] 已标注改版: " + DISPLAY_NAME_PATCHED);
        } else if (toml.contains(DISPLAY_NAME_PATCHED)) {
            System.out.println("[SKIP] 已标注过改版");
        } else {
            // 不静默通过：名字没改成，说明 jar 结构与预期不符，明说
            System.out.println("[WARN] 没找到 displayName = \"Easy Magic\"，跳过改版标注");
        }
        entries.put(MODS_TOML, toml.getBytes(StandardCharsets.UTF_8));

        writeZip(outJar, entries);

        System.out.println("[OK] 新增类 " + addedClasses.size() + " 个:");
        for (String name : addedClasses) {
            System.out.println("       " + name);
        }
        System.out.println("[OK] 输出 " + outJar.toAbsolutePath() + "  ("
                + Files.size(outJar) + " bytes)");
    }

    private static Map<String, byte[]> readZip(Path jar) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    entries.put(entry.getName(), new byte[0]);
                    continue;
                }
                entries.put(entry.getName(), readAll(in));
            }
        }
        return entries;
    }

    private static void writeZip(Path jar, Map<String, byte[]> entries) throws IOException {
        Files.createDirectories(jar.toAbsolutePath().getParent());
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                ZipEntry entry = new ZipEntry(e.getKey());
                // 固定时间戳，让同样的输入产出同样的 jar（便于比对 SHA256）
                entry.setTime(0L);
                out.putNextEntry(entry);
                out.write(e.getValue());
                out.closeEntry();
            }
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) > 0) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private PatchEasyMagic() {
    }
}
