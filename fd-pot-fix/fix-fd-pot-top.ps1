# 农夫乐事煮锅顶面（水面）修复 —— status / apply / rollback
#
# 问题：资源包 SPBR-Farmer's Delight 把 cooking_pot_top.png 的锅口中间 8x8 改成**全透明**，
#       并给模型加了 render_type:cutout。而农夫乐事**没有**煮锅渲染器、原版模型也**没有内壁几何**，
#       所以那个洞是"看穿"的 —— 从上方看既没有水面，也没有锅内侧材质，只看到外面的地面。
#
# 修法：把资源包里的 cooking_pot_top.png **换回模组原版**（锅口恢复实心深灰）。
#       三种锅状态（直接放置 / 带提手 / 放托盘）共用这一张贴图，所以换贴图一次修好全部。
#       法线贴图 cooking_pot_top_n.png 已确认无透明洞（16x16 全不透明），不需要动。
#
# 用法：
#   .\fix-fd-pot-top.ps1 status     只看现状（不改任何文件）
#   .\fix-fd-pot-top.ps1 apply      备份资源包 + 换回原版贴图
#   .\fix-fd-pot-top.ps1 rollback   从备份还原资源包
#
# 注意：本文件必须保存为 UTF-8 with BOM（PowerShell 5.1 按 GBK 读无 BOM 的中文 .ps1 会出错）

param(
    [ValidateSet("status", "apply", "rollback")]
    [string]$Action = "status"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

$cfgPath = Join-Path $PSScriptRoot "fix.config.psd1"
if (-not (Test-Path -LiteralPath $cfgPath)) { throw "找不到配置: $cfgPath" }
$cfg = Import-PowerShellDataFile -LiteralPath $cfgPath

# 工作区临时目录（沙箱允许自由读写，用来生成新 zip）
$workDir = Join-Path $PSScriptRoot "work"
if (-not (Test-Path -LiteralPath $workDir)) { New-Item -ItemType Directory -Path $workDir | Out-Null }

$instance = Get-ChildItem -LiteralPath $cfg.VersionsDir -Directory |
    Where-Object { $_.Name -like $cfg.InstanceMatch } | Select-Object -First 1
if (-not $instance) { throw "在 $($cfg.VersionsDir) 下找不到匹配 '$($cfg.InstanceMatch)' 的实例" }

$packPath = Join-Path $instance.FullName "resourcepacks\$($cfg.ResourcePackName)"
$backupPath = "$packPath.bak"
$sourceJar = Get-ChildItem -LiteralPath (Join-Path $instance.FullName 'mods') -Filter $cfg.SourceModJarPattern |
    Select-Object -First 1
if (-not $sourceJar) { throw "找不到提供原版纹理的模组 jar（模式 $($cfg.SourceModJarPattern)）" }

function Get-EntryBytes([string]$zipPath, [string]$entryName) {
    $z = [IO.Compression.ZipFile]::OpenRead($zipPath)
    try {
        $e = @($z.Entries | Where-Object { $_.FullName -eq $entryName })[0]
        if (-not $e) { return $null }
        $ms = New-Object IO.MemoryStream
        $s = $e.Open(); $s.CopyTo($ms); $s.Close()
        return $ms.ToArray()
    } finally { $z.Dispose() }
}

function Get-EntryCount([string]$zipPath) {
    $z = [IO.Compression.ZipFile]::OpenRead($zipPath)
    try { return $z.Entries.Count } finally { $z.Dispose() }
}

function Get-Sha([byte[]]$bytes) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash($bytes)) -replace '-','').Substring(0,16) } finally { $sha.Dispose() }
}

function Test-GameRunning {
    $procs = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object {
            $_.CommandLine -and
            ($_.CommandLine -like "*$($instance.FullName)*" -or $_.CommandLine -like "*bootstraplauncher*")
        }
    return [bool]$procs
}

$baseBytes = Get-EntryBytes $sourceJar.FullName $cfg.TextureEntry
if (-not $baseBytes) { throw "模组 jar 里没有 $($cfg.TextureEntry)" }
$baseSha = Get-Sha $baseBytes

