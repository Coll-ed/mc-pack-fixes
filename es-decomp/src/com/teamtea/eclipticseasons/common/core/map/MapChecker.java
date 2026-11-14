/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.core.QuartPos
 *  net.minecraft.core.Registry
 *  net.minecraft.core.SectionPos
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ChunkHolder
 *  net.minecraft.server.level.ChunkResult
 *  net.minecraft.server.level.ServerChunkCache
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.EmptyBlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.biome.BiomeSource
 *  net.minecraft.world.level.biome.Biomes
 *  net.minecraft.world.level.biome.Climate$ParameterList
 *  net.minecraft.world.level.biome.Climate$Sampler
 *  net.minecraft.world.level.biome.Climate$TargetPoint
 *  net.minecraft.world.level.biome.MultiNoiseBiomeSource
 *  net.minecraft.world.level.block.AnvilBlock
 *  net.minecraft.world.level.block.AzaleaBlock
 *  net.minecraft.world.level.block.BambooStalkBlock
 *  net.minecraft.world.level.block.BasePressurePlateBlock
 *  net.minecraft.world.level.block.BellBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.CampfireBlock
 *  net.minecraft.world.level.block.ComposterBlock
 *  net.minecraft.world.level.block.DirtPathBlock
 *  net.minecraft.world.level.block.DoorBlock
 *  net.minecraft.world.level.block.DoublePlantBlock
 *  net.minecraft.world.level.block.FarmBlock
 *  net.minecraft.world.level.block.FenceBlock
 *  net.minecraft.world.level.block.FenceGateBlock
 *  net.minecraft.world.level.block.IronBarsBlock
 *  net.minecraft.world.level.block.LeavesBlock
 *  net.minecraft.world.level.block.LecternBlock
 *  net.minecraft.world.level.block.LightBlock
 *  net.minecraft.world.level.block.LightningRodBlock
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.StairBlock
 *  net.minecraft.world.level.block.TrapDoorBlock
 *  net.minecraft.world.level.block.VineBlock
 *  net.minecraft.world.level.block.WallBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.DoubleBlockHalf
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.block.state.properties.SlabType
 *  net.minecraft.world.level.chunk.ChunkAccess
 *  net.minecraft.world.level.chunk.ChunkSource
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.chunk.status.ChunkStatus
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  org.jetbrains.annotations.NotNull
 */
package com.teamtea.eclipticseasons.common.core.map;

import com.mojang.datafixers.util.Pair;
import com.teamtea.eclipticseasons.EclipticSeasons;
import com.teamtea.eclipticseasons.api.constant.tag.EclipticBlockTags;
import com.teamtea.eclipticseasons.api.data.season.SnowDefinition;
import com.teamtea.eclipticseasons.api.misc.IBiomeTagHolder;
import com.teamtea.eclipticseasons.api.misc.IBlockStateFlagger;
import com.teamtea.eclipticseasons.api.misc.IChunkBiomeHolder;
import com.teamtea.eclipticseasons.api.util.EclipticUtil;
import com.teamtea.eclipticseasons.common.core.SolarHolders;
import com.teamtea.eclipticseasons.common.core.biome.BiomeClimateManager;
import com.teamtea.eclipticseasons.common.core.biome.WeatherManager;
import com.teamtea.eclipticseasons.common.core.map.BiomeHolder;
import com.teamtea.eclipticseasons.common.core.map.ChunkInfoMap;
import com.teamtea.eclipticseasons.common.core.map.NoneSnowArea;
import com.teamtea.eclipticseasons.common.core.map.SnowyRemover;
import com.teamtea.eclipticseasons.common.core.map.river.RiverBiomeResolver;
import com.teamtea.eclipticseasons.common.core.map.stub.PlainsStubHolder;
import com.teamtea.eclipticseasons.common.core.snow.SnowChecker;
import com.teamtea.eclipticseasons.common.core.snow.SnowyMapChecker;
import com.teamtea.eclipticseasons.common.core.solar.SolarDataManager;
import com.teamtea.eclipticseasons.common.misc.SimplePair;
import com.teamtea.eclipticseasons.common.network.SimpleNetworkHandler;
import com.teamtea.eclipticseasons.common.network.message.ChunkBiomeUpdateMessage;
import com.teamtea.eclipticseasons.common.network.message.ChunkUpdateMessage;
import com.teamtea.eclipticseasons.common.registry.AttachmentRegistry;
import com.teamtea.eclipticseasons.config.CommonConfig;
import java.lang.invoke.LambdaMetafactory;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.DirtPathBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;

public class MapChecker {
    public static final int ChunkSize = 512;
    public static final int ChunkSizeLoc = 511;
    public static final int ChunkSizeAxis = 9;
    public static final List<Level> validDimension = new ArrayList<Level>();
    public static final Map<Level, List<ChunkInfoMap>> REGION_LIST_COLLECTOR = new IdentityHashMap<Level, List<ChunkInfoMap>>();
    public static List<ChunkInfoMap> CLIENT_REGION_LIST = new ArrayList<ChunkInfoMap>();
    public static final int FLAG_IGNORE = -1;
    public static final int FLAG_NONE = 0;
    public static final int FLAG_BLOCK = 1;
    public static final int FLAG_SLAB = 2;
    public static final int FLAG_STAIRS = 3;
    public static final int FLAG_STAIRS_TOP = 301;
    public static final int FLAG_LEAVES = 4;
    public static final int FLAG_GRASS = 5;
    public static final int FLAG_GRASS_LARGE = 501;
    public static final int FLAG_FARMLAND = 6;
    public static final int FLAG_VINE = 7;
    public static final int FLAG_CUSTOM = 999;
    public static final int FLAG_CUSTOM_AO = 998;
    public static final int FLAG_CUSTOM_JSON = 1000;
    public static final int FLAG_CUSTOM_JSON_PLANTS = 1001;
    public static final int FLAG_CUSTOM_JSON_WITH_TOP = 1100;
    public static final int FLAG_CUSTOM_JSON_WITH_TOP_LEAVES = 1101;
    public static final int FLAG_CUSTOM_JSON_VINE_LIKE = 1200;
    public static final SimplePair<Direction, Direction>[] SMALL_OFFSET_DIRECTIONS = new SimplePair[]{SimplePair.of(Direction.NORTH, null), SimplePair.of(Direction.NORTH, Direction.EAST), SimplePair.of(Direction.EAST, null), SimplePair.of(Direction.EAST, Direction.SOUTH), SimplePair.of(Direction.SOUTH, null), SimplePair.of(Direction.SOUTH, Direction.WEST), SimplePair.of(Direction.WEST, null), SimplePair.of(Direction.WEST, Direction.NORTH)};
    public static final Map<Level, Climate.ParameterList<Holder<Biome>>> LEVEL_PARAMETER_LIST_MAP = new IdentityHashMap<Level, Climate.ParameterList<Holder<Biome>>>();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void unloadLevel(Level level) {
        List<ChunkInfoMap> orDefault;
        List<ChunkInfoMap> list = orDefault = MapChecker.getMapsListOrCreate(level);
        synchronized (list) {
            orDefault.clear();
        }
        REGION_LIST_COLLECTOR.remove(level);
        validDimension.removeIf(level1 -> level1 == level);
        LEVEL_PARAMETER_LIST_MAP.remove(level);
    }

