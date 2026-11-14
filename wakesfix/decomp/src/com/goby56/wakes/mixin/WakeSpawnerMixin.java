/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1299
 *  net.minecraft.class_1690
 *  net.minecraft.class_1937
 *  net.minecraft.class_243
 *  net.minecraft.class_703
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.goby56.wakes.mixin;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.utils.WakesUtils;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1690;
import net.minecraft.class_1937;
import net.minecraft.class_243;
import net.minecraft.class_703;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_1297.class})
public abstract class WakeSpawnerMixin
implements ProducesWake {
    private class_703 wakeParticle;
    @Shadow
    private class_1937 field_6002;
    @Shadow
    private class_243 field_18276;
    @Shadow
    public float field_5973;
    @Shadow
    public float field_6039;
    @Shadow
    private class_243 field_22467;
    @Shadow
    public double field_6014;
    @Shadow
    public double field_6036;
    @Shadow
    public double field_5969;
    @Shadow
    public float field_6017;
    public boolean onWaterSurface = false;
    public boolean hasWake = false;
    public class_243 prevWakeProdPos = null;
    public double horizontalNumericalVelocity = 0.0;
    public double verticalNumericalVelocity = 0.0;
    public Float producingWaterLevel = null;

    @Shadow
    public abstract boolean method_5869();

    @Shadow
    public abstract String toString();

    @Shadow
    public abstract boolean method_5799();

    @Shadow
    public abstract class_243 method_19538();

    @Shadow
    public abstract class_1299<?> method_5864();

    @Shadow
    public abstract boolean method_31747();

    @Override
    public boolean onWaterSurface() {
        return this.onWaterSurface;
    }

    @Override
    public double getHorizontalVelocity() {
        return this.horizontalNumericalVelocity;
    }

    @Override
    public double getVerticalVelocity() {
        return this.verticalNumericalVelocity;
    }

    @Override
    public class_243 getPrevPos() {
        if (this.prevWakeProdPos == null) {
            return null;
        }
        return new class_243(this.prevWakeProdPos.field_1352, this.prevWakeProdPos.field_1351, this.prevWakeProdPos.field_1350);
    }

    @Override
    public void setPrevPos(class_243 pos) {
        this.prevWakeProdPos = pos;
    }

    @Override
    public float producingHeight() {
        return this.producingWaterLevel.floatValue();
    }

    @Override
    public void setWakeParticle(class_703 particle) {
        this.wakeParticle = particle;
    }

    @Inject(at={@At(value="HEAD")}, method={"tick"})
    private void tick(CallbackInfo info) {
        class_243 vel = this.prevWakeProdPos == null ? class_243.field_1353 : this.field_22467.method_1020(this.prevWakeProdPos);
        this.horizontalNumericalVelocity = vel.method_37267();
        this.verticalNumericalVelocity = vel.field_1351;
        boolean bl = this.onWaterSurface = this.method_5799() && !this.method_5869();
        if (!WakesClient.CONFIG_INSTANCE.spawnWakes) {
            return;
        }
        if (this.onWaterSurface) {
            if (this.producingWaterLevel == null) {
                this.producingWaterLevel = Float.valueOf(WakesUtils.getWaterLevel(this.field_6002, (class_1297)this));
            }
            if (WakesClient.CONFIG_INSTANCE.getSpawningRule((class_1297)((class_1297)this)).spawnsWake) {
                if (this.wakeParticle == null && vel.method_37267() > 0.01 && WakesClient.CONFIG_INSTANCE.renderSplashPlane) {
                    WakesUtils.spawnSplashPlaneParticle(this.field_6002, (class_1297)this);
                }
                WakesUtils.placeWakeTrail((class_1297)this);
                class_1297 class_12972 = (class_1297)this;
                if (class_12972 instanceof class_1690) {
                    class_1690 boat = (class_1690)class_12972;
                    if (WakesClient.CONFIG_INSTANCE.spawnParticles) {
                        WakesUtils.spawnPaddleSplashCloudParticle(this.field_6002, boat);
                    }
                }
            } else {
                this.prevWakeProdPos = null;
            }
        } else {
            this.producingWaterLevel = null;
            this.prevWakeProdPos = null;
        }
    }

    @Inject(at={@At(value="TAIL")}, method={"onSwimmingStart"})
    private void onSwimmingStart(CallbackInfo ci) {
        if (!WakesClient.CONFIG_INSTANCE.spawnWakes) {
            return;
        }
        WakesConfig.WakeSpawningRule spawningRule = WakesClient.CONFIG_INSTANCE.getSpawningRule((class_1297)this);
        if (spawningRule.spawnsSplashes) {
            if (this.producingWaterLevel == null) {
                this.producingWaterLevel = Float.valueOf(WakesUtils.getWaterLevel(this.field_6002, (class_1297)this));
            }
            WakesUtils.placeFallSplash((class_1297)this);
        }
    }
}

