/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_238
 */
package com.goby56.wakes.utils;

import net.minecraft.class_238;

public interface Position<T> {
    public int x();

    public int z();

    public boolean inValidPos();

    public class_238 toBox();

    public void updateAdjacency(T var1);
}

