/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_238
 *  net.minecraft.class_4604
 *  org.apache.commons.lang3.NotImplementedException
 */
package com.goby56.wakes.utils;

import com.goby56.wakes.utils.Age;
import com.goby56.wakes.utils.Position;
import java.util.ArrayList;
import java.util.Stack;
import net.minecraft.class_238;
import net.minecraft.class_4604;
import org.apache.commons.lang3.NotImplementedException;

public class QuadTree<T extends Position<T> & Age<T>> {
    private static final int CAPACITY = 64;
    private final QuadTree<T> ROOT;
    private QuadTree<T> NE;
    private QuadTree<T> NW;
    private QuadTree<T> SW;
    private QuadTree<T> SE;
    private final AABB bounds;
    private final int depth;
    private final ArrayList<T> nodes = new ArrayList(64);

    public QuadTree(int x, int z, int width) {
        this(x, z, width, 0, null);
    }

    private QuadTree(int x, int z, int width, int depth, QuadTree<T> root) {
        this.bounds = new AABB(x, z, width);
        this.depth = depth;
        this.ROOT = root == null ? this : root;
    }

    public void tick() {
        Stack<Integer> indicesToDelete = new Stack<Integer>();
        int i = 0;
        for (Position node : this.nodes) {
            if (!((Age)((Object)node)).isDead()) {
                ((Age)((Object)node)).tick();
            } else {
                indicesToDelete.add(i);
            }
            ++i;
        }
        for (i = 0; i < indicesToDelete.size(); ++i) {
            this.nodes.remove((Integer)indicesToDelete.pop());
        }
        if (this.NE == null) {
            return;
        }
        this.NE.tick();
        this.NW.tick();
        this.SW.tick();
        this.SE.tick();
    }

    private void tryAdd(T node) {
        if (!node.inValidPos()) {
            return;
        }
        ArrayList<Position> nodesNearby = new ArrayList<Position>();
        this.ROOT.query(new AABB(node.x(), node.z(), 1), nodesNearby);
        boolean nodeAlreadyExists = false;
        for (Position otherNode : nodesNearby) {
            if (!node.equals(otherNode)) continue;
            nodeAlreadyExists = true;
            ((Age)((Object)otherNode)).revive(node);
        }
        if (!nodeAlreadyExists) {
            this.nodes.add(node);
            nodesNearby.forEach(x$0 -> node.updateAdjacency(x$0));
        }
    }

    public boolean insert(T node) {
        if (!this.bounds.contains(node.x(), node.z())) {
            return false;
        }
        if (this.nodes.size() < 64) {
            this.tryAdd(node);
            return true;
        }
        if (this.NE == null) {
            this.subdivide();
        }
        if (this.NE.insert(node)) {
            return true;
        }
        if (this.NW.insert(node)) {
            return true;
        }
        if (this.SW.insert(node)) {
            return true;
        }
        return this.SE.insert(node);
    }

    public void query(AABB range, ArrayList<T> output) {
        if (!this.bounds.intersects(range)) {
            return;
        }
        for (Position node : this.nodes) {
            if (!range.contains(node.x(), node.z())) continue;
            output.add(node);
        }
        if (this.NE == null) {
            return;
        }
        this.NE.query(range, output);
        this.NW.query(range, output);
        this.SW.query(range, output);
        this.SE.query(range, output);
    }

    public void query(Circle range, ArrayList<T> output) {
        if (!this.bounds.intersects(range)) {
            return;
        }
        for (Position node : this.nodes) {
            if (!range.contains(node.x(), node.z())) continue;
            output.add(node);
        }
        if (this.NE == null) {
            return;
        }
        this.NE.query(range, output);
        this.NW.query(range, output);
        this.SW.query(range, output);
        this.SE.query(range, output);
    }

    public void query(class_4604 frustum, int y, ArrayList<T> output) {
        if (!frustum.method_23093(this.bounds.toBox(y))) {
            return;
        }
        for (Position node : this.nodes) {
            if (!frustum.method_23093(node.toBox())) continue;
            output.add(node);
        }
        if (this.NE == null) {
            return;
        }
        this.NE.query(frustum, y, output);
        this.NW.query(frustum, y, output);
        this.SW.query(frustum, y, output);
        this.SE.query(frustum, y, output);
    }

    private void subdivide() {
        int x = this.bounds.x;
        int z = this.bounds.z;
        int w = this.bounds.width >> 1;
        this.NE = new QuadTree<T>(x + w, z - w, w, this.depth + 1, this.ROOT);
        this.NW = new QuadTree<T>(x - w, z - w, w, this.depth + 1, this.ROOT);
        this.SW = new QuadTree<T>(x - w, z + w, w, this.depth + 1, this.ROOT);
        this.SE = new QuadTree<T>(x + w, z + w, w, this.depth + 1, this.ROOT);
    }

    public void prune() {
        this.nodes.forEach(rec$ -> ((Age)rec$).markDead());
        this.nodes.clear();
        if (this.NE == null) {
            return;
        }
        this.NE.prune();
        this.NW.prune();
        this.SW.prune();
        this.SE.prune();
    }

    private void distribute() {
        throw new NotImplementedException();
    }

    public record AABB(int x, int z, int width) {
        public boolean contains(int x, int z) {
            return this.x - this.width <= x && x <= this.x + this.width && this.z - this.width <= z && z <= this.z + this.width;
        }

        public boolean intersects(AABB other) {
            return this.x - this.width <= other.x + other.width && this.x + this.width >= other.x - other.width && this.z - this.width <= other.z + other.width && this.z + this.width >= other.z - other.width;
        }

        public boolean intersects(Circle other) {
            if (this.contains(other.x, other.z)) {
                return true;
            }
            return this.x - this.width <= other.x + other.radius && this.x + this.width >= other.x - other.radius && this.z - this.width <= other.z + other.radius && this.z + this.width >= other.z - other.radius;
        }

        public class_238 toBox(int y) {
            return new class_238((double)(this.x - this.width), (double)y, (double)(this.z - this.width), (double)(this.x + this.width), (double)(y + 1), (double)(this.z + this.width));
        }
    }

    public record Circle(int x, int z, int radius) {
        public boolean contains(int x, int z) {
            return Math.sqrt(Math.pow(this.x - x, 2.0) + Math.pow(this.z - z, 2.0)) <= (double)this.radius;
        }
    }
}

