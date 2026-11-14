/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.core.Holder$Reference$Type
 *  net.minecraft.core.HolderOwner
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.biome.Biome$BiomeBuilder
 *  net.minecraft.world.level.biome.BiomeGenerationSettings
 *  net.minecraft.world.level.biome.BiomeSpecialEffects$Builder
 *  net.minecraft.world.level.biome.Biomes
 *  net.minecraft.world.level.biome.MobSpawnSettings$Builder
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.teamtea.eclipticseasons.common.core.map.stub;

import com.teamtea.eclipticseasons.common.core.map.stub.DummyHolderOwner;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlainsStubHolder
extends Holder.Reference<Biome> {
    public static final Holder.Reference<Biome> PLAINS = new PlainsStubHolder(Holder.Reference.Type.STAND_ALONE, new DummyHolderOwner<Biome>(), (ResourceKey<Biome>)Biomes.PLAINS, PlainsStubHolder.createPlainsBiomeBehavior().build());
    public static final Holder.Reference<Biome> VOID = new PlainsStubHolder(Holder.Reference.Type.STAND_ALONE, new DummyHolderOwner<Biome>(), (ResourceKey<Biome>)Biomes.THE_VOID, PlainsStubHolder.createVoidBiomeBehavior().build());
    private static final int DEFAULT_WATER_COLOR = 4159204;

    private PlainsStubHolder(Holder.Reference.Type type, HolderOwner<Biome> owner, @Nullable ResourceKey<Biome> key, @Nullable Biome value) {
        super(type, owner, key, (Object)value);
    }

    @NotNull
    public Stream<TagKey<Biome>> tags() {
        return Stream.empty();
    }

    public boolean is(@NotNull TagKey<Biome> tag) {
        return false;
    }

    private static Biome.BiomeBuilder createPlainsBiomeBehavior() {
        float temperature = 0.5f;
        return new Biome.BiomeBuilder().hasPrecipitation(true).temperature(temperature).downfall(0.5f).specialEffects(new BiomeSpecialEffects.Builder().fogColor(12638463).skyColor(8103167).waterColor(4159204).waterFogColor(329011).build()).mobSpawnSettings(new MobSpawnSettings.Builder().build()).generationSettings(new BiomeGenerationSettings(Map.of(), List.of()));
    }

    private static Biome.BiomeBuilder createVoidBiomeBehavior() {
        return new Biome.BiomeBuilder().hasPrecipitation(false).temperature(0.5f).downfall(0.0f).specialEffects(new BiomeSpecialEffects.Builder().fogColor(12638463).skyColor(8103167).waterColor(4159204).waterFogColor(329011).build()).mobSpawnSettings(new MobSpawnSettings.Builder().build()).generationSettings(new BiomeGenerationSettings(Map.of(), List.of()));
    }
}

