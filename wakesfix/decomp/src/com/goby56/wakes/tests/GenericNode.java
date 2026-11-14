/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_238
 */
package com.goby56.wakes.tests;

import com.goby56.wakes.utils.Age;
import com.goby56.wakes.utils.Position;
import java.util.Objects;
import net.minecraft.class_238;

public class GenericNode
implements Position<GenericNode>,
Age<GenericNode> {
    private final int x;
    private final int z;
    private final float height;
    private final int floodLevel;

    public GenericNode(int x, int z, float height, int floodLevel) {
        this.x = x;
        this.z = z;
        this.height = height;
        this.floodLevel = floodLevel;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        GenericNode genericNode = (GenericNode)o;
        return this.x == genericNode.x && this.z == genericNode.z;
    }

    public int hashCode() {
        return Objects.hash(this.x, this.z);
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public void markDead() {
    }

    @Override
    public void revive(GenericNode newNode) {
    }

    @Override
    public void tick() {
    }

    @Override
    public int x() {
        return this.x;
    }

    @Override
    public int z() {
        return this.z;
    }

    @Override
    public boolean inValidPos() {
        return true;
    }

    @Override
    public class_238 toBox() {
        return null;
    }

    @Override
    public void updateAdjacency(GenericNode node) {
    }
}

