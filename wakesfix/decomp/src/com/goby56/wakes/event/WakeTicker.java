/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents$EndWorldTick
 *  net.minecraft.class_638
 */
package com.goby56.wakes.event;

import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.utils.WakeHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.class_638;

public class WakeTicker
implements ClientTickEvents.EndWorldTick {
    public void onEndTick(class_638 world) {
        WakeHandler.getInstance().tick();
        SplashPlaneRenderer.tick();
    }
}

