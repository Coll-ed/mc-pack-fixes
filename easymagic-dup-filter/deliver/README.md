# EasyMagic「附魔刷新重复」修复

## 结论：过滤键本来就是"按种类"，坏的是**空池保护会绕过过滤**

你说"要按种类过滤不是按等级"——**键上确实已经是按种类的**（`keyOf` = `ResourceLocation.toString()`，如
`minecraft:sharpness`，`recordShown` 只读 `EnchantmentInstance.enchantment`、**从不读 `.level`**）。
重复的真正来源是另一处：当"过滤后某个槽位凑不出附魔"时，代码会**取消过滤重算**，把刷过的又摆出来。

## 缺陷（读部署版字节码确认，非推测）

`fuzs.easymagic.mixin.rerollmemory.EnchantmentMenuRerollMemoryMixin`：

```java
// rerollMemory$protectEmptyPool（挂在 getEnchantmentList 返回处）
List<EnchantmentInstance> result = cir.getReturnValue();
if (result != null && !result.isEmpty()) return;   // 有结果就不管
if (!filterApplied || retrying) return;
logEmptyPoolProtection(...);
retrying = true;
cir.setReturnValue(accessor.callGetEnchantmentList(...));  // ← 重入
retrying = false;
```

```java
// rerollMemory$filterCandidates
if (this.rerollMemory$retrying) {
    this.rerollMemory$filterApplied = false;
    return candidates;        // ← 原样返回 = 取消过滤
}
```

**所以"过滤后凑不出"时会拿完全不过滤的结果顶上**——刷过的附魔立刻又出现。

## 实测证据（`logs/latest.log`，21:11:19 ~ 21:26:14）

| 指标 | 数值 |
|---|---|
| `=> rerunning this lookup unfiltered` 回退 | **197 次** |
| `every candidate enchantment was shown`（真正的"池子刷完→重置"） | **0 次** |
| 物品分布 | `minecraft:book` 191 次、`minecraft:bow` 6 次 |
| 槽位分布 | slot 0 / 1 / 2 各 81 / 65 / 51 次 |
| 池子大小走向 | 5 → 52 → **89（44 次）→ 98（49 次）** |

**为什么"一多就重复"**：池子涨到 98（书能吃到的附魔几乎全集）后，
- 过滤后剩下的"没见过"的**凑不出槽位**（低费槽位用不上那些贵的）→ 走回退 → 重复；
- 而"池子刷完→重置"的判据是**未见集合为空**，实际上还剩几个永远出不来的 → **判据永不成立** → 池子**永久卡在饱和态**，过滤彻底失效。

所以不是"过滤规则写错"，而是**失效后没有任何机制把它救回来**。

## 修法（最小改动，只手改两个类的两个方法）

把"空池保护"从「**绕过过滤**」改成「**重置池子、开新一轮，然后照常过滤**」：

```java
private Stream<Holder<Enchantment>> rerollMemory$filterCandidates(Stream<Holder<Enchantment>> candidates) {
    if (this.rerollMemory$retrying) {
        // 修：不再返回未过滤的流，而是把该池子清空（SEEN/PENDING/EXHAUSTED 全清）后照常过滤
        RerollMemory.clear(this.rerollMemory$player(), this.rerollMemory$rollingItem);
    }
    FilterOutcome outcome = RerollMemory.filterCandidates(
            this.rerollMemory$player(), this.rerollMemory$rollingItem, candidates);
    this.rerollMemory$filterApplied = outcome.removedAny();
    return outcome.stream();
}
```

顺带把 3 处已经不准确的日志文案改对（原来写着 `rerunning unfiltered` / `enchantment applied`）：

| 旧 | 新 |
|---|---|
| `... => empty-pool protection: rerunning this lookup unfiltered (vanilla result)` | `... => pool saturated: starting a new round` |
| `[{}] enchantment applied; filter pool cleared ({} kinds) - back to vanilla enchanting` | `[{}] filter pool cleared ({} kinds) - a fresh round starts` |
| `[{}] enchantment applied; filter pool was already empty` | `[{}] filter pool was already empty` |

### 修完的行为

| 阶段 | 行为 |
|---|---|
| 池子未饱和 | 过滤正常生效，看到的都是**没刷过的**附魔 |
| 某槽位再也凑不出新的 | **开新一轮**（池子清空）→ 该轮继续能刷出新的，不会把旧的硬塞回来 |
| 饱和后每轮 | 每轮重置一次 ≈ 原版随机——诚实，且**不会卡死** |

**这也修掉了那 191 行 WARN 刷屏**（回退路径消失）。

### 一个必须说清的取舍

"凑不出时"只有三种可能做法，我选了第 3 种：

1. **保持不过滤（原状）** → 重复，你反馈的问题；
2. **让槽位空着** → 完全不重复，但书在饱和后**三个槽位会一起变空**，附魔台会看起来"坏了"（而且那个
   判据永不成立，会**永久卡死**）；
3. **重置开新一轮（本次采用）** → 槽位照常填满，重复只发生在**被明确记录的"新一轮"边界**上。

如果你更想要第 2 种（宁可空着也不要重复），把 `retrying` 分支改成直接 `return candidates` 之外的
"原样返回但**已经过滤过**的流"即可 —— 说一声我改。

## 交付与校验

| 项 | 值 |
|---|---|
| 被改文件 | `mods\EasyMagic-v21.1.4-1.21.1-NeoForge.jar` |
| 旧版 SHA16 | `ECB00732C2A1F3BC` |
| 新版 SHA16 | `58FBAD115A1295FE` |
| 备份 | `EasyMagic-v21.1.4-1.21.1-NeoForge.jar.dupfilter-bak`（=`ECB00732C2A1F3BC`） |
| 回滚 | `.\deploy-easymagic-dupfilter.ps1 rollback` |

