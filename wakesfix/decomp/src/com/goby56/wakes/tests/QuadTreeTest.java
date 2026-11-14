/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.junit.jupiter.api.Assertions
 *  org.junit.jupiter.api.Test
 */
package com.goby56.wakes.tests;

import com.goby56.wakes.tests.GenericNode;
import com.goby56.wakes.utils.QuadTree;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QuadTreeTest {
    QuadTreeTest() {
    }

    @Test
    void pointInsideAABB() {
        QuadTree.AABB aabb = new QuadTree.AABB(0, 0, 1);
        Assertions.assertTrue((boolean)aabb.contains(0, 0));
        Assertions.assertTrue((boolean)aabb.contains(0, -1));
        Assertions.assertTrue((boolean)aabb.contains(1, -1));
        Assertions.assertTrue((boolean)aabb.contains(1, 0));
        Assertions.assertTrue((boolean)aabb.contains(1, 1));
        Assertions.assertTrue((boolean)aabb.contains(0, 1));
        Assertions.assertTrue((boolean)aabb.contains(-1, 1));
        Assertions.assertTrue((boolean)aabb.contains(-1, 0));
        Assertions.assertTrue((boolean)aabb.contains(-1, -1));
        Assertions.assertFalse((boolean)aabb.contains(0, -2));
        Assertions.assertFalse((boolean)aabb.contains(2, -2));
        Assertions.assertFalse((boolean)aabb.contains(2, 0));
        Assertions.assertFalse((boolean)aabb.contains(2, 2));
        Assertions.assertFalse((boolean)aabb.contains(0, 2));
        Assertions.assertFalse((boolean)aabb.contains(-2, 2));
        Assertions.assertFalse((boolean)aabb.contains(-2, 0));
        Assertions.assertFalse((boolean)aabb.contains(-2, -2));
    }

    @Test
    void AABBintersectsAABB() {
        QuadTree.AABB aabb1 = new QuadTree.AABB(-1, 0, 1);
        QuadTree.AABB aabb2 = new QuadTree.AABB(1, 0, 1);
        Assertions.assertTrue((boolean)aabb1.intersects(aabb2));
        Assertions.assertTrue((boolean)aabb2.intersects(aabb1));
        QuadTree.AABB aabb3 = new QuadTree.AABB(0, -1, 1);
        QuadTree.AABB aabb4 = new QuadTree.AABB(0, 1, 1);
        Assertions.assertTrue((boolean)aabb3.intersects(aabb4));
        Assertions.assertTrue((boolean)aabb4.intersects(aabb3));
        QuadTree.AABB aabb5 = new QuadTree.AABB(-2, 0, 1);
        QuadTree.AABB aabb6 = new QuadTree.AABB(2, 0, 1);
        Assertions.assertFalse((boolean)aabb5.intersects(aabb6));
        Assertions.assertFalse((boolean)aabb6.intersects(aabb5));
        QuadTree.AABB aabb7 = new QuadTree.AABB(0, -2, 1);
        QuadTree.AABB aabb8 = new QuadTree.AABB(0, 2, 1);
        Assertions.assertFalse((boolean)aabb7.intersects(aabb8));
        Assertions.assertFalse((boolean)aabb8.intersects(aabb7));
    }

    @Test
    void noduplicateInsertions() {
        QuadTree<GenericNode> tree = new QuadTree<GenericNode>(0, 0, 10000);
        tree.insert(new GenericNode(1, 1, 0.89f, 0));
        tree.insert(new GenericNode(1, 1, 62.888f, 15));
        tree.insert(new GenericNode(1, 1, -23.5f, 8));
        ArrayList nodes = new ArrayList();
        tree.query(new QuadTree.AABB(0, 0, 1), nodes);
        Assertions.assertEquals((int)1, (int)nodes.size());
    }
}

