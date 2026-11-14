package com.yglcs.lddshine.mixin;

import com.yglcs.lddshine.BridgeConstants;
import com.yglcs.lddshine.BridgeSettings;
import com.yglcs.lddshine.BridgeTargets;
import com.yglcs.lddshine.DynamicLightSamples;
import com.yglcs.lddshine.ItemLightColors;
import com.yglcs.lddshine.LddShine;
import com.yglcs.lddshine.ShineActiveLightFactory;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 在 Shine 每帧把 ACTIVE_LIGHTS 拷进 UPLOAD_LIGHTS 之后，把 LDD 的动态光追加进去。
 *
 * <p>选这个点是因为它是最窄的接缝：之后 Shine 自己会做
 * 签名计算（决定要不要重传）→ 填 POS_RADIUS/COLOR buffer → Sodium 分区裁剪上传，
 * 追加进去的光会自动享受全部这些逻辑，我们一行都不需要复制。</p>
 */
@Mixin(targets = BridgeTargets.SHINE_COLORED_LIGHT_RENDERER)
public abstract class ShineColoredLightRendererMixin {

    /** 与目标字段同名同描述符（Ljava/util/List;）；实际泛型是包私有的 ActiveLight。 */
    @Shadow
    @Final
    private static List<Object> UPLOAD_LIGHTS;

    @Inject(method = BridgeTargets.SHINE_REFRESH_METHOD, at = @At("RETURN"))
    private static void lddshine$appendDynamicLights(Vec3 camera, CallbackInfo ci) {
        if (!BridgeSettings.enabled() || !ItemLightColors.coloredLightEnabled()) {
            return;
        }
        int room = BridgeConstants.SHINE_SHADER_LIGHT_CAP - UPLOAD_LIGHTS.size();
        if (room <= 0) {
            return;
        }
        int cap = Math.min(BridgeSettings.maxExtraLights(), room);
        if (cap <= 0) {
            return;
        }

        List<DynamicLightSamples.Sample> samples = DynamicLightSamples.collect(cap);
        if (samples.isEmpty()) {
            return;
        }

        float luminanceScale = 1.0f / BridgeConstants.LUMINANCE_MAX;
        int added = 0;
        for (DynamicLightSamples.Sample sample : samples) {
            ItemLightColors.Resolved color = ItemLightColors.resolve(sample.stack());
            if (color == null) {
                continue;
            }
            // 亮度决定强度：LDD 的 luminance 与原版光等级同为 0..15
            float intensity = color.intensity() * Math.min(1.0f, sample.luminance() * luminanceScale);
            if (intensity <= 0.0f) {
                continue;
            }
            Object activeLight = ShineActiveLightFactory.create(
                    sample.x(), sample.y(), sample.z(),
                    color.radius(), color.r(), color.g(), color.b(), intensity);
            if (activeLight == null) {
                break;
            }
            UPLOAD_LIGHTS.add(activeLight);
            added++;
        }
        LddShine.debugBridge(added, UPLOAD_LIGHTS.size());
    }
}
