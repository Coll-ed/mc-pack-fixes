/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3532
 *  org.junit.jupiter.api.Assertions
 *  org.junit.jupiter.api.Test
 */
package com.goby56.wakes.tests;

import net.minecraft.class_3532;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResolutionTest {
    @Test
    void logFunctions() {
        Assertions.assertEquals((int)class_3532.method_15351((int)4), (int)2);
        Assertions.assertEquals((int)class_3532.method_15351((int)8), (int)3);
        Assertions.assertEquals((int)class_3532.method_15351((int)16), (int)4);
        Assertions.assertEquals((int)class_3532.method_15351((int)32), (int)5);
        Assertions.assertEquals((int)class_3532.method_15351((int)64), (int)6);
        Assertions.assertEquals((int)class_3532.method_15351((int)128), (int)7);
        Assertions.assertEquals((int)class_3532.method_15342((int)4), (int)2);
        Assertions.assertEquals((int)class_3532.method_15342((int)8), (int)3);
        Assertions.assertEquals((int)class_3532.method_15342((int)16), (int)4);
        Assertions.assertEquals((int)class_3532.method_15342((int)32), (int)5);
        Assertions.assertEquals((int)class_3532.method_15342((int)64), (int)6);
        Assertions.assertEquals((int)class_3532.method_15342((int)128), (int)7);
    }
}