**逐条目对比**：122 → 122 条（无增无删），内容有差异的**恰好 2 个**（就是我改的那两个类），
**其余 120 条逐字节相同**。

**实载校验**：部署文件中那两个类用 `-Xverify:all` 装载通过；
并反汇编**部署后的文件本身**确认新字节码在（不是只验工作区副本）。

## 复跑

```powershell
# 重新生成修复版 jar（需要 JDK + ASM）
java -cp "tools;<...>\asm-9.10.1.jar;<...>\asm-tree-9.10.1.jar" `
     PatchEasyMagicDupFilter <原jar> <输出jar>

.\deploy-easymagic-dupfilter.ps1 status|apply|rollback
```

路径/文件名/指纹都在 `deploy.config.psd1`（脚本不含写死路径；指纹标注了 `hardcode-ok` 与理由）。
`deploy.config.psd1` 与 `.ps1` 都必须是 **UTF-8 with BOM**。

---

# 附：v1 → v2 → v3 的来龙去脉（为什么最后是这样）

同一件事改了三版，把过程和证据留在这里，避免以后重新踩。

| 版本 | 规则 | 实测结果 |
|---|---|---|
| 原版（另一 agent 版） | 槽位凑不出时**取消过滤**重算 | 508 次 `rerunning unfiltered` → 大量重复 |
| **v1**（我的第一版） | **任一**槽位凑不出就清池、开新一轮 | 日志：`pool=55 kinds => pool saturated` / `filter pool cleared (55 kinds)`。当时全集约 98 种 —— **还剩 40 多种没见过的就被清空**，于是很快又开始重复。用户实测反馈："又又出现重复了" |
| **v2**（未部署） | 连续 **3** 个槽位失败才清池 | 3 是**硬编码的槽位数**，与池子实际状态无关；用户否掉了这个思路 |
| **v3**（当前部署） | 槽位留空；**整轮刷新全部为空**才在下一轮开头重洗池子 | 不依赖槽位总数、不依赖池子大小 |

## v3 的两条规则（用户口径）

1. 某槽位筛不出"没见过"的附魔 → **该槽位留空**，绝不用见过的顶替
2. 观察**整轮刷新**：**一个附魔都没出来（全部为空）** ⇒ 过滤池真的筛空了 ⇒ **下一轮开头重洗池子**

## v3 怎么做到"不硬编码"

- **不数槽位**：用**槽位序号回绕**（再次看到 0 号槽）判断"上一轮刷新结束了"
  —— 3 个槽位也好、更多也好，判据都一样
- **不看池子大小**：只看筛选的**实际产出**（这一轮有没有任何非空结果）
  —— 用户装了新模组、书能出的附魔已近 200+ 种，判据依然成立
- 常量只有两个，且都是**语义锚点**而非可调参数（都带 `hardcode-ok` 说明）：
  `FIRST_SLOT = 0`（回绕判据的锚点）、`NO_SLOT_SEEN_YET = -1`（哨兵）

## 改了哪些字节码（最小改动）

从**当前部署的 v1 jar** 出发，只动 3 处：

| 位置 | 改动 |
|---|---|
| `EnchantmentMenuRerollMemoryMixin.rerollMemory$filterCandidates` | 删掉"retrying 就绕过过滤"的分支，永远走正常过滤 |
| `EnchantmentMenuRerollMemoryMixin.rerollMemory$protectEmptyPool` | 改为：报告本槽位结果给判定器 → 整轮全空时 `RerollMemory.clear(...)`；否则该槽位留空 |
| `RerollMemory` 的 1 处日志文案 | 从"pool saturated: starting a new round"改为"slot left empty（整轮全空才重洗）" |
| 新增 `compatpatch/easymagic/DupFilterState` | 判定状态类（回绕识别 + 整轮产出判定） |

条目数 122 → 123（改 2 个 + 加 1 个），其余逐字节不变。

## 验证（离线，未启动游戏）

- **判定逻辑单元测试**：`tools/TestDupFilterState.java`，**16 项断言全过**
  （含：只有 slot0 空不该重洗 / 整轮全空才重洗 / 5 槽位场景同样成立 ⇒ 与槽位总数无关 / 同槽位重复回调不算回绕 / 首次见到槽位不触发）
- **反汇编核对**：`protectEmptyPool` 的指令序列与设计逐条一致；
  未过滤重算入口 `callGetEnchantmentList` **已彻底消失**
- **类装载**：`-Xverify:all` 装载 `DupFilterState` 与 mixin 类通过
- **逐条目diff**：122 → 123，只动 2 个类 + 新增 1 个

## 指纹与回滚

| 项 | 值 |
|---|---|
| 部署前（v1） | `58FBAD115A1295FE` |
| 当前（v3） | `C37DF41415C0C57E` |
| 备份 | `EasyMagic-...jar.dupfilter-bak` = `ECB00732C2A1F3BC`（更早那版） |
| 回滚 | `.\deploy-easymagic-dupfilter.ps1 rollback` |

## 已知的取舍（说清楚，不藏）

槽位留空是针对"便宜附魔先用完"的直接后果：**最便宜的那个槽位（slot 0）会经常空着**，
因为它能用的便宜附魔最先被刷完。这是"绝不重复"的必要代价 ——
如果更希望槽位始终有东西（宁可偶尔重复），说一声我把它换成可配置的两种模式。