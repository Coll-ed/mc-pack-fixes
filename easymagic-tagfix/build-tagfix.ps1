# Build ONE datapack that repairs every broken tag found by the audit.
#
# Paths are externalised: instance dir comes in as a parameter, everything else is
# derived from it. Bad / renamed entries live in $edits - the single source of truth.
# ASCII-only (PowerShell 5.1 reads no-BOM files as GBK).
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Instance,
    [Parameter(Mandatory = $true)][string]$OutDir
)
Add-Type -AssemblyName System.IO.Compression.FileSystem
$ErrorActionPreference = 'Stop'

# ---- what to fix: jar-name pattern, path in jar, registry, and the edit ----
# drop = entries to remove; rename = id replacements; forceEmpty = declare values:[]
$edits = @(
    @{ jar = 'kaleidoscope_nether'; path = 'data/c/tags/item/cooked_meats.json';                  reg = 'item';          drop = @('kaleidoscope_cookery:cooked_donkey_meat') },
    @{ jar = 'lunarinispread';      path = 'data/lunarinispread/tags/item/fowl.json';              reg = 'item';          drop = @('kaleidoscope_cookery:yakitori') },
    @{ jar = 'lunarinispread';      path = 'data/lunarinispread/tags/item/fish.json';              reg = 'item';          drop = @('kaleidoscope_cookery:braised_fish') },
    @{ jar = 'stickit';             path = 'data/plonk/tags/item/unplaceable.json';                reg = 'item';          drop = @('plonk:placed_items') },
    @{ jar = 'kaleidoscope_nether'; path = 'data/kaleidoscope_nether/tags/item/mod_items.json';    reg = 'item';          drop = @('kaleidoscope_nether:glowing_soup', 'kaleidoscope_nether:glowing_pudding', 'kaleidoscope_nether:glowing_kabob', 'kaleidoscope_nether:glowing_salad') },
    @{ jar = 'spear-backport';      path = 'data/enderitemod/tags/item/enderite_items.json';       reg = 'item';          drop = @('enderitemod:enderite_spear') },
    @{ jar = 'spear-backport';      path = 'data/dragonloot/tags/item/explosion_resistant.json';   reg = 'item';          drop = @('dragonloot:dragon_spear') },
    # blueprint wants TRIM MATERIALS here, the mod listed the ingredient ITEMS -> 1:1 rename
    @{ jar = 'unusualend';          path = 'data/blueprint/tags/trim_material/generates_overrides.json'; reg = 'trim_material';
       rename = @{ 'unusualend:prismalite_gem' = 'unusualend:prismatic_material'; 'unusualend:shiny_crystal' = 'unusualend:shiny_material';
                   'unusualend:citrine_chunk' = 'unusualend:citrine_material'; 'unusualend:pearlescent_ingot' = 'unusualend:pearlescent_material' } },
    # keep the other contributor of the same tag, so the merged result stays complete
    @{ jar = 'creaturefeature';     path = 'data/blueprint/tags/trim_material/generates_overrides.json'; reg = 'trim_material'; keep = $true },
    @{ jar = 'kaleidoscope_compat'; path = 'data/kaleidoscope_cookery/tags/block/sickle_breakable_no_drops.json';   reg = 'block'; forceEmpty = $true },
    @{ jar = 'kaleidoscope_compat'; path = 'data/kaleidoscope_cookery/tags/block/sickle_breakable_with_drops.json'; reg = 'block'; forceEmpty = $true },
    # self-referencing tags (the tag lists an item with the very same id) -> declare valid+empty
    @{ jar = 'touhou_little_maid_spell'; path = 'data/touhou_little_maid_spell/tags/item/fairy_maid_cafe.json'; reg = 'item'; forceEmpty = $true },
    @{ jar = 'touhou_little_maid_spell'; path = 'data/touhou_little_maid_spell/tags/item/yin_yang_altar.json';  reg = 'item'; forceEmpty = $true }
)

$jars = Get-ChildItem -LiteralPath (Join-Path $Instance 'mods') -Filter *.jar

