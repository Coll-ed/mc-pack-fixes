# Easy Magic「刷新记忆」改版 —— 部署 / 回滚 / 状态
#
# 用法：
#   .\deploy-easymagic.ps1 status              查看当前部署与原版备份
#   .\deploy-easymagic.ps1 deploy              备份原版并部署改版 jar
#   .\deploy-easymagic.ps1 deploy -Force       跳过"游戏是否在运行"检查（危险，明确知道自己在做什么时用）
#   .\deploy-easymagic.ps1 rollback            从 .orig 还原原版
#
# 所有机器相关的路径/文件名都来自 deploy.config.psd1，本脚本不含写死路径。
# 注意：本文件必须保存为 UTF-8 with BOM（PowerShell 5.1 会按 GBK 读无 BOM 的 .ps1，中文会乱）

param(
    [ValidateSet("status", "deploy", "rollback")]
    [string]$Action = "status",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$session = $PSScriptRoot
$configPath = Join-Path $session "deploy.config.psd1"
if (-not (Test-Path -LiteralPath $configPath)) {
    throw "找不到部署配置: $configPath"
}
$config = Import-PowerShellDataFile -LiteralPath $configPath

$newJar = Join-Path $session "build\EasyMagic-v21.1.4-1.21.1-NeoForge-rerollmemory.jar"
$jarName = $config.JarName

$instance = Get-ChildItem -LiteralPath $config.VersionsDir -Directory |
    Where-Object { $_.Name -like $config.InstanceMatch } | Select-Object -First 1
if (-not $instance) {
    throw "在 $($config.VersionsDir) 下找不到匹配 '$($config.InstanceMatch)' 的实例目录"
}
$mods = Join-Path $instance.FullName "mods"
$target = Join-Path $mods $jarName
$backup = "$target.orig"

function Get-Sha([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) { return "(缺失)" }
    return (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.Substring(0, 16)
}

# ── 游戏是否在运行：**失败即中止**，绝不静默当成"没在跑" ──
#
# 教训（实测）：受限环境（DSH 沙箱等）里进程枚举会被拒，Get-Process / Get-CimInstance /
# tasklist 都拿不到进程列表、返回空 —— 于是"游戏正在运行"被误判成"未运行"，
# jar 在游戏运行中被替换（Windows 允许覆盖已加载的 jar，不报错），修复看似成功其实没生效。
# 所以这里先确认"我能不能枚举进程"这个基本事实，不能就中止，由人显式决定。
function Test-CanEnumerateProcesses {
    try {
        $all = @(Get-Process -ErrorAction Stop)
    } catch {
        return $false
    }
    return ($all.Count -gt 0)
}

# 精确判定：只认真正的游戏进程（启动器 HMCL 自己的 javaw 命令行里不含实例路径/bootstraplauncher）
function Get-GameProcesses {
    $procs = @(Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" -ErrorAction SilentlyContinue)
    return @($procs | Where-Object {
        $_.CommandLine -and
        ($_.CommandLine -like "*$($instance.FullName)*" -or $_.CommandLine -like "*bootstraplauncher*")
    })
}

function Assert-SafeToWrite {
    if ($Force) {
        Write-Host "警告: 已用 -Force 跳过『游戏是否在运行』检查 —— 若游戏正在运行，改动不会生效。"
        return
    }
    if (-not (Test-CanEnumerateProcesses)) {
        throw "无法枚举进程（受限环境），因此**不能确认游戏是否已退出**；为安全起见不做部署。请先退出游戏，或明确知道自己在做什么时加 -Force。"
    }
    $game = Get-GameProcesses
    if ($game.Count -gt 0) {
        throw "游戏正在运行（$($game.Count) 个 java 进程匹配本实例），请先退出再操作（mod jar 需重启才生效；只改数据包可 /reload）"
    }
}

function Show-Status {
    Write-Host "实例:      $($instance.FullName)"
    Write-Host "mods jar:  $(Get-Sha $target)  $jarName"
    Write-Host "原版备份:  $(Get-Sha $backup)  $jarName.orig"
    Write-Host "改版产物:  $(Get-Sha $newJar)"
    if (-not (Test-CanEnumerateProcesses)) {
        Write-Host "游戏状态:  无法确认（本环境不能枚举进程；请用日志 logs\latest.log 的修改时间自行判断）"
    } elseif ((Get-GameProcesses).Count -gt 0) {
        Write-Host "游戏状态:  正在运行（部署/回滚前请先退出游戏）"
    } else {
        Write-Host "游戏状态:  未运行"
    }
}

switch ($Action) {
    "status" { Show-Status }

    "deploy" {
        if (-not (Test-Path -LiteralPath $newJar)) { throw "找不到改版产物: $newJar" }
        Assert-SafeToWrite
        if (-not (Test-Path -LiteralPath $backup)) {
            Copy-Item -LiteralPath $target -Destination $backup -Force
            Write-Host "已备份原版 -> $jarName.orig  ($(Get-Sha $backup))"
        } else {
            Write-Host "原版备份已存在，保留不覆盖: $(Get-Sha $backup)"
        }
        Copy-Item -LiteralPath $newJar -Destination $target -Force
        Write-Host "已部署改版 -> $(Get-Sha $target)"
        Write-Host ""
        Write-Host "**必须重启游戏才会生效**（mixin 在启动时注入）。"
        Write-Host "首次打开附魔台时会在 config 目录生成: easymagic-rerollmemory.properties"
    }

    "rollback" {
        if (-not (Test-Path -LiteralPath $backup)) { throw "找不到原版备份: $backup" }
        Assert-SafeToWrite
        Copy-Item -LiteralPath $backup -Destination $target -Force
        Write-Host "已回滚到原版 -> $(Get-Sha $target)（同样需要重启游戏）"
    }
}
