package com.yglcs.wakesfix;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

/**
 * 校正 Wakes 的尾迹水位。
 *
 * <p>Wakes 把「产生尾迹的水面高度」存在实体上的 {@code producingWaterLevel} 字段里，
 * 只有在 {@code onWaterSurface} 为真时才刷新。船离开水面（落差、跳水）后该值失效，
 * 尾迹粒子仍按它定位 → 波浪跟着船往下掉。</p>
 *
 * <p>这里用弱引用表记住每个实体最后一次在水面时的水位，离开水面期间把字段写回该值；
 * 重新接触水面后自动更新。字段通过反射访问，避免编译期依赖 Wakes。</p>
 */
final class WakeHeightCorrector {

    /** Wakes 的 duck 接口：实现了它的实体带尾迹数据。 */
    private static final String DUCK_INTERFACE = "com.goby56.wakes.duck.ProducesWake";
    /** 是否贴在水面上。 */
    private static final String FIELD_ON_SURFACE = "onWaterSurface";
    /** 产生尾迹的水面高度。 */
    private static final String FIELD_WATER_LEVEL = "producingWaterLevel";

    private static Class<?> duckInterface;
    private static Field onSurfaceField;
    private static Field waterLevelField;
    private static boolean unavailable;

    /** 每个实体最后一次「在水面上」时的水位（弱引用，实体卸载自动回收）。 */
    private static final Map<Entity, Float> LAST_SURFACE_LEVEL = new WeakHashMap<>();

    private WakeHeightCorrector() {
    }

    static void tick() {
        if (unavailable) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            LAST_SURFACE_LEVEL.clear();
            return;
        }
        if (!resolve()) {
            return;
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (!duckInterface.isInstance(entity)) {
                continue;
            }
            applyTo(entity);
        }
    }

    private static void applyTo(Entity entity) {
        try {
            boolean onSurface = onSurfaceField.getBoolean(entity);
            if (onSurface) {
                Object value = waterLevelField.get(entity);
                if (value instanceof Float height) {
                    LAST_SURFACE_LEVEL.put(entity, height);
                }
                return;
            }
            Float remembered = LAST_SURFACE_LEVEL.get(entity);
            if (remembered != null) {
                waterLevelField.set(entity, remembered);
            }
        } catch (IllegalAccessException e) {
            // 字段不可写：直接停用，避免每 tick 抛异常刷日志
            unavailable = true;
            WakesFix.LOGGER.warn("[wakesfix] 无法访问 Wakes 字段，已停用：{}", e.toString());
        }
    }

    /** 延迟解析 Wakes 的类与字段；Wakes 未安装时一次性停用。 */
    private static boolean resolve() {
        if (duckInterface != null) {
            return true;
        }
        try {
            duckInterface = Class.forName(DUCK_INTERFACE);
            onSurfaceField = duckInterface.getField(FIELD_ON_SURFACE);
            waterLevelField = duckInterface.getField(FIELD_WATER_LEVEL);
            return true;
        } catch (ReflectiveOperationException e) {
            unavailable = true;
            WakesFix.LOGGER.info("[wakesfix] 未检测到 Wakes（{}），本次会话不启用", e.toString());
            return false;
        }
    }
}
