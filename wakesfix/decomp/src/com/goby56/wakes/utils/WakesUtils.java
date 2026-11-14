/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1690
 *  net.minecraft.class_1922
 *  net.minecraft.class_1937
 *  net.minecraft.class_2338
 *  net.minecraft.class_2338$class_2339
 *  net.minecraft.class_238
 *  net.minecraft.class_2394
 *  net.minecraft.class_243
 *  net.minecraft.class_2561
 *  net.minecraft.class_3486
 *  net.minecraft.class_3532
 *  net.minecraft.class_3610
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_5250
 */
package com.goby56.wakes.utils;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import java.util.ArrayList;
import net.minecraft.class_1297;
import net.minecraft.class_1690;
import net.minecraft.class_1922;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_2394;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_3486;
import net.minecraft.class_3532;
import net.minecraft.class_3610;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_5250;

public class WakesUtils {
    public static void placeFallSplash(class_1297 entity) {
        WakeHandler instance = WakeHandler.getInstance();
        if (instance == null) {
            return;
        }
        for (WakeNode node : WakeNode.Factory.splashNodes(entity, ((ProducesWake)entity).producingHeight())) {
            instance.insert(node);
        }
    }

    public static void spawnPaddleSplashCloudParticle(class_1937 world, class_1690 boat) {
        for (int i = 0; i < 2; ++i) {
            double phase;
            if (!boat.method_7556(i) || !(0.19634954631328583 <= (phase = (double)boat.field_7704[i] % (Math.PI * 2))) || !(phase <= 1.178097277879715)) continue;
            class_243 rot = boat.method_5828(1.0f);
            double x = boat.method_23317() + (i == 1 ? -rot.field_1350 : rot.field_1350);
            double z = boat.method_23321() + (i == 1 ? rot.field_1352 : -rot.field_1352);
            class_243 pos = new class_243(x, (double)((ProducesWake)boat).producingHeight(), z);
            world.method_8406((class_2394)ModParticles.SPLASH_CLOUD, pos.field_1352, pos.field_1351, pos.field_1350, boat.method_18798().field_1352, 0.0, boat.method_18798().field_1350);
        }
    }

    public static void spawnSplashPlaneParticle(class_1937 world, class_1297 owner) {
        WithOwnerParticleType wake = ModParticles.SPLASH_PLANE.withOwner(owner);
        class_243 pos = owner.method_19538();
        world.method_8406((class_2394)wake, pos.field_1352, pos.field_1351, pos.field_1350, 0.0, 0.0, 0.0);
    }

