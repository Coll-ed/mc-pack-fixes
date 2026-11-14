# Easy Magic「刷新记忆」改版

给 Easy Magic（NeoForge 21.1.4 / MC 1.21.1）新增一个机制：**刷新时把刷出来的附魔记下来，之后的刷新不再出现它们，从而不断刷出新附魔。**

---

## 一、机制（按你确认的规格）

### 记忆范围（你选定的）
| 维度 | 规格 |
|---|---|
| **归属** | 池子按 **(玩家, 物品类别)** 划分 —— 给剑刷不会消耗书/镐的池子 |
| **去重粒度** | **附魔种类**（registry id），不区分等级 |
| **生命周期** | **不落盘**：附魔完成时清空；关掉再开附魔台不清空（可继续刷）；**重启游戏全部清空** |

**物品类别 = 物品的 Java 类名**，所以在原版物品下：

| 物品 | 类别 | 结果 |
|---|---|---|
| 钻石剑 / 铁剑 / 木剑 … | `SwordItem` | **共用一个池子** |
| 镐 / 斧 / 锹 / 锄 | `PickaxeItem` / `AxeItem` / `ShovelItem` / `HoeItem` | **各一个池子** |
| 4 种盔甲（头/胸/腿/脚） | `ArmorItem` | **共用一个池子** |
| 书 | `BookItem` | 单独一个 |

规则只有一处实现（`RerollMemory.categoryOf`），想改成更粗/更细的分组只改那一个方法。

### 1. 刷新记录 → 不再出现
附魔台每次刷新，把这一轮生成的附魔记进**该玩家该物品类别**的筛选池。之后的刷新**只从没见过的附魔里抽**。

关键点：过滤发生在**候选阶段**，也就是交给 `selectEnchantment` 之前。
不是"先抽再剔除"，所以：
- 加权抽取本身只在没见过的附魔里进行，**权重不会被扭曲**；
- 不会出现"抽出来全被剔除、候选变成空"的情况。

### 2. 附魔完成后清空池子
真正附魔成功之后（原版那条路径返回 `true`），立刻清空池子，**回归正常附魔**。

清空范围默认是**当前物品类别**（`clearAllItemPools=false`）——池子本来就按类别独立，
给剑附魔不该把书的进度一起抹掉。想要"一次附魔清掉该玩家所有类别"就把它设为 `true`。

Easy Magic 的「刷新」是 `buttonId 4`，在它自己的重写里就处理掉了、**不会走到这条路径** ——
所以刷的过程不会误清池子。

### 3. 池子刷完之后
该类别能出的附魔都见过一遍后：

| `resetWhenExhausted` | 行为 |
|---|---|
| `true`（默认） | 清空记忆、重新开始一轮（可无限循环刷） |
| `false` | 从此刻起不再排除，回到原版行为（日志会记一条 WARN） |

---

## 二、配置

**文件**：`<游戏目录>\config\easymagic-rerollmemory.properties`
（首次打开附魔台时自动生成；**只在不存在时生成**，之后不覆盖，注释和你的改动都保留）

```properties
# 是否启用本机制。false = 完全回到 Easy Magic 原版行为。
enabled=true

# 池子被刷完（该类别能出的附魔都见过一遍）时，是否清空记忆、重新开始一轮。
resetWhenExhausted=true

# 附魔完成时清空的范围。
#   false = 只清当前物品类别的池子（默认；给剑附魔不会抹掉书的进度）
#   true  = 清空该玩家所有物品类别的池子
clearAllItemPools=false
```

**改完保存即生效**，不需要重启游戏（每次生成附魔时比对一次文件修改时间）。

日志过滤关键字：`easymagic-rerollmemory`（日志里会带类别名，如 `[SwordItem]`）

---

## 三、实现（怎么挂上去的）

### 挂钩点：只有一个
全游戏（含原版附魔台）只有**一个**附魔生成点：

```
net.minecraft.world.inventory.EnchantmentMenu#getEnchantmentList(RegistryAccess, ItemStack, int, int)
```

