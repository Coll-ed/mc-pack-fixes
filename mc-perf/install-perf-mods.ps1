# MC 性能补强 —— status / install / rollback
#
# 装两个模组（都来自 Modrinth，带 SHA1 校验）：
#   · Chunky —— 空闲时自动预生成新区块
#   · spark  —— 内存/性能诊断（找出到底是谁在涨）
#
# 用法：
#   .\install-perf-mods.ps1 status     只看现状（不下载不安装）
#   .\install-perf-mods.ps1 install    下载 → 校验 SHA1/大小 → 校验 jar 结构与游戏版本 → 放进 mods
#   .\install-perf-mods.ps1 rollback   只删掉本脚本装过的那两个 jar（依据 installed.json）
#
# 注意：本文件必须保存为 UTF-8 with BOM。

param(
    [ValidateSet("status", "install", "rollback")]
    [string]$Action = "status"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

$cfgPath = Join-Path $PSScriptRoot "perf.config.psd1"
if (-not (Test-Path -LiteralPath $cfgPath)) { throw "找不到配置: $cfgPath" }
$cfg = Import-PowerShellDataFile -LiteralPath $cfgPath
if (-not $cfg.VersionsDir) { throw "配置导入后 VersionsDir 为空 —— 多半是 perf.config.psd1 丢了 UTF-8 BOM" }

$instance = Get-ChildItem -LiteralPath $cfg.VersionsDir -Directory |
    Where-Object { $_.Name -like $cfg.InstanceMatch } | Select-Object -First 1
if (-not $instance) { throw "在 $($cfg.VersionsDir) 下找不到匹配 '$($cfg.InstanceMatch)' 的实例" }
$modsDir = Join-Path $instance.FullName 'mods'
$downloadDir = Join-Path $PSScriptRoot $cfg.DownloadDir
$manifestPath = Join-Path $PSScriptRoot $cfg.ManifestName

function Get-Sha1([string]$path) {
    $sha = [Security.Cryptography.SHA1]::Create()
    try {
        $fs = [IO.File]::Open($path, 'Open', 'Read', 'ReadWrite')
        try { return ([BitConverter]::ToString($sha.ComputeHash($fs)) -replace '-','').ToLowerInvariant() }
        finally { $fs.Close() }
    } finally { $sha.Dispose() }
}

function Get-ModsToml([string]$jarPath) {
    $z = [IO.Compression.ZipFile]::OpenRead($jarPath)
    try {
        foreach ($n in @('META-INF/neoforge.mods.toml', 'META-INF/mods.toml')) {
            $e = @($z.Entries | Where-Object { $_.FullName -eq $n })[0]
            if ($e) {
                $sr = New-Object IO.StreamReader($e.Open())
                try { return $sr.ReadToEnd() } finally { $sr.Close() }
            }
        }
        return $null
    } finally { $z.Dispose() }
}

function Test-GameRunning {
    $procs = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -and ($_.CommandLine -like "*$($instance.FullName)*" -or $_.CommandLine -like '*bootstraplauncher*') }
    return [bool]$procs
}

