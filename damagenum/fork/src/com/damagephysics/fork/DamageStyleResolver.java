package com.damagephysics.fork;

import com.damagephysics.config.DamagePhysicsConfig;

/**
 * 伤害数字的「三层流水线」。执行顺序固定：
 *
 * <pre>
 * 判定层  family = classifyFamily(...)          // 决定进入哪个族
 *   ↓
 * 底色层  int fill    = fillStage(ctx)          // 必出颜色
 *   ↓
 * 描边层  int outline = outlineStage(ctx)       // 0 = 跳过（不描边）
 *   ↓
 * 缩放层  Scaling s   = scalingStage(ctx)       // 可选参与冲击/距离缩放
 *   ↓
 * 渲染层  DamageNumberRenderer                    // 只消费上面三个结果
 * </pre>
 *
 * 每一层都是独立的 {@code switch (family)}，只读配置，互不依赖：
 * 想测某族某层，改那一层的那个 case 或那几个配置键即可，不会牵动其它族。
 *
 * 以中毒为例：
 * <ul>
 *   <li>判定层 → DEBUFF（typeId 命中 poison 关键字）</li>
 *   <li>底色层 → typeColorMap 命中 poison → 深绿</li>
 *   <li>描边层 → 跳过（debuffOutline=false）</li>
 *   <li>缩放层 → 跳过（impact/distance 皆 false），改用 debuffFixedScale</li>
 * </ul>
 */
public final class DamageStyleResolver {

   /**
    * 底色层结果：普通颜色，或者“彩虹”（彩虹没有固定色，由渲染层按时间取色）。
    * 这样「if 逐步变红 … elif 彩虹」就是底色层内部的分支，不需要层外再判一次。
    */
   public record Fill(int color, boolean rainbow) {
      public static Fill of(int color) {
         return new Fill(color, false);
      }

      public static final Fill RAINBOW = new Fill(0, true);
   }

   /** 缩放层结果：是否参与冲击缩放、是否参与距离/深度缩放。 */
   public record Scaling(boolean impact, boolean distance) {
      /** 完全跳过缩放（固定字号）。 */
      public static final Scaling SKIP = new Scaling(false, false);
   }

   /** 三层计算所需的只读上下文。 */
   public record Context(
      DamageFamily family,
      DamageCategory category,
      String typeId,
      float impactRatio,
      float scaleRatio,
      float colorRatio,
      boolean crit,
      boolean enchanted,
      boolean heal,
      boolean rainbowHit
   ) {
   }

   private DamageStyleResolver() {
   }

   private static final int COLOR_WHITE = 0xFFFFFFFF;
   /** 描边层返回该值表示「跳过」。 */
   public static final int OUTLINE_SKIP = 0;

   // ────────────────────────── 底色层 ──────────────────────────

