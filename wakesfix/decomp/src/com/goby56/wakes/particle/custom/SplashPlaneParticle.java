/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_2394
 *  net.minecraft.class_2400
 *  net.minecraft.class_243
 *  net.minecraft.class_3532
 *  net.minecraft.class_3999
 *  net.minecraft.class_4002
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_4588
 *  net.minecraft.class_638
 *  net.minecraft.class_703
 *  net.minecraft.class_707
 *  net.minecraft.class_7833
 *  org.jetbrains.annotations.Nullable
 */
package com.goby56.wakes.particle.custom;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.render.SplashPlaneRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_2394;
import net.minecraft.class_2400;
import net.minecraft.class_243;
import net.minecraft.class_3532;
import net.minecraft.class_3999;
import net.minecraft.class_4002;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_638;
import net.minecraft.class_703;
import net.minecraft.class_707;
import net.minecraft.class_7833;
import org.jetbrains.annotations.Nullable;

public class SplashPlaneParticle
extends class_703 {
    class_1297 owner;
    float yaw;
    float prevYaw;
    int ticksSinceSplash = 0;

    protected SplashPlaneParticle(class_638 world, double x, double y, double z) {
        super(world, x, y, z);
    }

    public void method_3085() {
        class_1297 class_12972 = this.owner;
        if (class_12972 instanceof ProducesWake) {
            ProducesWake wakeOwner = (ProducesWake)class_12972;
            wakeOwner.setWakeParticle(null);
        }
        this.owner = null;
        super.method_3085();
    }

    public void method_3070() {
        if (!WakesClient.CONFIG_INSTANCE.renderSplashPlane) {
            this.method_3085();
        }
        this.field_3858 = this.field_3874;
        this.field_3838 = this.field_3854;
        this.field_3856 = this.field_3871;
        this.prevYaw = this.yaw;
        if (this.owner == null) {
            this.method_3085();
            return;
        }
        class_1297 class_12972 = this.owner;
        if (class_12972 instanceof ProducesWake) {
            class_1657 player;
            class_1297 class_12973;
            ProducesWake wakeOwner = (ProducesWake)class_12972;
            if (!wakeOwner.onWaterSurface() || (class_12973 = this.owner) instanceof class_1657 && (player = (class_1657)class_12973).method_7325() || this.owner.method_18798().method_37267() < 0.01) {
                this.method_3085();
            } else {
                this.aliveTick(wakeOwner);
            }
        } else {
            this.method_3085();
        }
    }

    private void aliveTick(ProducesWake wakeProducer) {
        ++this.ticksSinceSplash;
        class_243 vel = this.owner.method_18798();
        this.yaw = 90.0f - (float)(57.29577951308232 * Math.atan2(vel.field_1350, vel.field_1352));
        class_243 normVel = vel.method_1029();
        class_243 planePos = this.owner.method_19538().method_1019(normVel.method_1021((double)(this.owner.method_17681() + WakesClient.CONFIG_INSTANCE.splashPlaneOffset)));
        this.method_3063(planePos.field_1352, wakeProducer.producingHeight(), planePos.field_1350);
        int t = (int)Math.floor((double)WakesClient.CONFIG_INSTANCE.maxSplashPlaneVelocity / vel.method_37267());
        if (this.ticksSinceSplash > t && WakesClient.CONFIG_INSTANCE.spawnParticles) {
            this.ticksSinceSplash = 0;
            class_243 particlePos = planePos.method_1020(new class_243(normVel.field_1352, 0.0, normVel.field_1350).method_1021((double)(this.owner.method_17681() / 2.0f)));
            class_243 particleOffset = new class_243(-normVel.field_1350, 0.0, normVel.field_1352).method_1021((double)(this.owner.method_17681() / 2.0f));
            class_243 pos = particlePos.method_1019(particleOffset);
            this.field_3851.method_8406((class_2394)ModParticles.SPLASH_CLOUD, pos.field_1352, (double)wakeProducer.producingHeight(), pos.field_1350, vel.field_1352, vel.field_1351, vel.field_1350);
            pos = particlePos.method_1019(particleOffset.method_1021(-1.0));
            this.field_3851.method_8406((class_2394)ModParticles.SPLASH_CLOUD, pos.field_1352, (double)wakeProducer.producingHeight(), pos.field_1350, vel.field_1352, vel.field_1351, vel.field_1350);
        }
    }

    public void method_3074(class_4588 vertexConsumer, class_4184 camera, float tickDelta) {
        if (this.field_3843) {
            return;
        }
        class_4587 modelMatrix = this.getMatrixStackFromCamera(camera, tickDelta);
        int light = this.method_3068(tickDelta);
        float yawLerp = class_3532.method_16439((float)tickDelta, (float)this.prevYaw, (float)this.yaw);
        modelMatrix.method_22907(class_7833.field_40716.rotationDegrees(yawLerp + 180.0f));
        SplashPlaneRenderer.render(this.owner, yawLerp, tickDelta, modelMatrix, light);
    }

    private class_4587 getMatrixStackFromCamera(class_4184 camera, float tickDelta) {
        class_243 cameraPos = camera.method_19326();
        float x = (float)(class_3532.method_16436((double)tickDelta, (double)this.field_3858, (double)this.field_3874) - cameraPos.method_10216());
        float y = (float)(class_3532.method_16436((double)tickDelta, (double)this.field_3838, (double)this.field_3854) - cameraPos.method_10214());
        float z = (float)(class_3532.method_16436((double)tickDelta, (double)this.field_3856, (double)this.field_3871) - cameraPos.method_10215());
        class_4587 matrixStack = new class_4587();
        matrixStack.method_46416(x, y, z);
        return matrixStack;
    }

    public class_3999 method_18122() {
        return class_3999.field_17831;
    }

    @Environment(value=EnvType.CLIENT)
    public static class Factory
    implements class_707<class_2400> {
        public Factory(class_4002 spriteSet) {
        }

        @Nullable
        public class_703 createParticle(class_2400 parameters, class_638 world, double x, double y, double z, double velX, double velY, double velZ) {
            SplashPlaneParticle wake = new SplashPlaneParticle(world, x, y, z);
            if (parameters instanceof WithOwnerParticleType) {
                WithOwnerParticleType type = (WithOwnerParticleType)parameters;
                wake.owner = type.owner;
                wake.yaw = wake.prevYaw = type.owner.method_36454();
                ((ProducesWake)wake.owner).setWakeParticle(wake);
            }
            return wake;
        }
    }
}

