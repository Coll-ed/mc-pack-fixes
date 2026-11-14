# lddshine — LambDynamicLights × Shine 彩色动态光桥接

把 **LambDynamicLights（LDD）** 算出来的动态光源（手持火把、发光方块、掉落物品……）
喂进 **Shine** 的彩色光着色管线，让动态光也带上 Shine 里配好的颜色和半径。

纯客户端。两侧任一缺失就不加载（mods.toml 里是 required + AFTER）。

## 数据流

```
LDD: dynamicLightSources (Set<DynamicLightSource>, 持 lightSourcesLock 读锁)
        │  取 EntityDynamicLightSource → luminance(0..15) + 插值坐标 + 发光物品
        ▼
ItemLightColors.resolve(stack)
        │  BlockItem → Shine 的 ColoredLightConfig.lightForState(blockState)  ← 复用现有颜色表
        │  否则     → 本模组 itemOverrides 覆盖表
        ▼
Shine: ColoredLightRenderer#refreshUploadLights(Vec3) @RETURN
        │  反射构造 ActiveLight 追加进 UPLOAD_LIGHTS（受 64 名额限制）
        ▼
Shine 原有的：签名计算 → 填 POS_RADIUS/COLOR buffer → Sodium 分区裁剪上传
```

## 为什么选 `refreshUploadLights` 的 RETURN 作为唯一接缝

它是 Shine 里最窄的一个点：追加进去之后，重传判定、uniform 填充、Sodium 的
`shouldUploadSodiumUniformsForRegion` 分区裁剪全部沿用 Shine 自己的逻辑，我们一行都不用抄。
用 `RETURN` 而不是 `TAIL`，是因为该方法在 `ACTIVE_LIGHTS` 为空时会**提前 return**，
只有 `RETURN` 能同时命中提前返回和正常路径。

## 已核对事实（`verify_targets.sh` 逐条断言，12/12 PASS）

| 事实 | 出处 |
|---|---|
| `refreshUploadLights(net.minecraft.world.phys.Vec3)` 为 private static | Shine `javap` |
| `UPLOAD_LIGHTS: List<ColoredLightRenderer$ActiveLight>` | 同上 |
| `ActiveLight` 11 参构造器 `(int,int,int,double,double,double,float,float,float,float,float)`，包私有 | 同上 |
| 单次 uniform 上传上限 = 64（`bipush 64`），与 Shine 自己的方块光共享 | `uploadSodiumUniforms` 字节码 |
| `ColoredLightConfig.enabled` / `lightForState(BlockState)` / `ColoredLightConfigManager.get()` 可用 | 同上 |
| LDD `LambDynLights.INSTANCE`、`dynamicLightSources: Set`、`lightSourcesLock: ReentrantReadWriteLock` | LDD `javap` |
| `EntityDynamicLightSource#getLuminance/getDynamicLightX/isDynamicLightEnabled` | 同上 |
| `PlayerEntityMixin implements EntityDynamicLightSource`（所以实体本身就能当光源读） | 同上 |

## 构建（本机没有 Gradle，用 javac 直编）

```
bash build.sh          # 产物 build/dist/lddshine-<version>.jar
bash verify_targets.sh # 静态自证：12 条假设逐条对真实 jar 断言
```

- 依赖清单：`build.env`（绝对路径只出现在这个配置文件里）
- 版本号：`gradle.properties`（`mods.toml` 里的 `${version}` 等占位由 `build.sh` 替换）
- `build.gradle` 保留给有 Gradle 的环境，逻辑等价

## 配置

`config/lddshine-client.toml`

| 键 | 默认 | 说明 |
|---|---|---|
| `bridge.enabled` | `true` | 总开关 |
| `bridge.maxExtraLights` | `16` | 每帧最多追加几个动态光（0–32；Shine 单次上传上限 64） |
| `bridge.debugLog` | `false` | 每 5 秒打印一次桥接统计 |
| `colors.fallbackRadius` | `12.0` | 仅 `itemOverrides` 命中时使用 |
| `colors.fallbackIntensity` | `1.0` | 同上 |
| `colors.itemOverrides` | `[]` | `"minecraft:glowstone_dust=#FFD27F"` 形式，只对非方块物品有意义 |

亮度→强度：`intensity = 表里的 intensity × (luminance / 15)`；半径按表里的原值，不随亮度缩放。

## 已知脆弱点（升级时先看这里）

1. Shine 哪天重命名 `refreshUploadLights` / 改 `ActiveLight` 构造器 → `verify_targets.sh` 立刻报红，改 `BridgeTargets` 与 `ShineActiveLightFactory` 即可。
2. 64 个名额是共享的：Shine 自己的方块光先占，我们只在剩余名额里追加（默认最多 16）。
3. LDD 只输出亮度、不输出颜色 —— 颜色必须来自 Shine 的 `blockLights` 表或 `itemOverrides`。
4. 同一个光源如果 LDD 那边关掉了（`isDynamicLightEnabled() == false`），我们不会硬点。
