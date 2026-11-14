package compatpatch.easymagic;

/**
 * EasyMagic「附魔刷新不重复」的决策状态（v3）。
 *
 * ── 规则（按用户口径）──
 * <ul>
 *   <li>某槽位筛不出"没见过"的附魔 → 该槽位**留空**，绝不用见过的顶替</li>
 *   <li>观察**整轮刷新**：如果这一轮**一个附魔都没出来（全部为空）** ⇒ 说明过滤池真的筛空了
 *       ⇒ **下一轮刷新开始时重洗池子**（开新一轮）</li>
 * </ul>
 *
 * ── 为什么不数槽位、不看池子大小 ──
 * 之前两版都不对：
 * <ul>
 *   <li>v1：**任一**槽位筛不出就清池。实测 {@code pool=55 kinds} 就被清空，
 *       而当时书能出的附魔全集约 98 种（现在装了新模组已近 200+）——
 *       还剩一大半没见过的就被误判成"刷完了"。</li>
 *   <li>v2：改成"连续 3 个槽位失败才清池"——但 **3 是硬编码的槽位数**，
 *       既没依据也跟池子实际状态无关。</li>
 * </ul>
 * v3 不依赖任何"槽位总数/池子大小"的假设：
 * 只从**槽位序号回绕**（再次看到 0 号槽）判断"上一轮刷新结束了"，
 * 再用那一轮**有没有任何非空结果**决定是否重洗 —— 全程只看筛选的实际产出。
 *
 * 由 {@code EnchantmentMenuRerollMemoryMixin.rerollMemory$protectEmptyPool} 每槽位调用一次
 * （该回调在主线程按槽位顺序执行）。
 */
public final class DupFilterState {

    /**
     * 附魔台的第一个槽位下标。槽位序号**回绕**到它，就说明上一轮刷新结束了。
     *
     * hardcode-ok: 原版附魔台的三个槽位从 0 开始编号，这是"回绕判据"的锚点，
     * 不是可配置项；写成具名常量只为让判据自解释。注意这里**没有**任何槽位总数的假设。
     */
    private static final int FIRST_SLOT = 0;

    /** 「还没见过任何槽位」的哨兵值。 */
    private static final int NO_SLOT_SEEN_YET = -1;

    private static int lastSlot = NO_SLOT_SEEN_YET;
    private static boolean sawAnySlot;
    private static boolean producedAnyResult;

    private DupFilterState() {
    }

    /**
     * 报告一个槽位的筛选结果，并判断此刻是否该重洗池子。
     *
     * @param slot           本次槽位下标（0 起）
     * @param producedResult 该槽位**有没有**拿到附魔（true = 拿到了）
     * @return true 表示"上一轮全部为空"⇒ 调用方应立刻重洗过滤池（新的一轮从那之后开始）
     */
    public static synchronized boolean beginSlot(int slot, boolean producedResult) {
        boolean shouldReset = false;
        if (slot == FIRST_SLOT && lastSlot != NO_SLOT_SEEN_YET && lastSlot != FIRST_SLOT) {
            // 槽位序号回绕 ⇒ 上一轮刷新已结束：整轮一个附魔都没出来才算"筛空了"
            shouldReset = sawAnySlot && !producedAnyResult;
            sawAnySlot = false;
            producedAnyResult = false;
        }
        sawAnySlot = true;
        if (producedResult) {
            producedAnyResult = true;
        }
        lastSlot = slot;
        return shouldReset;
    }

    /** 供诊断/测试读取：本轮是否已经出过附魔。 */
    public static synchronized boolean producedAnyResult() {
        return producedAnyResult;
    }

    /** 供诊断/测试读取：是否见过槽位。 */
    public static synchronized boolean sawAnySlot() {
        return sawAnySlot;
    }

    /** 供测试重置状态。 */
    public static synchronized void resetForTest() {
        lastSlot = NO_SLOT_SEEN_YET;
        sawAnySlot = false;
        producedAnyResult = false;
    }

    /** 供测试设置"上一轮已经走过哪些槽位"。 */
    public static synchronized void setLastSlotForTest(int slot) {
        lastSlot = slot;
    }
}
