/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1690
 *  net.minecraft.class_2338
 *  net.minecraft.class_238
 *  net.minecraft.class_243
 *  net.minecraft.class_310
 *  net.minecraft.class_3486
 *  net.minecraft.class_3610
 */
package com.goby56.wakes.utils;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.utils.Age;
import com.goby56.wakes.utils.Position;
import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakesUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.class_1297;
import net.minecraft.class_1690;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3486;
import net.minecraft.class_3610;

public class WakeNode
implements Position<WakeNode>,
Age<WakeNode> {
    private final WakeHandler wakeHandler = WakeHandler.getInstance();
    public final int res;
    private static float alpha;
    public float[][][] u;
    public float[][] initialValues;
    public final int x;
    public final int z;
    public float height;
    public WakeNode NORTH = null;
    public WakeNode EAST = null;
    public WakeNode SOUTH = null;
    public WakeNode WEST = null;
    public final int maxAge = 30;
    public int age = 0;
    private boolean dead = false;
    public float t = 0.0f;
    public int floodLevel;

    public WakeNode(class_243 position, int initialStrength) {
        this.res = this.wakeHandler.resolution.res;
        this.initValues();
        this.x = (int)Math.floor(position.field_1352);
        this.z = (int)Math.floor(position.field_1350);
        this.height = (float)position.method_10214();
        int sx = (int)Math.floor((double)this.res * (position.field_1352 - (double)this.x));
        int sz = (int)Math.floor((double)this.res * (position.field_1350 - (double)this.z));
        for (int z = -1; z < 2; ++z) {
            for (int x = -1; x < 2; ++x) {
                this.u[0][sz + 1 + z][sx + 1 + x] = initialStrength;
            }
        }
        this.floodLevel = WakesClient.CONFIG_INSTANCE.floodFillDistance;
    }

    private WakeNode(int x, int z, float height, int floodLevel) {
        this.res = this.wakeHandler.resolution.res;
        this.initValues();
        this.x = x;
        this.z = z;
        this.height = height;
        this.floodLevel = floodLevel;
    }

    private WakeNode(long pos, float height) {
        this.res = this.wakeHandler.resolution.res;
        this.initValues();
        int[] xz = WakesUtils.longAsPos(pos);
        this.x = xz[0];
        this.z = xz[1];
        this.height = height;
        this.floodLevel = WakesClient.CONFIG_INSTANCE.floodFillDistance;
    }

    private void initValues() {
        this.u = new float[3][this.res + 2][this.res + 2];
        this.initialValues = new float[this.res + 2][this.res + 2];
    }

    public void setInitialValue(long pos, int val) {
        float resFactor = (float)this.res / 16.0f;
        int[] xz = WakesUtils.longAsPos(pos);
        if (xz[0] < 0) {
            xz[0] = xz[0] + this.res;
        }
        if (xz[1] < 0) {
            xz[1] = xz[1] + this.res;
        }
        for (int i = -1; i < 2; ++i) {
            for (int j = -1; j < 2; ++j) {
                this.initialValues[xz[1] + i + 1][xz[0] + j + 1] = (float)val * resFactor;
            }
        }
    }

    public static void calculateAlpha() {
        float time = 20.0f;
        alpha = (float)Math.pow(WakesClient.CONFIG_INSTANCE.waveSpeed * 16.0f / time, 2.0);
    }

    @Override
    public void tick() {
        int x;
        int z;
        if (this.isDead()) {
            return;
        }
        if (this.age++ >= this.maxAge || this.res != this.wakeHandler.resolution.res) {
            this.markDead();
            return;
        }
        float f = this.age;
        Objects.requireNonNull(this);
        this.t = f / 30.0f;
        for (int i = 2; i >= 1; --i) {
            if (this.NORTH != null) {
                this.u[i][0] = this.NORTH.u[i][this.res];
            }
            if (this.SOUTH != null) {
                this.u[i][this.res + 1] = this.SOUTH.u[i][1];
            }
            for (int z2 = 0; z2 < this.res + 2 && (this.EAST != null || this.WEST != null); ++z2) {
                if (this.EAST != null) {
                    this.u[i][z2][this.res + 1] = this.EAST.u[i][z2][1];
                }
                if (this.WEST == null) continue;
                this.u[i][z2][0] = this.WEST.u[i][z2][this.res];
            }
        }
        for (z = 1; z < this.res + 1; ++z) {
            for (x = 1; x < this.res + 1; ++x) {
                float[] fArray = this.u[0][z];
                int n = x;
                fArray[n] = fArray[n] + this.initialValues[z][x];
                this.initialValues[z][x] = 0.0f;
                this.u[2][z][x] = this.u[1][z][x];
                this.u[1][z][x] = this.u[0][z][x];
            }
        }
        for (z = 1; z < this.res + 1; ++z) {
            x = 1;
            while (x < this.res + 1) {
                this.u[0][z][x] = WakesClient.CONFIG_INSTANCE.use9PointStencil ? (float)((double)alpha * (0.5 * (double)this.u[1][z - 1][x] + 0.25 * (double)this.u[1][z - 1][x + 1] + 0.5 * (double)this.u[1][z][x + 1] + 0.25 * (double)this.u[1][z + 1][x + 1] + 0.5 * (double)this.u[1][z + 1][x] + 0.25 * (double)this.u[1][z + 1][x - 1] + 0.5 * (double)this.u[1][z][x - 1] + 0.25 * (double)this.u[1][z - 1][x - 1] - (double)(3.0f * this.u[1][z][x])) + (double)(2.0f * this.u[1][z][x]) - (double)this.u[2][z][x]) : alpha * (this.u[1][z - 1][x] + this.u[1][z + 1][x] + this.u[1][z][x - 1] + this.u[1][z][x + 1] - 4.0f * this.u[1][z][x]) + 2.0f * this.u[1][z][x] - this.u[2][z][x];
                float[] fArray = this.u[0][z];
                int n = x++;
                fArray[n] = (float)((double)fArray[n] * (Math.log(10.0f * WakesClient.CONFIG_INSTANCE.waveDecay + 10.0f) / Math.log(20.0)));
            }
        }
        if (this.floodLevel > 0 && this.age > WakesClient.CONFIG_INSTANCE.ticksBeforeFill) {
            if (this.NORTH == null) {
                this.wakeHandler.insert(new WakeNode(this.x, this.z - 1, this.height, this.floodLevel - 1));
            } else {
                this.NORTH.updateFloodLevel(this.floodLevel - 1);
            }
            if (this.EAST == null) {
                this.wakeHandler.insert(new WakeNode(this.x + 1, this.z, this.height, this.floodLevel - 1));
            } else {
                this.EAST.updateFloodLevel(this.floodLevel - 1);
            }
            if (this.SOUTH == null) {
                this.wakeHandler.insert(new WakeNode(this.x, this.z + 1, this.height, this.floodLevel - 1));
            } else {
                this.SOUTH.updateFloodLevel(this.floodLevel - 1);
            }
            if (this.WEST == null) {
                this.wakeHandler.insert(new WakeNode(this.x - 1, this.z, this.height, this.floodLevel - 1));
            } else {
                this.WEST.updateFloodLevel(this.floodLevel - 1);
            }
            this.floodLevel = 0;
        }
    }

    @Override
    public void updateAdjacency(WakeNode node) {
        if (node.x == this.x && node.z == this.z - 1) {
            this.NORTH = node;
            node.SOUTH = this;
            return;
        }
        if (node.x == this.x + 1 && node.z == this.z) {
            this.EAST = node;
            node.WEST = this;
            return;
        }
        if (node.x == this.x && node.z == this.z + 1) {
            this.SOUTH = node;
            node.NORTH = this;
            return;
        }
        if (node.x == this.x - 1 && node.z == this.z) {
            this.WEST = node;
            node.EAST = this;
        }
    }

    public void updateFloodLevel(int newLevel) {
        this.age = 0;
        if (newLevel > this.floodLevel) {
            this.floodLevel = newLevel;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        WakeNode wakeNode = (WakeNode)o;
        return this.x == wakeNode.x && this.z == wakeNode.z;
    }

    public int hashCode() {
        return Objects.hash(this.x, this.z);
    }

    public String toString() {
        return "WakeNode{x=" + this.x + ", z=" + this.z + ", height=" + this.height + "}";
    }

    @Override
    public class_238 toBox() {
        return new class_238((double)this.x, Math.floor(this.height), (double)this.z, (double)(this.x + 1), Math.ceil(this.height), (double)(this.z + 1));
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
    public void revive(WakeNode node) {
        this.age = 0;
        this.floodLevel = WakesClient.CONFIG_INSTANCE.floodFillDistance;
        this.initialValues = node.initialValues;
    }

    @Override
    public void markDead() {
        this.dead = true;
    }

    @Override
    public boolean isDead() {
        return this.dead;
    }

    @Override
    public boolean inValidPos() {
        class_3610 fluidState = class_310.method_1551().field_1687.method_8316(this.blockPos());
        class_3610 fluidStateAbove = class_310.method_1551().field_1687.method_8316(this.blockPos().method_10084());
        if (fluidState.method_15767(class_3486.field_15517) && !fluidStateAbove.method_15767(class_3486.field_15517)) {
            return fluidState.method_15771() || WakesClient.CONFIG_INSTANCE.wakesInRunningWater;
        }
        return false;
    }

    public class_243 getPos() {
        return new class_243((double)this.x, (double)this.height, (double)this.z);
    }

    public class_2338 blockPos() {
        return new class_2338(this.x, (int)Math.floor(this.height), this.z);
    }

    public static class Factory {
        public static Set<WakeNode> splashNodes(class_1297 entity, float height) {
            int res = WakeHandler.getInstance().resolution.res;
            int w = (int)(0.8 * (double)entity.method_17681() * (double)res / 2.0);
            int x = (int)(entity.method_23317() * (double)res);
            int z = (int)(entity.method_23321() * (double)res);
            ArrayList<Long> pixelsAffected = new ArrayList<Long>();
            for (int i = -w; i < w; ++i) {
                for (int j = -w; j < w; ++j) {
                    if (i * i + j * j >= w * w) continue;
                    pixelsAffected.add(WakesUtils.posAsLong(x + i, z + j));
                }
            }
            return Factory.pixelsToNodes(pixelsAffected, height, WakesClient.CONFIG_INSTANCE.splashStrength, Math.abs(entity.method_18798().field_1351));
        }

        public static Set<WakeNode> rowingNodes(class_1690 boat, float height) {
            HashSet<WakeNode> nodesAffected = new HashSet<WakeNode>();
            double velocity = Math.max(WakesClient.CONFIG_INSTANCE.minimumProducerVelocity, boat.method_18798().method_37267());
            for (int i = 0; i < 2; ++i) {
                double phase;
                if (!boat.method_7556(i) || !(0.19634954631328583 <= (phase = (double)boat.field_7704[i] % (Math.PI * 2))) || !(phase <= 1.178097277879715)) continue;
                class_243 rot = boat.method_5828(1.0f);
                double x = boat.method_23317() + (i == 1 ? -rot.field_1350 : rot.field_1350);
                double z = boat.method_23321() + (i == 1 ? rot.field_1352 : -rot.field_1352);
                class_243 paddlePos = new class_243(x, (double)height, z);
                class_243 dir = class_243.method_1030((float)0.0f, (float)boat.method_36454()).method_1021(velocity);
                class_243 from = paddlePos;
                class_243 to = paddlePos.method_1019(dir.method_1021(2.0));
                nodesAffected.addAll(Factory.nodeTrail(from.field_1352, from.field_1350, to.field_1352, to.field_1350, height, WakesClient.CONFIG_INSTANCE.paddleStrength, velocity));
            }
            return nodesAffected;
        }

        public static Set<WakeNode> nodeTrail(double fromX, double fromZ, double toX, double toZ, float height, float waveStrength, double velocity) {
            int res = WakeHandler.getInstance().resolution.res;
            int x1 = (int)(fromX * (double)res);
            int z1 = (int)(fromZ * (double)res);
            int x2 = (int)(toX * (double)res);
            int z2 = (int)(toZ * (double)res);
            ArrayList<Long> pixelsAffected = new ArrayList<Long>();
            WakesUtils.bresenhamLine(x1, z1, x2, z2, pixelsAffected);
            return Factory.pixelsToNodes(pixelsAffected, height, waveStrength, velocity);
        }

        public static Set<WakeNode> thickNodeTrail(double fromX, double fromZ, double toX, double toZ, float height, float waveStrength, double velocity, float width) {
            int res = WakeHandler.getInstance().resolution.res;
            int x1 = (int)(fromX * (double)res);
            int z1 = (int)(fromZ * (double)res);
            int x2 = (int)(toX * (double)res);
            int z2 = (int)(toZ * (double)res);
            int w = (int)(0.8 * (double)width * (double)res / 2.0);
            float len = (float)Math.sqrt(Math.pow(z1 - z2, 2.0) + Math.pow(x2 - x1, 2.0));
            float nx = (float)(z1 - z2) / len;
            float nz = (float)(x2 - x1) / len;
            ArrayList<Long> pixelsAffected = new ArrayList<Long>();
            for (int i = -w; i < w; ++i) {
                WakesUtils.bresenhamLine((int)((float)x1 + nx * (float)i), (int)((float)z1 + nz * (float)i), (int)((float)x2 + nx * (float)i), (int)((float)z2 + nz * (float)i), pixelsAffected);
            }
            return Factory.pixelsToNodes(pixelsAffected, height, waveStrength, velocity);
        }

        public static Set<WakeNode> nodeLine(double x, double z, float height, float waveStrength, class_243 velocity, float width) {
            int res = WakeHandler.getInstance().resolution.res;
            class_243 dir = velocity.method_1029();
            double nx = -dir.field_1350;
            double nz = dir.field_1352;
            int w = (int)(0.8 * (double)width * (double)res / 2.0);
            int x1 = (int)(x * (double)res - nx * (double)w);
            int z1 = (int)(z * (double)res - nz * (double)w);
            int x2 = (int)(x * (double)res + nx * (double)w);
            int z2 = (int)(z * (double)res + nz * (double)w);
            ArrayList<Long> pixelsAffected = new ArrayList<Long>();
            WakesUtils.bresenhamLine(x1, z1, x2, z2, pixelsAffected);
            return Factory.pixelsToNodes(pixelsAffected, height, waveStrength, velocity.method_37267());
        }

        private static Set<WakeNode> pixelsToNodes(ArrayList<Long> pixelsAffected, float height, float waveStrength, double velocity) {
            WakesConfig.Resolution res = WakeHandler.getInstance().resolution;
            HashMap pixelsInNodes = new HashMap();
            for (Long pixel : pixelsAffected) {
                int[] pos = WakesUtils.longAsPos(pixel);
                long k = WakesUtils.posAsLong(pos[0] >> res.power, pos[1] >> res.power);
                pos[0] = pos[0] % res.res;
                pos[1] = pos[1] % res.res;
                long v = WakesUtils.posAsLong(pos[0], pos[1]);
                if (pixelsInNodes.containsKey(k)) {
                    ((HashSet)pixelsInNodes.get(k)).add(v);
                    continue;
                }
                HashSet<Long> set = new HashSet<Long>();
                set.add(v);
                pixelsInNodes.put(k, set);
            }
            HashSet<WakeNode> nodesAffected = new HashSet<WakeNode>();
            for (Long nodePos : pixelsInNodes.keySet()) {
                WakeNode node = new WakeNode(nodePos, height);
                for (Long subPos : (HashSet)pixelsInNodes.get(nodePos)) {
                    node.setInitialValue(subPos, (int)((double)waveStrength * velocity));
                }
                nodesAffected.add(node);
            }
            return nodesAffected;
        }
    }
}

