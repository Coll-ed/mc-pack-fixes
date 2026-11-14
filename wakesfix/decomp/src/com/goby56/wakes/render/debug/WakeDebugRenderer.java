/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents$DebugRender
 *  net.minecraft.class_238
 *  net.minecraft.class_243
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_863
 */
package com.goby56.wakes.render.debug;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import java.util.ArrayList;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_863;

public class WakeDebugRenderer
implements WorldRenderEvents.DebugRender {
    public void beforeDebugRender(WorldRenderContext context) {
        if (!WakesClient.CONFIG_INSTANCE.drawDebugBoxes) {
            return;
        }
        ArrayList<WakeNode> nodes = WakeHandler.getInstance().getVisible(context.frustum());
        for (WakeNode node : nodes) {
            class_243 pos = node.getPos().method_1019(context.camera().method_19326().method_22882());
            class_238 box = new class_238(pos.field_1352, pos.field_1351 - 0.1, pos.field_1350, pos.field_1352 + 1.0, pos.field_1351 - 0.2, pos.field_1350 + 1.0);
            class_863.method_23102((class_4587)context.matrixStack(), (class_4597)context.consumers(), (class_238)box, (float)1.0f, (float)0.0f, (float)1.0f, (float)0.4f);
        }
    }
}

