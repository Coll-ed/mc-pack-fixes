/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.teamtea.eclipticseasons.api.constant.solar.SolarTerm
 *  com.teamtea.eclipticseasons.api.data.client.model.ModelTester
 *  com.teamtea.eclipticseasons.api.data.client.model.seasonal.SeasonBlockDefinition
 *  com.teamtea.eclipticseasons.api.data.client.model.seasonal.SeasonBlockDefinition$FlatSlice
 *  com.teamtea.eclipticseasons.api.data.client.model.seasonal.SeasonBlockDefinition$FlatSliceHolder
 *  com.teamtea.eclipticseasons.api.data.season.SnowDefinition
 *  com.teamtea.eclipticseasons.client.color.season.FoliageColorSource
 *  com.teamtea.eclipticseasons.client.color.season.FoliageColorSource$Impl
 *  com.teamtea.eclipticseasons.client.core.ExtraModelManager
 *  com.teamtea.eclipticseasons.client.util.ClientCon
 *  com.teamtea.eclipticseasons.client.util.ClientRef
 *  com.teamtea.eclipticseasons.common.core.map.MapChecker
 *  com.teamtea.eclipticseasons.common.core.map.stub.PlainsStubHolder
 *  com.teamtea.eclipticseasons.config.ClientConfig$Debug
 *  com.teamtea.eclipticseasons.config.ClientConfig$Renderer
 *  com.teamtea.eclipticseasons.config.CommonConfig
 *  com.teamtea.eclipticseasons.config.CommonConfig$Snow
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.client.resources.model.ModelResourceLocation
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.level.material.Fluid
 *  net.minecraft.world.level.material.Fluids
 */
package me.cortex.voxy.client.core.compat.eclipticseasons;

