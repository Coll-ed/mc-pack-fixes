package com.yglcs.lddshine;

import com.yglcs.lddshine.mixin.LambDynLightsAccessor;
import dev.lambdaurora.lambdynlights.engine.source.DynamicLightSource;
import dev.lambdaurora.lambdynlights.engine.source.EntityDynamicLightSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 从 LambDynamicLights 读当前生效的动态光源。
 *
 * <p>LDD 把实现了 {@link EntityDynamicLightSource} 的实体自己放进 {@code dynamicLightSources}，
 * 读写由 {@code lightSourcesLock} 保护 —— 所以这里持读锁快照，避免并发改动。</p>
 */
public final class DynamicLightSamples {

    /** 一个候选动态光源：世界坐标（已插值）+ 亮度 + 发光物品。 */
    public record Sample(double x, double y, double z, int luminance, ItemStack stack) {
    }

    private static Field instanceField;
    private static boolean instanceFieldUnavailable;
    private static boolean accessorWarned;

    private DynamicLightSamples() {
    }

    public static List<Sample> collect(int cap) {
        if (cap <= 0) {
            return List.of();
        }
        Object lambDynLights = instance();
        if (lambDynLights == null) {
            return List.of();
        }
        if (!(lambDynLights instanceof LambDynLightsAccessor accessor)) {
            // 混入没生效（LDD 换了内部结构）——显式报一次，不静默当没有光
            if (!accessorWarned) {
                accessorWarned = true;
                LddShine.LOGGER.error("[lddshine] 无法访问 LDD 的动态光源集合（{} 未生效），桥接停用",
                        LambDynLightsAccessor.class.getSimpleName());
            }
            return List.of();
        }

        ReentrantReadWriteLock lock = accessor.lddshine$lock();
        lock.readLock().lock();
        try {
            Set<DynamicLightSource> sources = accessor.lddshine$sources();
            if (sources == null || sources.isEmpty()) {
                return List.of();
            }
            List<Sample> samples = new ArrayList<>(Math.min(cap, sources.size()));
            for (DynamicLightSource source : sources) {
                if (!(source instanceof EntityDynamicLightSource light)) {
                    continue;
                }
                if (!light.isDynamicLightEnabled()) {
                    continue;
                }
                int luminance = light.getLuminance();
                if (luminance <= 0) {
                    continue;
                }
                if (!(light instanceof Entity entity)) {
                    continue;
                }
                ItemStack stack = heldStack(entity);
                if (stack.isEmpty()) {
                    continue;
                }
                samples.add(new Sample(
                        light.getDynamicLightX(),
                        light.getDynamicLightY(),
                        light.getDynamicLightZ(),
                        luminance,
                        stack));
                if (samples.size() >= cap) {
                    break;
                }
            }
            return samples;
        } finally {
            lock.readLock().unlock();
        }
    }

    /** 手持主手优先、其次副手；掉落物取物品栏里的那一份。 */
    private static ItemStack heldStack(Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            return itemEntity.getItem();
        }
        if (entity instanceof LivingEntity living) {
            ItemStack mainHand = living.getMainHandItem();
            return mainHand.isEmpty() ? living.getOffhandItem() : mainHand;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 取 LDD 单例。刻意用反射而不是直接引用 {@code LambDynLights} 类型：
     * 那个类实现了 Yumi 的初始化接口，直接引用会给我们引入一个我们并不需要的编译期依赖。
     */
    private static Object instance() {
        if (instanceFieldUnavailable) {
            return null;
        }
        if (instanceField == null) {
            try {
                Class<?> main = Class.forName(BridgeTargets.LDD_MAIN);
                instanceField = main.getField(BridgeTargets.LDD_INSTANCE_FIELD);
            } catch (ClassNotFoundException | NoSuchFieldException | RuntimeException e) {
                instanceFieldUnavailable = true;
                LddShine.LOGGER.error("[lddshine] 找不到 LambDynamicLights 单例，桥接停用", e);
                return null;
            }
        }
        try {
            return instanceField.get(null);
        } catch (IllegalAccessException e) {
            instanceFieldUnavailable = true;
            LddShine.LOGGER.error("[lddshine] 读取 LambDynamicLights 单例失败，桥接停用", e);
            return null;
        }
    }
}
