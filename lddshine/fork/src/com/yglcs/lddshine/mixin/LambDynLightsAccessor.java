package com.yglcs.lddshine.mixin;

import com.yglcs.lddshine.BridgeTargets;
import dev.lambdaurora.lambdynlights.engine.source.DynamicLightSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 打开 LambDynamicLights 的私有状态：当前动态光源集合 + 保护它的读写锁。
 */
@Mixin(targets = BridgeTargets.LDD_MAIN)
public interface LambDynLightsAccessor {

    @Accessor(BridgeTargets.LDD_SOURCES_FIELD)
    Set<DynamicLightSource> lddshine$sources();

    @Accessor(BridgeTargets.LDD_LOCK_FIELD)
    ReentrantReadWriteLock lddshine$lock();
}
