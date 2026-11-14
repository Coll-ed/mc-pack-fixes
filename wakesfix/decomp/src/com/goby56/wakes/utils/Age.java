/*
 * Decompiled with CFR 0.152.
 */
package com.goby56.wakes.utils;

public interface Age<T> {
    public boolean isDead();

    public void markDead();

    public void revive(T var1);

    public void tick();
}

