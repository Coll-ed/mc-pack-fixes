# 清理管家工具 · 端到端验收记录（Lead 亲测）

- 验收时间：2026-10-15 12:19–12:20 UTC
- 验收环境：**Windows PowerShell 5.1.26100.9444 (Desktop)**
- 验收对象：`D:\AI\AI工具箱\_janitor\janitor.ps1` + `lib\Janitor.{Core,Scan,Dedup,Quarantine}.ps1`
- 沙盒配置：`_selftest\selftest.config.jsonc`（根 = `_selftest\sandbox`，与真实巡检根隔离）

## 结论：6 步全部通过

| # | 步骤 | 命令 | 实测结果 | 判定 |
| --- | --- | --- | --- | --- |
| 1 | 重建夹具 | `_selftest\New-Fixtures.ps1` | 23 文件 / 11 目录 / 3,932,789 B | ✅ |
| 2 | 只读扫描 | `janitor.ps1 scan -Config …` | 23 文件 / 3.75 MiB；候选 10 条；枚举错误 0 | ✅ |
| 3 | 内容去重 | `janitor.ps1 dedup -Config …` | 扫描 23 → 预筛 13 → 实哈希 13；**1 组**重复；可回收 **2.50 MiB** | ✅ |
| 4 | 隔离 dry-run | `janitor.ps1 quarantine -Manifest …` | 打印 2 条待搬（2.50 MiB）；**隔离区未创建**；dup-a/b/c 全在原位 | ✅ |
| 5 | 隔离 Apply | `… -Apply` | 实际搬移 2 条（dup-a、dup-b）；**dup-c 按 `newestMtime` 保留**；`manifest.json` 含每条 sha256 | ✅ |
| 6 | 一键回滚 | `janitor.ps1 restore -Manifest <manifest.json>` | 成功 **2** 条 / 失败 **0** 条；报告 `restore-*.json` | ✅ |

## 关键正确性证据

**去重数值自洽**：3 个同内容文件各 1.25 MiB（1,310,720 B），保留 1 份即回收 `2 × 1310720 = 2621440 B = 2.50 MiB`，与工具报告一致。

**保留规则生效**：夹具刻意把 `dup-c` 设为最新（`dup-a` 10 天前、`dup-b` 5 天前），
工具正确选择保留 `dup-c`，与配置 `dedup.keepRule = newestMtime` 相符。

**阈值边界正确**：另有 2 个 100 B 的重复文件低于 `minGroupBytes`(1 MiB)，**未被报告** —— 边界过滤有效。

**回滚无损（哈希级验证）**：回滚后三份文件的 SHA-256 均为
`C067948830BED16FA947AEA8185C24170D1290438F13A9D012D1320F760B0F66`，
与隔离清单中记录的 `sha256` **逐字节一致**；隔离区除清单与回滚报告外无残留文件。

**安全护栏真实生效（非静默跳过）**：夹具刻意埋了 4 个陷阱，全部被**拒绝并记 WARN**：

```
[WARN] pycache-dir 命中受保护路径，已从候选清单剔除：…\active-project\__pycache__
       （命中 protection.protectedRoots）
[WARN] tmp-file   命中受保护路径，已从候选清单剔除：…\project\.git\scratch.tmp
       （命中 protection.protectedPatterns[git-dir]）
[WARN] tmp-file   命中受保护路径，已从候选清单剔除：…\project\.dsh\session.tmp
       （命中 protection.protectedPatterns[dsh-session]）
[WARN] tmp-file   命中受保护路径，已从候选清单剔除：…\active-project\data.tmp
       （命中 protection.protectedRoots）
```

报告同时给出「其中受保护未列入候选：95 B」，即被剔除的体积可见、可审计。

## 验收过程中修掉的真实缺陷（均为实测定位）

