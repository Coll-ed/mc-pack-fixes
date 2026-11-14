package com.yglcs.witherblastfix;

/**
 * 集中放需要跟目标模组对齐的符号，升级时只改这里。
 */
public final class WitherBlastFixSymbols {

    /** Wither Reincarnated 的齐射 AI 目标类。 */
    public static final String BARRAGE_GOAL = "com.alexander.witherreincarnated.ai.goals.wither.WitherBarrageGoal";
    /** 齐射在 tick 里发射炮弹的方法名。 */
    public static final String BARRAGE_TICK = "tick";
    /** 发射炮弹的调用点（把实体放进世界）。 */
    public static final String ADD_FRESH_ENTITY =
            "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z";
    /** 我们给齐射炮弹打的持久化标记键。 */
    public static final String NO_GRIEF_TAG = "witherblastfix:no_grief";

    private WitherBlastFixSymbols() {
    }
}
