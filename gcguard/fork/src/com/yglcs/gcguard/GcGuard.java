package com.yglcs.gcguard;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * 主动回收（初版逻辑，0.1.0/0.2.0）。
 *
 * <p>判定基准是<b>整堆占用率</b>，两级阈值：</p>
 * <ul>
 *   <li><b>软阈值</b>（默认 65%）：只在「静止 / 慢速」时回收；飞行、疾跑、快速水平移动期间跳过，
 *       避免跑图时突然卡一下；</li>
 *   <li><b>硬阈值</b>（默认 85%）：无视速度立即回收。</li>
 * </ul>
 *
 * <p>间隔由 {@code minIntervalSeconds}（默认 180 秒）限制；打开界面时不动作。</p>
 */
@Mod(value = GcGuard.MOD_ID, dist = Dist.CLIENT)
public final class GcGuard {

    public static final String MOD_ID = "gcguard";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 原版 tick 频率。 */
    private static final int TICKS_PER_SECOND = 20;
    /** 检查间隔（秒）。 */
    private static final int CHECK_INTERVAL_SECONDS = 4;
    /** 毫秒换算。 */
    private static final long MILLIS_PER_SECOND = 1_000L;
    /** MB 换算。 */
    private static final long BYTES_PER_MB = 1024L * 1024L;
    /** 疾跑速度门限（格/tick）：超过即视为快速移动，软阈值跳过。 */
    private static final double SPRINT_SPEED_THRESHOLD = 0.20D;
    /** 连续无效回收时，间隔最多退避到配置值的几倍。 */
    private static final int MAX_BACKOFF_MULTIPLIER = 8;

    private static int tickCounter;
    private static long lastGcMillis;
    private static int ineffectiveRuns;
    private static boolean bootstrapDone;

    public GcGuard(ModContainer container, IEventBus modBus) {
        container.registerConfig(ModConfig.Type.CLIENT, GcGuardConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(GcGuard::onClientTick);
        LOGGER.info("[gcguard] 已加载，配置在首个客户端 tick 生效");
    }

    /**
     * 首个客户端 tick 才读配置。
     * 模组构造阶段配置尚未加载，直接取值会抛 IllegalStateException。
     */
    private static void bootstrap() {
        if (bootstrapDone) {
            return;
        }
        bootstrapDone = true;
        if (GcGuardConfig.useJvmPeriodicGC()) {
            HotSpot.installPeriodicGc(GcGuardConfig.jvmPeriodicIntervalSeconds());
        }
        LOGGER.info("[gcguard] 0.1 判定 + JVM 参数 + 无效回收退避：软阈值 {}% / 硬阈值 {}%，最小间隔 {}s，无效阈值 {}MB",
                GcGuardConfig.softPercent(), GcGuardConfig.hardPercent(),
                GcGuardConfig.minIntervalSeconds(), GcGuardConfig.minReclaimMegabytes());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        bootstrap();
        if (!GcGuardConfig.enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (GcGuardConfig.onlyWithoutScreen() && minecraft.screen != null) {
            return;
        }
        if (++tickCounter < CHECK_INTERVAL_SECONDS * TICKS_PER_SECOND) {
            return;
        }
        tickCounter = 0;

        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        if (max <= 0L) {
            return;
        }
        long used = runtime.totalMemory() - runtime.freeMemory();
        double percent = used * 100.0D / max;

        boolean fastMoving = isFastMoving(minecraft);
        int soft = GcGuardConfig.softPercent();
        int hard = GcGuardConfig.hardPercent();

        if (percent < soft) {
            return;
        }
        boolean hardHit = percent >= hard;
        if (!hardHit && fastMoving) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldown = GcGuardConfig.minIntervalSeconds() * MILLIS_PER_SECOND * backoffMultiplier();
        if (lastGcMillis != 0L && now - lastGcMillis < cooldown) {
            return;
        }
        lastGcMillis = now;

        long beforeMb = used / BYTES_PER_MB;
        long startedAt = System.nanoTime();
        System.gc();
        long spentMs = (System.nanoTime() - startedAt) / 1_000_000L;
        long afterMb = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB;

        long reclaimedMb = beforeMb - afterMb;
        if (reclaimedMb < GcGuardConfig.minReclaimMegabytes()) {
            // 活集已满：这批对象是可达的，GC 收不回来，只会白停一秒多，所以拉长下次间隔。
            if (ineffectiveRuns < MAX_BACKOFF_MULTIPLIER) {
                ineffectiveRuns++;
            }
            LOGGER.warn("[gcguard] 本次仅回收 {}MB（< {}MB 视为无效），间隔退避为 {} 倍（下次最早 {}s 后）",
                    reclaimedMb, GcGuardConfig.minReclaimMegabytes(), backoffMultiplier(),
                    GcGuardConfig.minIntervalSeconds() * backoffMultiplier());
        } else {
            ineffectiveRuns = 0;
        }

        LOGGER.info("[gcguard] 主动回收（{}{}）：堆 {}% ({}MB) → {}MB（回收 {}MB），停顿约 {}ms",
                hardHit ? "硬阈值" : "软阈值",
                fastMoving ? "，当前快速移动" : "",
                Math.round(percent), beforeMb, afterMb, reclaimedMb, spentMs);
    }

    /** 退避倍数：连续 n 次无效回收 → 2^n 倍间隔，上限 MAX_BACKOFF_MULTIPLIER。 */
    private static int backoffMultiplier() {
        int multiplier = 1;
        for (int i = 0; i < ineffectiveRuns && multiplier < MAX_BACKOFF_MULTIPLIER; i++) {
            multiplier *= 2;
        }
        return Math.min(multiplier, MAX_BACKOFF_MULTIPLIER);
    }

    /** 与初版一致：水平速度超阈值、鞘翅飞行、创造飞行、疾跑都算「快速移动」。 */
    private static boolean isFastMoving(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null) {
            return false;
        }
        double dx = player.getX() - player.xOld;
        double dz = player.getZ() - player.zOld;
        double horizontalSpeed = Math.sqrt(dx * dx + dz * dz);
        return horizontalSpeed > GcGuardConfig.maxSpeedPerTick()
                || horizontalSpeed > SPRINT_SPEED_THRESHOLD && player.isSprinting()
                || player.isFallFlying()
                || player.getAbilities().flying;
    }
}
