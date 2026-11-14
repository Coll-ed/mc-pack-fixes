/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$class_4534
 *  com.mojang.blaze3d.platform.GlStateManager$class_4535
 *  net.minecraft.class_310
 */
package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.class_310;

public enum BlendingFunction {
    DEFAULT(true),
    SCREEN(GlStateManager.class_4535.ONE, GlStateManager.class_4534.ONE_MINUS_SRC_ALPHA, false);

    public final GlStateManager.class_4535 srcFactor;
    public final GlStateManager.class_4534 dstFactor;
    public final boolean canVaryOpacity;

    private BlendingFunction(boolean variableOpacity) {
        this.srcFactor = null;
        this.dstFactor = null;
        this.canVaryOpacity = variableOpacity;
    }

    private BlendingFunction(GlStateManager.class_4535 srcFactor, GlStateManager.class_4534 dstFactor, boolean variableOpacity) {
        this.srcFactor = srcFactor;
        this.dstFactor = dstFactor;
        this.canVaryOpacity = variableOpacity;
    }

    public static BlendingFunction getBlendFunc() {
        if (class_310.method_29611()) {
            return SCREEN;
        }
        return WakesClient.CONFIG_INSTANCE.blendMode;
    }
}

