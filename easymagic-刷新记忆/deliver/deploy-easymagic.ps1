# Easy Magic「刷新记忆」改版 —— 部署 / 回滚 / 状态
#
# 用法：
#   .\deploy-easymagic.ps1 status     查看当前部署与原版备份
#   .\deploy-easymagic.ps1 deploy     备份原版并部署改版 jar
#   .\deploy-easymagic.ps1 rollback   从 .orig 还原原版
#
# 所有机器相关的路径/文件名都来自 deploy.config.psd1，本脚本不含写死路径。
# 注意：本文件必须保存为 UTF-8 with BOM（PowerShell 5.1 会按 GBK 读无 BOM 的 .ps1，中文会乱）

param(
    [ValidateSet("status", "deploy", "rollback")]
    [string]$Action = "status"
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

# 只认真正的游戏进程：启动器(HMCL)自己的 javaw 命令行里不含实例路径/bootstraplauncher
function Test-GameRunning {
    $procs = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object {
            $_.CommandLine -and
            ($_.CommandLine -like "*$($instance.FullName)*" -or $_.CommandLine -like "*bootstraplauncher*")
        }
    return [bool]$procs
}

function Show-Status {
    Write-Host "实例:      $($instance.FullName)"
    Write-Host "mods jar:  $(Get-Sha $target)  $jarName"
    Write-Host "原版备份:  $(Get-Sha $backup)  $jarName.orig"
    Write-Host "改版产物:  $(Get-Sha $newJar)"
    if (Test-GameRunning) {
        Write-Host "游戏状态:  正在运行（部署/回滚前请先退出游戏）"
    } else {
        Write-Host "游戏状态:  未运行"
    }
}

switch ($Action) {
    "status" { Show-Status }

    "deploy" {
        if (-not (Test-Path -LiteralPath $newJar)) { throw "找不到改版产物: $newJar" }
        if (Test-GameRunning) { throw "游戏正在运行，请先退出再部署（避免文件占用/半加载）" }
        if (-not (Test-Path -LiteralPath $backup)) {
            Copy-Item -LiteralPath $target -Destination $backup -Force
            Write-Host "已备份原版 -> $jarName.orig  ($(Get-Sha $backup))"
        } else {
            Write-Host "原版备份已存在，保留不覆盖: $(Get-Sha $backup)"
        }
        Copy-Item -LiteralPath $newJar -Destination $target -Force
        Write-Host "已部署改版 -> $(Get-Sha $target)"
        Write-Host ""
        Write-Host "首次打开附魔台时会在 config 目录生成: easymagic-rerollmemory.properties"
    }

    "rollback" {
        if (-not (Test-Path -LiteralPath $backup)) { throw "找不到原版备份: $backup" }
        if (Test-GameRunning) { throw "游戏正在运行，请先退出再回滚" }
        Copy-Item -LiteralPath $backup -Destination $target -Force
        Write-Host "已回滚到原版 -> $(Get-Sha $target)"
    }
}
