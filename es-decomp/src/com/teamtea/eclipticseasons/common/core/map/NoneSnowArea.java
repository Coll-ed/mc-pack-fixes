/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 *  it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet
 *  lombok.Generated
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.codec.StreamCodec
 */
package com.teamtea.eclipticseasons.common.core.map;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamtea.eclipticseasons.api.util.EclipticUtil;
import com.teamtea.eclipticseasons.api.util.serializer.SerializerHolder;
import com.teamtea.eclipticseasons.common.network.message.MessageCodec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.Collection;
import lombok.Generated;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;

public class NoneSnowArea
implements SerializerHolder {
    public static final Codec<NoneSnowArea> CODEC = RecordCodecBuilder.create(ins -> ins.group((App)Codec.LONG.listOf().fieldOf("pos_list").forGetter(o -> o.posMap.longStream().boxed().toList())).apply((Applicative)ins, longs -> new NoneSnowArea(new LongLinkedOpenHashSet((Collection)longs))));
    public static final StreamCodec<ByteBuf, NoneSnowArea> STREAM_CODEC = StreamCodec.composite(MessageCodec.longlistStreamCodec, o -> o.posMap.longStream().boxed().toList(), longs -> new NoneSnowArea(new LongLinkedOpenHashSet((Collection)longs)));
    private final LongLinkedOpenHashSet posMap;
    private transient CompoundTag cacheTag = null;

    public boolean neverSnowyAt(BlockPos blockPos) {
        return !EclipticUtil.canSnowyBlockInteract() && this.posMap.contains(blockPos.asLong());
    }

    public boolean canSkip() {
        return this.posMap.isEmpty();
    }

    public boolean add(BlockPos blockPos) {
        boolean add = this.posMap.add(blockPos.asLong());
        if (add) {
            this.setChanged();
        }
        return add;
    }

    public boolean remove(BlockPos blockPos) {
        boolean remove = this.posMap.remove(blockPos.asLong());
        if (remove) {
            this.setChanged();
        }
        return remove;
    }

    public static NoneSnowArea empty() {
        return new NoneSnowArea(new LongLinkedOpenHashSet());
    }

    @Generated
    public NoneSnowArea(LongLinkedOpenHashSet posMap) {
        this.posMap = posMap;
    }

    @Generated
    public LongLinkedOpenHashSet getPosMap() {
        return this.posMap;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof NoneSnowArea)) {
            return false;
        }
        NoneSnowArea other = (NoneSnowArea)o;
        if (!other.canEqual(this)) {
            return false;
        }
        LongLinkedOpenHashSet this$posMap = this.getPosMap();
        LongLinkedOpenHashSet other$posMap = other.getPosMap();
        return !(this$posMap == null ? other$posMap != null : !this$posMap.equals(other$posMap));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof NoneSnowArea;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        LongLinkedOpenHashSet $posMap = this.getPosMap();
        result = result * 59 + ($posMap == null ? 43 : $posMap.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "NoneSnowArea(posMap=" + String.valueOf(this.getPosMap()) + ")";
    }

    @Override
    @Generated
    public CompoundTag getCacheTag() {
        return this.cacheTag;
    }

    @Override
    @Generated
    public void setCacheTag(CompoundTag cacheTag) {
        this.cacheTag = cacheTag;
    }
}

