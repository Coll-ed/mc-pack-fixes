/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.teamtea.eclipticseasons.client.util.ClientCon
 *  com.teamtea.eclipticseasons.common.core.map.MapChecker
 *  com.teamtea.eclipticseasons.config.CommonConfig$Snow
 *  it.unimi.dsi.fastutil.longs.LongArrayList
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 */
package me.cortex.voxy.client.core.compat.eclipticseasons;

import com.teamtea.eclipticseasons.client.util.ClientCon;
import com.teamtea.eclipticseasons.common.core.map.MapChecker;
import com.teamtea.eclipticseasons.config.CommonConfig;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import me.cortex.voxy.client.core.compat.eclipticseasons.VoxyTool;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.common.world.WorldSection;
import me.cortex.voxy.common.world.other.Mapper;
import me.cortex.voxy.common.world.other.Mipper;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SeasonalSnowRefresher {
    private static final int MAX_BLOCK_ID = 1048575;
    private static final long BLOCK_ID_MASK = 140737354137600L;
    private static final int BLOCK_ID_SHIFT = 27;
    private static final int SECTION_WIDTH = 32;
    private static final int MIN_SKY_LIGHT = 9;
    private static final int UNKNOWN = -1;
    private static final int NO = 0;
    private static final int YES = 1;
    private static final int SECTIONS_PER_BATCH = 128;
    private static final long BATCH_PAUSE_MILLIS = 10L;
    private static Thread worker;
    private static volatile Run active;
    public static volatile long runs;
    public static volatile long sectionsScanned;
    public static volatile long sectionsRewritten;
    public static volatile long voxelsFlipped;
    public static volatile String status;
    private static final ThreadLocal<long[][]> MIP_SCRATCH;

    private SeasonalSnowRefresher() {
    }

    public static synchronized boolean isRunning() {
        return worker != null && worker.isAlive();
    }

    public static synchronized void start(Level level, WorldEngine engine) {
        Run run;
        if (SeasonalSnowRefresher.isRunning()) {
            return;
        }
        if (!VoxyTool.isVoxyTest()) {
            return;
        }
        active = run = new Run();
        ++runs;
        worker = new Thread(() -> SeasonalSnowRefresher.run(level, engine, run), "Voxy seasonal snow refresh");
        worker.setDaemon(true);
        worker.setPriority(1);
        worker.start();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void cancelAndJoin() {
        Class<SeasonalSnowRefresher> clazz = SeasonalSnowRefresher.class;
        synchronized (SeasonalSnowRefresher.class) {
            Run run = active;
            if (run != null) {
                run.cancelled = true;
            }
            Thread t = worker;
            // ** MonitorExit[var1] (shouldn't be in output)
            if (t != null && t.isAlive()) {
                t.interrupt();
                try {
                    t.join(2000L);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return;
        }
    }

    public static String describe() {
        return "status=" + status + " runs=" + runs + " sectionsScanned=" + sectionsScanned + " sectionsRewritten=" + sectionsRewritten + " voxelsFlipped=" + voxelsFlipped;
    }

    public static String startManually() {
        if (!VoxyTool.isVoxyTest()) {
            return "seasonal snow LOD is off (ecliptic_seasons_snow_lod)";
        }
        if (SeasonalSnowRefresher.isRunning()) {
            return "already running: " + SeasonalSnowRefresher.describe();
        }
        Level level = ClientCon.getUseLevel();
        if (level == null) {
            return "no level";
        }
        WorldEngine engine = WorldIdentifier.ofEngineNullable(level);
        if (engine == null || !engine.isLive()) {
            return "no live world engine";
        }
        SeasonalSnowRefresher.start(level, engine);
        return "started - poll /voxy debug seasons for progress";
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void run(Level level, WorldEngine engine, Run run) {
        long scanned = 0L;
        long rewritten = 0L;
        long flipped = 0L;
        boolean referenced = false;
        try {
            engine.acquireRef();
            referenced = true;
            Mapper mapper = engine.getMapper();
            BiomeCache biomes = new BiomeCache(level, mapper);
            boolean glowBlocksSnow = (Boolean)CommonConfig.Snow.notSnowyNearGlowingBlock.get();
            int glowLevel = CommonConfig.Snow.notSnowyNearGlowingBlockLevel.getAsInt();
            boolean snowyTree = (Boolean)CommonConfig.Snow.snowyTree.get();
            LongArrayList keys = new LongArrayList();
            try {
                engine.storage.iteratePositions(0, key -> {
                    if (run.cancelled || Thread.currentThread().isInterrupted() || engine.instanceIn != null && !engine.instanceIn.isRunning()) {
                        throw new WalkCancelled();
                    }
                    keys.add(key);
                });
            }
            catch (WalkCancelled e) {
                status = "cancelled while listing sections";
                sectionsScanned = scanned;
                sectionsRewritten = rewritten;
                voxelsFlipped = flipped;
                if (referenced) {
                    try {
                        engine.releaseRef();
                    }
                    catch (RuntimeException runtimeException) {
                        // empty catch block
                    }
                }
                Class<SeasonalSnowRefresher> clazz = SeasonalSnowRefresher.class;
                synchronized (SeasonalSnowRefresher.class) {
                    if (worker == Thread.currentThread()) {
                        worker = null;
                        active = null;
                    }
                    // ** MonitorExit[var17_23] (shouldn't be in output)
                    return;
                }
            }
            status = "scanning " + keys.size() + " sections";
            for (int i = 0; i < keys.size() && !run.cancelled && engine.isLive() && !Thread.currentThread().isInterrupted() && (engine.instanceIn == null || engine.instanceIn.isRunning()); ++i) {
                long key2 = keys.getLong(i);
                WorldSection section = engine.acquireIfExists(key2);
                if (section == null) continue;
                try {
                    int changed = SeasonalSnowRefresher.refreshSection(engine, level, mapper, biomes, section, glowBlocksSnow, glowLevel, snowyTree);
                    if (changed > 0) {
                        flipped += (long)changed;
                        ++rewritten;
                        engine.markDirty(section, 1, 0);
                        SeasonalSnowRefresher.remipIntoParents(engine, mapper, section, section._rawOrNull());
                    }
                }
                finally {
                    section.release(WorldSection.RELEASE_HINT_DONT_CACHE);
                }
                ++scanned;
                if (i % 128 != 127) continue;
                sectionsScanned = scanned;
                sectionsRewritten = rewritten;
                voxelsFlipped = flipped;
                Thread.sleep(10L);
            }
            status = run.cancelled ? "cancelled after " + scanned + " sections" : "done";
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status = "interrupted after " + scanned + " sections";
        }
        catch (Throwable t) {
            status = "failed: " + String.valueOf(t);
            Logger.error("Seasonal snow LOD refresh failed", t);
        }
        finally {
            sectionsScanned = scanned;
            sectionsRewritten = rewritten;
            voxelsFlipped = flipped;
            if (referenced) {
                try {
                    engine.releaseRef();
                }
                catch (RuntimeException e) {}
            }
            Class<SeasonalSnowRefresher> e = SeasonalSnowRefresher.class;
            synchronized (SeasonalSnowRefresher.class) {
                if (worker == Thread.currentThread()) {
                    worker = null;
                    active = null;
                }
                // ** MonitorExit[e] (shouldn't be in output)
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static int refreshSection(WorldEngine engine, Level level, Mapper mapper, BiomeCache biomes, WorldSection section, boolean glowBlocksSnow, int glowLevel, boolean snowyTree) {
        long[] data = section._rawOrNull();
        if (data == null) {
            return 0;
        }
        int chunkX = section.x << 1;
        int chunkZ = section.z << 1;
        boolean[] quadrantLoaded = new boolean[]{MapChecker.isLoaded((Level)level, (int)chunkX, (int)chunkZ), MapChecker.isLoaded((Level)level, (int)(chunkX + 1), (int)chunkZ), MapChecker.isLoaded((Level)level, (int)chunkX, (int)(chunkZ + 1)), MapChecker.isLoaded((Level)level, (int)(chunkX + 1), (int)(chunkZ + 1))};
        if (quadrantLoaded[0] && quadrantLoaded[1] && quadrantLoaded[2] && quadrantLoaded[3]) {
            return 0;
        }
        int stateCount = mapper.getBlockStateCount();
        WorldSection above = null;
        boolean aboveResolved = false;
        int changed = 0;
        try {
            for (int y = 0; y < 32; ++y) {
                for (int z = 0; z < 32; ++z) {
                    for (int x = 0; x < 32; ++x) {
                        long current;
                        boolean wantSnowy;
                        long aboveVoxel;
                        BlockState state;
                        int baseId;
                        int idx;
                        long voxel;
                        int storedId;
                        if (quadrantLoaded[x >> 4 | z >> 4 << 1] || (storedId = Mapper.getBlockId(voxel = data[idx = WorldSection.getIndex(x, y, z)])) == 0) continue;
                        boolean storedSnowy = storedId >= stateCount;
                        int n = baseId = storedSnowy ? 1048575 - storedId : storedId;
                        if (baseId <= 0 || baseId >= stateCount || (state = mapper.getBlockStateFromBlockId(baseId)) == null || MapChecker.getDefaultBlockTypeFlag((BlockState)state) <= 0) continue;
                        if (y + 1 < 32) {
                            aboveVoxel = data[WorldSection.getIndex(x, y + 1, z)];
                        } else {
                            if (!aboveResolved) {
                                aboveResolved = true;
                                above = engine.acquireIfExists(section.lvl, section.x, section.y + 1, section.z);
                            }
                            if (above == null) continue;
                            aboveVoxel = above.get(WorldSection.getIndex(x, 0, z));
                        }
                        int verdict = SeasonalSnowRefresher.shouldBeSnowy(level, mapper, biomes, section, x, y, z, state, aboveVoxel, Mapper.getBiomeId(voxel), stateCount, glowBlocksSnow, glowLevel, snowyTree);
                        if (verdict == -1) continue;
                        boolean bl = wantSnowy = verdict == 1;
                        if (wantSnowy == storedSnowy || Mapper.getBlockId(current = data[idx]) != storedId) continue;
                        int newId = wantSnowy ? 1048575 - baseId : baseId;
                        data[idx] = current & 0xFFFF800007FFFFFFL | Integer.toUnsignedLong(newId) << 27;
                        ++changed;
                    }
                }
            }
        }
        finally {
            if (above != null) {
                above.release(WorldSection.RELEASE_HINT_DONT_CACHE);
            }
        }
        return changed;
    }

    private static void remipIntoParents(WorldEngine engine, Mapper mapper, WorldSection section, long[] data) {
        int stateCount = mapper.getBlockStateCount();
        long[][] scratch = MIP_SCRATCH.get();
        long[] cur = data;
        int curSide = 32;
        for (int lvl = 1; lvl <= 4; ++lvl) {
            int side = curSide >> 1;
            long[] next = scratch[lvl - 1];
            for (int y = 0; y < side; ++y) {
                for (int z = 0; z < side; ++z) {
                    for (int x = 0; x < side; ++x) {
                        int cx = x << 1;
                        int cy = y << 1;
                        int cz = z << 1;
                        next[SeasonalSnowRefresher.cubeIndex((int)x, (int)y, (int)z, (int)side)] = Mipper.mip(cur[SeasonalSnowRefresher.cubeIndex(cx, cy, cz, curSide)], cur[SeasonalSnowRefresher.cubeIndex(cx + 1, cy, cz, curSide)], cur[SeasonalSnowRefresher.cubeIndex(cx, cy, cz + 1, curSide)], cur[SeasonalSnowRefresher.cubeIndex(cx + 1, cy, cz + 1, curSide)], cur[SeasonalSnowRefresher.cubeIndex(cx, cy + 1, cz, curSide)], cur[SeasonalSnowRefresher.cubeIndex(cx + 1, cy + 1, cz, curSide)], cur[SeasonalSnowRefresher.cubeIndex(cx, cy + 1, cz + 1, curSide)], cur[SeasonalSnowRefresher.cubeIndex(cx + 1, cy + 1, cz + 1, curSide)], mapper);
                    }
                }
            }
            SeasonalSnowRefresher.applyLevel(engine, section, lvl, next, side, stateCount);
            cur = next;
            curSide = side;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void applyLevel(WorldEngine engine, WorldSection section, int lvl, long[] computed, int side, int stateCount) {
        WorldSection parent = engine.acquireIfExists(lvl, section.x >> lvl, section.y >> lvl, section.z >> lvl);
        if (parent == null) {
            return;
        }
        try {
            long[] pdata = parent._rawOrNull();
            if (pdata == null) {
                return;
            }
            int ox = section.x << 5 - lvl & 0x1F;
            int oy = section.y << 5 - lvl & 0x1F;
            int oz = section.z << 5 - lvl & 0x1F;
            boolean touched = false;
            for (int y = 0; y < side; ++y) {
                for (int z = 0; z < side; ++z) {
                    for (int x = 0; x < side; ++x) {
                        int haveBase;
                        int pidx;
                        long have;
                        long want = computed[SeasonalSnowRefresher.cubeIndex(x, y, z, side)];
                        if (want == (have = pdata[pidx = WorldSection.getIndex(ox + x, oy + y, oz + z)])) continue;
                        int wantId = Mapper.getBlockId(want);
                        int haveId = Mapper.getBlockId(have);
                        boolean wantSnowy = wantId >= stateCount;
                        boolean haveSnowy = haveId >= stateCount;
                        int wantBase = wantSnowy ? 1048575 - wantId : wantId;
                        int n = haveBase = haveSnowy ? 1048575 - haveId : haveId;
                        if (wantBase != haveBase || wantSnowy == haveSnowy) continue;
                        int newId = wantSnowy ? 1048575 - haveBase : haveBase;
                        pdata[pidx] = have & 0xFFFF800007FFFFFFL | Integer.toUnsignedLong(newId) << 27;
                        touched = true;
                    }
                }
            }
            if (touched) {
                engine.markDirty(parent, 1, 0);
            }
        }
        finally {
            parent.release();
        }
    }

    private static int cubeIndex(int x, int y, int z, int side) {
        return (y * side + z) * side + x;
    }

    private static int shouldBeSnowy(Level level, Mapper mapper, BiomeCache biomes, WorldSection section, int x, int y, int z, BlockState state, long aboveVoxel, int biomeId, int stateCount, boolean glowBlocksSnow, int glowLevel, boolean snowyTree) {
        Holder<Biome> biome;
        int aboveBase;
        int light = Mapper.getLightId(aboveVoxel);
        if ((light & 0xF) <= 9) {
            return 0;
        }
        if (glowBlocksSnow && (light >> 4 & 0xF) >= glowLevel) {
            return 0;
        }
        int aboveStored = Mapper.getBlockId(aboveVoxel);
        int n = aboveBase = aboveStored >= stateCount ? 1048575 - aboveStored : aboveStored;
        if (aboveBase < 0 || aboveBase >= stateCount) {
            return -1;
        }
        BlockState aboveState = mapper.getBlockStateFromBlockId(aboveBase);
        if (aboveState == null) {
            return -1;
        }
        int flag = MapChecker.getDefaultBlockTypeFlag((BlockState)state);
        if (MapChecker.leaveLike((int)flag)) {
            boolean specialLeaves;
            boolean bl = specialLeaves = aboveState.is(state.getBlock()) && (Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque().test(aboveState) || MapChecker.extraSnowPassable((BlockState)aboveState));
            if (specialLeaves && !snowyTree) {
                return 0;
            }
        } else if (MapChecker.extraSnowPassable((BlockState)state) && MapChecker.extraSnowPassable((BlockState)aboveState)) {
            return 0;
        }
        if ((biome = biomes.get(biomeId)) == null) {
            return -1;
        }
        BlockPos pos = new BlockPos((section.x << 5) + x, (section.y << 5) + y, (section.z << 5) + z);
        return MapChecker.shouldSnowAtBiome((Level)level, (Biome)((Biome)biome.value()), (BlockState)state, (RandomSource)level.getRandom(), (long)state.getSeed(pos), (BlockPos)pos) ? 1 : 0;
    }

    static {
        status = "never run";
        MIP_SCRATCH = ThreadLocal.withInitial(() -> {
            long[][] levels = new long[4][];
            int side = 32;
            for (int lvl = 0; lvl < levels.length; ++lvl) {
                levels[lvl] = new long[(side >>= 1) * side * side];
            }
            return levels;
        });
    }

    private static final class Run {
        volatile boolean cancelled;

        private Run() {
        }
    }

    private static final class BiomeCache {
        private final Level level;
        private final Mapper.BiomeEntry[] entries;
        private final Holder<Biome>[] resolved;
        private final boolean[] known;

        BiomeCache(Level level, Mapper mapper) {
            this.level = level;
            this.entries = mapper.getBiomeEntries();
            this.resolved = new Holder[Math.max(1, this.entries.length)];
            this.known = new boolean[this.resolved.length];
        }

        Holder<Biome> get(int biomeId) {
            if (biomeId < 0 || biomeId >= this.known.length) {
                return null;
            }
            if (this.known[biomeId]) {
                return this.resolved[biomeId];
            }
            this.known[biomeId] = true;
            try {
                if (biomeId >= this.entries.length || this.entries[biomeId] == null) {
                    return null;
                }
                ResourceKey key = ResourceKey.create((ResourceKey)Registries.BIOME, (ResourceLocation)ResourceLocation.parse((String)this.entries[biomeId].biome));
                this.resolved[biomeId] = this.level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
            }
            catch (RuntimeException e) {
                this.resolved[biomeId] = null;
            }
            return this.resolved[biomeId];
        }
    }

    private static final class WalkCancelled
    extends RuntimeException {
        WalkCancelled() {
            super(null, null, false, false);
        }
    }
}