Easy Magic 的 `updateLevelsAndClues`（刷新时算候选/提示）和原版 `clickMenuButton`（真正附魔）**都走它**。
所以只在这一个地方挂钩，**「看到的候选」与「真正附上的附魔」天然一致**，不会出现"显示一套、附上另一套"。

挂在原版 `EnchantmentMenu` 上（不是 `ModEnchantmentMenu`）：后者继承前者，两类都覆盖到；
而真正附魔走的是原版 `clickMenuButton` → 同一个 `getEnchantmentList`。

| 注入 | 位置 | 作用 |
|---|---|---|
| `@ModifyArg` | `getEnchantmentList` 里 `EnchantmentHelper.selectEnchantment` 的**第 4 个参数**（index 3，候选流） | 换成「没刷过的那些」 |
| `@Inject(RETURN)` | `getEnchantmentList` 返回处 | 记录本轮生成的附魔 |
| `@Inject(RETURN)` | `clickMenuButton(Player,int)` 返回处，**只认返回 `true`** | 附魔完成后清空筛选池 |

### 打包方式：不改任何已有字节码
只做三件事（`tools/PatchEasyMagic.java`）：
1. 加入 4 个新类；
2. 加入独立 mixin 配置 `easymagic.rerollmemory.mixins.json`；
3. 在 `META-INF/neoforge.mods.toml` 里挂载该配置（`[[mixins]]` 条目），并把显示名标为 **Easy Magic (Shin 改版)**。

**刻意没有改写任何已有字节码** —— 相比 ASM 改写，风险低得多，也容易回滚。

### 安全性考虑
- mixin 配置写成 **`"required": false`**：万一注入失败，**只记日志、不会崩游戏**（但仍会明确报错，不静默）。
- 本 mixin 配置**不声明 refmap**。原因：本实例运行时是 **Mojang 官方名**
  （Connector 的产物是 `*_mapped_moj_*.jar`），而 Easy Magic 自带的
  `easymagic.common.refmap.json` 是 `named:intermediary`，在此环境不参与重映射。
  用 Mojang 名 + 无 refmap，命名空间一致，最不容易出错。
- 只在**服务端**记账（客户端侧 `memoryKey` 返回 null），避免两端各记一份导致不一致。

---

## 四、规格确认情况

已在需求确认时定下（见上「记忆范围」）：**按 (玩家, 物品类别) 分池** / **按附魔种类去重** / **不落盘，附魔完成与重启游戏清零**。

仍有两点是我自己定的，如果与预期不符请说一声：

1. **附魔完成时只清当前物品类别**（`clearAllItemPools=false`）。
   依据：池子既然按类别独立，给剑附魔就不该抹掉书的进度。想改成"清掉该玩家所有类别"→ 改这个配置项即可，不用重编译。

2. **物品类别用「物品的 Java 类名」**（见上表）。
   换取的是：规则只有一处实现、零维护成本、对新物品自动生效。
   代价：`ArmorItem` 让 4 种盔甲共用一个池子；模组自定义物品若继承原版类会跟着归到原版那一组。
   → 想要更贴合语义的分组（例如按"武器/工具/盔甲/书"四大类，或按物品注册 id 精确分组），
   改 `RerollMemory.categoryOf` 这一个方法就行。

3. **没有附魔过的存档第一次打开附魔台就开始记录**（不是"先刷一次才开始"）。
   因为记录点就是生成点 —— 摆出来的就是"刷出来的"。

---

## 五、构建 / 部署 / 回滚

```powershell
# 状态
.\deploy-easymagic.ps1 status

# 部署（自动备份原版为 .jar.orig）
.\deploy-easymagic.ps1 deploy

# 回滚到原版
.\deploy-easymagic.ps1 rollback
```