switch ($Action) {
    "status" {
        Write-Host "实例: $($instance.FullName)"
        Write-Host "游戏运行中: $(Test-GameRunning)  （运行中装模组要下次启动才生效）"
        Write-Host ""
        foreach ($m in $cfg.Mods) {
            $dest = Join-Path $modsDir $m.File
            $dl = Join-Path $downloadDir $m.File
            Write-Host "· $($m.Name) —— $($m.Purpose)"
            Write-Host "    目标文件 : $($m.File)"
            Write-Host "    已安装   : $(if (Test-Path -LiteralPath $dest) { '是  SHA1=' + (Get-Sha1 $dest) } else { '否' })"
            Write-Host "    已下载   : $(if (Test-Path -LiteralPath $dl) { '是  SHA1=' + (Get-Sha1 $dl) } else { '否' })"
            Write-Host "    期望     : SHA1=$($m.Sha1)  大小=$($m.Size)"
        }
        Write-Host ""
        Write-Host "安装记录: $(if (Test-Path -LiteralPath $manifestPath) { $manifestPath } else { '(无 —— 说明本脚本还没装过任何东西)' })"
    }

    "install" {
        New-Item -ItemType Directory -Path $downloadDir -Force | Out-Null
        $added = New-Object System.Collections.Generic.List[object]

        foreach ($m in $cfg.Mods) {
            $dl = Join-Path $downloadDir $m.File
            $dest = Join-Path $modsDir $m.File

            # 下载（暂存到工作区；已存在且校验通过则跳过下载）
            $needDownload = $true
            if (Test-Path -LiteralPath $dl) {
                if ((Get-Sha1 $dl) -eq $m.Sha1 -and (Get-Item -LiteralPath $dl).Length -eq $m.Size) {
                    Write-Host "[$($m.Name)] 暂存文件已存在且校验通过，跳过下载"
                    $needDownload = $false
                } else {
                    Write-Host "[$($m.Name)] 暂存文件校验不符，重新下载"
                    Remove-Item -LiteralPath $dl -Force
                }
            }
            if ($needDownload) {
                Write-Host "[$($m.Name)] 下载 $($m.Url)"
                Invoke-WebRequest -Uri $m.Url -OutFile $dl -UseBasicParsing
            }

            # 校验：SHA1 + 大小（两者都必须对）
            $actualSha = Get-Sha1 $dl
            $actualSize = (Get-Item -LiteralPath $dl).Length
            Write-Host "[$($m.Name)] 校验 SHA1=$actualSha 大小=$actualSize"
            if ($actualSha -ne $m.Sha1) { throw "[$($m.Name)] SHA1 不匹配（期望 $($m.Sha1)）—— 拒绝安装" }
            if ($actualSize -ne $m.Size) { throw "[$($m.Name)] 大小不匹配（期望 $($m.Size)）—— 拒绝安装" }

            # 校验：确实是 Forge/NeoForge 模组，且声明支持 1.21.1
            $toml = Get-ModsToml $dl
            if (-not $toml) { throw "[$($m.Name)] 不是有效的 NeoForge 模组（jar 里没有 neoforge.mods.toml）" }
            if ($toml -notmatch '1\.21\.1') {
                Write-Warning "[$($m.Name)] 的 mods.toml 里没看到 1.21.1 —— 请人工确认后再用（本脚本仍会安装，但请留意）"
            }

            # 安装：不覆盖已存在的**不同**文件
            if (Test-Path -LiteralPath $dest) {
                if ((Get-Sha1 $dest) -eq $m.Sha1) {
                    Write-Host "[$($m.Name)] mods 里已是同一文件，无需处理"
                } else {
                    throw "[$($m.Name)] mods 里已存在同名但内容不同的文件：$dest —— 请先人工确认（拒绝覆盖）"
                }
            } else {
                Copy-Item -LiteralPath $dl -Destination $dest -Force
                Write-Host "[$($m.Name)] 已安装 -> $dest"
            }
            # 不论本次是"复制"还是"已存在且校验一致"，都登记进清单。
            # 理由：重复执行 install 应当幂等；并且能把"文件已复制成功、但清单没写成"的状态自愈回来
            # （否则 rollback 会因为找不到记录而拒绝工作）。
            $added.Add([pscustomobject]@{ File = $m.File; Sha1 = $m.Sha1; Name = $m.Name })
        }

        if ($added.Count -gt 0) {
            # 用 List 累加，不用数组 +（PowerShell 里对 List[object] 做数组相加会抛"参数类型不匹配"）
            $managed = New-Object System.Collections.Generic.List[object]
            if (Test-Path -LiteralPath $manifestPath) {
                foreach ($e in @(Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json)) {
                    if ($e -and $e.File) { $managed.Add($e) }
                }
            }
            foreach ($a in $added) {
                $dup = $false
                foreach ($e in $managed) { if ($e.File -eq $a.File) { $dup = $true; break } }
                if (-not $dup) { $managed.Add($a) }
            }
            # 用 -InputObject 传数组：管道形式会把单元素数组拆开，写成对象而不是数组
            $manifestArray = [object[]]$managed.ToArray()
            [IO.File]::WriteAllText($manifestPath,
                (ConvertTo-Json -InputObject $manifestArray -Depth 4), [Text.UTF8Encoding]::new($false))
            Write-Host ""
            Write-Host "已记录安装清单（$($managed.Count) 项）: $manifestPath"
        }
        Write-Host ""
        Write-Host "完成。若游戏正在运行，请重启后生效。"
    }

    "rollback" {
        if (-not (Test-Path -LiteralPath $manifestPath)) { throw "找不到安装记录 $manifestPath —— 本脚本没装过东西" }
        $list = @((Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json))
        foreach ($m in $list) {
            $dest = Join-Path $modsDir $m.File
            if (Test-Path -LiteralPath $dest) {
                # 只删"内容与记录一致"的文件，避免误删用户自己换过的版本
                if ((Get-Sha1 $dest) -eq $m.Sha1) {
                    Remove-Item -LiteralPath $dest -Force
                    Write-Host "已移除 $($m.File)"
                } else {
                    Write-Warning "$($m.File) 内容与记录不符（可能你手动换过），跳过不删"
                }
            } else {
                Write-Host "$($m.File) 本来就不在 mods 里，跳过"
            }
        }
    }
}