    public static void placeWakeTrail(class_1297 entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler == null) {
            return;
        }
        ProducesWake producer = (ProducesWake)entity;
        double velocity = producer.getHorizontalVelocity();
        float height = producer.producingHeight();
        if (entity instanceof class_1690) {
            class_1690 boat = (class_1690)entity;
            for (WakeNode node : WakeNode.Factory.rowingNodes(boat, height)) {
                wakeHandler.insert(node);
            }
        }
        class_243 prevPos = producer.getPrevPos();
        class_243 currPos = new class_243(entity.method_23317(), (double)height, entity.method_23321());
        producer.setPrevPos(currPos);
        if (prevPos == null) {
            return;
        }
        for (WakeNode node : WakeNode.Factory.thickNodeTrail(prevPos.field_1352, prevPos.field_1350, currPos.field_1352, currPos.field_1350, height, WakesClient.CONFIG_INSTANCE.initialStrength, velocity, entity.method_17681())) {
            wakeHandler.insert(node);
        }
    }

    public static void bresenhamLine(int x1, int y1, int x2, int y2, ArrayList<Long> points) {
        int dy = y2 - y1;
        int dx = x2 - x1;
        if (dx == 0) {
            if (y2 < y1) {
                int temp = y1;
                y1 = y2;
                y2 = temp;
            }
            for (int y = y1; y < y2 + 1; ++y) {
                points.add(WakesUtils.posAsLong(x1, y));
            }
        } else {
            float k = (float)dy / (float)dx;
            int adjust = k >= 0.0f ? 1 : -1;
            int offset = 0;
            if (k <= 1.0f && k >= -1.0f) {
                int delta = Math.abs(dy) * 2;
                int threshold = Math.abs(dx);
                int thresholdInc = Math.abs(dx) * 2;
                int y = y1;
                if (x2 < x1) {
                    int temp = x1;
                    x1 = x2;
                    x2 = temp;
                    y = y2;
                }
                for (int x = x1; x < x2 + 1; ++x) {
                    points.add(WakesUtils.posAsLong(x, y));
                    if ((offset += delta) < threshold) continue;
                    y += adjust;
                    threshold += thresholdInc;
                }
            } else {
                int delta = Math.abs(dx) * 2;
                int threshold = Math.abs(dy);
                int thresholdInc = Math.abs(dy) * 2;
                int x = x1;
                if (y2 < y1) {
                    int temp = y1;
                    y1 = y2;
                    y2 = temp;
                }
                for (int y = y1; y < y2 + 1; ++y) {
                    points.add(WakesUtils.posAsLong(x, y));
                    if ((offset += delta) < threshold) continue;
                    x += adjust;
                    threshold += thresholdInc;
                }
            }
        }
    }

    public static long posAsLong(int x, int y) {
        int xs = x >> 31 & 1;
        int ys = y >> 31 & 1;
        long pos = (long)(x &= Integer.MAX_VALUE) << 32 | (long)(y &= Integer.MAX_VALUE);
        pos ^= ((long)(-xs) ^ pos) & Long.MIN_VALUE;
        pos ^= ((long)(-ys) ^ pos) & 0x80000000L;
        return pos;
    }

    public static int[] longAsPos(long pos) {
        return new int[]{(int)(pos >> 32), (int)pos};
    }

    public static class_5250 translatable(String category, String field) {
        return class_2561.method_43471((String)String.format("%s.%s.%s", category, "wakes", field));
    }

    public static int[] abgrInt2rgbaArr(int n) {
        int[] arr = new int[4];
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 8; ++j) {
                int n2 = i;
                arr[n2] = arr[n2] | (n >> i * 8 + j & 1) << 7 - j;
            }
        }
        return arr;
    }

    public static int rgbaArr2abgrInt(int[] arr) {
        int n = 0;
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 8; ++j) {
                n |= (arr[i] >> j & 1) << i * 8 + j;
            }
        }
        return n;
    }

    public static float getWaterLevel(class_1937 world, class_1297 entityInWater) {
        class_238 box = entityInWater.method_5829();
        return WakesUtils.getWaterLevel(world, class_3532.method_15357((double)box.field_1323), class_3532.method_15384((double)box.field_1320), class_3532.method_15357((double)box.field_1322), class_3532.method_15384((double)box.field_1325), class_3532.method_15357((double)box.field_1321), class_3532.method_15384((double)box.field_1324));
    }

    private static float getWaterLevel(class_1937 world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        class_2338.class_2339 blockPos = new class_2338.class_2339();
        block0: for (int y = minY; y < maxY; ++y) {
            float f = 0.0f;
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    blockPos.method_10103(x, y, z);
                    class_3610 fluidState = world.method_8316((class_2338)blockPos);
                    if (fluidState.method_15767(class_3486.field_15517)) {
                        f = Math.max(f, fluidState.method_15763((class_1922)world, (class_2338)blockPos));
                    }
                    if (f >= 1.0f) continue block0;
                }
            }
            if (!(f < 1.0f)) continue;
            return (float)blockPos.method_10264() + f;
        }
        return maxY + 1;
    }

    public static class_2338 vecToBlockPos(class_243 pos) {
        return new class_2338((int)Math.floor(pos.field_1352), (int)Math.floor(pos.field_1351), (int)Math.floor(pos.field_1350));
    }

    public static class_4587 getMatrixStackFromCamera(class_4184 camera, float tickDelta, class_243 prevPos, class_243 currPos) {
        class_243 cameraPos = camera.method_19326();
        float x = (float)(class_3532.method_16436((double)tickDelta, (double)prevPos.field_1352, (double)currPos.field_1352) - cameraPos.method_10216());
        float y = (float)(class_3532.method_16436((double)tickDelta, (double)prevPos.field_1351, (double)currPos.field_1351) - cameraPos.method_10214());
        float z = (float)(class_3532.method_16436((double)tickDelta, (double)prevPos.field_1350, (double)currPos.field_1350) - cameraPos.method_10215());
        class_4587 matrixStack = new class_4587();
        matrixStack.method_46416(x, y, z);
        return matrixStack;
    }
}

