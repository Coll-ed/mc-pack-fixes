/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderOwner
 *  org.jetbrains.annotations.NotNull
 */
package com.teamtea.eclipticseasons.common.core.map.stub;

import net.minecraft.core.HolderOwner;
import org.jetbrains.annotations.NotNull;

public class DummyHolderOwner<T>
implements HolderOwner<T> {
    public boolean canSerializeIn(@NotNull HolderOwner<T> context) {
        return false;
    }
}

