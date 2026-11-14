/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 */
package com.teamtea.eclipticseasons.common.core.map;

import java.util.Objects;
import net.minecraft.core.BlockPos;

public record XZPos(int x, int z, long startTick, int startY) {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        XZPos xzPos = (XZPos)o;
        return this.x == xzPos.x && this.z == xzPos.z;
    }

    public long toLongKey() {
        return BlockPos.asLong((int)this.x, (int)0, (int)this.z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.z);
    }

    @Override
    public String toString() {
        return "XZPos{startTick=" + this.startTick + ", x=" + this.x + ", z=" + this.z + ", startY=" + this.startY + "}";
    }
}

