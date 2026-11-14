package com.yglcs.stuckarrow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.phys.Vec3;

/**
 * 命中记录表：每次箭命中生物，记录「相对身体的偏移 + 弹道方向」。
 *
 * <p>为什么不用 AITK 自己的数据：AITK 只统计“身上有几支箭”（`numStuck`），
 * 位置用 `getRandomCube` + `nextFloat×3` 随机生成，朝向也是随机 —— 它从来没有命中点。
 * 所以命中点必须由我们在命中那一刻采集。
 *
 * <p>数据只在同一进程内使用（单人/整合服务器）：服务端线程写入、客户端线程读取。
 * 多人服客户端拿不到，会自然回退到 AITK 原行为。
 */
public final class StuckArrows {

   /** 一支插在生物身上的箭。 */
   public record Stuck(Vec3 offset, float yaw, float pitch, long gameTime) {
   }

   /** 每支箭保留的存活时间（tick）。20 tick = 1 秒；600 tick = 30 秒。 */
   public static final int LIFETIME_TICKS = 600;

   /** 每个实体最多记录多少支（防止箭雨刷爆内存）。 */
   public static final int MAX_PER_ENTITY = 32;

   private static final Map<Integer, List<Stuck>> BY_ENTITY = new ConcurrentHashMap<>();

   private StuckArrows() {
   }

   /** 记录一支箭。 */
   public static void add(int entityId, Vec3 offset, float yaw, float pitch, long gameTime) {
      List<Stuck> list = BY_ENTITY.computeIfAbsent(entityId, id -> new ArrayList<>());
      synchronized (list) {
         list.add(new Stuck(offset, yaw, pitch, gameTime));
         while (list.size() > MAX_PER_ENTITY) {
            list.remove(0);
         }
      }
   }

   /** 取某实体身上的箭（已过期的会被剔除）。 */
   public static List<Stuck> get(int entityId, long now) {
      List<Stuck> list = BY_ENTITY.get(entityId);
      if (list == null) {
         return List.of();
      }
      synchronized (list) {
         list.removeIf(stuck -> now - stuck.gameTime() > LIFETIME_TICKS);
         return List.copyOf(list);
      }
   }

   /** 世界按维度切换时清空（实体 id 会复用）。 */
   public static void clear() {
      BY_ENTITY.clear();
   }
}
