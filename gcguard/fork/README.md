# GC Guard — 主动回收（0.6.0）

整合包分发用：放进 `mods/` 即可。零启动参数也能工作。

## 两条路径

### 主路径：接管官方周期性 GC（模组自己写进运行中的 JVM）

`G1PeriodicGCInterval` 与 `G1PeriodicGCSystemLoadThreshold` 在 HotSpot 里是 **manageable**，
实测 `isWriteable() == true`，所以模组启动时用 `HotSpotDiagnosticMXBean.setVMOption` 直接设进去：

```
[gcguard] 已接管官方周期回收：G1PeriodicGCInterval = 120000 ms（走 Concurrent Start，不是 Full GC）
```

这走的是 Oracle G1 文档里的 “Periodic Garbage Collections”：以 **Concurrent Start** 触发，
停顿几十毫秒量级。

⚠️ 官方语义限制：该机制只在“距上一次**任何** GC 暂停已超过设定间隔、且无并发周期进行中”时才触发。
也就是说 —— **挂机/站着不动时非常有效**；边玩边涨时 Young GC 会不断重置计时器，它不会介入。

### 兜底：老年代到阈值就 System.gc()

```
[gcguard] 兜底回收（已补 1 次辅助清理）：老年代 72% (3600MB) → 1200MB，整堆 6900MB → 3200MB，停顿约 600ms
```

判定基准是 **G1 老年代占用率**（与官方 IHOP 的定义域一致，不是整堆），默认 70%；
大回收（≥ 老年代容量 15%）后自动补一次辅助清理。

## 配置 `config/gcguard-client.toml`（3 个键）

| 键 | 默认 | 说明 |
|---|---|---|
| `gc.enabled` | `true` | 总开关 |
| `gc.periodicGCIntervalSeconds` | `120` | 写入 G1PeriodicGCInterval 的值（10–3600） |
| `gc.fallbackOldGenPercent` | `70` | 兜底 Full GC 的老年代阈值（40–95） |
| `gc.minIntervalSeconds` | `150` | 两次兜底回收最小间隔 |

> 注：实际是 4 个键（enabled + 3 个参数）。

## 运行时改不了的两个参数

```
ExplicitGCInvokesConcurrent = false (writeable=false)   ← 想让 System.gc() 变并发，只能启动期加
InitiatingHeapOccupancyPercent = 45  (writeable=false)  ← 想提前混合回收，只能启动期加
```

整合包若想再进一步，可在启动器 JVM 参数里加 `-XX:+ExplicitGCInvokesConcurrent`（兜底那 600ms 会降到几十毫秒）。

## 实测（1.21.1 / 310 模组包 / 7.7GB 上限）

- Full GC 兜底：回收 2.5~3GB，停顿 500~700ms；
- 官方 periodic：Concurrent Start，几十毫秒（挂机场景）。