    public static boolean unloadChunk(Level level, ChunkPos chunkPos) {
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void tickLevel(Level level) {
        List<ChunkInfoMap> mapsList = MapChecker.getMapsListOrCreate(level);
        if (level.isClientSide()) {
            CLIENT_REGION_LIST = mapsList;
        }
        if (((Boolean)CommonConfig.Debug.disableChunkCacheCleaner.get()).booleanValue()) {
            return;
        }
        List chunkInfoMaps = null;
        if (mapsList != null && level.getRandom().nextInt(100) == 0) {
            for (int zz = 0; zz < mapsList.size(); ++zz) {
                boolean shouldRemove = true;
                ChunkInfoMap map = mapsList.get(zz);
                if (map != null) {
                    int x0 = MapChecker.regionCoordToChunkStart(map.getX());
                    int z0 = MapChecker.regionCoordToChunkStart(map.getZ());
                    int mapChunkSize = MapChecker.mapChunkSize();
                    block4: for (int i = 0; i < mapChunkSize; ++i) {
                        for (int j = 0; j < mapChunkSize; ++j) {
                            if (level instanceof ServerLevel) {
                                ServerLevel serverLevel = (ServerLevel)level;
                                if (!serverLevel.getChunkSource().hasChunk(i + x0, j + z0)) continue;
                                shouldRemove = false;
                                break block4;
                            }
                            if (!MapChecker.isLoaded(level, i + x0, j + z0)) continue;
                            shouldRemove = false;
                            break block4;
                        }
                    }
                }
                if (!shouldRemove) continue;
                chunkInfoMaps = chunkInfoMaps == null ? new ArrayList() : chunkInfoMaps;
                chunkInfoMaps.add(map);
            }
            if (chunkInfoMaps != null) {
                for (ChunkInfoMap chunkInfoMap : chunkInfoMaps) {
                    EclipticSeasons.extraLogger(true, String.format("Remove the empty Height Map [%s, %s]", chunkInfoMap.getX(), chunkInfoMap.getZ()));
                }
                List<ChunkInfoMap> list = mapsList;
                synchronized (list) {
                    mapsList.removeAll(chunkInfoMaps);
                }
            }
        }
    }

    public static int blockToRegionCoord(int i) {
        return i >> 9;
    }

    public static int chunkToRegionCoord(int chunkI) {
        return chunkI >> 5;
    }

    public static int regionCoordToChunkStart(int i) {
        return SectionPos.blockToSectionCoord((int)(i << 9));
    }

    public static int mapChunkSize() {
        return 32;
    }

    public static List<ChunkInfoMap> getMapsListOrCreate(Level level) {
        return REGION_LIST_COLLECTOR.computeIfAbsent(level, level1 -> new ArrayList());
    }

    public static List<ChunkInfoMap> getMapsList(Level level) {
        return level.isClientSide ? (CLIENT_REGION_LIST == null ? new ArrayList() : CLIENT_REGION_LIST) : MapChecker.getMapsListOrCreate(level);
    }

    public static ChunkInfoMap getChunkMap(Level level, BlockPos pos) {
        int x = MapChecker.blockToRegionCoord(pos.getX());
        int z = MapChecker.blockToRegionCoord(pos.getZ());
        return MapChecker.getChunkMap(level, x, z);
    }

    public static ChunkInfoMap getChunkMap(Level level, int regionX, int regionZ) {
        return MapChecker.getChunkMap(MapChecker.getMapsList(level), regionX, regionZ);
    }

    public static ChunkInfoMap getChunkMap(List<ChunkInfoMap> orDefault, int regionX, int regionZ) {
        ChunkInfoMap map = null;
        for (int i = 0; i < orDefault.size(); ++i) {
            ChunkInfoMap chunkHeightMap = orDefault.get(i);
            if (chunkHeightMap == null || chunkHeightMap.x != regionX || chunkHeightMap.z != regionZ) continue;
            map = chunkHeightMap;
            break;
        }
        return map;
    }

    @Nullable
    public static ChunkAccess getChunkView(Level level, BlockPos pos) {
        if (level == null) {
            return null;
        }
        int cx = SectionPos.blockToSectionCoord((int)pos.getX());
        int cz = SectionPos.blockToSectionCoord((int)pos.getZ());
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        return chunk != null && chunk.getPersistedStatus().isOrAfter(ChunkStatus.SURFACE) ? chunk : null;
    }

    @Nullable
    public static ChunkAccess getChunkView(Level level, int cx, int cz) {
        if (level == null) {
            return null;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        return chunk != null && chunk.getPersistedStatus().isOrAfter(ChunkStatus.SURFACE) ? chunk : null;
    }

    public static int getVanillaSolidHeightOrSelf(Level level, BlockPos pos) {
        ChunkAccess biomeChunk = MapChecker.getChunkView(level, pos);
        return biomeChunk != null ? biomeChunk.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) + 1 : pos.getY();
    }

    public static int getMCHeightWithCheck(Level level, BlockPos pos) {
        return MapChecker.getMCHeightWithCheck(level, pos, null);
    }

    public static int getMCHeightWithCheck(Level level, BlockPos pos, @Nullable Integer oldY) {
        ChunkAccess chunkAt = MapChecker.getChunkView(level, pos);
        SnowyRemover snowyRemover = null;
        if (chunkAt != null && chunkAt.hasData(AttachmentRegistry.SNOWY_REMOVER)) {
            snowyRemover = (SnowyRemover)chunkAt.getData(AttachmentRegistry.SNOWY_REMOVER);
        }
        return chunkAt == null ? pos.getY() : MapChecker.getMCHeightWithCheck(level, pos, chunkAt, snowyRemover, null, oldY);
    }

    public static int getMCHeightWithCheck(Level level, BlockPos pos, @Nonnull ChunkAccess chunkAt, @Nullable SnowyRemover snowyRemover, @Nullable BlockPos.MutableBlockPos checkPos, @Nullable Integer oldHeight) {
        BlockState state;
        BlockState blockState;
        if (oldHeight != null && oldHeight <= level.getMaxBuildHeight() && oldHeight >= level.getMinBuildHeight() && pos.getY() <= oldHeight - 2) {
            return oldHeight;
        }
        if (snowyRemover != null && snowyRemover.notSnowyAt(pos)) {
            return level.getMaxBuildHeight() + 1;
        }
        int posX = pos.getX();
        int posZ = pos.getZ();
        Heightmap.Types typesUse = level.isClientSide || (Boolean)CommonConfig.Snow.snowyTree.get() == false ? Heightmap.Types.MOTION_BLOCKING : Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        int height = chunkAt.getHeight(typesUse, posX, posZ);
        if (checkPos == null) {
            checkPos = new BlockPos.MutableBlockPos(posX, height, posZ);
        } else {
            checkPos.setY(height);
        }
        while (height >= chunkAt.getMinBuildHeight() && (blockState = chunkAt.getBlockState((BlockPos)checkPos)) instanceof BlockState && !MapChecker.solidTest(state = blockState)) {
            checkPos.setY(--height);
        }
        if (height < chunkAt.getMinBuildHeight()) {
            height = chunkAt.getMinBuildHeight();
        }
        return height;
    }

    public static boolean solidTest(BlockState state) {
        return Heightmap.Types.MOTION_BLOCKING.isOpaque().test(state) && !MapChecker.extraSnowPassable(state);
    }

    public static boolean extraSnowPassable(BlockState state) {
        Block onBlock;
        SnowDefinition.Info snow = SnowChecker.getUncacheSnow(state);
        if (snow != SnowDefinition.Info.EMPTY) {
            return snow.isSnowPassable() || snow.getFlag() == 1101;
        }
        Block block = state.getBlock();
        return block instanceof Block && ((onBlock = block) instanceof LeavesBlock && (Boolean)CommonConfig.Snow.snowyTree.get() != false || onBlock instanceof TrapDoorBlock || onBlock instanceof DoorBlock || onBlock instanceof FenceBlock || onBlock instanceof FenceGateBlock || onBlock instanceof WallBlock || onBlock instanceof BellBlock || onBlock instanceof ComposterBlock || onBlock instanceof CampfireBlock || onBlock instanceof AnvilBlock || onBlock instanceof BasePressurePlateBlock || onBlock instanceof IronBarsBlock || onBlock instanceof LightningRodBlock || onBlock instanceof LecternBlock || onBlock instanceof BambooStalkBlock);
    }

    public static int getHeightSafe(@NotNull Level level, BlockPos pos) {
        ChunkInfoMap chunkMap = MapChecker.getChunkMap(level, pos);
        if (chunkMap != null) {
            return chunkMap.getHeight(pos);
        }
        return level.getMinBuildHeight() - 1;
    }

    public static int getHeight(Level levelNull, BlockPos pos) {
        return MapChecker.getHeightOrUpdate(levelNull, pos, false);
    }

    public static int getHeightOrUpdate(Level levelNull, BlockPos pos, boolean forceUpdate) {
        return MapChecker.getSurfaceOrUpdate(levelNull, pos, forceUpdate, 0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static int getSurfaceOrUpdate(Level level, BlockPos pos, boolean forceUpdate, int type) {
        if (level == null) {
            return 0;
        }
        int x = MapChecker.blockToRegionCoord(pos.getX());
        int z = MapChecker.blockToRegionCoord(pos.getZ());
        List<ChunkInfoMap> mapsList = MapChecker.getMapsList(level);
        ChunkInfoMap map = MapChecker.getChunkMap(mapsList, x, z);
        int value = 0;
        if (map != null) {
            if (type == 0) {
                value = map.getHeight(pos);
                if (value <= map.minY || forceUpdate) {
                    int rh = MapChecker.getMCHeightWithCheck(level, pos, value);
                    map.updateHeight(pos, rh);
                    value = rh;
                }
            } else if (type == 1 && ((value = map.getBiome(pos)) == -1 || forceUpdate)) {
                value = MapChecker.biomeToId(level, (Biome)level.getBiome(pos).value());
                if (MapChecker.isLoadNearBy(level, pos)) {
                    map.updateBiome(pos, value);
                }
            }
        } else {
            List<ChunkInfoMap> list = mapsList;
            synchronized (list) {
                boolean hasBuild = false;
                for (ChunkInfoMap chunkHeightMap : mapsList) {
                    if (chunkHeightMap.x != x || chunkHeightMap.z != z) continue;
                    hasBuild = true;
                    map = chunkHeightMap;
                    break;
                }
                if (!hasBuild) {
                    map = new ChunkInfoMap(x, z, level.getMinBuildHeight() - 1, level.isClientSide);
                    mapsList.add(map);
                }
            }
            if (type == 0) {
                value = MapChecker.getMCHeightWithCheck(level, pos);
                map.updateHeight(pos, value);
            } else if (type == 1) {
                value = MapChecker.biomeToId(level, (Biome)level.getBiome(pos).value());
                if (MapChecker.isLoadNearBy(level, pos)) {
                    map.updateBiome(pos, value);
                }
            }
        }
        return value;
    }

    @Nullable
    public static ChunkInfoMap getChunkInfoMapOrCreate(Level level, BlockPos pos) {
        if (level == null) {
            return null;
        }
        int x = MapChecker.blockToRegionCoord(pos.getX());
        int z = MapChecker.blockToRegionCoord(pos.getZ());
        return MapChecker.getChunkInfoMapOrCreate(level, x, z);
    }

    @Nullable
    public static ChunkInfoMap getChunkInfoMapOrCreate(Level level, ChunkPos pos) {
        if (level == null) {
            return null;
        }
        int x = MapChecker.chunkToRegionCoord(pos.x);
        int z = MapChecker.chunkToRegionCoord(pos.z);
        return MapChecker.getChunkInfoMapOrCreate(level, x, z);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @NotNull
    public static ChunkInfoMap getChunkInfoMapOrCreate(@NotNull Level level, int regionX, int regionZ) {
        List<ChunkInfoMap> mapsList = MapChecker.getMapsListOrCreate(level);
        ChunkInfoMap map = MapChecker.getChunkMap(mapsList, regionX, regionZ);
        if (map != null) {
            return map;
        }
        List<ChunkInfoMap> list = mapsList;
        synchronized (list) {
            boolean hasBuild = false;
            for (ChunkInfoMap chunkHeightMap : mapsList) {
                if (chunkHeightMap.x != regionX || chunkHeightMap.z != regionZ) continue;
                hasBuild = true;
                map = chunkHeightMap;
                break;
            }
            if (!hasBuild) {
                map = new ChunkInfoMap(regionX, regionZ, level.getMinBuildHeight() - 1, level.isClientSide);
                mapsList.add(map);
            }
        }
        return map;
    }

    public static boolean isLoaded(Level level, BlockPos pos) {
        int chunkX = SectionPos.blockToSectionCoord((int)pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord((int)pos.getZ());
        return !level.isOutsideBuildHeight(pos) && MapChecker.isLoaded(level, chunkX, chunkZ);
    }

    public static boolean isLoaded(Level level, int chunkX, int chunkZ) {
        ChunkSource chunkSource = level.getChunkSource();
        if (chunkSource instanceof ServerChunkCache) {
            ServerChunkCache serverChunkCache = (ServerChunkCache)chunkSource;
            ChunkHolder visibleChunkIfPresent = serverChunkCache.getVisibleChunkIfPresent(ChunkPos.asLong((int)chunkX, (int)chunkZ));
            if (visibleChunkIfPresent == null) {
                return false;
            }
            CompletableFuture completablefuture = visibleChunkIfPresent.getFullChunkFuture();
            ChunkResult either = completablefuture.getNow(null);
            LevelChunk o = either == null ? null : (LevelChunk)either.orElse(null);
            return o != null;
        }
        return level.getChunkSource().hasChunk(chunkX, chunkZ);
    }

    public static boolean isLoadedOnlyServer(Level level, BlockPos pos) {
        return !(level instanceof ServerLevel) || MapChecker.isLoaded(level, pos);
    }

    public static boolean isLoadNearByOnlyServer(Level level, BlockPos pos) {
        return !(level instanceof ServerLevel) || MapChecker.isLoadNearBy(level, pos);
    }

    public static boolean isLoadNearBy(Level level, BlockPos pos) {
        int chunkX = SectionPos.blockToSectionCoord((int)pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord((int)pos.getZ());
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        int i1 = (pos.getX() & 0xF) - 2;
        int l1 = (pos.getZ() & 0xF) - 2;
        int xe = i1 >> 2 > 2 ? 1 : 0;
        int ze = l1 >> 2 > 2 ? 1 : 0;
        int xs = i1 < 2 ? -1 : 0;
        int zs = l1 < 2 ? -1 : 0;
        for (int i = xs; i <= xe; ++i) {
            for (int j = zs; j <= ze; ++j) {
                if (MapChecker.isLoaded(level, chunkX + i, chunkZ + j)) continue;
                return false;
            }
        }
        return true;
    }

    public static void updatePosForce(Level level, BlockPos setPos, int y) {
        ChunkInfoMap map = MapChecker.getChunkMap(level, setPos);
        if (map != null) {
            map.updateHeight(setPos, y);
        }
    }

    public static boolean withoutSaltWandAffect(Level level, BlockPos pos) {
        if (MapChecker.isLoadedOnlyServer(level, pos)) {
            LevelChunk chunkAt = level.getChunkAt(pos);
            NoneSnowArea snowArea = (NoneSnowArea)chunkAt.getExistingDataOrNull(AttachmentRegistry.NONE_SNOW_AREA);
            return snowArea == null || !snowArea.neverSnowyAt(pos);
        }
        return true;
    }

    public static boolean notLightAbove(Level level, BlockPos pos, int times) {
        if (!((Boolean)CommonConfig.Debug.notLightAbove.get()).booleanValue()) {
            return true;
        }
        BlockPos abovePos = pos.above();
        if (level.isLoaded(abovePos)) {
            BlockState stateAbove;
            try {
                stateAbove = level.getBlockState(abovePos);
            }
            catch (Exception e) {
                EclipticSeasons.LOGGER.error("Logic thread change the block in render thread with {}", (Object)pos);
                return true;
            }
            if (stateAbove.getBlock() instanceof LightBlock) {
                if ((Integer)stateAbove.getValue((Property)LightBlock.LEVEL) == 0) {
                    return false;
                }
            } else if (!stateAbove.isAir() && !MapChecker.solidTest(stateAbove) && times > 0) {
                return MapChecker.notLightAbove(level, abovePos, times - 1);
            }
        }
        return true;
    }

    public static boolean isAboveSnowLine(@NotNull Level level, Biome biome, BlockPos pos) {
        return MapChecker.isAboveSnowLine(biome, pos.getY(), level instanceof ServerLevel);
    }

    public static boolean isAboveSnowLine(Biome biome, int pos, boolean isServer) {
        return pos > BiomeClimateManager.getSnowLine(biome, isServer);
    }

    public static boolean shouldSnowAt(@Nonnull Level level, BlockPos pos, BlockState state, RandomSource random, long seed) {
        boolean isSnowy;
        if (SnowyMapChecker.shouldCheckSnowyStatus(level, pos) && MapChecker.notWater(state)) {
            return SnowyMapChecker.isSnowyBlock(level, pos);
        }
        Holder<Biome> biomeHolder = MapChecker.getSurfaceBiome(level, pos);
        Biome biome = (Biome)biomeHolder.value();
        boolean bl = isSnowy = (long)WeatherManager.getSnowDepthAtBiome(level, biome) > Math.abs(seed % 100L);
        if (!isSnowy) {
            isSnowy = MapChecker.isAboveSnowLine(level, biome, pos);
        }
        if (isSnowy) {
            isSnowy = MapChecker.notLightAbove(level, pos, 4);
        }
        if (isSnowy) {
            isSnowy = MapChecker.withoutSaltWandAffect(level, pos);
        }
        return isSnowy;
    }

    public static boolean shouldSnowAt(@Nonnull Level level, BlockPos pos, int biomeId, BlockState state, @Nullable RandomSource random, long seed) {
        boolean isSnowy;
        if (SnowyMapChecker.shouldCheckSnowyStatus(level, pos) && MapChecker.notWater(state)) {
            return SnowyMapChecker.isSnowyBlock(level, pos);
        }
        Biome biome = (Biome)MapChecker.idToBiome(level, biomeId).value();
        ArrayList<WeatherManager.BiomeWeather> biomeList = WeatherManager.getBiomeList(level);
        boolean bl = isSnowy = biomeList != null && (long)WeatherManager.getSnowDepthAtBiome(level, biome) > Math.abs(seed % 100L);
        if (!isSnowy) {
            isSnowy = MapChecker.isAboveSnowLine(level, biome, pos);
        }
        if (isSnowy) {
            isSnowy = MapChecker.notLightAbove(level, pos, 4);
        }
        return isSnowy;
    }

    public static boolean notWater(BlockState state) {
        return state == null || !state.is(Blocks.WATER);
    }

    public static boolean shouldSnowAtBiome(@Nonnull Level level, Biome biome, BlockState state, RandomSource random, long seed, BlockPos mcPos) {
        if (MapChecker.isAboveSnowLine(level, biome, mcPos)) {
            return true;
        }
        return (long)WeatherManager.getSnowDepthAtBiome(level, biome) > Math.abs(seed % 100L);
    }

    public static boolean isSmallBiome(@Nonnull Holder<Biome> biomeHolder) {
        return biomeHolder != null && MapChecker.isSmallBiome((Biome)biomeHolder.value());
    }

    public static boolean isSmallBiome(@Nonnull Biome biomeHolder) {
        return ((IBiomeTagHolder)biomeHolder).eclipticseasons$isSmallBiome();
    }

    @NotNull
    public static Holder<Biome> idToBiome(Level level, int id) {
        Holder<Biome> biomeHolder;
        ArrayList<WeatherManager.BiomeWeather> list = WeatherManager.getBiomeList(level);
        if (list != null && id < list.size() && (biomeHolder = list.get((int)id).biomeHolder) != null) {
            return biomeHolder;
        }
        Optional biomeRegistry = level.registryAccess().registry(Registries.BIOME);
        if (biomeRegistry.isPresent()) {
            Optional holder = ((Registry)biomeRegistry.get()).getHolder(id);
            if (holder.isPresent()) {
                return (Holder)holder.get();
            }
            EclipticSeasons.extraLogger(false, "Failed to resolve biome ID in level", id, level.dimension().location(), level.getClass().getName(), level);
            return (Holder)((Registry)biomeRegistry.get()).getHolder(Biomes.PLAINS).orElse(PlainsStubHolder.PLAINS);
        }
        EclipticSeasons.extraLogger(false, "Registry access for BIOME is missing in abnormal level context", id, level.getClass().getName(), level);
        return PlainsStubHolder.VOID;
    }

    public static Holder<Biome> idToBiome(Registry<Biome> biomes, int id) {
        Optional holder = biomes.getHolder(id);
        if (holder.isPresent()) {
            return (Holder)holder.get();
        }
        EclipticSeasons.extraLogger(false, "Unknown ID for biome within registry", id, biomes.getClass().getName(), biomes);
        return (Holder)biomes.getHolder(Biomes.PLAINS).orElse(PlainsStubHolder.PLAINS);
    }

    public static int biomeToId(Level level, Biome b) {
        IBiomeTagHolder iBiomeTagHolder;
        int id;
        Biome o = b;
        if (o instanceof IBiomeTagHolder && (id = (iBiomeTagHolder = (IBiomeTagHolder)o).eclipticseasons$getBindId()) > -1) {
            return id;
        }
        return MapChecker.biomeToId((Registry<Biome>)level.registryAccess().registryOrThrow(Registries.BIOME), b);
    }

    public static int biomeToId(Registry<Biome> biomes, Biome b) {
        int id = biomes.getId((Object)b);
        if (id < 0) {
            Biome plainsBiome = (Biome)biomes.get(Biomes.PLAINS);
            id = biomes.getId((Object)plainsBiome);
        }
        return biomes.getId((Object)b);
    }

    public static Holder<Biome> getSurfaceBiome(Level level, BlockPos pos) {
        IChunkBiomeHolder iChunkBiomeHolder;
        BiomeHolder biomeHolder;
        ChunkAccess chunkAt = MapChecker.getChunkView(level, pos);
        if (chunkAt instanceof IChunkBiomeHolder && (biomeHolder = (iChunkBiomeHolder = (IChunkBiomeHolder)chunkAt).eclipticseasons$getBiomeHolder()) != null && biomeHolder.version() == EclipticUtil.getBiomeDataVersion(level)) {
            return MapChecker.getSurfaceBiome(level, pos, biomeHolder);
        }
        return MapChecker.getUnCachedSurfaceBiome(level, pos);
    }

    public static Holder<Biome> getSurfaceBiomeByChunk(Level level, LevelChunk chunkAt, BlockPos pos) {
        IChunkBiomeHolder iChunkBiomeHolder;
        BiomeHolder biomeHolder;
        if (chunkAt instanceof IChunkBiomeHolder && (biomeHolder = (iChunkBiomeHolder = (IChunkBiomeHolder)chunkAt).eclipticseasons$getBiomeHolder()) != null && biomeHolder.version() == EclipticUtil.getBiomeDataVersion(level)) {
            return MapChecker.getSurfaceBiome(level, pos, biomeHolder);
        }
        return MapChecker.getUnCachedSurfaceBiome(level, pos);
    }

    public static Holder<Biome> getSurfaceBiome(Level level, BlockPos pos, BiomeHolder biomeHolder) {
        int biomeId = biomeHolder == null ? -1 : biomeHolder.getBiomeId(pos);
        return biomeId > -1 ? MapChecker.idToBiome(level, biomeId) : MapChecker.getUnCachedSurfaceBiome(level, pos);
    }

    /*
     * Unable to fully structure code
     */
    public static Holder<Biome> getUnCachedSurfaceBiome(Level level, BlockPos pos) {
        maxBuildHeight = level.getMaxBuildHeight();
        minBuildHeight = level.getMinBuildHeight();
        chunkMap = MapChecker.getChunkMap(level, pos);
        biome = null;
        bid = 0;
        y = 0;
        if (chunkMap != null && (bid = chunkMap.getBiome(pos)) > -1 && MapChecker.isSmallBiome(biome = MapChecker.idToBiome(level, bid)) && ((y = chunkMap.getHeight(pos) + 1) > maxBuildHeight || y <= minBuildHeight)) {
            y = MapChecker.getVanillaSolidHeightOrSelf(level, pos);
        }
        if (biome == null) {
            if (chunkMap != null) {
                y = chunkMap.getHeight(pos) + 1;
            }
            if (y > maxBuildHeight || y <= minBuildHeight) {
                y = MapChecker.getVanillaSolidHeightOrSelf(level, pos);
            }
            pos = new BlockPos(pos.getX(), y, pos.getZ());
            bid = MapChecker.getSurfaceOrUpdate(level, pos, false, 1);
            biome = MapChecker.idToBiome(level, bid);
        }
        if (biome == null) {
            biome = level.registryAccess().holderOrThrow(Biomes.PLAINS);
        }
        if (MapChecker.isSmallBiome(biome) && level instanceof ServerLevel) {
            serverLevel = (ServerLevel)level;
            sample = RiverBiomeResolver.getClimateTargetPoint(serverLevel.getChunkSource().randomState(), pos.mutable());
            biomeResourceKey = RiverBiomeResolver.getClimateBiome(sample);
            biome = level.registryAccess().holder(biomeResourceKey).map((Function<Holder.Reference, Holder>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda$getUnCachedSurfaceBiome$2(net.minecraft.core.Holder$Reference ), (Lnet/minecraft/core/Holder$Reference;)Lnet/minecraft/core/Holder;)()).orElse(biome);
        }
        relative = null;
        i = 0;
        last_ii = 0;
        shouldBreak = false;
        while (MapChecker.isSmallBiome(biome)) {
            if (relative == null) {
                relative = new BlockPos.MutableBlockPos(pos.getX(), y, pos.getZ());
            }
            ++i;
            for (SimplePair<Direction, Direction> pair : MapChecker.SMALL_OFFSET_DIRECTIONS) {
                if (pair.getValue() != null) {
                    ii = i - 1;
                    if (ii == last_ii) continue;
                    relative.move(pair.getKey(), ii);
                    relative.move(pair.getValue(), ii);
                    last_ii = ii;
                } else {
                    relative.move(pair.getKey(), i);
                }
                if (chunkMap != null) {
                    x = MapChecker.blockToRegionCoord(relative.getX());
                    z = MapChecker.blockToRegionCoord(relative.getZ());
                    if (chunkMap.getX() == x && chunkMap.getZ() == z) {
                        bid = chunkMap.getBiome((BlockPos)relative);
                    }
                    if (bid > -1) {
                        biome = MapChecker.idToBiome(level, bid);
                    }
                }
                if (i <= 20 || !(level instanceof ServerLevel)) ** GOTO lbl-1000
                serverLevel = (ServerLevel)level;
                if (!MapChecker.isLoadNearBy(level, (BlockPos)relative)) {
                    biomeSource = serverLevel.getChunkSource().getGenerator().getBiomeSource();
                    qx = QuartPos.fromBlock((int)relative.getX());
                    qy = QuartPos.fromBlock((int)relative.getY());
                    qz = QuartPos.fromBlock((int)relative.getZ());
                    biome = biomeSource.getNoiseBiome(qx, qy, qz, serverLevel.getChunkSource().randomState().sampler());
                } else if (bid < 0) {
                    y = MapChecker.getHeightSafe(level, (BlockPos)relative) + 1;
                    if (y > maxBuildHeight || y <= minBuildHeight) {
                        y = MapChecker.getVanillaSolidHeightOrSelf(level, (BlockPos)relative);
                    }
                    relative.setY(y);
                    bid = MapChecker.getSurfaceOrUpdate(level, (BlockPos)relative, false, 1);
                    biome = MapChecker.idToBiome(level, bid);
                }
                if (!MapChecker.isSmallBiome(biome)) {
                    shouldBreak = true;
                    break;
                }
                relative.setX(pos.getX());
                relative.setZ(pos.getZ());
            }
            if (!shouldBreak && i <= 128) continue;
            break;
        }
        return biome;
    }

    private static Holder<Biome> fixBiomeOnServer(ServerLevel level, BlockPos pos, Holder<Biome> biome, ChunkInfoMap map) {
        BiomeSource biomeSource;
        Climate.ParameterList parameters = LEVEL_PARAMETER_LIST_MAP.get(level);
        if (parameters == null && (biomeSource = level.getChunkSource().getGenerator().getBiomeSource()) instanceof MultiNoiseBiomeSource) {
            MultiNoiseBiomeSource multiNoiseBiomeSource = (MultiNoiseBiomeSource)biomeSource;
            Climate.ParameterList parameters2 = multiNoiseBiomeSource.parameters();
            List<Pair> list = parameters2.values().stream().filter(p -> !MapChecker.isSmallBiome((Holder<Biome>)((Holder)p.getSecond()))).toList();
            parameters = new Climate.ParameterList(list);
            LEVEL_PARAMETER_LIST_MAP.put((Level)level, (Climate.ParameterList<Holder<Biome>>)parameters);
        }
        if (parameters != null) {
            Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
            Climate.TargetPoint sample = sampler.sample(QuartPos.fromBlock((int)pos.getX()), QuartPos.fromBlock((int)pos.getY()), QuartPos.fromBlock((int)pos.getZ()));
            biome = (Holder)parameters.findValue(sample);
        }
        return biome;
    }

    public static int getBlockType(BlockState state, BlockGetter level, BlockPos pos) {
        int flag = 0;
        Block onBlock = state.getBlock();
        if (!CommonConfig.Debug.snowOverlayGlowingBlock.getAsBoolean() && state.getLightEmission(level, pos) > 0) {
            flag = 0;
        } else if (state.is(EclipticBlockTags.SNOW_OVERLAY_CANNOT_SURVIVE_ON)) {
            flag = 0;
        } else {
            if (state.getBlock().builtInRegistryHolder().getKey().location().getNamespace().equals("snowrealmagic")) {
                return 0;
            }
            if (onBlock instanceof LeavesBlock) {
                flag = 4;
            } else if (onBlock == Blocks.GRASS_BLOCK || onBlock == Blocks.DIRT || onBlock == Blocks.STONE || onBlock == Blocks.SAND) {
                flag = 1;
            } else if (onBlock == Blocks.SHORT_GRASS || onBlock == Blocks.FERN) {
                flag = 5;
            } else if (onBlock == Blocks.TALL_GRASS || onBlock == Blocks.LARGE_FERN) {
                flag = 501;
            } else if (onBlock instanceof VineBlock) {
                flag = 7;
            } else if (onBlock instanceof FarmBlock || onBlock instanceof DirtPathBlock) {
                flag = 6;
            } else if (onBlock instanceof TrapDoorBlock || onBlock instanceof DoorBlock && state.getValue((Property)DoorBlock.HALF) == DoubleBlockHalf.UPPER || onBlock instanceof FenceBlock || onBlock instanceof FenceGateBlock || onBlock instanceof WallBlock || onBlock instanceof BellBlock || onBlock instanceof ComposterBlock || onBlock instanceof CampfireBlock && (Boolean)state.getValue((Property)CampfireBlock.LIT) == false || onBlock == Blocks.IRON_BARS || onBlock instanceof LightningRodBlock || onBlock instanceof AzaleaBlock) {
                flag = 999;
            } else {
                ResourceLocation blockName = onBlock.builtInRegistryHolder().key().location();
                if (state.isSolidRender(level, pos)) {
                    flag = 1;
                } else if (onBlock instanceof SlabBlock) {
                    SlabType value = (SlabType)state.getValue((Property)SlabBlock.TYPE);
                    flag = value == SlabType.TOP ? 301 : (value == SlabType.BOTTOM ? 2 : 1);
                    if (blockName.toString().equals("xkdeco:dirt_path_slab")) {
                        flag = 999;
                    }
                } else if (onBlock instanceof StairBlock) {
                    flag = 3;
                }
            }
        }
        return flag;
    }

    public static int getSnowOffset(BlockState state, int flag) {
        SnowDefinition.Info uncacheSnow = SnowChecker.getUncacheSnow(state);
        if (uncacheSnow.isValid()) {
            return uncacheSnow.getOffset();
        }
        int offset = 0;
        if (flag == 5 || flag == 501) {
            if (flag == 5) {
                offset = 1;
            } else if (flag == 501) {
                offset = state.getValue((Property)DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? 1 : 2;
            }
        } else if (MapChecker.customBuiltin(flag) && state.getBlock() instanceof AzaleaBlock) {
            offset = 1;
        }
        return offset;
    }

    public static int getDefaultBlockTypeFlag(BlockState state) {
        IBlockStateFlagger flagger = (IBlockStateFlagger)state;
        int flag = flagger.getBlockTypeFlag();
        if (flag < 0) {
            SnowDefinition.Info uncacheSnow = SnowChecker.getUncacheSnow(state);
            if (CommonConfig.getForceBlocksNotSnowy().contains(state.getBlock())) {
                flag = 0;
            } else if (uncacheSnow.isValid()) {
                flag = uncacheSnow.getFlag();
            } else {
                try {
                    flag = MapChecker.getBlockType(state, (BlockGetter)EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                }
                catch (Exception e) {
                    flag = 0;
                    EclipticSeasons.logger(e);
                }
            }
            flagger.setBlockTypeFlag(flag);
        }
        return flag;
    }

    @Deprecated(forRemoval=true)
    public static int getBlockTypeFlag(BlockGetter blockGetter, BlockPos pos, BlockState state) {
        SnowDefinition.Info uncacheSnow;
        IBlockStateFlagger flagger = (IBlockStateFlagger)state;
        int flag = CommonConfig.getForceBlocksNotSnowy().contains(state.getBlock()) ? 0 : ((uncacheSnow = SnowChecker.getUncacheSnow(state)).isValid() ? uncacheSnow.getFlag() : MapChecker.getBlockType(state, blockGetter, pos));
        return flag;
    }

    public static List<Holder<Biome>> getBiomes(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos(pos.getX(), level.getMaxBuildHeight(), pos.getZ());
        ArrayList<Holder<Biome>> list = new ArrayList<Holder<Biome>>();
        while (mPos.getY() >= level.getMinBuildHeight()) {
            list.add((Holder<Biome>)level.getBiome((BlockPos)mPos));
            mPos = mPos.move(Direction.DOWN);
        }
        return list;
    }

    public static boolean isValidDimension(@Nullable Level level) {
        boolean result;
        boolean bl = result = level != null;
        if (result) {
            for (int i = 0; i < validDimension.size(); ++i) {
                if (validDimension.get(i) != level) continue;
                return true;
            }
        }
        return false;
    }

    public static void sendChunkInfo(LevelChunk chunk, ChunkPos chunkPos, ServerPlayer player, List<Integer> section_y, List<BlockPos> clickedPos) {
        Object object;
        int[] bytes = new int[256];
        if (chunk.hasData(AttachmentRegistry.SNOWY_REMOVER) && (object = chunk.getData(AttachmentRegistry.SNOWY_REMOVER)) instanceof SnowyRemover) {
            SnowyRemover snowyRemover = (SnowyRemover)object;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(chunkPos.getMinBlockX(), 64, chunkPos.getMinBlockZ());
            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) {
                    bytes[i * 16 + j] = snowyRemover.blockWatcher()[i][j];
                }
            }
        }
        SimpleNetworkHandler.send(player, new ChunkUpdateMessage(bytes, chunk.getPos().x, chunk.getPos().z, section_y, clickedPos));
    }

    public static void sendChunkLoginInfo(ServerLevel serverLevel, LevelChunk chunk, ChunkPos chunkPos, ServerPlayer player) {
        BiomeHolder biomeHolder = MapChecker.getOrUpdateChunkBiomeData(serverLevel, (ChunkAccess)chunk, chunkPos);
        if (biomeHolder != null && biomeHolder.hasUpdated()) {
            SimpleNetworkHandler.send(player, new ChunkBiomeUpdateMessage(biomeHolder.biomes(), chunk.getPos().x, chunk.getPos().z, biomeHolder.version()));
        }
        MapChecker.sendChunkInfo(chunk, chunkPos, player, List.of(), List.of());
    }

    @NotNull
    public static BiomeHolder getOrUpdateChunkBiomeData(ServerLevel serverLevel, ChunkAccess chunk, ChunkPos chunkPos) {
        BiomeHolder biomeHolder;
        int biomeDataVersion = EclipticUtil.getBiomeDataVersion((Level)serverLevel);
        if (!chunk.hasData(AttachmentRegistry.BIOME_HOLDER)) {
            biomeHolder = BiomeHolder.prepareBiomes((Level)serverLevel, chunk, chunkPos, biomeDataVersion, false);
            chunk.setData(AttachmentRegistry.BIOME_HOLDER, (Object)biomeHolder);
        } else {
            biomeHolder = (BiomeHolder)chunk.getData(AttachmentRegistry.BIOME_HOLDER);
            if (biomeHolder.hasUpdated() && biomeHolder.version() == -2) {
                biomeHolder = BiomeHolder.fillSmallBiomes((Level)serverLevel, chunk, biomeHolder, biomeDataVersion);
                chunk.setData(AttachmentRegistry.BIOME_HOLDER, (Object)biomeHolder);
            } else if (!biomeHolder.hasUpdated() || biomeHolder.version() != biomeDataVersion) {
                biomeHolder = BiomeHolder.prepareBiomes((Level)serverLevel, chunk, chunkPos, biomeDataVersion, biomeHolder.version() != biomeDataVersion);
                chunk.setData(AttachmentRegistry.BIOME_HOLDER, (Object)biomeHolder);
            }
        }
        return biomeHolder;
    }

    public static void resetBiomeHolder(ServerLevel serverLevel, BlockPos pos) {
        int biomeDataVersion = EclipticUtil.getBiomeDataVersion((Level)serverLevel);
        ChunkPos chunkPos = new ChunkPos(pos);
        BiomeHolder biomeHolder = BiomeHolder.prepareBiomes((Level)serverLevel, serverLevel.getChunk(pos), chunkPos, biomeDataVersion, true);
        ChunkAccess chunk = serverLevel.getChunk(pos);
        chunk.setData(AttachmentRegistry.BIOME_HOLDER, (Object)biomeHolder);
        if (chunk instanceof IChunkBiomeHolder) {
            IChunkBiomeHolder chunkBiomeHolder = (IChunkBiomeHolder)chunk;
            chunkBiomeHolder.eclipticseasons$resetBiomeHolder();
        }
        if (chunk instanceof LevelChunk) {
            LevelChunk levelChunk = (LevelChunk)chunk;
            for (ServerPlayer player : serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)) {
                MapChecker.sendChunkLoginInfo(serverLevel, levelChunk, chunkPos, player);
            }
        }
    }

    public static ChunkInfoMap forceChunkUpdateHeight(Level level, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());
        ChunkInfoMap chunkMap = MapChecker.getChunkInfoMapOrCreate(level, (BlockPos)checkPos);
        if (chunkMap != null) {
            SnowyRemover existingData = chunk.getExistingData(AttachmentRegistry.SNOWY_REMOVER).orElse(null);
            for (int i = chunkPos.getMinBlockX(); i <= chunkPos.getMaxBlockX(); ++i) {
                for (int j = chunkPos.getMinBlockZ(); j <= chunkPos.getMaxBlockZ(); ++j) {
                    checkPos.setX(i);
                    checkPos.setZ(j);
                    int k = MapChecker.getMCHeightWithCheck(level, (BlockPos)checkPos, chunk, existingData, checkPos, null);
                    chunkMap.updateHeight(i, j, k);
                }
            }
        }
        return chunkMap;
    }

    public static void setNewChunk(ServerLevel serverLevel, ChunkAccess chunk) {
        if (chunk.hasData(AttachmentRegistry.BIOME_HOLDER)) {
            BiomeHolder biomeHolder = (BiomeHolder)chunk.getData(AttachmentRegistry.BIOME_HOLDER);
            SolarDataManager data = SolarHolders.getSaveData((Level)serverLevel);
            if (data != null && biomeHolder.hasUpdated() && biomeHolder.version() == -1) {
                chunk.setData(AttachmentRegistry.BIOME_HOLDER, (Object)new BiomeHolder(biomeHolder.biomes(), true, data.getBiomeDataVersion()));
            }
        }
    }

    public static boolean leaveLike(int flag) {
        return flag == 4 || flag == 1100 || flag == 1101;
    }

    public static boolean vineLike(int flag) {
        return flag == 7 || flag == 1200;
    }

    public static boolean solidBlockLike(int flag) {
        return flag == 1 || flag == 1000;
    }

    public static boolean customBuiltin(int flag) {
        return flag == 998 || flag == 999;
    }

    private static /* synthetic */ Holder lambda$getUnCachedSurfaceBiome$2(Holder.Reference bh) {
        return bh;
    }
}