function Get-JarEntryText($jarFile, $entryPath) {
    $z = [System.IO.Compression.ZipFile]::OpenRead($jarFile)
    try {
        $e = $z.GetEntry($entryPath)
        if (-not $e) { return $null }
        $r = New-Object System.IO.StreamReader($e.Open()); $c = $r.ReadToEnd(); $r.Close(); return $c
    } finally { $z.Dispose() }
}

# target -> merged value list (order preserved, deduped)
$merged = @{}
foreach ($ed in $edits) {
    $jar = $jars | Where-Object { $_.Name -like "*$($ed.jar)*" } | Select-Object -First 1
    if (-not $jar) { Write-Output "[WARN] jar not found for pattern: $($ed.jar)"; continue }
    $key = "$($ed.path)"
    if (-not $merged.ContainsKey($key)) { $merged[$key] = @{ reg = $ed.reg; values = New-Object System.Collections.Generic.List[string] } }

    if ($ed.forceEmpty) { continue }                     # nothing to copy: stays empty
    $body = Get-JarEntryText $jar.FullName $ed.path
    if (-not $body) { Write-Output "[WARN] entry not found: $($ed.jar) :: $($ed.path)"; continue }
    $json = $body | ConvertFrom-Json
    foreach ($v in $json.values) {
        $id = if ($v -is [string]) { $v } else { $v.id }
        if (-not $id) { continue }
        if ($ed.drop -and ($ed.drop -contains $id)) { Write-Output "  drop   $id   ($($ed.path))"; continue }
        if ($ed.rename -and $ed.rename.ContainsKey($id)) {
            $new = $ed.rename[$id]
            Write-Output "  rename $id -> $new"
            if (-not $merged[$key].values.Contains($new)) { [void]$merged[$key].values.Add($new) }
            continue
        }
        if (-not $merged[$key].values.Contains($id)) { [void]$merged[$key].values.Add($id) }
    }
}

# hand-written replacements: semantics that drop/rename cannot express.
# c:foods/meat wants to union two convention sub-tags that NO pack defines; the mod
# wrote them without "required": false, which makes the whole tag fail to load.
# Keep the intent, mark them optional.
$explicit = @(
    @{ path = 'data/c/tags/item/foods/meat.json'; raw = @'
{
  "replace": true,
  "values": [
    { "id": "#c:foods/meat/raw", "required": false },
    { "id": "#c:foods/meat/cooked", "required": false }
  ]
}
'@ }
)
foreach ($ex in $explicit) {
    $merged[$ex.path] = @{ reg = 'item'; raw = $ex.raw; values = @() }
    Write-Output "  explicit $($ex.path)"
}

# ---- write staging tree ----
if (Test-Path -LiteralPath $OutDir) { Remove-Item -LiteralPath $OutDir -Recurse -Force }
New-Item -ItemType Directory -Path $OutDir | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)

$meta = @'
{
  "pack": {
    "pack_format": 48,
    "description": [
      {
        "text": "Tag fixes: repairs tags that fail to load because they reference non-existent entries",
        "color": "aqua"
      }
    ]
  }
}
'@
[System.IO.File]::WriteAllText((Join-Path $OutDir 'pack.mcmeta'), $meta, $utf8)

$written = 0
foreach ($path in ($merged.Keys | Sort-Object)) {
    $m = $merged[$path]
    if ($m.raw) { $json = $m.raw } else { $json = ([ordered]@{ replace = $true; values = @($m.values) } | ConvertTo-Json -Depth 6) }
    $target = Join-Path $OutDir ($path -replace '/', '\')
    New-Item -ItemType Directory -Path (Split-Path $target -Parent) -Force | Out-Null
    [System.IO.File]::WriteAllText($target, $json, $utf8)
    $written++
    Write-Output ("WROTE {0}  ({1})" -f $path, $(if ($m.raw) { 'explicit' } else { "$($m.values.Count) values" }))
}
Write-Output "files written: $written"