机器相关路径都在 `deploy.config.psd1` 里（脚本不含写死路径）。
> 注意：`deploy.config.psd1` 与 `.ps1` 都必须是 **UTF-8 with BOM** ——
> PowerShell 5.1 会按 GBK 读无 BOM 的文件，中文注释会导致整行被吞（实测踩到过）。

重新编译打包：

```powershell
javac --release 17 -proc:none -encoding UTF-8 -cp "<1.21.1 client jar>;<libraries...>;<解出的原版 jar>" -d build\classes src\...\*.java
javac --release 17 -d build\tools tools\PatchEasyMagic.java
java -cp build\tools PatchEasyMagic EasyMagic-orig.jar build\classes res build\<输出>.jar
```

**类路径务必排除 `libraries\net\minecraft\client\1.20.1*`** —— 它会遮蔽 1.21.1 的类，
表现为 `Player.level()` 之类"找不到符号"（实测踩到过）。

---

## 六、部署状态

| 项 | 值 |
|---|---|
| 实例 | `D:\GAME\MC\.minecraft\versions\你所留下的足迹` |
| 已部署 | `mods\EasyMagic-v21.1.4-1.21.1-NeoForge.jar`（SHA256 `4A73567E9C315740…`，157839 字节） |
| 原版备份 | `mods\EasyMagic-v21.1.4-1.21.1-NeoForge.jar.orig`（SHA256 `E641A83DCD0DD1A6…`，143992 字节） |
| 配置文件 | `config\easymagic-rerollmemory.properties`（首次开附魔台时生成） |

---

## 七、验证情况（离线，未启动游戏）

`tools/VerifyEasyMagic.java`，**43 项校验全部通过**，并且是**对部署后的实际文件**跑的：

- **A. jar 内容**：5 个新类 + mixin 配置都在；原版 72 个条目**一个都没丢**（78 = 72 + 6）。
- **B. mixin 配置**：`required=false`、package 正确、列出了 mixin 类。
- **C. mods.toml**：`[[mixins]]` 恰好 3 条（原 2 + 新 1），原版两条仍在，改版标注已写。
- **D. 注解常量已内联**：三个目标描述符确实作为字面量进了类常量池
  （若没内联，运行期注解值就是字段引用，直接出错）。
- **D2. mixin 类结构**：`@Shadow` / `@Unique` 注解在；3 个字段都按预期类型声明。
- **E. 注入点在目标类字节码里确实存在**（最大风险点，直接读游戏 jar 验证）：
  - `EnchantmentMenu.getEnchantmentList` / `clickMenuButton` 方法签名匹配；
  - `getEnchantmentList` 里确实有 `selectEnchantment` 调用；
  - 该调用**前一条指令压入 Stream** ⇒ 候选流就是 `index 3`；
  - `clickMenuButton` 返回 `boolean` ⇒ 可以用 `true` 判定是否真的附魔成功；
  - **`@Shadow` 的目标字段 `enchantSlots` 真实存在**（名字 + 描述符都核对）。
- **F. 单元测试**：
  - `removeSeen`（刻意做成纯函数，脱离游戏环境测）：没见过全保留 / 部分见过只留新的 /
    全见过返回空 / 取不到 key 不误排除 / seen 为 null / 空候选；
  - `PoolKey`（池子标识）：同玩家同类别相等且 hashCode 一致 / **不同类别不相等**（剑不消耗书的池子）/
    不同玩家不相等。
  - 另加 `-Xverify:all` 校验新类字节码合法。

### 校验器实际抓到的一个 bug
写 `enchantSlots` 的描述符常量时我写成了 `Lnet/minecraft/world/container/Container;`，
实际是 `Lnet/minecraft/world/Container;`（没有 `container` 这一段）。
**Java 代码因为 import 写对了所以照样编译通过**——只有那个常量是错的。
校验器比对目标类真实字段描述符时把它抓了出来（否则会留下一份**错误的文档**，比没有更糟）。

**尚未实机验证**：游戏内实际刷新手感、「刷过的不再出现」是否符合预期。
需要你启动游戏试一下（第一次开附魔台会生成配置文件）。
