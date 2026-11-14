package com.yglcs.gcguard;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 初版参数（0.1.0/0.2.0）：以「整堆占用率」判定，软/硬两级阈值 + 速度门控。
 */
public final class GcGuardConfig {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.IntValue SOFT_PERCENT;
    private static final ModConfigSpec.IntValue HARD_PERCENT;
    private static final ModConfigSpec.IntValue MIN_INTERVAL_SECONDS;
    private static final ModConfigSpec.DoubleValue MAX_SPEED_PER_TICK;
    private static final ModConfigSpec.BooleanValue ONLY_WITHOUT_SCREEN;
    private static final ModConfigSpec.BooleanValue USE_JVM_PERIODIC_GC;
    private static final ModConfigSpec.IntValue JVM_PERIODIC_INTERVAL_SECONDS;
    private static final ModConfigSpec.IntValue MIN_RECLAIM_MEGABYTES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("主动回收（客户端，初版逻辑：整堆占用率 + 速度门控）").push("gc");
        ENABLED = builder.comment("总开关").define("enabled", true);
        SOFT_PERCENT = builder
                .comment("软阈值：整堆占用达到该百分比时，仅在「静止/慢速」状态下回收")
                .defineInRange("softPercent", 65, 40, 95);
        HARD_PERCENT = builder
                .comment("硬阈值：整堆占用达到该百分比时，无视移动速度立刻回收")
                .defineInRange("hardPercent", 85, 50, 98);
        MIN_INTERVAL_SECONDS = builder
                .comment("两次回收最小间隔（秒）")
                .defineInRange("minIntervalSeconds", 180, 30, 1800);
        MAX_SPEED_PER_TICK = builder
                .comment("速度门控：水平速度超过该值（格/tick）视为快速移动，软阈值不触发；飞行/疾跑也按快速处理")
                .defineInRange("maxSpeedPerTick", 0.30D, 0.05D, 2.0D);
        ONLY_WITHOUT_SCREEN = builder
                .comment("打开任何界面时不回收（避免界面里卡顿）")
                .define("onlyWithoutScreen", true);
        builder.pop();

        builder.comment("JVM 参数（运行时通过 HotSpotDiagnosticMXBean 设置）").push("jvm");
        USE_JVM_PERIODIC_GC = builder
                .comment("是否在运行时设置 G1PeriodicGCInterval（G1 并发周期回收，挂机场景几十毫秒，不是 Full GC）")
                .define("useJvmPeriodicGC", true);
        JVM_PERIODIC_INTERVAL_SECONDS = builder
                .comment("G1PeriodicGCInterval 的间隔（秒）")
                .defineInRange("jvmPeriodicIntervalSeconds", 120, 10, 3600);
        builder.pop();

        builder.comment("兜底回收的收益判定").push("feedback");
        MIN_RECLAIM_MEGABYTES = builder
                .comment("单次回收低于该值即视为无效回收（活集已满），后续自动拉长间隔，避免每次白停")
                .defineInRange("minReclaimMegabytes", 200, 0, 2048);
        builder.pop();

        SPEC = builder.build();
    }

    private GcGuardConfig() {
    }

    public static boolean enabled() {
        return ENABLED.get();
    }

    public static int softPercent() {
        return SOFT_PERCENT.get();
    }

    public static int hardPercent() {
        return HARD_PERCENT.get();
    }

    public static int minIntervalSeconds() {
        return MIN_INTERVAL_SECONDS.get();
    }

    public static double maxSpeedPerTick() {
        return MAX_SPEED_PER_TICK.get();
    }

    public static boolean onlyWithoutScreen() {
        return ONLY_WITHOUT_SCREEN.get();
    }

    public static boolean useJvmPeriodicGC() {
        return USE_JVM_PERIODIC_GC.get();
    }

    public static int jvmPeriodicIntervalSeconds() {
        return JVM_PERIODIC_INTERVAL_SECONDS.get();
    }

    public static int minReclaimMegabytes() {
        return MIN_RECLAIM_MEGABYTES.get();
    }
}