   /**
    * 底色层：按判定层输出的集合分派，一定产出颜色（或彩虹）。
    *
    * <p>普通攻击的写法就是用户要的口径：
    * <pre>
    * if (达成本族彩虹条件) → 彩虹
    * elif (冲击比例)        → 白→红逐步变红
    * </pre>
    * 其它集合里的 if/else 也是同一形态，顺序固定：先特例，再常规。
    */
   public static Fill fillStage(Context ctx) {
      return switch (ctx.family()) {
         case DEBUFF -> {
            int c = ForkStyle.colorForTypeId(ctx.typeId());
            yield Fill.of(c != 0 ? c
               : ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.DEBUFF_FALLBACK_COLOR.get(), COLOR_WHITE));
         }
         case SPELL -> {
            // [法术] 底色：固定紫（spellFixedColor）。不再看稀有度/学派，颜色恒定。
            yield Fill.of(ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.SPELL_FIXED_COLOR.get(), COLOR_WHITE));
         }
         case NORMAL -> {
            // if 彩虹 … elif 变红
            if (ctx.rainbowHit() && rainbowAllowed(ctx)) {
               yield Fill.RAINBOW;
            }
            yield Fill.of(ForkStyle.impactColor(ctx.colorRatio()));
         }
         case PROJECTILE, GUN -> Fill.of(ForkStyle.impactColor(ctx.colorRatio()));
         case HEAL -> Fill.of(ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.HEAL_COLOR.get(), COLOR_WHITE));
      };
   }

   // ────────────────────────── 描边层 ──────────────────────────

   /** 描边层：按集合分派，返回 {@link #OUTLINE_SKIP} 表示该集合跳过描边。 */
   public static int outlineStage(Context ctx) {
      return switch (ctx.family()) {
         case DEBUFF -> (Boolean)DamagePhysicsConfig.DEBUFF_OUTLINE.get()
            ? ForkStyle.parseHexOrDefault(ForkStyle.outlineHexFor(ctx.category()), OUTLINE_SKIP)
            : OUTLINE_SKIP;
         case SPELL -> (Boolean)DamagePhysicsConfig.FAMILY_SPELL_OUTLINE.get()
            ? ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.SPELL_OUTLINE_COLOR.get(), OUTLINE_SKIP)
            : OUTLINE_SKIP;
         case PROJECTILE -> (Boolean)DamagePhysicsConfig.FAMILY_PROJECTILE_OUTLINE.get()
            ? ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.PROJECTILE_OUTLINE_COLOR.get(), OUTLINE_SKIP)
            : OUTLINE_SKIP;
         case GUN -> OUTLINE_SKIP;
         case HEAL -> (Boolean)DamagePhysicsConfig.FAMILY_HEAL_OUTLINE.get()
            ? ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.HEAL_OUTLINE_COLOR.get(), OUTLINE_SKIP)
            : OUTLINE_SKIP;
         case NORMAL -> (Boolean)DamagePhysicsConfig.FAMILY_NORMAL_OUTLINE_WHEN_ENCHANTED.get() && ctx.enchanted()
            ? ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.ENCHANT_OUTLINE_COLOR.get(), OUTLINE_SKIP)
            : OUTLINE_SKIP;
      };
   }

   // ────────────────────────── 缩放层 ──────────────────────────

   /** 缩放层：按集合分派，返回是否参与冲击/距离缩放（不参与则用固定字号）。 */
   public static Scaling scalingStage(Context ctx) {
      return switch (ctx.family()) {
         case DEBUFF -> new Scaling(
            (Boolean)DamagePhysicsConfig.FAMILY_DEBUFF_USES_IMPACT.get(),
            (Boolean)DamagePhysicsConfig.FAMILY_DEBUFF_USES_DISTANCE.get());
         case SPELL -> new Scaling(
            (Boolean)DamagePhysicsConfig.FAMILY_SPELL_USES_IMPACT.get(),
            (Boolean)DamagePhysicsConfig.FAMILY_SPELL_USES_DISTANCE.get());
         case PROJECTILE -> new Scaling(
            (Boolean)DamagePhysicsConfig.FAMILY_PROJECTILE_USES_IMPACT.get(),
            (Boolean)DamagePhysicsConfig.FAMILY_PROJECTILE_USES_DISTANCE.get());
         case GUN -> new Scaling(
            (Boolean)DamagePhysicsConfig.FAMILY_GUN_USES_IMPACT.get(),
            (Boolean)DamagePhysicsConfig.FAMILY_GUN_USES_DISTANCE.get());
         case HEAL -> new Scaling(
            (Boolean)DamagePhysicsConfig.FAMILY_HEAL_USES_IMPACT.get(),
            (Boolean)DamagePhysicsConfig.FAMILY_HEAL_USES_DISTANCE.get());
         case NORMAL -> new Scaling(
            (Boolean)DamagePhysicsConfig.FAMILY_NORMAL_USES_IMPACT.get(),
            (Boolean)DamagePhysicsConfig.FAMILY_NORMAL_USES_DISTANCE.get());
      };
   }

   // ────────────────────────── 彩虹（附加判定）──────────────────

   /** 彩虹只允许普通攻击（可被 rainbowOnlyNormal 关掉限制，则退回“除减益外都允许”）。 */
   public static boolean rainbowAllowed(Context ctx) {
      if (ctx.heal() || ctx.family() == DamageFamily.DEBUFF) {
         return false;
      }
      return !(Boolean)DamagePhysicsConfig.RAINBOW_ONLY_NORMAL.get() || ctx.family() == DamageFamily.NORMAL;
   }
}
