package com.yglcs.lddshine;

/**
 * 具名常量。来源都写在这里，代码里不再出现裸数字。
 */
public final class BridgeConstants {

    /** 原版光等级量程 0..15；LambDynamicLights 的 luminance 与 Shine 的亮度同量程。 */
    public static final int LUMINANCE_MAX = 15;

    /**
     * Shine 单次 uniform 上传的硬上限 64：
     * ColoredLightRenderer#uploadSodiumUniforms 里的循环条件是 {@code bipush 64}，
     * 超出的光源直接被丢弃（javap 实读）。Shine 自己的方块光也占这 64 个名额。
     */
    public static final int SHINE_SHADER_LIGHT_CAP = 64;

    /** 调试日志最小间隔（毫秒），避免每帧刷屏。 */
    public static final long DEBUG_LOG_INTERVAL_MS = 5_000L;

    /** 颜色分量上限（8 bit 通道）。 */
    public static final int CHANNEL_MAX = 255;

    /** RGB 掩码（Shine 的 BlockLight.color 只用低 24 位）。 */
    public static final int RGB_MASK = 0xFF_FFFF;

    private BridgeConstants() {
    }
}
