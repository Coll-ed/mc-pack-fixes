# 从 perf.config.psd1 生成"自动预生成"数据包
#
# 产物：build\<DatapackName>.zip（根目录含 pack.mcmeta）
#       → 放进 .minecraft\datapacks 后，每个存档**世界加载时自动恢复 Chunky 的区块生成任务**
#
# 做法（依据 Chunky 官方文档）：
#   chunky radius / chunky shape / chunky spawn 设置选择范围
#   chunky start     启动生成任务（Chunky 会保存任务进度）
#   chunky continue  恢复"当前或已保存的"任务 —— 这句就是"自动续跑"
# 计分板标记用来保证"每个存档只初始化一次"，之后每次加载都只 continue。
#
# 注意：本文件必须保存为 UTF-8 with BOM。

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$cfgPath = Join-Path $PSScriptRoot 'perf.config.psd1'
if (-not (Test-Path -LiteralPath $cfgPath)) { throw "找不到配置: $cfgPath" }
$cfg = Import-PowerShellDataFile -LiteralPath $cfgPath
if (-not $cfg.VersionsDir) { throw "配置导入后 VersionsDir 为空 —— 多半是 perf.config.psd1 丢了 UTF-8 BOM" }
if (-not $cfg.Pregen) { throw "配置里没有 Pregen 段" }
$p = $cfg.Pregen

# ── 输入校验：该抛就抛，不静默 ──
if (-not $p.Namespace) { throw "Pregen.Namespace 为空" }
if (-not $p.DatapackName) { throw "Pregen.DatapackName 为空" }
# hardcode-ok: 16 是 Minecraft 对计分板名长度的硬限制，不是可调参数
if ($p.ScoreboardObjective.Length -gt 16) {
    throw "计分板名 '$($p.ScoreboardObjective)' 有 $($p.ScoreboardObjective.Length) 字符，超过 Minecraft 的 16 字符上限"
}
if (-not $p.Worlds -or @($p.Worlds).Count -eq 0) { throw "Pregen.Worlds 为空" }
if ($p.Radius -lt 1) { throw "Pregen.Radius 必须为正整数（当前 $($p.Radius)）" }
if ($p.PackFormat -lt 1) { throw "Pregen.PackFormat 必须为正整数" }

$buildDir = Join-Path $PSScriptRoot $cfg.BuildDir
$root = Join-Path $buildDir $p.DatapackName
if (Test-Path -LiteralPath $root) { Remove-Item -LiteralPath $root -Recurse -Force }
$fnDir = Join-Path $root "data\$($p.Namespace)\function"
$tagDir = Join-Path $root 'data\minecraft\tags\function'
New-Item -ItemType Directory -Path $fnDir -Force | Out-Null
New-Item -ItemType Directory -Path $tagDir -Force | Out-Null

# 不带 BOM：Minecraft 解析 JSON/mcfunction 时 BOM 会造成解析问题
$utf8 = [Text.UTF8Encoding]::new($false)

# ── pack.mcmeta ──
$packMeta = [ordered]@{
    pack = [ordered]@{
        pack_format = $p.PackFormat
        description = '自动预生成：世界加载时恢复 Chunky 区块生成任务'
    }
}
$packMetaPath = Join-Path $root 'pack.mcmeta'
[IO.File]::WriteAllText($packMetaPath, ($packMeta | ConvertTo-Json -Depth 4), $utf8)

# ── first_setup：每个存档只跑一次 ──
$setup = New-Object System.Collections.Generic.List[string]
$setup.Add("# 每个存档只执行一次：设置选择范围并启动生成任务")
$setup.Add("# 之后由 $($p.OnLoadFunction) 里的计分板标记 $($p.ScoreboardHolder) 判定，只走 chunky continue")
$setup.Add("scoreboard objectives add $($p.ScoreboardObjective) dummy")
foreach ($w in @($p.Worlds)) {
    $setup.Add("chunky world $w")
    $setup.Add("chunky shape $($p.Shape)")
    if ($p.CenterOnSpawn) { $setup.Add('chunky spawn') }
    $setup.Add("chunky radius $($p.Radius)")
    $setup.Add('chunky start')
}
$setup.Add("scoreboard players set $($p.ScoreboardHolder) configured $($p.ScoreboardObjective) 1")
$setupPath = Join-Path $fnDir "$($p.FirstSetupFunction).mcfunction"
[IO.File]::WriteAllLines($setupPath, [string[]]$setup.ToArray(), $utf8)

# ── on_load：每次世界加载 ──
$onLoad = @(
    '# 每次世界加载都会执行：首次则初始化，然后恢复已保存的生成任务',
    "execute unless score $($p.ScoreboardHolder) configured $($p.ScoreboardObjective) matches 1 run function $($p.Namespace):$($p.FirstSetupFunction)",
    'chunky continue'
)
$onLoadPath = Join-Path $fnDir "$($p.OnLoadFunction).mcfunction"
[IO.File]::WriteAllLines($onLoadPath, [string[]]$onLoad, $utf8)

# ── load 标签 ──
$tagPath = Join-Path $tagDir 'load.json'
[IO.File]::WriteAllText($tagPath,
    ([ordered]@{ values = @("$($p.Namespace):$($p.OnLoadFunction)") } | ConvertTo-Json -Depth 4), $utf8)

# ── 生成后验证（JSON 合法性）──
$jsonOk = $true
foreach ($j in @($packMetaPath, $tagPath)) {
    try {
        [void](Get-Content -LiteralPath $j -Raw | ConvertFrom-Json)
        Write-Host "  [OK] JSON 合法: $($j.Replace("$buildDir\", ''))"
    } catch {
        Write-Host "  [FAIL] JSON 不合法: $j -> $($_.Exception.Message)"
        $jsonOk = $false
    }
}
if (-not $jsonOk) { throw "生成的 JSON 校验失败" }

# ── 打包 ──
$zip = Join-Path $buildDir "$($p.DatapackName).zip"
if (Test-Path -LiteralPath $zip) { Remove-Item -LiteralPath $zip -Force }
[IO.Compression.ZipFile]::CreateFromDirectory($root, $zip, [IO.Compression.CompressionLevel]::Optimal, $false)

# ── 生成后验证（zip 结构：pack.mcmeta 必须在根）──
$z = [IO.Compression.ZipFile]::OpenRead($zip)
$entryNames = @($z.Entries | ForEach-Object { $_.FullName })
$z.Dispose()
if ($entryNames -notcontains 'pack.mcmeta') {
    throw "数据包根目录缺 pack.mcmeta（实际条目：$($entryNames -join ', ')）"
}
Write-Host "  [OK] 压缩包根目录含 pack.mcmeta"
Write-Host ""
Write-Host "产物: $zip  ($((Get-Item -LiteralPath $zip).Length) 字节)"
Write-Host ""
Write-Host "── 生成内容预览（这就是将来会自动执行的东西）──"
Write-Host "data\$($p.Namespace)\function\$($p.FirstSetupFunction).mcfunction"
Get-Content -LiteralPath $setupPath | ForEach-Object { Write-Host "   $_" }
Write-Host "data\$($p.Namespace)\function\$($p.OnLoadFunction).mcfunction"
Get-Content -LiteralPath $onLoadPath | ForEach-Object { Write-Host "   $_" }
Write-Host ""
Write-Host "安装位置: $($cfg.GlobalDatapackDir)\$($p.DatapackName).zip"