| 缺陷 | 表象 | 真因 | 修法 |
| --- | --- | --- | --- |
| `$` 前缀丢失 | `Import-JConfig` 抛「无法将".Length"项识别为 cmdlet」，被入口 catch 成「失败：无法使用指定的命名参数解析参数集」 | `Get-JParentPath` / `Get-JLeafName` 里 `$p`/`$n` 被批量替换吃掉 | 恢复 `$p` / `$n` |
| `[pscustomobject]` 构造崩 | `ArgumentException`「参数类型不匹配」，位置指向 `return [pscustomobject]@{` | PS 5.1 **与** 7.x 都无法枚举 `List[object]` 实例（`@($list)`、裸 `$list` 均炸） | `.ToArray()` / `[object[]](...)` |
| `@()` 与方法调用优先级 | 「PSCustomObject 不包含 ToArray」 | `@($x \| Sort-Object …).ToArray()` 先调用了 `.ToArray()` | 改为 `[object[]]($x \| Sort-Object …)` |
| 相对路径解析少一层 | `Test-Path` 判定为真，归一后却「配置文件不存在」 | **`$PWD` 与进程 `CurrentDirectory` 分叉**；`[IO.Path]::GetFullPath` 对相对路径按进程 CWD 计算 | 相对路径先 `Resolve-Path -LiteralPath` 取 `.ProviderPath` |
| 中文串报成括号不匹配 | 解析器报「语句块缺少 }」 | 无 BOM 的 UTF-8 被 PS 5.1 按 GBK 读 | 所有 `.ps1`/`.psm1` 存 UTF-8 **with BOM** |
| 内嵌双引号 | 「方法调用中缺少 )」 | `$md.Add("…ToString("yyyy-MM-dd…")")` 外层双引号里嵌双引号 | 改从 `$Config.TimestampFormat` 取值（单一来源） |

## 交付前必须复做的检查（因编辑工具会丢 BOM）

```powershell
# 1) BOM 检查与修复（本机 PS 5.1 必需）
$u = New-Object System.Text.UTF8Encoding($true)
Get-ChildItem D:\AI\AI工具箱\_janitor -Recurse -File -Include *.ps1,*.psm1 | ForEach-Object {
  $b=[IO.File]::ReadAllBytes($_.FullName)
  if(-not($b.Length -ge 3 -and $b[0] -eq 0xEF -and $b[1] -eq 0xBB -and $b[2] -eq 0xBF)){
    $t=[IO.File]::ReadAllText($_.FullName,(New-Object System.Text.UTF8Encoding($false)))
    [IO.File]::WriteAllText($_.FullName,$t,$u)
  }
}
# 2) 语法复验（必须全部 OK 再交付）
Get-ChildItem D:\AI\AI工具箱\_janitor -Recurse -File -Include *.ps1 | ForEach-Object {
  $e=$null; $null=[System.Management.Automation.Language.Parser]::ParseFile($_.FullName,[ref]$null,[ref]$e)
  if($e){ "FAIL $($_.Name): $($e[0].Message)" } else { "OK   $($_.Name)" }
}
```

> **反复出现的坑**：`edit`/批量替换类工具**不保留 BOM**，而缺 BOM 时的报错完全指向别处（括号不匹配）。
> 本轮 `Janitor.Scan.ps1`、`Janitor.Dedup.ps1`、`New-Fixtures.ps1` 各被剥过一次 BOM，均造成「假崩溃」。

## 未完成 / 不在本次验收范围

- 真实巡检根（`D:\AI\AI工具箱` / `AI实验室` / `灵感`）上的 `scan` / `dedup` **尚未由本工具跑过** ——
  验收只在沙盒内完成。真实根 2,694 MB / 32,378 文件，建议首次运行只跑 `scan`（只读）。
- `_selftest` 下的 `_tmp-check.ps1`、`_tmp-dbg1-4.ps1`、`_tmp-fix.ps1`、`_tmp-fix2.ps1`、
  `_tmp-patch.ps1`、`_tmp-probe.ps1` 为调试临时件，交付前应删除。
- 子代理自建的 `SELFTEST.md` 未落盘；本文件即为**验收方的独立记录**，不替代实现方自述。
