/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2246
 *  net.minecraft.class_2400
 *  net.minecraft.class_243
 *  net.minecraft.class_2680
 *  net.minecraft.class_3999
 *  net.minecraft.class_4002
 *  net.minecraft.class_4003
 *  net.minecraft.class_638
 *  net.minecraft.class_703
 *  net.minecraft.class_707
 *  org.jetbrains.annotations.Nullable
 */
package com.goby56.wakes.particle.custom;

import com.goby56.wakes.utils.WakesUtils;
import net.minecraft.class_2246;
import net.minecraft.class_2400;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_3999;
import net.minecraft.class_4002;
import net.minecraft.class_4003;
import net.minecraft.class_638;
import net.minecraft.class_703;
import net.minecraft.class_707;
import org.jetbrains.annotations.Nullable;

public class SplashCloudParticle
extends class_4003 {
    public SplashCloudParticle(class_638 world, double x, double y, double z, class_4002 sprites, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.field_3852 += (double)0.001f * (world.field_9229.method_43059() - 0.5);
        this.field_3869 += (double)0.01f * Math.abs(world.field_9229.method_43059() - 0.5);
        this.field_3850 += (double)0.001f * (world.field_9229.method_43059() - 0.5);
        this.field_3858 = x;
        this.field_3838 = y;
        this.field_3856 = z;
        this.field_3847 = 15;
        this.field_17867 *= 2.0f;
        this.method_18141(sprites.method_18139(world.field_9229));
    }

    public void method_3070() {
        this.field_3858 = this.field_3874;
        this.field_3838 = this.field_3854;
        this.field_3856 = this.field_3871;
        if (this.field_3866++ >= this.field_3847) {
            this.method_3085();
            return;
        }
        this.field_3841 = 1.0f - (float)this.field_3866 / (float)this.field_3847;
        class_243 pos = new class_243(this.field_3874, this.field_3854, this.field_3871);
        class_2680 currBlock = this.field_3851.method_8320(WakesUtils.vecToBlockPos(pos));
        if (currBlock.method_26215()) {
            this.field_3869 -= (double)0.02f;
        } else if (currBlock.method_27852(class_2246.field_10382)) {
            this.field_3869 += (double)0.01f;
        }
        this.field_3852 *= (double)0.99f;
        this.field_3869 *= 0.5;
        this.field_3850 *= (double)0.99f;
        this.method_3069(this.field_3852, this.field_3869, this.field_3850);
    }

    public class_3999 method_18122() {
        return class_3999.field_17829;
    }

    public static class Factory
    implements class_707<class_2400> {
        private final class_4002 sprites;

        public Factory(class_4002 spriteSet) {
            this.sprites = spriteSet;
        }

        @Nullable
        public class_703 createParticle(class_2400 parameters, class_638 world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new SplashCloudParticle(world, x, y, z, this.sprites, velocityX, velocityY, velocityZ);
        }
    }
}

