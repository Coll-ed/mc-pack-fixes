/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 */
package com.teamtea.eclipticseasons.common.core.map;

import com.teamtea.eclipticseasons.EclipticSeasons;
import java.util.Arrays;
import net.minecraft.core.BlockPos;

public class ChunkInfoMap {
    public static final int TYPE_BIOME = 1;
    public static final int TYPE_HEIGHT = 0;
    private final short[] matrix = new short[262144];
    private final short[] biomes = new short[262144];
    final int x;
    final int z;
    final short minY;
    final boolean isClient;

    public ChunkInfoMap(int x, int z, int minY, boolean isClient) {
        this.minY = (short)minY;
        this.isClient = isClient;
        this.x = x;
        this.z = z;
        Arrays.fill(this.biomes, (short)-1);
        Arrays.fill(this.matrix, this.minY);
        EclipticSeasons.extraLogger(true, String.format("End create [%s, %s]", x, z));
    }

    public static int getChunkValue(int i) {
        return i & 0x1FF;
    }

    private static int getIndex(int x, int z) {
        return x * 512 + z;
    }

    public int getHeight(int x, int z) {
        x = ChunkInfoMap.getChunkValue(x);
        z = ChunkInfoMap.getChunkValue(z);
        return this.matrix[ChunkInfoMap.getIndex(x, z)];
    }

    public int getBiome(int x, int z) {
        x = ChunkInfoMap.getChunkValue(x);
        z = ChunkInfoMap.getChunkValue(z);
        return this.biomes[ChunkInfoMap.getIndex(x, z)];
    }

    public int getHeight(BlockPos pos) {
        return this.getHeight(pos.getX(), pos.getZ());
    }

    public int getBiome(BlockPos pos) {
        return this.getBiome(pos.getX(), pos.getZ());
    }

    public int updateHeight(int x, int z, int y) {
        x = ChunkInfoMap.getChunkValue(x);
        z = ChunkInfoMap.getChunkValue(z);
        int index = ChunkInfoMap.getIndex(x, z);
        short old = this.matrix[index];
        this.matrix[index] = (short)y;
        return old;
    }

    public int updateBiome(int x, int z, int id) {
        x = ChunkInfoMap.getChunkValue(x);
        z = ChunkInfoMap.getChunkValue(z);
        int index = ChunkInfoMap.getIndex(x, z);
        short old = this.biomes[index];
        this.biomes[index] = (short)id;
        return old;
    }

    public int updateBiome(BlockPos pos, int id) {
        return this.updateBiome(pos.getX(), pos.getZ(), id);
    }

    public int updateHeight(BlockPos pos, int y) {
        return this.updateHeight(pos.getX(), pos.getZ(), y);
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public short getMinY() {
        return this.minY;
    }
}

