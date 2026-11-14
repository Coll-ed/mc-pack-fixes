/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.junit.jupiter.api.Assertions
 *  org.junit.jupiter.api.Test
 */
package com.goby56.wakes.tests;

import com.goby56.wakes.utils.WakesUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WakesUtilsTest {
    WakesUtilsTest() {
    }

    @Test
    void rgbaArr2abgrInt() {
        int[] rgba = new int[]{0, 255, 0, 255};
        int val = WakesUtils.rgbaArr2abgrInt(rgba);
        Assertions.assertEquals((int)-16711936, (int)val);
    }

    @Test
    void abgrInt2rgbaArr() {
        int val = -16711936;
        int[] rgba = WakesUtils.abgrInt2rgbaArr(val);
        Assertions.assertArrayEquals((int[])new int[]{0, 255, 0, 255}, (int[])rgba);
    }

    @Test
    void longAsPos() {
        int[] pos = WakesUtils.longAsPos(0xD00000009L);
        Assertions.assertEquals((int)13, (int)pos[0]);
        Assertions.assertEquals((int)9, (int)pos[1]);
        pos = WakesUtils.longAsPos(-8589934541L);
        Assertions.assertEquals((int)-2, (int)pos[0]);
        Assertions.assertEquals((int)51, (int)pos[1]);
    }

    @Test
    void posAsLong() {
        long l = WakesUtils.posAsLong(13, 9);
        Assertions.assertEquals((long)0xD00000009L, (long)l);
        l = WakesUtils.posAsLong(-2, 51);
        Assertions.assertEquals((long)-8589934541L, (long)l);
    }
}