import com.teamtea.eclipticseasons.api.constant.solar.SolarTerm;
import com.teamtea.eclipticseasons.api.data.client.model.ModelTester;
import com.teamtea.eclipticseasons.api.data.client.model.seasonal.SeasonBlockDefinition;
import com.teamtea.eclipticseasons.api.data.season.SnowDefinition;
import com.teamtea.eclipticseasons.client.color.season.FoliageColorSource;
import com.teamtea.eclipticseasons.client.core.ExtraModelManager;
import com.teamtea.eclipticseasons.client.util.ClientCon;
import com.teamtea.eclipticseasons.client.util.ClientRef;
import com.teamtea.eclipticseasons.common.core.map.MapChecker;
import com.teamtea.eclipticseasons.common.core.map.stub.PlainsStubHolder;
import com.teamtea.eclipticseasons.config.ClientConfig;
import com.teamtea.eclipticseasons.config.CommonConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.compat.eclipticseasons.SeasonalLod;
import me.cortex.voxy.client.core.compat.eclipticseasons.VoxyClientTool;
import me.cortex.voxy.client.core.model.bakery.ReuseVertexConsumer;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.common.world.WorldSection;
import me.cortex.voxy.common.world.other.Mapper;
import me.cortex.voxy.common.world.other.SeasonalIdSpace;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class SeasonalMeshView
implements SeasonalLod.View {
    private static final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(RandomSource::createNewThreadLocalInstance);
    private static final ThreadLocal<BlockPos.MutableBlockPos> POS = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final int F_COMPUTED = Integer.MIN_VALUE;
    private static final int F_SEASONAL = 0x40000000;
    private static final int F_FREEZABLE = 0x20000000;
    private static final int FLAG_MASK = 65535;
    private static final int[] EMPTY_CANDIDATES = new int[0];
    private static final Holder<Biome>[] EMPTY_BIOMES = new Holder[0];
    private static final Map<Mapper, int[]> CANDIDATE_CACHES = new WeakHashMap<Mapper, int[]>();
    private static final Map<Mapper, Holder<Biome>[]> BIOME_CACHES = new WeakHashMap<Mapper, Holder<Biome>[]>();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void clearJudgementCaches() {
        Map<Mapper, Object[]> map = CANDIDATE_CACHES;
        synchronized (map) {
            CANDIDATE_CACHES.clear();
        }
        map = BIOME_CACHES;
        synchronized (map) {
            BIOME_CACHES.clear();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static Ctx makeCtx(Level level, Mapper mapper) {
        Object[] cache;
        Ctx ctx = new Ctx();
        ctx.level = level;
        ctx.mapper = mapper;
        Map<Mapper, Object[]> map = CANDIDATE_CACHES;
        synchronized (map) {
            cache = CANDIDATE_CACHES.get(mapper);
            ctx.candidates = (int[])(cache != null ? cache : EMPTY_CANDIDATES);
        }
        map = BIOME_CACHES;
        synchronized (map) {
            cache = BIOME_CACHES.get(mapper);
            ctx.biomes = (Holder<Biome>[])((int[])(cache != null ? cache : (int[])EMPTY_BIOMES));
        }
        ctx.snowyWinter = CommonConfig.isSnowyWinter();
        ctx.glowGate = (Boolean)CommonConfig.Snow.notSnowyNearGlowingBlock.get();
        ctx.glowLevel = (Integer)CommonConfig.Snow.notSnowyNearGlowingBlockLevel.get();
        ctx.frozenWater = (Boolean)ClientConfig.Debug.frozenWater.get();
        ctx.frozenWaterCheckLight = (Boolean)ClientConfig.Debug.frozenWaterCheckLight.get();
        ctx.snowyTree = (Boolean)CommonConfig.Snow.snowyTree.get();
        ctx.snowUnderFence = (Boolean)ClientConfig.Renderer.snowUnderFence.get();
        return ctx;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static int computeCandidate(Ctx ctx, int realBlockId) {
        List snowDefinitions;
        int snowFlag;
        BlockState state = ctx.mapper.getBlockStateFromBlockId(realBlockId);
        int v = Integer.MIN_VALUE;
        if (ClientRef.seasonDef.containsKey(state.getBlock())) {
            v |= 0x40000000;
        }
        if (state.is(Blocks.WATER) && state.getFluidState().isSourceOfType((Fluid)Fluids.WATER)) {
            v |= 0x20000000;
        }
        if ((snowFlag = Math.max(MapChecker.getDefaultBlockTypeFlag((BlockState)state), 0)) <= 0 && (snowDefinitions = (List)ClientRef.snowClientDef.get(state.getBlock())) != null && !snowDefinitions.isEmpty()) {
            snowFlag = Math.max(((SnowDefinition)snowDefinitions.getFirst()).getInfo().getFlag(), 0);
        }
        v |= snowFlag & 0xFFFF;
        Map<Mapper, int[]> map = CANDIDATE_CACHES;
        synchronized (map) {
            int[] cache = CANDIDATE_CACHES.get(ctx.mapper);
            if (cache == null || realBlockId >= cache.length) {
                int[] grown = new int[Math.max(realBlockId + 1, cache == null ? 1024 : cache.length << 1)];
                if (cache != null) {
                    System.arraycopy(cache, 0, grown, 0, cache.length);
                }
                cache = grown;
            }
            cache[realBlockId] = v;
            CANDIDATE_CACHES.put(ctx.mapper, cache);
            ctx.candidates = cache;
        }
        return v;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static Holder<Biome> biomeFor(Ctx ctx, int biomeId) {
        Holder<Biome> hit;
        if (biomeId < 0) {
            return null;
        }
        Holder<Biome>[] cache = ctx.biomes;
        if (biomeId < cache.length && (hit = cache[biomeId]) != null) {
            return hit;
        }
        Mapper.BiomeEntry entry = ctx.mapper.getBiomeEntry(biomeId);
        if (entry == null) {
            return null;
        }
        ResourceKey key = ResourceKey.create((ResourceKey)Registries.BIOME, (ResourceLocation)ResourceLocation.parse((String)entry.biome));
        Holder resolved = ctx.level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(key).map(h -> h).orElse((Holder)PlainsStubHolder.PLAINS);
        Map<Mapper, Holder<Biome>[]> map = BIOME_CACHES;
        synchronized (map) {
            Holder<Biome>[] current = BIOME_CACHES.get(ctx.mapper);
            if (current == null || current.length <= biomeId) {
                Holder[] grown = new Holder[Math.max(biomeId + 1, current == null ? 512 : current.length << 1)];
                if (current != null) {
                    System.arraycopy(current, 0, grown, 0, current.length);
                }
                current = grown;
            }
            current[biomeId] = resolved;
            BIOME_CACHES.put(ctx.mapper, current);
            ctx.biomes = current;
        }
        return resolved;
    }

    private static boolean seasonInactive() {
        SolarTerm term = ClientCon.nowSolarTerm;
        return term == null || term == SolarTerm.NONE;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public long[] substituteSection(WorldEngine world, WorldSection section, long[] source) {
        if (!VoxyConfig.CONFIG.eclipticSeasonsSnowLod) {
            return source;
        }
        if (SeasonalMeshView.seasonInactive()) {
            return source;
        }
        Level level = ClientCon.getUseLevel();
        if (level == null) {
            return source;
        }
        Ctx ctx = SeasonalMeshView.makeCtx(level, world.getMapper());
        long[] output = source;
        int scale = 1 << section.lvl;
        int centerOffset = section.lvl == 0 ? 0 : scale >> 1;
        int baseX = (section.x << 5 << section.lvl) + centerOffset;
        int baseY = (section.y << 5 << section.lvl) + centerOffset;
        int baseZ = (section.z << 5 << section.lvl) + centerOffset;
        BlockPos.MutableBlockPos pos = POS.get();
        WorldSection aboveSection = null;
        boolean aboveChecked = false;
        try {
            for (int index = 0; index < source.length; ++index) {
                boolean canFreeze;
                long aboveMappingId;
                int cand;
                long mappingId = source[index];
                if (Mapper.isAir(mappingId)) continue;
                int storedBlockId = Mapper.getBlockId(mappingId);
                int originalBlockId = SeasonalIdSpace.decode(ctx.mapper, storedBlockId);
                int n = cand = originalBlockId < ctx.candidates.length ? ctx.candidates[originalBlockId] : 0;
                if (cand == 0) {
                    cand = SeasonalMeshView.computeCandidate(ctx, originalBlockId);
                }
                if ((cand & 0x60000000) == 0 && (cand & 0xFFFF) <= 0) continue;
                boolean aboveKnown = true;
                int localY = index >>> 10;
                if (localY < 31) {
                    aboveMappingId = source[index + 1024];
                } else {
                    if (!aboveChecked) {
                        aboveChecked = true;
                        aboveSection = world.acquireIfExists(section.lvl, section.x, section.y + 1, section.z);
                    }
                    if (aboveSection == null) {
                        if ((cand & 0x40000000) == 0) continue;
                        aboveKnown = false;
                        aboveMappingId = 0L;
                    } else {
                        aboveMappingId = aboveSection.get(index & 0x3FF);
                    }
                }
                boolean canSnow = aboveKnown && SeasonalMeshView.canSnow(ctx, aboveMappingId);
                boolean bl = canFreeze = aboveKnown && SeasonalMeshView.canFreeze(ctx, aboveMappingId);
                if (!canSnow && !canFreeze && (cand & 0x40000000) == 0) continue;
                pos.set(baseX + (index & 0x1F) * scale, baseY + localY * scale, baseZ + (index >> 5 & 0x1F) * scale);
                int renderBlockId = SeasonalMeshView.judge(ctx, mappingId, aboveMappingId, aboveKnown, originalBlockId, cand, canSnow, canFreeze, (BlockPos)pos);
                if (renderBlockId == storedBlockId) continue;
                if (output == source) {
                    output = Arrays.copyOf(source, source.length);
                }
                output[index] = Mapper.withBlockBiome(mappingId, renderBlockId, Mapper.getBiomeId(mappingId));
            }
            long[] lArray = output;
            return lArray;
        }
        finally {
            if (aboveSection != null) {
                aboveSection.release(WorldSection.RELEASE_HINT_POSSIBLE_REUSE);
            }
        }
    }

    @Override
    public void substituteLateralSlices(WorldEngine world, WorldSection section, long[] neighborFaces, int neighborMsk) {
        if (!VoxyConfig.CONFIG.eclipticSeasonsSnowLod) {
            return;
        }
        if (SeasonalMeshView.seasonInactive()) {
            return;
        }
        Level level = ClientCon.getUseLevel();
        if (level == null) {
            return;
        }
        Ctx ctx = SeasonalMeshView.makeCtx(level, world.getMapper());
        int scale = 1 << section.lvl;
        int centerOffset = section.lvl == 0 ? 0 : scale >> 1;
        int baseX = (section.x << 5 << section.lvl) + centerOffset;
        int baseY = (section.y << 5 << section.lvl) + centerOffset;
        int baseZ = (section.z << 5 << section.lvl) + centerOffset;
        this.substituteSlice(ctx, neighborFaces, neighborMsk, 1, 0, baseX - scale, baseY, baseZ, true, scale);
        this.substituteSlice(ctx, neighborFaces, neighborMsk, 2, 1, baseX + 32 * scale, baseY, baseZ, true, scale);
        this.substituteSlice(ctx, neighborFaces, neighborMsk, 16, 4, baseZ - scale, baseY, baseX, false, scale);
        this.substituteSlice(ctx, neighborFaces, neighborMsk, 32, 5, baseZ + 32 * scale, baseY, baseX, false, scale);
    }

    private void substituteSlice(Ctx ctx, long[] faces, int msk, int bit, int slice, int fixedCoord, int baseY, int baseH, boolean fixedIsX, int scale) {
        if ((msk & bit) == 0) {
            return;
        }
        int base = slice * 32 * 32;
        BlockPos.MutableBlockPos pos = POS.get();
        for (int i = 0; i < 992; ++i) {
            int cand;
            long mappingId = faces[base + i];
            if (mappingId == 0L || Mapper.isAir(mappingId)) continue;
            int storedBlockId = Mapper.getBlockId(mappingId);
            int originalBlockId = SeasonalIdSpace.decode(ctx.mapper, storedBlockId);
            int n = cand = originalBlockId < ctx.candidates.length ? ctx.candidates[originalBlockId] : 0;
            if (cand == 0) {
                cand = SeasonalMeshView.computeCandidate(ctx, originalBlockId);
            }
            if ((cand & 0x60000000) == 0 && (cand & 0xFFFF) <= 0) continue;
            long aboveMappingId = faces[base + i + 32];
            boolean canSnow = SeasonalMeshView.canSnow(ctx, aboveMappingId);
            boolean canFreeze = SeasonalMeshView.canFreeze(ctx, aboveMappingId);
            if (!canSnow && !canFreeze && (cand & 0x40000000) == 0) continue;
            int h = i & 0x1F;
            int y = i >> 5;
            if (fixedIsX) {
                pos.set(fixedCoord, baseY + y * scale, baseH + h * scale);
            } else {
                pos.set(baseH + h * scale, baseY + y * scale, fixedCoord);
            }
            int renderBlockId = SeasonalMeshView.judge(ctx, mappingId, aboveMappingId, true, originalBlockId, cand, canSnow, canFreeze, (BlockPos)pos);
            if (renderBlockId == storedBlockId) continue;
            faces[base + i] = Mapper.withBlockBiome(mappingId, renderBlockId, Mapper.getBiomeId(mappingId));
        }
    }

    private static boolean canSnow(Ctx ctx, long aboveMappingId) {
        if (!ctx.snowyWinter) {
            return false;
        }
        int light = Mapper.getLightId(aboveMappingId);
        return (light & 0xF) > 9 && (!ctx.glowGate || (light >> 4 & 0xF) < ctx.glowLevel);
    }

    private static boolean canFreeze(Ctx ctx, long aboveMappingId) {
        if (!ctx.snowyWinter || !ctx.frozenWater) {
            return false;
        }
        int light = Mapper.getLightId(aboveMappingId);
        return (light & 0xF) > 9 && (!ctx.frozenWaterCheckLight || !ctx.glowGate || (light >> 4 & 0xF) < ctx.glowLevel);
    }

    private static int judge(Ctx ctx, long mappingId, long aboveMappingId, boolean aboveKnown, int originalBlockId, int cand, boolean canSnow, boolean canFreeze, BlockPos pos) {
        int model;
        boolean snowy;
        boolean snowRenderable;
        int flag;
        boolean seasonalModelRelated = (cand & 0x40000000) != 0;
        boolean freezable = canFreeze && (cand & 0x20000000) != 0;
        int n = flag = canSnow ? cand & 0xFFFF : 0;
        if (!seasonalModelRelated && !freezable && flag <= 0) {
            return originalBlockId;
        }
        Mapper mapper = ctx.mapper;
        BlockState state = mapper.getBlockStateFromBlockId(originalBlockId);
        BlockState above = mapper.getBlockStateFromBlockId(Mapper.getBlockId(aboveMappingId));
        if (state.is(Blocks.SNOW_BLOCK)) {
            flag = 0;
        }
        boolean bl = snowRenderable = flag > 0 && SeasonalMeshView.canRenderSnow(ctx, state, above, flag);
        if (!(seasonalModelRelated || freezable || snowRenderable)) {
            return originalBlockId;
        }
        Holder<Biome> biome = SeasonalMeshView.biomeFor(ctx, Mapper.getBiomeId(mappingId));
        if (biome == null) {
            return originalBlockId;
        }
        long seed = state.getSeed(pos);
        if (freezable && above.isAir() && MapChecker.shouldSnowAtBiome((Level)ctx.level, (Biome)((Biome)biome.value()), (BlockState)state, (RandomSource)RANDOM.get(), (long)seed, (BlockPos)pos)) {
            return 1048575;
        }
        boolean bl2 = snowy = snowRenderable && MapChecker.shouldSnowAtBiome((Level)ctx.level, (Biome)((Biome)biome.value()), (BlockState)state, (RandomSource)RANDOM.get(), (long)seed, (BlockPos)pos);
        int n2 = seasonalModelRelated ? SeasonalMeshView.findSeasonalModel(mapper, originalBlockId, biome, state, aboveKnown && above.isAir(), pos, seed, snowy) : (model = originalBlockId);
        return model != originalBlockId ? model : (snowy ? 1048575 - originalBlockId : originalBlockId);
    }

    private static int findSeasonalModel(Mapper mapper, int originalBlockId, Holder<Biome> biome, BlockState state, boolean airAbove, BlockPos pos, long seed, boolean snowy) {
        List defs = (List)ClientRef.seasonDef.get(state.getBlock());
        if (defs == null) {
            return originalBlockId;
        }
        for (int i = 0; i < defs.size(); ++i) {
            SeasonBlockDefinition def = (SeasonBlockDefinition)defs.get(i);
            List holders = (List)def.getFlatSliceEnumMap().get((Enum)ClientCon.nowSolarTerm);
            if (holders == null || holders.isEmpty() || !def.getBiomes().contains(biome)) continue;
            for (int j = 0; j < holders.size(); ++j) {
                ResourceLocation modelId;
                SeasonBlockDefinition.FlatSlice flatSlice = ((SeasonBlockDefinition.FlatSliceHolder)holders.get(j)).flatSlice();
                if (flatSlice.emptyAbove() && !airAbove) continue;
                ResourceLocation resourceLocation = flatSlice.transitionModels() == null ? flatSlice.mid() : (modelId = Mth.abs((int)((int)(seed + (long)pos.getX()))) % 100 > ClientCon.progress ? (ResourceLocation)flatSlice.transitionModels().getFirst() : (ResourceLocation)flatSlice.transitionModels().getSecond());
                if (modelId == null) continue;
                return SeasonalIdSpace.getOrCreate(mapper, originalBlockId, modelId, snowy);
            }
        }
        return originalBlockId;
    }

    private static boolean canRenderSnow(Ctx ctx, BlockState state, BlockState above, int flag) {
        if (MapChecker.leaveLike((int)flag)) {
            boolean specialLeaves = above.is(state.getBlock()) && (Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque().test(above) || MapChecker.extraSnowPassable((BlockState)above));
            return !specialLeaves || ctx.snowyTree;
        }
        if (MapChecker.extraSnowPassable((BlockState)state)) {
            return !MapChecker.extraSnowPassable((BlockState)above);
        }
        return ctx.snowUnderFence || !MapChecker.solidTest((BlockState)above);
    }

    @Override
    public boolean isSeasonalConstantTint(BlockState state, Object colourProvider) {
        return colourProvider instanceof FoliageColorSource || colourProvider instanceof FoliageColorSource.Impl || state.is(Blocks.BIRCH_LEAVES) || state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.MANGROVE_LEAVES);
    }

    @Override
    public SeasonalLod.SeasonalBakedModel resolveSeasonalModel(BlockState state, ResourceLocation modelId) {
        ModelTester tester = ExtraModelManager.getSeasonalModel((BlockState)state, (ResourceLocation)modelId);
        if (tester == null) {
            return null;
        }
        BakedModel model = ExtraModelManager.getExtraModel((ModelResourceLocation)tester.modelResourceLocation());
        return model == null ? null : new SeasonalLod.SeasonalBakedModel(model, tester.replace());
    }

    @Override
    public void renderSnowOverlay(BlockState state, RenderType layer, ReuseVertexConsumer translucentVC, ReuseVertexConsumer opaqueVC) {
        VoxyClientTool.renderToStream(state, layer, translucentVC, opaqueVC);
    }

    @Override
    public void clearCaches() {
        SeasonalMeshView.clearJudgementCaches();
    }

    private static final class Ctx {
        Level level;
        Mapper mapper;
        int[] candidates;
        Holder<Biome>[] biomes;
        boolean snowyWinter;
        boolean glowGate;
        int glowLevel;
        boolean frozenWater;
        boolean frozenWaterCheckLight;
        boolean snowyTree;
        boolean snowUnderFence;

        private Ctx() {
        }
    }
}

