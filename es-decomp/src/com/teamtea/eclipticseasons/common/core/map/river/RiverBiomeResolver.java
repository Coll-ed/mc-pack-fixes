/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.QuartPos
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.biome.Biomes
 *  net.minecraft.world.level.biome.Climate
 *  net.minecraft.world.level.biome.Climate$Sampler
 *  net.minecraft.world.level.biome.Climate$TargetPoint
 *  net.minecraft.world.level.levelgen.RandomState
 *  org.jetbrains.annotations.NotNull
 */
package com.teamtea.eclipticseasons.common.core.map.river;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;
import org.jetbrains.annotations.NotNull;

public class RiverBiomeResolver {
    private static final ResourceKey<Biome>[][] CLIMATE_BIOMES = new ResourceKey[][]{{Biomes.SNOWY_PLAINS, Biomes.SNOWY_PLAINS, Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.TAIGA}, {Biomes.PLAINS, Biomes.PLAINS, Biomes.FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA}, {Biomes.FLOWER_FOREST, Biomes.PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.DARK_FOREST}, {Biomes.SAVANNA, Biomes.SAVANNA, Biomes.FOREST, Biomes.JUNGLE, Biomes.JUNGLE}, {Biomes.DESERT, Biomes.DESERT, Biomes.DESERT, Biomes.DESERT, Biomes.DESERT}};

    public static ResourceKey<Biome> getClimateBiome(Climate.TargetPoint point) {
        float temperature = Climate.unquantizeCoord((long)point.temperature());
        float humidity = Climate.unquantizeCoord((long)point.humidity());
        int tempIndex = RiverBiomeResolver.getTemperatureIndex(temperature);
        int humidityIndex = RiverBiomeResolver.getHumidityIndex(humidity);
        return CLIMATE_BIOMES[tempIndex][humidityIndex];
    }

    private static int getTemperatureIndex(float value) {
        if (value < -0.45f) {
            return 0;
        }
        if (value < -0.15f) {
            return 1;
        }
        if (value < 0.2f) {
            return 2;
        }
        if (value < 0.55f) {
            return 3;
        }
        return 4;
    }

    private static int getHumidityIndex(float value) {
        if (value < -0.35f) {
            return 0;
        }
        if (value < -0.1f) {
            return 1;
        }
        if (value < 0.1f) {
            return 2;
        }
        if (value < 0.3f) {
            return 3;
        }
        return 4;
    }

    public static // Could not load outer class - annotation placement on inner may be incorrect
    @NotNull Climate.TargetPoint getClimateTargetPoint(RandomState randomState, BlockPos.MutableBlockPos blockPos) {
        int qx = QuartPos.fromBlock((int)blockPos.getX());
        int qy = QuartPos.fromBlock((int)blockPos.getY());
        int qz = QuartPos.fromBlock((int)blockPos.getZ());
        Climate.Sampler sampler = randomState.sampler();
        return sampler.sample(qx, qy, qz);
    }
}

