/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  net.minecraft.core.Holder
 *  net.minecraft.core.HolderSet
 *  net.minecraft.core.Registry
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.block.Block
 */
package com.teamtea.eclipticseasons.client.util;

import com.mojang.datafixers.util.Pair;
import com.teamtea.eclipticseasons.api.data.client.BiomeColor;
import com.teamtea.eclipticseasons.api.data.client.LeafColor;
import com.teamtea.eclipticseasons.api.data.client.SeasonalBackgroundMusic;
import com.teamtea.eclipticseasons.api.data.client.SeasonalBiomeAmbient;
import com.teamtea.eclipticseasons.api.data.client.model.seasonal.SeasonBlockDefinition;
import com.teamtea.eclipticseasons.api.data.client.ui.UIParser;
import com.teamtea.eclipticseasons.api.data.season.SnowDefinition;
import com.teamtea.eclipticseasons.api.misc.client.IBiomeColorHolder;
import com.teamtea.eclipticseasons.api.misc.util.HolderMappable;
import com.teamtea.eclipticseasons.api.misc.util.Mergable;
import com.teamtea.eclipticseasons.api.util.SimpleUtil;
import com.teamtea.eclipticseasons.client.lod.color.SeasonalBlockColorCache;
import com.teamtea.eclipticseasons.client.reload.ClientJsonCacheListener;
import com.teamtea.eclipticseasons.config.ClientConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public class ClientRef {
    public static final Map<Biome, BiomeColor.Instance> biomeColors = new IdentityHashMap<Biome, BiomeColor.Instance>();
    public static final Map<Block, List<Pair<LeafColor.InstanceHolder, LeafColor.Instance>>> leaveColors = new IdentityHashMap<Block, List<Pair<LeafColor.InstanceHolder, LeafColor.Instance>>>();
    public static final List<SeasonalBiomeAmbient> sounds = new ArrayList<SeasonalBiomeAmbient>();
    public static final Map<Block, List<SeasonBlockDefinition>> seasonDef = new IdentityHashMap<Block, List<SeasonBlockDefinition>>();
    public static final Map<Block, List<SnowDefinition>> snowClientDef = new IdentityHashMap<Block, List<SnowDefinition>>();
    public static final List<UIParser> uiParsers = new ArrayList<UIParser>();
    public static final List<SeasonalBackgroundMusic> musics = new ArrayList<SeasonalBackgroundMusic>();

    public static void updateClientSide(RegistryAccess registryAccess) {
        biomeColors.clear();
        leaveColors.clear();
        sounds.clear();
        seasonDef.clear();
        snowClientDef.clear();
        uiParsers.clear();
        musics.clear();
        ClientRef.buildBiomeColors(registryAccess);
        ClientRef.buildLeafColors(registryAccess);
        ClientRef.buildSeasonalSounds(registryAccess);
        ClientRef.buildSeasonalModels(registryAccess);
        ClientRef.buildOverrideSnowModels(registryAccess);
        ClientRef.buildUIParsers(registryAccess);
        ClientRef.buildSeasonalMusics(registryAccess);
        SeasonalBlockColorCache.clear();
    }

    private static void buildSeasonalMusics(RegistryAccess registryAccess) {
        musics.addAll(ClientJsonCacheListener.backgroundMusicCache.build(SeasonalBackgroundMusic.CODEC, registryAccess).values());
    }

    private static void buildSeasonalSounds(RegistryAccess registryAccess) {
        sounds.addAll(ClientJsonCacheListener.ambientCache.build(SeasonalBiomeAmbient.CODEC, registryAccess).values());
    }

    private static void buildUIParsers(RegistryAccess registryAccess) {
        uiParsers.addAll(ClientJsonCacheListener.uiParserCache.build(UIParser.CODEC, registryAccess).values());
    }

    private static void buildOverrideSnowModels(RegistryAccess registryAccess) {
        ArrayList collect = ClientJsonCacheListener.snowDefOverrideCache.build(SnowDefinition.CODEC, registryAccess).values().stream().map(HolderMappable::asHolderMapping).collect(Collectors.toCollection(ArrayList::new));
        Map biomeListMap = ClientRef.buildFromHolders(collect, ClientRef.getHolders(registryAccess, Registries.BLOCK));
        snowClientDef.putAll(biomeListMap);
    }

    private static void buildSeasonalModels(RegistryAccess registryAccess) {
        ArrayList collect = ClientJsonCacheListener.seasonDefCache.build(SeasonBlockDefinition.CODEC, registryAccess).entrySet().stream().filter(r -> (Boolean)ClientConfig.Renderer.flowerOnGrass.get() != false || !((ResourceLocation)r.getKey()).equals((Object)SeasonBlockDefinition.GRASS_BLOCK)).map(Map.Entry::getValue).map(HolderMappable::asHolderMapping).collect(Collectors.toCollection(ArrayList::new));
        Map biomeListMap = ClientRef.buildFromHolders(collect, ClientRef.getHolders(registryAccess, Registries.BLOCK));
        seasonDef.putAll(biomeListMap);
    }

    private static void buildLeafColors(RegistryAccess registryAccess) {
        ArrayList collect = ClientJsonCacheListener.leafCache.build(LeafColor.CODEC, registryAccess).values().stream().map(HolderMappable::asHolderMapping).collect(Collectors.toCollection(ArrayList::new));
        Map biomeListMap = ClientRef.buildFromHolders(collect, ClientRef.getHolders(registryAccess, Registries.BLOCK));
        biomeListMap.forEach((pairs, instances) -> {
            List instance = ClientRef.mergePairList(instances);
            leaveColors.put((Block)pairs, instance);
        });
    }

    private static void buildBiomeColors(RegistryAccess registryAccess) {
        ArrayList collect = ClientJsonCacheListener.biomeCache.build(BiomeColor.CODEC, registryAccess).values().stream().map(HolderMappable::asHolderMapping).collect(Collectors.toCollection(ArrayList::new));
        Map biomeListMap = ClientRef.buildFromHolders(collect, ClientRef.getHolders(registryAccess, Registries.BIOME));
        biomeListMap.forEach((biome, instances) -> {
            BiomeColor.Instance instance = (BiomeColor.Instance)ClientRef.mergeList(instances);
            biomeColors.put((Biome)biome, instance);
            Biome patt0$temp = biome;
            if (patt0$temp instanceof IBiomeColorHolder) {
                IBiomeColorHolder biomeColorHolder = (IBiomeColorHolder)patt0$temp;
                biomeColorHolder.setBiomeColor(instance);
            }
        });
    }

    public static <T, V> Map<T, List<V>> buildFromHolders(List<Pair<HolderSet<T>, V>> pairs, List<Holder<T>> holders) {
        HashMap resultMap = new HashMap();
        for (Pair<HolderSet<T>, V> pair : pairs) {
            HolderSet holderSet = (HolderSet)pair.getFirst();
            Object value = pair.getSecond();
            for (Holder th : holderSet) {
                Object t = th.value();
                resultMap.putIfAbsent(t, new ArrayList());
                if (!holderSet.contains(th)) continue;
                ((List)resultMap.get(t)).add(value);
            }
        }
        return resultMap;
    }

    public static <E> ArrayList<Holder<E>> getHolders(RegistryAccess registryAccess, ResourceKey<? extends Registry<? extends E>> registryKey) {
        Optional registry = registryAccess.registry(registryKey);
        if (registry.isEmpty()) {
            SimpleUtil.warningForModWrongCalling(registryKey);
            return new ArrayList<Holder<E>>();
        }
        return ((Registry)registry.get()).holders().collect(Collectors.toCollection(ArrayList::new));
    }

    private static <T extends Mergable<T>> T mergeList(List<T> instances) {
        if (instances.isEmpty()) {
            return null;
        }
        if (instances.size() == 1) {
            return (T)((Mergable)instances.get(0));
        }
        return (T)((Mergable)instances.stream().reduce(Mergable::merge).orElse(null));
    }

    private static <S, T extends Mergable<T>> List<Pair<S, T>> mergePairList(List<Pair<S, T>> instances) {
        if (instances == null || instances.size() <= 1) {
            return instances;
        }
        HashMap<Object, Mergable> mergedMap = new HashMap<Object, Mergable>();
        for (Pair<S, T> pair : instances) {
            Object key = pair.getFirst();
            Mergable value = (Mergable)pair.getSecond();
            mergedMap.merge(key, value, Mergable::merge);
        }
        return mergedMap.entrySet().stream().map(entry -> new Pair(entry.getKey(), (Object)((Mergable)entry.getValue()))).collect(Collectors.toList());
    }

    public static void onClientPlayerExit() {
        biomeColors.clear();
        leaveColors.clear();
        sounds.clear();
        seasonDef.clear();
        snowClientDef.clear();
        uiParsers.clear();
        musics.clear();
    }
}

