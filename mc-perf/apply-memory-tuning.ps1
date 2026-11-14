# 内存调优：按 perf.config.psd1 的 Tuning 段，对模组配置文件做**精确到节**的键值替换
#
#   .\apply-memory-tuning.ps1 status    只看现状（默认，什么也不写）
#   .\apply-memory-tuning.ps1 apply     备份 → 替换 → 回读验证
#   .\apply-memory-tuning.ps1 revert    从最新备份恢复（只恢复本脚本管过的文件）
#
# 设计要点（对应工程纪律）：
#   · 值全部来自 perf.config.psd1（单一来源），脚本不写死任何阈值
#   · 只改 Tuning 里点名的键；c2me.toml 这种多节文件按 **节名** 精确定位，不会误改同名键
#   · 每次改动前先备份（<文件>.premem-<yyyyMMddHHmmss>），未备份成功就中止
#   · 改完**回读验证**期望值确实生效；不符就报错并把备份路径告诉你
#   · 不做"顺手重排/重格式化"，只替换目标行的值
#
# 注意：本文件必须保存为 UTF-8 with BOM。

param(
    [ValidateSet("status", "apply", "revert")]
    [string]$Action = "status"
)

$ErrorActionPreference = "Stop"

$cfgPath = Join-Path $PSScriptRoot "perf.config.psd1"
if (-not (Test-Path -LiteralPath $cfgPath)) { throw "找不到配置: $cfgPath" }
$cfg = Import-PowerShellDataFile -LiteralPath $cfgPath
if (-not $cfg.VersionsDir) { throw "配置导入后 VersionsDir 为空 —— 两种可能：① perf.config.psd1 丢了 UTF-8 BOM；② 文件里用了数据文件不支持的写法（例如 [ordered]@{}，Import-PowerShellDataFile 会直接拒绝整个文件）" }
if (-not $cfg.Tuning) { throw "配置里没有 Tuning 段" }

$instance = Get-ChildItem -LiteralPath $cfg.VersionsDir -Directory |
    Where-Object { $_.Name -like $cfg.InstanceMatch } | Select-Object -First 1
if (-not $instance) { throw "找不到实例 '$($cfg.InstanceMatch)'" }
$configDir = Join-Path $instance.FullName 'config'

# 备份文件名的统一标记（revert 靠它找）
# hardcode-ok: 这是本脚本自己的文件命名约定，不是可调参数
$BACKUP_MARK = '.premem-'

function Get-KvLineRegex([string]$key) {
    # 匹配 "  key = value" 形式，捕获缩进/键/值三段，保留原格式
    return "^(\s*$([regex]::Escape($key))\s*=\s*)(.*)$"
}

function Invoke-Status {
    foreach ($item in $cfg.Tuning) {
        $path = Join-Path $configDir $item.File
        Write-Host "· $($item.File)  [$($item.Section)]"
        if (-not (Test-Path -LiteralPath $path)) { Write-Host "    (文件不存在)"; continue }
        $found = $false
        foreach ($kv in $item.Keys.GetEnumerator()) {
            $rx = Get-KvLineRegex $kv.Key
            $line = (Get-Content -LiteralPath $path) | Where-Object { $_ -match $rx } | Select-Object -First 1
            if ($line) {
                $cur = ([regex]::Match($line, $rx)).Groups[2].Value.Trim()
                $mark = if ($cur -eq $kv.Value) { '✓ 已是期望值' } else { "→ 期望 $($kv.Value)" }
                Write-Host "    $($kv.Key) = $cur    $mark"
                $found = $true
            } else {
                Write-Host "    $($kv.Key) : 没找到这一行（跳过，不新增键）"
            }
        }
        if (-not $found) { Write-Host "    (没有可改的键)" }
    }
}