switch ($Action) {
    "status" {
        Write-Host "实例:            $($instance.FullName)"
        Write-Host "资源包:          $($cfg.ResourcePackName)"
        Write-Host "模组(原版纹理):  $($sourceJar.Name)"
        Write-Host "原版纹理 SHA:    $baseSha"
        if (Test-Path -LiteralPath $packPath) {
            $cur = Get-EntryBytes $packPath $cfg.TextureEntry
            Write-Host "资源包内纹理 SHA: $(if ($cur) { Get-Sha $cur } else { '(缺失)' })"
            Write-Host "资源包条目数:    $(Get-EntryCount $packPath)"
            if ($cur) {
                $same = (Get-Sha $cur) -eq $baseSha
                Write-Host "状态:            $(if ($same) { '已修（与原版一致）' } else { '未修（仍是包里的挖洞版）' })"
            }
        } else { Write-Host "资源包:          (不存在)" }
        Write-Host "备份:            $(if (Test-Path -LiteralPath $backupPath) { "$backupPath  $((Get-Item -LiteralPath $backupPath).Length) 字节  SHA=$(Get-Sha ([IO.File]::ReadAllBytes($backupPath)))" } else { '(无)' })"
        Write-Host "游戏运行中:      $(Test-GameRunning)"
    }

    "apply" {
        if (-not (Test-Path -LiteralPath $packPath)) { throw "找不到资源包: $packPath" }
        if (Test-GameRunning) { throw "游戏正在运行，请先退出（资源包可能被占用），再执行 apply" }

        # ── 为什么不在原文件上就地改 ──
        # 实测：ZipFile 的 Update 模式要求独占（FileShare.None），而这个 zip 被某个进程
        # 以「允许读写、但不允许删除」的方式持有着 —— 于是 Update 打开失败、改名也失败，
        # 但「共享读写打开」与「覆写」都可以。
        # 所以改成：拷到工作区 -> 在工作区生成修好的新 zip -> 再覆写原文件。
        $tmpOriginal = Join-Path $workDir "work_original.zip"
        $tmpFixed = Join-Path $workDir "work_fixed.zip"
        Copy-Item -LiteralPath $packPath -Destination $tmpOriginal -Force

        if (-not (Test-Path -LiteralPath $backupPath)) {
            Copy-Item -LiteralPath $packPath -Destination $backupPath -Force
            Write-Host "已备份资源包 -> $(Split-Path -Leaf $backupPath)  ($((Get-Item -LiteralPath $backupPath).Length) 字节)"
        } else {
            Write-Host "备份已存在，保留不覆盖: $backupPath"
        }
        $curHash = Get-Sha ([IO.File]::ReadAllBytes($packPath))
        $bakHash = Get-Sha ([IO.File]::ReadAllBytes($backupPath))
        Write-Host "当前文件 SHA: $curHash   备份 SHA: $bakHash"
        if ($curHash -ne $bakHash) { throw "备份与当前文件不一致，停止以免覆盖出错（请先人工确认）" }

        if (Test-Path -LiteralPath $tmpFixed) { Remove-Item -LiteralPath $tmpFixed -Force }
        $src = [IO.Compression.ZipFile]::OpenRead($tmpOriginal)
        $dst = [IO.Compression.ZipFile]::Open($tmpFixed, 'Create')
        try {
            foreach ($e in $src.Entries) {
                if ($e.FullName -eq $cfg.TextureEntry) { continue }   # 先跳过，稍后写入修好的
                $ne = $dst.CreateEntry($e.FullName, [IO.Compression.CompressionLevel]::Optimal)
                $ne.LastWriteTime = $e.LastWriteTime
                if ($e.Length -gt 0) {
                    $in = $e.Open(); $out = $ne.Open()
                    $in.CopyTo($out)
                    $out.Close(); $in.Close()
                }
            }
            $new = $dst.CreateEntry($cfg.TextureEntry, [IO.Compression.CompressionLevel]::Optimal)
            $new.LastWriteTime = [DateTimeOffset]::new(1980, 1, 1, 0, 0, 0, [TimeSpan]::Zero)   # zip 时间戳合法下限为 1980
            $out = $new.Open(); $out.Write($baseBytes, 0, $baseBytes.Length); $out.Close()
        } finally { $src.Dispose(); $dst.Dispose() }

        $beforeCount = Get-EntryCount $tmpOriginal
        $afterCount = Get-EntryCount $tmpFixed
        Write-Host "条目数: 原 $beforeCount -> 新 $afterCount"
        if ($afterCount -ne $beforeCount) { throw "条目数不一致（$beforeCount -> $afterCount），已中止，原文件未改动" }
        $fixedTex = Get-EntryBytes $tmpFixed $cfg.TextureEntry
        if ((Get-Sha $fixedTex) -ne $baseSha) { throw "新包内贴图与原版不一致，已中止，原文件未改动" }

        # 覆写原文件（覆写只需写共享，不需要删除共享，所以这条能成）
        [IO.File]::WriteAllBytes($packPath, [IO.File]::ReadAllBytes($tmpFixed))

        # 覆写后回读校验
        $nowCount = Get-EntryCount $packPath
        $nowTex = Get-EntryBytes $packPath $cfg.TextureEntry
        Write-Host "覆写后条目数: $nowCount    贴图 SHA: $(Get-Sha $nowTex)  (原版 $baseSha)"
        if ($nowCount -ne $beforeCount) { throw "覆写后条目数异常（$beforeCount -> $nowCount），请执行 rollback" }
        if ((Get-Sha $nowTex) -ne $baseSha) { throw "覆写后贴图与原版不一致，请执行 rollback" }
        Remove-Item -LiteralPath $tmpOriginal, $tmpFixed -Force -ErrorAction SilentlyContinue
        Write-Host "已修复：锅口恢复为原版实心深灰（三种锅状态共用这张贴图，一并修好）"
        Write-Host "进游戏后按 F3+T 重载资源，或重启游戏生效。"
    }

    "rollback" {
        if (-not (Test-Path -LiteralPath $backupPath)) { throw "找不到备份: $backupPath" }
        if (Test-GameRunning) { throw "游戏正在运行，请先退出再回滚" }
        [IO.File]::WriteAllBytes($packPath, [IO.File]::ReadAllBytes($backupPath))
        $now = Get-EntryBytes $packPath $cfg.TextureEntry
        Write-Host "已从备份还原 -> 文件 $((Get-Item -LiteralPath $packPath).Length) 字节  贴图 SHA $(Get-Sha $now)"
    }
}
