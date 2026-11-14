import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * 用 ASM 把 jar 内的字符串常量替换成中文（只改 LDC 字符串，不动结构）。
 * 用法：java StringPatch <in.jar> <out.jar> <mapping.properties>
 */
public final class StringPatch {

    public static void main(String[] args) throws Exception {
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Map<String, String> map = new HashMap<>();
        for (String line : Files.readAllLines(Path.of(args[2]), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            int i = line.indexOf('=');
            if (i <= 0) continue;
            map.put(unescape(line.substring(0, i)), unescape(line.substring(i + 1)));
        }
        Remapper remapper = new Remapper() {
            @Override
            public Object mapValue(Object value) {
                if (value instanceof String s) {
                    String r = map.get(s);
                    if (r != null) return r;
                }
                return super.mapValue(value);
            }
        };
        int patched = 0;
        byte[][] needles = map.keySet().stream()
                .map(k -> k.getBytes(StandardCharsets.UTF_8)).toArray(byte[][]::new);
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(in));
             ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(out))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                byte[] data = zin.readAllBytes();
                if (e.getName().endsWith(".class") && containsAny(data, needles)) {
                    try {
                        ClassReader cr = new ClassReader(data);
                        ClassWriter cw = new ClassWriter(0);
                        ClassRemapper cm = new ClassRemapper(cw, remapper);
                        cr.accept(cm, 0);
                        byte[] nd = cw.toByteArray();
                        if (!Arrays.equals(nd, data)) patched++;
                        data = nd;
                    } catch (Throwable t) {
                        System.err.println("  ! 跳过 " + e.getName() + ": " + t);
                    }
                }
                ZipEntry ne = new ZipEntry(e.getName());
                zout.putNextEntry(ne);
                zout.write(data);
                zout.closeEntry();
            }
        }
        System.out.println("  改写 class 数: " + patched);
    }

    private static String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\");
    }

    /** 只在 class 字节里确实含有待替换字面量时才做 ASM 往返，避免无谓重写。 */
    private static boolean containsAny(byte[] haystack, byte[][] needles) {
        // ISO-8859-1 与字节一一对应，可用于二进制安全的子串查找
        String hay = new String(haystack, java.nio.charset.StandardCharsets.ISO_8859_1);
        for (byte[] n : needles) {
            if (n.length == 0) continue;
            if (hay.contains(new String(n, java.nio.charset.StandardCharsets.ISO_8859_1))) {
                return true;
            }
        }
        return false;
    }
}
