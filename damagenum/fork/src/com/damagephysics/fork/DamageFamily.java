package com.damagephysics.fork;

/**
 * 伤害族（改版新增）：先分类，再按族决定底色 / 描边 / 是否参与冲击缩放。
 *
 * <ul>
 *   <li>{@link #DEBUFF}：中毒、凋零这类环境伤害 —— 固定颜色、固定字号、不参与冲击缩放。</li>
 *   <li>{@link #SPELL}：法术（铁魔法等）—— 法术底色 + 法术描边，参与冲击缩放。</li>
 *   <li>{@link #NORMAL}：近战/普通攻击 —— 冲击渐变底色 + 附魔时白色描边，参与冲击缩放。</li>
 *   <li>{@link #GUN}：枪械（Scorched Guns 等）—— 冲击渐变底色、无描边、不参与冲击缩放。</li>
 * </ul>
 */
public enum DamageFamily {
   /** 集合名：debuff（中毒/流血/腐蚀/毒云等持续伤害，兼容其它模组的 DoT）。 */
   DEBUFF("debuff"),
   SPELL("spell"),
   NORMAL("normal"),
   GUN("gun"),
   /** 弓/弩/三叉戟等投掷物：不吃距离缩放，吃冲击缩放与生命变色，无描边。 */
   PROJECTILE("projectile"),
   /** 非玩家的恢复生命数字：吃距离与冲击缩放，固定绿色、无描边。 */
   HEAL("heal");

   private final String label;

   DamageFamily(String label) {
      this.label = label;
   }

   /**
    * 判定层输出的集合名（英文 key，供日志与拔插测试使用）。
    * 例：中毒/流血/腐蚀 都属于 debuff 集合，具体类型交给 typeColorMap 上色。
    */
   public String displayName() {
      return this.label;
   }
}
