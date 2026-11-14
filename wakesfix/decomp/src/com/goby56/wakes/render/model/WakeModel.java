/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_2960
 *  net.minecraft.class_4587
 *  net.minecraft.class_4588
 *  net.minecraft.class_5601
 *  net.minecraft.class_5603
 *  net.minecraft.class_5605
 *  net.minecraft.class_5606
 *  net.minecraft.class_5607
 *  net.minecraft.class_5609
 *  net.minecraft.class_5610
 *  net.minecraft.class_583
 *  net.minecraft.class_630
 */
package com.goby56.wakes.render.model;

import net.minecraft.class_1297;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_5601;
import net.minecraft.class_5603;
import net.minecraft.class_5605;
import net.minecraft.class_5606;
import net.minecraft.class_5607;
import net.minecraft.class_5609;
import net.minecraft.class_5610;
import net.minecraft.class_583;
import net.minecraft.class_630;

public class WakeModel<T extends class_1297>
extends class_583<T> {
    public static final class_5601 MODEL_LAYER = new class_5601(class_2960.method_60655((String)"wakes", (String)"wake"), "main");
    private final class_630 wake;

    public WakeModel(class_630 root) {
        this.wake = root.method_32086("wake");
    }

    public static class_5607 getTexturedModelData() {
        class_5609 modelData = new class_5609();
        class_5610 modelPartData = modelData.method_32111();
        class_5610 bone = modelPartData.method_32117("wake", class_5606.method_32108().method_32101(36, 42).method_32098(-8.0f, 0.0f, -24.0f, 16.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 42).method_32098(-9.0f, 0.0f, -22.0f, 18.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(40, 40).method_32098(-10.0f, 0.0f, -20.0f, 20.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 40).method_32098(-10.0f, 0.0f, -18.0f, 20.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 38).method_32098(-11.0f, 0.0f, -16.0f, 22.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 36).method_32098(-11.0f, 0.0f, -14.0f, 22.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 34).method_32098(-12.0f, 0.0f, -12.0f, 24.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 32).method_32098(-12.0f, 0.0f, -10.0f, 24.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 30).method_32098(-13.0f, 0.0f, -8.0f, 26.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 28).method_32098(-13.0f, 0.0f, -6.0f, 26.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 26).method_32098(-14.0f, 0.0f, -4.0f, 28.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 24).method_32098(-14.0f, 0.0f, -2.0f, 28.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 22).method_32098(-15.0f, 0.0f, 0.0f, 30.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 20).method_32098(-15.0f, 0.0f, 2.0f, 30.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 18).method_32098(-16.0f, 0.0f, 4.0f, 32.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 16).method_32098(-16.0f, 0.0f, 6.0f, 32.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 14).method_32098(-17.0f, 0.0f, 8.0f, 34.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 12).method_32098(-17.0f, 0.0f, 10.0f, 34.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 10).method_32098(-18.0f, 0.0f, 12.0f, 36.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 8).method_32098(-18.0f, 0.0f, 14.0f, 36.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 6).method_32098(-19.0f, 0.0f, 18.0f, 38.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 4).method_32098(-19.0f, 0.0f, 16.0f, 38.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 2).method_32098(-20.0f, 0.0f, 22.0f, 40.0f, 0.0f, 2.0f, new class_5605(0.0f)).method_32101(0, 0).method_32098(-20.0f, 0.0f, 20.0f, 40.0f, 0.0f, 2.0f, new class_5605(0.0f)), class_5603.method_32090((float)0.0f, (float)24.0f, (float)0.0f));
        return class_5607.method_32110((class_5609)modelData, (int)128, (int)128);
    }

    public void method_2819(class_1297 entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    public void method_2828(class_4587 matrices, class_4588 vertices, int light, int overlay, int color) {
        this.wake.method_22699(matrices, vertices, light, overlay, color);
    }
}

