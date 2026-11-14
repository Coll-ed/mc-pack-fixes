/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_243
 *  net.minecraft.class_703
 */
package com.goby56.wakes.duck;

import net.minecraft.class_243;
import net.minecraft.class_703;

public interface ProducesWake {
    public boolean onWaterSurface();

    public float producingHeight();

    public class_243 getPrevPos();

    public void setPrevPos(class_243 var1);

    public double getHorizontalVelocity();

    public double getVerticalVelocity();

    public void setWakeParticle(class_703 var1);
}

