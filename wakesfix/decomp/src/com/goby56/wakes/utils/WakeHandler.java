/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1937
 *  net.minecraft.class_310
 *  net.minecraft.class_4604
 *  net.minecraft.class_638
 */
package com.goby56.wakes.utils;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.utils.QuadTree;
import com.goby56.wakes.utils.QueueSet;
import com.goby56.wakes.utils.WakeNode;
import java.util.ArrayList;
import java.util.Queue;
import net.minecraft.class_1937;
import net.minecraft.class_310;
import net.minecraft.class_4604;
import net.minecraft.class_638;

public class WakeHandler {
    public final int MAX_QUERY_RANGE = 10;
    private static WakeHandler INSTANCE;
    public class_1937 world;
    private final ArrayList<QuadTree<WakeNode>> trees;
    private final ArrayList<Queue<WakeNode>> toBeInserted;
    public boolean resetScheduled = false;
    public WakesConfig.Resolution resolution;
    private WakesConfig.Resolution newResolution;
    private final int minY;
    private final int maxY;
    public int glWakeTexId;
    public long wakeImgPtr;
    public int glFoamTexId;
    public long foamImgPtr;

    private WakeHandler(class_638 world) {
        this.resolution = WakesClient.CONFIG_INSTANCE.wakeResolution;
        this.newResolution = null;
        this.glWakeTexId = -1;
        this.wakeImgPtr = -1L;
        this.glFoamTexId = -1;
        this.foamImgPtr = -1L;
        this.world = world;
        WakeNode.calculateAlpha();
        this.minY = world.method_31607();
        this.maxY = world.method_31600();
        int worldHeight = this.maxY - this.minY;
        this.trees = new ArrayList(worldHeight);
        this.toBeInserted = new ArrayList(worldHeight);
        for (int i = 0; i < worldHeight; ++i) {
            this.trees.add(null);
            this.toBeInserted.add(new QueueSet());
        }
    }

    public static WakeHandler getInstance() {
        if (INSTANCE == null) {
            if (class_310.method_1551().field_1687 == null) {
                return null;
            }
            INSTANCE = new WakeHandler(class_310.method_1551().field_1687);
        }
        return INSTANCE;
    }

    public void tick() {
        for (int i = 0; i < this.maxY - this.minY; ++i) {
            if (this.resetScheduled) {
                this.toBeInserted.get(i).clear();
                continue;
            }
            QuadTree<WakeNode> tree = this.trees.get(i);
            if (tree == null) continue;
            tree.tick();
            Queue<WakeNode> pendingNodes = this.toBeInserted.get(i);
            while (pendingNodes.peek() != null) {
                tree.insert(pendingNodes.poll());
            }
        }
        if (this.resetScheduled) {
            this.changeResolution();
        }
    }

    public void insert(WakeNode node) {
        if (this.resetScheduled) {
            return;
        }
        int i = this.getArrayIndex((int)node.height);
        if (i < 0) {
            return;
        }
        if (this.trees.get(i) == null) {
            this.trees.add(i, new QuadTree(0, 0, 30000000));
        }
        this.toBeInserted.get(i).add(node);
    }

    public ArrayList<WakeNode> getVisible(class_4604 frustum) {
        ArrayList<WakeNode> foundNodes = new ArrayList<WakeNode>();
        for (int i = 0; i < this.maxY - this.minY; ++i) {
            if (this.trees.get(i) == null) continue;
            this.trees.get(i).query(frustum, i + this.minY, foundNodes);
        }
        return foundNodes;
    }

    public ArrayList<WakeNode> getNearby(int x, int y, int z) {
        ArrayList<WakeNode> foundNodes = new ArrayList<WakeNode>();
        int i = this.getArrayIndex(y);
        if (i < 0) {
            return foundNodes;
        }
        if (this.trees.get(i) == null) {
            return foundNodes;
        }
        QuadTree.Circle range = new QuadTree.Circle(x, z, this.MAX_QUERY_RANGE);
        this.trees.get(i).query(range, foundNodes);
        return foundNodes;
    }

    private int getArrayIndex(int height) {
        if (height < this.minY || height > this.maxY) {
            return -1;
        }
        return height + Math.abs(this.minY);
    }

    public static void scheduleResolutionChange(WakesConfig.Resolution newRes) {
        WakesClient.CONFIG_INSTANCE.wakeResolution = newRes;
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler == null) {
            return;
        }
        wakeHandler.resetScheduled = true;
        wakeHandler.newResolution = newRes;
    }

    private void changeResolution() {
        this.reset();
        this.glWakeTexId = -1;
        this.wakeImgPtr = -1L;
        this.glFoamTexId = -1;
        this.foamImgPtr = -1L;
        this.resolution = this.newResolution;
        this.resetScheduled = false;
        this.newResolution = null;
    }

    private void reset() {
        for (int i = 0; i < this.maxY - this.minY; ++i) {
            QuadTree<WakeNode> tree = this.trees.get(i);
            if (tree == null) continue;
            tree.prune();
        }
    }

    public static class WorldNotFoundException
    extends Exception {
        public WorldNotFoundException() {
            super("WakeHandler singleton was accessed too early! Player needs to be in a world.");
        }
    }
}

