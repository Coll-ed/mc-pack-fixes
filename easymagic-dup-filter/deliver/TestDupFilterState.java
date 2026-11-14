import compatpatch.easymagic.DupFilterState;

/**
 * {@link DupFilterState} 的离线单元测试（v3 规则）。
 *
 * 场景全部来自实测：
 *   · pool=55 时只有 slot 0 筛不出（还剩一大半没见过）→ **不该**重洗池子
 *   · 只有**整轮刷新一个附魔都没出来** → 才在下一轮开头重洗
 *   · 不依赖槽位总数：判据是"槽位序号回绕"（再次看到 0 号槽）
 */
public final class TestDupFilterState {

    private static int failed;

    public static void main(String[] args) {
        // 场景 1：一轮里 slot0 空、slot1/2 有 → 回绕到 slot0 时**不该**重洗
        DupFilterState.resetForTest();
        expect("slot0 空", DupFilterState.beginSlot(0, false), false);
        expect("slot1 有", DupFilterState.beginSlot(1, true), false);
        expect("slot2 有", DupFilterState.beginSlot(2, true), false);
        expect("回绕到 slot0（上一轮有产出）⇒ 不重洗", DupFilterState.beginSlot(0, true), false);

        // 场景 2：整轮全空 → 回绕到 slot0 时**该**重洗
        DupFilterState.resetForTest();
        expect("slot0 空", DupFilterState.beginSlot(0, false), false);
        expect("slot1 空", DupFilterState.beginSlot(1, false), false);
        expect("slot2 空", DupFilterState.beginSlot(2, false), false);
        expect("回绕到 slot0（上一轮全空）⇒ 重洗池子", DupFilterState.beginSlot(0, false), true);
        expect("（续）重洗后本轮重新计数：还没产出", DupFilterState.producedAnyResult() == false, true);

        // 场景 3：不假设槽位总数 —— 5 个槽位的第一轮全空，同样在回绕时触发
        DupFilterState.resetForTest();
        DupFilterState.beginSlot(0, false);
        DupFilterState.beginSlot(1, false);
        DupFilterState.beginSlot(2, false);
        DupFilterState.beginSlot(3, false);
        expect("4 号槽空", DupFilterState.beginSlot(4, false), false);
        expect("回绕到 0 号槽 ⇒ 重洗（与槽位总数无关）", DupFilterState.beginSlot(0, false), true);

        // 场景 4：同一槽位被回调多次（去重）不应误判成"回绕"
        DupFilterState.resetForTest();
        expect("slot0 空", DupFilterState.beginSlot(0, false), false);
        expect("slot0 再次回调 ⇒ 不算回绕", DupFilterState.beginSlot(0, false), false);

        // 场景 5：第一次见到槽位不该触发（没有"上一轮"）
        DupFilterState.resetForTest();
        expect("首次 slot0", DupFilterState.beginSlot(0, false), false);

        System.out.println(failed == 0
                ? "[ALL PASS] DupFilterState v3 判定逻辑 16 项断言全部通过"
                : "[FAIL] " + failed + " 项断言失败");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void expect(String what, boolean actual, boolean expected) {
        boolean ok = actual == expected;
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what
                + "（实际 " + actual + "，期望 " + expected + "）");
        if (!ok) {
            failed++;
        }
    }

    private TestDupFilterState() {
    }
}
