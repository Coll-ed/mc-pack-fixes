/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.ints.IntList
 *  lombok.Generated
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Registry
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtOps
 *  net.minecraft.nbt.Tag
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.chunk.ChunkAccess
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.neoforged.neoforge.attachment.IAttachmentHolder
 *  net.neoforged.neoforge.attachment.IAttachmentSerializer
 *  org.jetbrains.annotations.NotNull
 */
package com.teamtea.eclipticseasons.common.core.map;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamtea.eclipticseasons.common.core.map.MapChecker;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Optional;
import lombok.Generated;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.NotNull;

public class BiomeHolder {
    final int[] biomes;
    final boolean hasUpdated;
    final int version;
    public static final int FLAG_NEED_VERSION = -1;
    public static final int FLAG_FILL_SMALL = -2;
    public static final Codec<BiomeHolder> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(snowyRemoverInstance -> snowyRemoverInstance.group((App)Codec.INT.sizeLimitedListOf(256).fieldOf("blocks").forGetter(snowyRemover -> IntList.of((int[])snowyRemover.biomes())), (App)Codec.BOOL.fieldOf("has_updated").forGetter(BiomeHolder::hasUpdated), (App)Codec.INT.fieldOf("version").forGetter(BiomeHolder::version)).apply((Applicative)snowyRemoverInstance, (biomes, hasUpdated, v) -> {
        int[] biomeArryas;
        if (biomes instanceof IntList) {
            IntList intList = (IntList)biomes;
            biomeArryas = intList.toIntArray();
        } else {
            biomeArryas = new int[biomes.size()];
            for (int i = 0; i < biomes.size(); ++i) {
                biomeArryas[i] = (Integer)biomes.get(i);
            }
        }
        return new BiomeHolder(biomeArryas, (boolean)hasUpdated, (int)v);
    })));
    private transient int cacheVersion = Integer.MIN_VALUE;
    private transient boolean cacheUpdate = false;
    private transient CompoundTag cacheTag = null;

    public int[] biomes() {
        return this.biomes;
    }

    public boolean hasUpdated() {
        return this.hasUpdated;
    }

    public int version() {
        return this.version;
    }

    public static BiomeHolder prepareBiomes(Level serverLevel, ChunkAccess chunk, ChunkPos chunkPos, int biomeDataVersion, boolean registryUpdate) {
        int[] newBiomes = new int[256];
        boolean near = true;
        Registry biomeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.BIOME);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        block0: for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                int xm = chunkPos.getBlockX(i);
                int zm = chunkPos.getBlockZ(j);
                mutableBlockPos.set(xm, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, i, j) + 1, zm);
                Holder<Biome> unCachedSurfaceBiome = MapChecker.getUnCachedSurfaceBiome(serverLevel, (BlockPos)mutableBlockPos);
                int n = newBiomes[i * 16 + j] = registryUpdate ? MapChecker.biomeToId((Registry<Biome>)biomeRegistry, (Biome)unCachedSurfaceBiome.value()) : MapChecker.biomeToId(serverLevel, (Biome)unCachedSurfaceBiome.value());
                if (!near) continue block0;
            }
        }
        return new BiomeHolder(newBiomes, near, biomeDataVersion);
    }

    public static BiomeHolder fillSmallBiomes(Level serverLevel, ChunkAccess chunk, BiomeHolder oldHolder, int biomeDataVersion) {
        int[] newBiomes = new int[256];
        boolean near = true;
        int[] oldBiomes = oldHolder.biomes;
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        ChunkPos chunkPos = chunk.getPos();
        block0: for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                Holder<Biome> biomeHolder = MapChecker.idToBiome(serverLevel, oldBiomes[i * 16 + j]);
                if (MapChecker.isSmallBiome(biomeHolder)) {
                    int xm = chunkPos.getBlockX(i);
                    int zm = chunkPos.getBlockZ(j);
                    mutableBlockPos.set(xm, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, i, j) + 1, zm);
                    newBiomes[i * 16 + j] = MapChecker.biomeToId(serverLevel, (Biome)MapChecker.getUnCachedSurfaceBiome(serverLevel, (BlockPos)mutableBlockPos).value());
                    if (near) continue;
                    continue block0;
                }
                newBiomes[i * 16 + j] = oldBiomes[i * 16 + j];
            }
        }
        return new BiomeHolder(newBiomes, near, biomeDataVersion);
    }

    public int getBiomeId(BlockPos blockPos) {
        return this.hasUpdated ? this.biomes[(blockPos.getX() & 0xF) * 16 + (blockPos.getZ() & 0xF)] : -1;
    }

    public static BiomeHolder empty() {
        return new BiomeHolder(new int[256], false, 0);
    }

    @Generated
    public BiomeHolder(int[] biomes, boolean hasUpdated, int version) {
        this.biomes = biomes;
        this.hasUpdated = hasUpdated;
        this.version = version;
    }

    @Generated
    public int[] getBiomes() {
        return this.biomes;
    }

    @Generated
    public boolean isHasUpdated() {
        return this.hasUpdated;
    }

    @Generated
    public int getVersion() {
        return this.version;
    }

    @Generated
    public void setCacheVersion(int cacheVersion) {
        this.cacheVersion = cacheVersion;
    }

    @Generated
    public void setCacheUpdate(boolean cacheUpdate) {
        this.cacheUpdate = cacheUpdate;
    }

    @Generated
    public void setCacheTag(CompoundTag cacheTag) {
        this.cacheTag = cacheTag;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof BiomeHolder)) {
            return false;
        }
        BiomeHolder other = (BiomeHolder)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isHasUpdated() != other.isHasUpdated()) {
            return false;
        }
        if (this.getVersion() != other.getVersion()) {
            return false;
        }
        return Arrays.equals(this.getBiomes(), other.getBiomes());
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof BiomeHolder;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isHasUpdated() ? 79 : 97);
        result = result * 59 + this.getVersion();
        result = result * 59 + Arrays.hashCode(this.getBiomes());
        return result;
    }

    @Generated
    public String toString() {
        return "BiomeHolder(biomes=" + Arrays.toString(this.getBiomes()) + ", hasUpdated=" + this.isHasUpdated() + ", version=" + this.getVersion() + ")";
    }

    public static class Serializer
    implements IAttachmentSerializer<Tag, BiomeHolder> {
        @NotNull
        public BiomeHolder read(@NotNull IAttachmentHolder holder, @NotNull Tag tag, // Could not load outer class - annotation placement on inner may be incorrect
         @NotNull HolderLookup.Provider provider) {
            Optional result = CODEC.parse((DynamicOps)provider.createSerializationContext((DynamicOps)NbtOps.INSTANCE), (Object)tag).result();
            return result.orElseGet(BiomeHolder::empty);
        }

        public Tag write(@NotNull BiomeHolder attachment, // Could not load outer class - annotation placement on inner may be incorrect
         @NotNull HolderLookup.Provider provider) {
            if (attachment.cacheUpdate == attachment.hasUpdated && attachment.cacheVersion == attachment.version && attachment.cacheTag != null) {
                return attachment.cacheTag;
            }
            Optional result = CODEC.encodeStart((DynamicOps)provider.createSerializationContext((DynamicOps)NbtOps.INSTANCE), (Object)attachment).result();
            Object var5_4 = result.orElse(null);
            if (var5_4 instanceof CompoundTag) {
                CompoundTag compoundTag;
                attachment.cacheTag = compoundTag = (CompoundTag)var5_4;
                attachment.cacheUpdate = attachment.hasUpdated;
                attachment.cacheVersion = attachment.version;
                return compoundTag;
            }
            return new CompoundTag();
        }
    }
}

