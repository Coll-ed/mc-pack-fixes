package com.yglcs.gcguard;

import com.mojang.logging.LogUtils;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import org.slf4j.Logger;

/**
 * 通过 JVM 自带的 HotSpotDiagnosticMXBean 在运行时设置 G1 的周期性 GC 参数。
 *
 * <p>只做一件事：把 {@code G1PeriodicGCInterval} 设成指定毫秒值。
 * 该参数走的是 G1 的 Concurrent Start（并发标记），不是 Full GC，对游戏线程几乎无感。</p>
 */
final class HotSpot {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** HotSpotDiagnosticMXBean 的对象名。 */
    private static final String DIAGNOSTIC_MXBEAN = "com.sun.management:type=HotSpotDiagnostic";
    /** G1 周期性回收间隔的 VM 选项名。 */
    private static final String OPTION_PERIODIC_GC_INTERVAL = "G1PeriodicGCInterval";

    private HotSpot() {
    }

    /**
     * 安装周期回收间隔。
     *
     * @param intervalSeconds 间隔秒数
     * @return 是否成功写入
     */
    static boolean installPeriodicGc(int intervalSeconds) {
        long millis = intervalSeconds * 1_000L;
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(DIAGNOSTIC_MXBEAN);
            Object bean = ManagementFactory.newPlatformMXBeanProxy(server, DIAGNOSTIC_MXBEAN,
                    Class.forName("com.sun.management.HotSpotDiagnosticMXBean"));
            Method setVMOption = bean.getClass().getMethod("setVMOption", String.class, String.class);
            setVMOption.invoke(bean, OPTION_PERIODIC_GC_INTERVAL, Long.toString(millis));
            LOGGER.info("[gcguard] 已设置 JVM 参数 {} = {} ms（G1 周期回收，Concurrent Start）",
                    OPTION_PERIODIC_GC_INTERVAL, millis);
            return true;
        } catch (ReflectiveOperationException | javax.management.OperationsException | java.io.IOException
                | RuntimeException e) {
            LOGGER.warn("[gcguard] 运行时设置 {} 失败：{}（仅保留 System.gc() 路径）",
                    OPTION_PERIODIC_GC_INTERVAL, e.toString());
            return false;
        }
    }
}
