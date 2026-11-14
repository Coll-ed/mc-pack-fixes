/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.state.BlockState
 */
package me.cortex.voxy.client.core.compat.eclipticseasons;

import me.cortex.voxy.client.core.model.bakery.ReuseVertexConsumer;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.common.world.WorldSection;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public final class SeasonalLod {
    public static volatile View view = null;

    private SeasonalLod() {
    }

    public static void disarm(LinkageError error) {
        if (view == null) {
            return;
        }
        view = null;
        Logger.error("Seasonal LOD disabled: EclipticSeasons symbol missing at mesh time", error);
    }

    public static interface View {
        public long[] substituteSection(WorldEngine var1, WorldSection var2, long[] var3);

        public void substituteLateralSlices(WorldEngine var1, WorldSection var2, long[] var3, int var4);

        public boolean isSeasonalConstantTint(BlockState var1, Object var2);

        public SeasonalBakedModel resolveSeasonalModel(BlockState var1, ResourceLocation var2);

        public void renderSnowOverlay(BlockState var1, RenderType var2, ReuseVertexConsumer var3, ReuseVertexConsumer var4);

        public void clearCaches();
    }

    public record SeasonalBakedModel(BakedModel model, boolean replace) {
    }
}