function Invoke-Apply {
    $stamp = Get-Date -Format 'yyyyMMddHHmmss'
    $changedFiles = New-Object System.Collections.Generic.List[string]

    foreach ($item in $cfg.Tuning) {
        $path = Join-Path $configDir $item.File
        if (-not (Test-Path -LiteralPath $path)) { Write-Warning "$($item.File) 不存在，跳过"; continue }

        $lines = [System.Collections.Generic.List[string]](Get-Content -LiteralPath $path)
        $section = ''
        $pending = @{}   # 本文件里还没落地的键
        foreach ($kv in $item.Keys.GetEnumerator()) { $pending[$kv.Key] = $kv.Value }
        $hitKeys = New-Object System.Collections.Generic.List[string]
        $changedThisFile = $false

        for ($i = 0; $i -lt $lines.Count; $i++) {
            $line = $lines[$i]
            # 跟踪当前所在节（只认 TOML 的 [section]）
            if ($line -match '^\s*\[(.+?)\]\s*$') { $section = $Matches[1]; continue }
            if ($section -ne $item.Section) { continue }
            foreach ($key in @($pending.Keys)) {
                $rx = Get-KvLineRegex $key
                $m = [regex]::Match($line, $rx)
                if ($m.Success) {
                    $cur = $m.Groups[2].Value.Trim()
                    $want = $pending[$key]
                    if ($cur -eq $want) {
                        Write-Host "  [$($item.File)] $key 已是 $want，无需改"
                    } else {
                        if (-not $changedThisFile) {
                            $backup = "$path$BACKUP_MARK$stamp"
                            Copy-Item -LiteralPath $path -Destination $backup -Force
                            Write-Host "  [$($item.File)] 已备份 -> $([IO.Path]::GetFileName($backup))"
                            $changedThisFile = $true
                        }
                        # 只替换值，保留原缩进与键名写法
                        $lines[$i] = "$($m.Groups[1].Value)$want"
                        Write-Host "  [$($item.File)] $key : $cur -> $want"
                    }
                    $hitKeys.Add($key)
                    $pending.Remove($key)
                }
            }
        }

        if ($changedThisFile) {
            [IO.File]::WriteAllLines($path, $lines.ToArray(), [Text.UTF8Encoding]::new($false))
            $changedFiles.Add($item.File)
        }
        if ($pending.Count -gt 0) {
            # 显式告警：不静默吞掉"想改但没找到"的情况
            Write-Warning "[$($item.File)] 以下键在 [$($item.Section)] 节里没找到，未改动：$($pending.Keys -join ', ')"
        }
    }

    # ── 回读验证：期望值必须真的在文件里 ──
    Write-Host ""
    Write-Host "回读验证:"
    $bad = 0
    foreach ($item in $cfg.Tuning) {
        $path = Join-Path $configDir $item.File
        if (-not (Test-Path -LiteralPath $path)) { continue }
        $lines = Get-Content -LiteralPath $path
        $section = ''
        $actual = @{}
        foreach ($line in $lines) {
            if ($line -match '^\s*\[(.+?)\]\s*$') { $section = $Matches[1]; continue }
            if ($section -ne $item.Section) { continue }
            foreach ($key in $item.Keys.Keys) {
                $m = [regex]::Match($line, (Get-KvLineRegex $key))
                if ($m.Success) { $actual[$key] = $m.Groups[2].Value.Trim() }
            }
        }
        foreach ($kv in $item.Keys.GetEnumerator()) {
            $got = $actual[$kv.Key]
            if ($got -eq $kv.Value) {
                Write-Host "  ✓ $($item.File) [$($item.Section)] $($kv.Key) = $got"
            } else {
                Write-Host "  ✗ $($item.File) [$($item.Section)] $($kv.Key) = '$got'（期望 $($kv.Value)）"
                $bad++
            }
        }
    }
    Write-Host ""
    if ($bad -gt 0) {
        throw "有 $bad 项未生效 —— 请检查上面的告警；备份文件后缀为 $BACKUP_MARK<时间戳>，可用 revert 恢复"
    }
    if ($changedFiles.Count -eq 0) { Write-Host "没有任何文件需要改动。" }
    else { Write-Host "改动完成: $($changedFiles -join ', ')" }
    Write-Host "回退命令: .\apply-memory-tuning.ps1 revert"
}

function Invoke-Revert {
    $any = $false
    foreach ($item in $cfg.Tuning) {
        $path = Join-Path $configDir $item.File
        $dir = Split-Path -Parent $path
        $name = Split-Path -Leaf $path
        $backups = Get-ChildItem -LiteralPath $dir -Filter "$name$BACKUP_MARK*" -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending
        if (-not $backups) { Write-Host "  $($item.File): 没有备份，跳过"; continue }
        $latest = $backups[0]
        Copy-Item -LiteralPath $latest.FullName -Destination $path -Force
        Write-Host "  $($item.File): 已从 $($latest.Name) 恢复"
        $any = $true
    }
    if (-not $any) { Write-Host "没有任何可恢复的备份。" }
}

switch ($Action) {
    "status" { Invoke-Status }
    "apply"  { Invoke-Apply }
    "revert" { Invoke-Revert }
}
