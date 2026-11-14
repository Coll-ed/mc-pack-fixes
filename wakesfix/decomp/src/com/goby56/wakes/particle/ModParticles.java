/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry
 *  net.fabricmc.fabric.api.particle.v1.FabricParticleTypes
 *  net.minecraft.class_2378
 *  net.minecraft.class_2396
 *  net.minecraft.class_2400
 *  net.minecraft.class_2960
 *  net.minecraft.class_7923
 */
package com.goby56.wakes.particle;

import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.particle.custom.SplashCloudParticle;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.class_2378;
import net.minecraft.class_2396;
import net.minecraft.class_2400;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

public class ModParticles {
    public static WithOwnerParticleType SPLASH_PLANE;
    public static class_2400 SPLASH_CLOUD;

    public static void registerParticles() {
        SPLASH_PLANE = (WithOwnerParticleType)((Object)class_2378.method_10230((class_2378)class_7923.field_41180, (class_2960)class_2960.method_60655((String)"wakes", (String)"splash_plane"), (Object)((Object)new WithOwnerParticleType(true))));
        ParticleFactoryRegistry.getInstance().register((class_2396)SPLASH_PLANE, SplashPlaneParticle.Factory::new);
        class_2378.method_10230((class_2378)class_7923.field_41180, (class_2960)class_2960.method_60655((String)"wakes", (String)"splash_cloud"), (Object)SPLASH_CLOUD);
        ParticleFactoryRegistry.getInstance().register((class_2396)SPLASH_CLOUD, SplashCloudParticle.Factory::new);
    }

    static {
        SPLASH_CLOUD = FabricParticleTypes.simple();
    }
}

