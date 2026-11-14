/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
 *  net.fabricmc.loader.api.FabricLoader
 *  net.fabricmc.loader.api.metadata.ModMetadata
 *  net.irisshaders.iris.api.v0.IrisApi
 *  net.minecraft.class_5601
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.goby56.wakes;

import com.goby56.wakes.command.DebugCommand;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.event.WakeTicker;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.render.WakeTextureRenderer;
import com.goby56.wakes.render.debug.WakeDebugRenderer;
import com.goby56.wakes.render.model.WakeModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.class_5601;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakesClient
implements ClientModInitializer {
    public static final String MOD_ID = "wakes";
    public static ModMetadata METADATA;
    public static final String CONFIG_PATH;
    public static final Logger LOGGER;
    public static WakesConfig CONFIG_INSTANCE;

    public void onInitializeClient() {
        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(container -> {
            METADATA = container.getMetadata();
        });
        CONFIG_INSTANCE = WakesConfig.loadConfig();
        EntityModelLayerRegistry.registerModelLayer((class_5601)WakeModel.MODEL_LAYER, WakeModel::getTexturedModelData);
        ModParticles.registerParticles();
        ClientTickEvents.END_WORLD_TICK.register((Object)new WakeTicker());
        WorldRenderEvents.AFTER_TRANSLUCENT.register((Object)new WakeTextureRenderer());
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register((Object)new WakeDebugRenderer());
        ClientLifecycleEvents.CLIENT_STARTED.register((Object)new SplashPlaneRenderer());
        ClientCommandRegistrationCallback.EVENT.register(DebugCommand::register);
    }

    public static boolean isYACLLoaded() {
        return FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3");
    }

    public static boolean areShadersEnabled() {
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            return IrisApi.getInstance().getConfig().areShadersEnabled();
        }
        return false;
    }

    static {
        CONFIG_PATH = String.format("%s/%s.json", FabricLoader.getInstance().getConfigDir().toString(), MOD_ID);
        LOGGER = LoggerFactory.getLogger((String)MOD_ID);
    }
}

