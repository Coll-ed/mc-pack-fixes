/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  lombok.Generated
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtOps
 *  net.minecraft.nbt.Tag
 *  net.minecraft.util.RandomSource
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
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import lombok.Generated;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.NotNull;

public class SnowyRemover {
    final int[][] blockWatcher;
    public static final Codec<SnowyRemover> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(snowyRemoverInstance -> snowyRemoverInstance.group((App)Codec.INT.sizeLimitedListOf(256).fieldOf("blocks").forGetter(snowyRemover -> Arrays.stream(snowyRemover.blockWatcher()).flatMapToInt(Arrays::stream).boxed().toList())).apply((Applicative)snowyRemoverInstance, ss -> new SnowyRemover((int[][])IntStream.range(0, 16).mapToObj(i -> ss.subList(i * 16, (i + 1) * 16).stream().mapToInt(Integer::intValue).toArray()).toArray(x$0 -> new int[x$0][])))));
    public static final int SNOWY = 0;
    public static final int NONE_SNOWY = 1;
    public static final int SNOWY_EVEN_LIGHT = 2;
    private transient CompoundTag cacheTag = null;

    public int[][] blockWatcher() {
        return this.blockWatcher;
    }

    public static int getChunkPos(int golobalPos) {
        return golobalPos & 0xF;
    }

    public void setChunkPos(BlockPos blockPos, int value) {
        this.blockWatcher[SnowyRemover.getChunkPos((int)blockPos.getX())][SnowyRemover.getChunkPos((int)blockPos.getZ())] = value;
        this.cacheTag = null;
    }

    public boolean notSnowyAt(int x, int z) {
        return this.blockWatcher[x][z] == 1;
    }

    public boolean notSnowyAt(BlockPos blockPos) {
        return this.notSnowyAt(SnowyRemover.getChunkPos(blockPos.getX()), SnowyRemover.getChunkPos(blockPos.getZ()));
    }

    public SnowyFlag getSnowyFlag(BlockPos blockPos) {
        return SnowyFlag.values()[this.blockWatcher[SnowyRemover.getChunkPos(blockPos.getX())][SnowyRemover.getChunkPos(blockPos.getZ())]];
    }

    public boolean allSnowAble() {
        return Arrays.stream(this.blockWatcher()).flatMapToInt(Arrays::stream).noneMatch(c -> c > 0);
    }

    public static SnowyRemover empty() {
        return new SnowyRemover(new int[16][16]);
    }

    @Generated
    public SnowyRemover(int[][] blockWatcher) {
        this.blockWatcher = blockWatcher;
    }

    @Generated
    public int[][] getBlockWatcher() {
        return this.blockWatcher;
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
        if (!(o instanceof SnowyRemover)) {
            return false;
        }
        SnowyRemover other = (SnowyRemover)o;
        if (!other.canEqual(this)) {
            return false;
        }
        return Arrays.deepEquals((Object[])this.getBlockWatcher(), (Object[])other.getBlockWatcher());
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SnowyRemover;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + Arrays.deepHashCode((Object[])this.getBlockWatcher());
        return result;
    }

    @Generated
    public String toString() {
        return "SnowyRemover(blockWatcher=" + Arrays.deepToString((Object[])this.getBlockWatcher()) + ")";
    }

    public static enum SnowyFlag {
        SNOWY((ParticleOptions)ParticleTypes.SNOWFLAKE, r -> null),
        NONE_SNOWY((ParticleOptions)ParticleTypes.SMOKE, r -> r.nextInt(3) > 0 ? ParticleTypes.SMOKE : ParticleTypes.CLOUD),
        SNOWY_EVEN_LIGHT((ParticleOptions)ParticleTypes.GLOW_SQUID_INK, r -> r.nextInt(3) > 0 ? ParticleTypes.GLOW_SQUID_INK : null),
        SNOWY_ALWAYS((ParticleOptions)ParticleTypes.ITEM_SNOWBALL, r -> r.nextInt(3) > 0 ? ParticleTypes.ITEM_SNOWBALL : null);

        final ParticleOptions particleOptions;
        final Function<RandomSource, ParticleOptions> randomSourceConsumer;
        private static final SnowyFlag[] VALUES;

        private SnowyFlag(ParticleOptions particleOptions, Function<RandomSource, ParticleOptions> randomSourceConsumer) {
            this.particleOptions = particleOptions;
            this.randomSourceConsumer = randomSourceConsumer;
        }

        public ParticleOptions getNextParticleOptions() {
            return this.particleOptions;
        }

        public ParticleOptions getIndicatorParticleOptions(RandomSource r) {
            return this.randomSourceConsumer.apply(r);
        }

        public static SnowyFlag[] collectValues() {
            return VALUES;
        }

        public SnowyFlag cycle() {
            SnowyFlag[] snowyFlags = SnowyFlag.collectValues();
            int index = this.ordinal();
            index = index < snowyFlags.length - 1 ? index + 1 : 0;
            return snowyFlags[index];
        }

        public static SnowyFlag cycle(int index) {
            SnowyFlag[] snowyFlags = SnowyFlag.collectValues();
            index = index < snowyFlags.length - 1 ? index + 1 : 0;
            return snowyFlags[index];
        }

        static {
            VALUES = SnowyFlag.values();
        }
    }

    public static class Serializer
    implements IAttachmentSerializer<Tag, SnowyRemover> {
        @NotNull
        public SnowyRemover read(@NotNull IAttachmentHolder holder, @NotNull Tag tag, // Could not load outer class - annotation placement on inner may be incorrect
         @NotNull HolderLookup.Provider provider) {
            Optional result = CODEC.parse((DynamicOps)provider.createSerializationContext((DynamicOps)NbtOps.INSTANCE), (Object)tag).result();
            return result.orElseGet(SnowyRemover::empty);
        }

        public Tag write(@NotNull SnowyRemover attachment, // Could not load outer class - annotation placement on inner may be incorrect
         @NotNull HolderLookup.Provider provider) {
            if (attachment.cacheTag != null) {
                return attachment.cacheTag;
            }
            Optional result = CODEC.encodeStart((DynamicOps)provider.createSerializationContext((DynamicOps)NbtOps.INSTANCE), (Object)attachment).result();
            Object var5_4 = result.orElse(null);
            if (var5_4 instanceof CompoundTag) {
                CompoundTag compoundTag;
                attachment.cacheTag = compoundTag = (CompoundTag)var5_4;
                return compoundTag;
            }
            return new CompoundTag();
        }
    }
}

