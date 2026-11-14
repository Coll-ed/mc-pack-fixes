package com.yglcs.lddshine;

import net.minecraft.util.Mth;

import java.lang.reflect.Constructor;

/**
 * 造 Shine 的 {@code ColoredLightRenderer$ActiveLight}。
 *
 * <p>那是个包私有 record，构造器签名 11 个参数：
 * {@code (int bx, int by, int bz, double x, double y, double z, float radius, float r, float g, float b, float intensity)}，
 * 由 {@code createInfluenceAabb} 自己算 AABB、{@code blockKey()} 自己算 key。
 * 我们不在同一个包、也不是同一份源码，只能反射；实例丢进 Shine 的 UPLOAD_LIGHTS 后，
 * 后面的签名计算 / uniform 填充 / 分区裁剪全部按它自己的逻辑走，不需要我们再插手。</p>
 */
public final class ShineActiveLightFactory {

    private static Constructor<?> constructor;
    private static boolean unavailable;

    private ShineActiveLightFactory() {
    }

    /** 造不出来时返回 null（已经记过日志并把桥接标记为停用）。 */
    public static Object create(double x, double y, double z,
                                float radius, float r, float g, float b, float intensity) {
        Constructor<?> ctor = constructor();
        if (ctor == null) {
            return null;
        }
        int blockX = Mth.floor(x);
        int blockY = Mth.floor(y);
        int blockZ = Mth.floor(z);
        try {
            return ctor.newInstance(blockX, blockY, blockZ, x, y, z,
                    radius, r, g, b, intensity);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            unavailable = true;
            LddShine.LOGGER.error("[lddshine] 构造 Shine 的 ActiveLight 失败，桥接停用", e);
            return null;
        }
    }

    private static Constructor<?> constructor() {
        if (constructor != null || unavailable) {
            return constructor;
        }
        try {
            Class<?> type = Class.forName(BridgeTargets.SHINE_ACTIVE_LIGHT);
            Constructor<?> ctor = type.getDeclaredConstructor(
                    int.class, int.class, int.class,
                    double.class, double.class, double.class,
                    float.class, float.class, float.class, float.class, float.class);
            ctor.setAccessible(true);
            constructor = ctor;
        } catch (ClassNotFoundException | NoSuchMethodException | RuntimeException e) {
            unavailable = true;
            LddShine.LOGGER.error("[lddshine] 找不到 Shine 的 ActiveLight 构造器（版本不匹配？），桥接停用", e);
        }
        return constructor;
    }
}
