# EasyMagic「附魔刷新重复」修复 —— status / apply / rollback
#
# 解决的问题：空池保护会**绕过过滤**（rerunning unfiltered），把刷过的附魔又摆出来；
#             真正的「池子刷完→重置」判据永远不成立（实测 197 次回退 / 0 次重置），
#             池子涨到 98 种后永久失效 → 一多就重复。
#
# 修法：retrying 时不再返回未过滤的流，而是**重置该池子**后照常过滤（新的一轮从空池开始）。
#
# 用法：
#   .\deploy-easymagic-dupfilter.ps1 status
#   .\deploy-easymagic-dupfilter.ps1 apply
#   .\deploy-easymagic-dupfilter.ps1 rollback
#
# 注意：本文件必须保存为 UTF-8 with BOM。

param(
    [ValidateSet("status", "apply", "rollback")]
    [string]$Action = "status"
)

$ErrorActionPreference = "Stop"

$cfgPath = Join-Path $PSScriptRoot "deploy.config.psd1"
if (-not (Test-Path -LiteralPath $cfgPath)) { throw "找不到配置: $cfgPath" }
$cfg = Import-PowerShellDataFile -LiteralPath $cfgPath
# 显式护栏：.psd1 必须是 UTF-8 **with BOM**。PowerShell 5.1 按 GBK 读无 BOM 的中文 .psd1 时
# 会吞掉整行，导入结果变成"有键但值为空"，随后报出与真实原因无关的绑定错误。
if (-not $cfg.VersionsDir) {
    throw "配置导入后 VersionsDir 为空 —— 多半是 $cfgPath 丢了 UTF-8 BOM（请用 with BOM 保存）"
}

$instance = Get-ChildItem -LiteralPath $cfg.VersionsDir -Directory |
    Where-Object { $_.Name -like $cfg.InstanceMatch } | Select-Object -First 1
if (-not $instance) { throw "在 $($cfg.VersionsDir) 下找不到匹配 '$($cfg.InstanceMatch)' 的实例" }

$target = Join-Path $instance.FullName "mods\$($cfg.ModJarName)"
$backup = "$target$($cfg.BackupSuffix)"
$patched = Join-Path $PSScriptRoot $cfg.PatchedJar

function Get-Sha16([string]$path) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        $fs = [IO.File]::Open($path, 'Open', 'Read', 'ReadWrite')
        try { return ([BitConverter]::ToString($sha.ComputeHash($fs)) -replace '-','').Substring(0,16) }
        finally { $fs.Close() }
    } finally { $sha.Dispose() }
}

function Test-GameRunning {
    $procs = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object {
            $_.CommandLine -and
            ($_.CommandLine -like "*$($instance.FullName)*" -or $_.CommandLine -like "*bootstraplauncher*")
        }
    return [bool]$procs
}

switch ($Action) {
    "status" {
        Write-Host "实例:        $($instance.FullName)"
        Write-Host "目标 jar:    $($cfg.ModJarName)"
        Write-Host "  当前 SHA16: $(if (Test-Path -LiteralPath $target) { Get-Sha16 $target } else { '(不存在)' })"
        Write-Host "  期望(旧):   $($cfg.ExpectedCurrentSha16)"
        Write-Host "修好的 jar:  $($cfg.PatchedJar)"
        Write-Host "  产物 SHA16: $(if (Test-Path -LiteralPath $patched) { Get-Sha16 $patched } else { '(不存在)' })"
        Write-Host "  期望(新):   $($cfg.ExpectedPatchedSha16)"
        Write-Host "备份:        $(if (Test-Path -LiteralPath $backup) { "$backup  SHA16=$(Get-Sha16 $backup)" } else { '(无)' })"
        Write-Host "游戏运行中:  $(Test-GameRunning)"
        if (Test-Path -LiteralPath $target) {
            $cur = Get-Sha16 $target
            Write-Host "状态:        $(if ($cur -eq $cfg.ExpectedPatchedSha16) { '已修' } elseif ($cur -eq $cfg.ExpectedCurrentSha16) { '未修（仍是旧版）' } else { '未知版本（既非旧版也非修复版，请人工确认）' })"
        }
    }

    "apply" {
        if (-not (Test-Path -LiteralPath $patched)) { throw "找不到修好的 jar: $patched" }
        if (-not (Test-Path -LiteralPath $target)) { throw "找不到目标 jar: $target" }
        if (Test-GameRunning) { throw "游戏正在运行，请先退出再部署" }

        $patchedSha = Get-Sha16 $patched
        if ($patchedSha -ne $cfg.ExpectedPatchedSha16) {
            throw "产物 SHA16=$patchedSha 与配置里的 $($cfg.ExpectedPatchedSha16) 不一致（产物变了就要同步改配置）"
        }
        $curSha = Get-Sha16 $target
        if ($curSha -ne $cfg.ExpectedCurrentSha16 -and $curSha -ne $patchedSha) {
            throw "目标 jar SHA16=$curSha 既不是旧版 $($cfg.ExpectedCurrentSha16) 也不是修复版，拒绝覆盖"
        }

        if (-not (Test-Path -LiteralPath $backup)) {
            Copy-Item -LiteralPath $target -Destination $backup -Force
            Write-Host "已备份 -> $(Split-Path -Leaf $backup)  SHA16=$(Get-Sha16 $backup)"
        } else {
            Write-Host "备份已存在，保留不覆盖: $backup"
        }

        # 覆写（不经改名/删除，避免共享冲突）
        [IO.File]::WriteAllBytes($target, [IO.File]::ReadAllBytes($patched))
        $nowSha = Get-Sha16 $target
        Write-Host "覆写后 SHA16: $nowSha  (期望 $($cfg.ExpectedPatchedSha16))"
        if ($nowSha -ne $cfg.ExpectedPatchedSha16) { throw "覆写后指纹不符，请执行 rollback" }
        Write-Host "已部署 v3 修复版。行为：筛不出「没见过」的槽位会【留空】而不是重复；只有整轮刷新一个附魔都没出来时，下一次刷新才重洗池子。"
    }

    "rollback" {
        if (-not (Test-Path -LiteralPath $backup)) { throw "找不到备份: $backup" }
        if (Test-GameRunning) { throw "游戏正在运行，请先退出再回滚" }
        [IO.File]::WriteAllBytes($target, [IO.File]::ReadAllBytes($backup))
        Write-Host "已从备份还原 -> SHA16=$(Get-Sha16 $target)"
    }
}
