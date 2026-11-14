package com.yglcs.lddshine;

/**
 * 被桥接方的内部符号。集中在这里，Shine / LDD 升级后只需改这一处。
 */
public final class BridgeTargets {

    /** LambDynamicLights 主类（静态字段 INSTANCE 保存单例）。 */
    public static final String LDD_MAIN = "dev.lambdaurora.lambdynlights.LambDynLights";
    /** 上面那个单例字段名。 */
    public static final String LDD_INSTANCE_FIELD = "INSTANCE";
    /** LDD 内部保存当前动态光源集合的字段（读写需持 lightSourcesLock）。 */
    public static final String LDD_SOURCES_FIELD = "dynamicLightSources";
    /** LDD 保护光源集合的读写锁字段。 */
    public static final String LDD_LOCK_FIELD = "lightSourcesLock";

    /** Shine 的彩色光渲染器（光源扫描 + uniform 上传）。 */
    public static final String SHINE_COLORED_LIGHT_RENDERER = "com.bloom.client.coloredlight.ColoredLightRenderer";
    /** 上面那个类里表示「一个生效中的彩色光」的包私有 record。 */
    public static final String SHINE_ACTIVE_LIGHT = SHINE_COLORED_LIGHT_RENDERER + "$ActiveLight";
    /** 每帧把 ACTIVE_LIGHTS 拷进 UPLOAD_LIGHTS 的方法名（我们在这里追加）。 */
    public static final String SHINE_REFRESH_METHOD = "refreshUploadLights";

    private BridgeTargets() {
    }
}
