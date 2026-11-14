# Audit broken Minecraft tags: for each failing tag id, list every contributing file,
# every entry, and which entries cannot be resolved against the installed packs.
#
# 路径全部外置：实例目录由 -Instance 传入，其余从它派生（不写死任何绝对路径）。
# ASCII-only on purpose (PowerShell 5.1 reads no-BOM files as GBK).
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Instance,
    [string[]]$ExtraPackDirs = @()
)
Add-Type -AssemblyName System.IO.Compression.FileSystem

$targets = @(
    'minecraft:enchantable/armor',
    'minecraft:enchantable/durability',
    'minecraft:enchantable/equippable',
    'minecraft:enchantable/vanishing',
    'vitalslayer:enchantable/percent_slayer',
    'dragonloot:explosion_resistant',
    'swiftstrike:enchantable/swift_strike',
    'enderitemod:enderite_items',
    'c:cooked_meats',
    'c:foods/meat',
    'c:enchantables',
    'lunarinispread:fowl',
    'lunarinispread:fish',
    'kaleidoscope_nether:mod_items',
    'plonk:unplaceable',
    'maid_storage_manager:use_components',
    'blueprint:generates_overrides',
    'touhou_little_maid_spell:fairy_maid_cafe',
    'touhou_little_maid_spell:yin_yang_altar',
    'kaleidoscope_cookery:sickle_breakable_no_drops',
    'kaleidoscope_cookery:sickle_breakable_with_drops'
)

# ---- collect packs (all derived from -Instance) ----
$jars = @()
$modsDir = Join-Path $Instance 'mods'
if (Test-Path -LiteralPath $modsDir) { $jars += Get-ChildItem -LiteralPath $modsDir -Filter *.jar }
# vanilla data jar = the single jar sitting directly in the instance folder
$jars += Get-ChildItem -LiteralPath $Instance -Filter *.jar -File
$saveRoot = Join-Path $Instance 'saves'
if (Test-Path -LiteralPath $saveRoot) {
    foreach ($s in Get-ChildItem -LiteralPath $saveRoot -Directory) {
        $dp = Join-Path $s.FullName 'datapacks'
        if (Test-Path -LiteralPath $dp) { $jars += Get-ChildItem -LiteralPath $dp -Filter *.zip }
    }
}
foreach ($d in $ExtraPackDirs) { if (Test-Path -LiteralPath $d) { $jars += Get-ChildItem -LiteralPath $d -Filter *.zip } }
Write-Output "packs scanned: $($jars.Count)"

# ---- existence indexes ----
$itemModels = New-Object System.Collections.Generic.HashSet[string]
$blockStates = New-Object System.Collections.Generic.HashSet[string]
$dataFiles = New-Object System.Collections.Generic.HashSet[string]
$tagFiles = New-Object System.Collections.Generic.HashSet[string]

foreach ($j in $jars) {
    try {
        $z = [System.IO.Compression.ZipFile]::OpenRead($j.FullName)
        foreach ($e in $z.Entries) {
            $n = $e.FullName
            if ($n -match '^assets/([^/]+)/models/item/(.+)\.json$') { [void]$itemModels.Add("$($matches[1]):$($matches[2])") }
            elseif ($n -match '^assets/([^/]+)/models/block/(.+)\.json$') { [void]$itemModels.Add("$($matches[1]):$($matches[2])") }
            elseif ($n -match '^assets/([^/]+)/blockstates/(.+)\.json$') { [void]$blockStates.Add("$($matches[1]):$($matches[2])") }
            if ($n -match '^data/([^/]+)/tags/([^/]+)/(.+)\.json$') { [void]$tagFiles.Add("$($matches[1]):$($matches[2]):$($matches[3])") }
            if ($n -match '^data/([^/]+)/([^/]+)/(.+)\.json$') { [void]$dataFiles.Add("$($matches[1]):$($matches[2]):$($matches[3])") }
        }
        $z.Dispose()
    } catch { }
}

function Test-Concrete([string]$registry, [string]$id) {
    $i = $id.IndexOf(':'); $ns = $id.Substring(0, $i); $path = $id.Substring($i + 1)
    if ($ns -eq 'minecraft') { return $true }   # vanilla registries register these ids; backports do too
    switch ($registry) {
        'item' { return ($itemModels.Contains("$ns`:$path") -or $blockStates.Contains("$ns`:$path")) }
        'block' { return $blockStates.Contains("$ns`:$path") }
        default { return $dataFiles.Contains("$ns`:$registry`:$path") }
    }
}
function Test-TagRef([string]$registry, [string]$ref) {
    $r = $ref.TrimStart('#')
    $i = $r.IndexOf(':'); $ns = $r.Substring(0, $i); $path = $r.Substring($i + 1)
    return $tagFiles.Contains("$ns`:$registry`:$path")
}

foreach ($t in $targets) {
    $i = $t.IndexOf(':'); $ns = $t.Substring(0, $i); $path = $t.Substring($i + 1)
    Write-Output "==================== $t ===================="
    $found = $false
    foreach ($j in $jars) {
        try {
            $z = [System.IO.Compression.ZipFile]::OpenRead($j.FullName)
            foreach ($e in $z.Entries) {
                $pattern = "^data/$([regex]::Escape($ns))/tags/([^/]+)/$([regex]::Escape($path))\.json$"
                if ($e.FullName -notmatch $pattern) { continue }
                $registry = $matches[1]
                $found = $true
                $r = New-Object System.IO.StreamReader($e.Open()); $body = $r.ReadToEnd(); $r.Close()
                $replace = if ($body -match '"replace"\s*:\s*true') { 'REPLACE=true' } else { 'replace=false/absent' }
                Write-Output "--- [$registry] $($j.Name) :: $($e.FullName)  ($replace, $($body.Length) chars)"
                $missing = @()
                foreach ($m in [regex]::Matches($body, '"#([a-z0-9_.\-]+:[a-z0-9_/.\-]+)"')) {
                    $ref = $m.Groups[1].Value
                    $ok = Test-TagRef $registry $ref
                    if (-not $ok) { $missing += "#$ref" }
                    if (-not $ok) { Write-Output "      tagref  MISS  #$ref" }
                }
                foreach ($m in [regex]::Matches($body, '"([a-z0-9_.\-]+:[a-z0-9_/.\-]+)"')) {
                    $id = $m.Groups[1].Value
                    if (-not (Test-Concrete $registry $id)) { $missing += $id; Write-Output "      entry   MISS  $id" }
                }
                if ($missing.Count -gt 0) { Write-Output ("      >> UNRESOLVED: " + ($missing -join ', ')) }
                if ($body.Length -lt 700) { Write-Output $body }
            }
            $z.Dispose()
        } catch { }
    }
    if (-not $found) { Write-Output "  (no tag file found in any pack)" }
}
