/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_2400
 */
package com.goby56.wakes.particle;

import net.minecraft.class_1297;
import net.minecraft.class_2400;

public class WithOwnerParticleType
extends class_2400 {
    public class_1297 owner;

    protected WithOwnerParticleType(boolean alwaysShow) {
        super(alwaysShow);
    }

    public WithOwnerParticleType withOwner(class_1297 owner) {
        this.owner = owner;
        return this;
    }
}

