#!/usr/bin/env bash
# 扫描 mods 目录，检测同一 modId 被多个 jar 提供（会直接导致加载失败/注册冲突）。
# 用法: check-duplicate-modids.sh <mods 目录>
set -uo pipefail

MODS="${1:-}"
[ -n "$MODS" ] && [ -d "$MODS" ] || { echo "用法: $0 <mods 目录>"; exit 1; }

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
out="$tmp/modids.txt"
: > "$out"

n=0
for j in "$MODS"/*.jar; do
  [ -f "$j" ] || continue
  n=$((n+1))
  names=""
  # NeoForge: META-INF/neoforge.mods.toml 或 META-INF/mods.toml
  for m in META-INF/neoforge.mods.toml META-INF/mods.toml; do
    if unzip -p "$j" "$m" > "$tmp/m.toml" 2>/dev/null && [ -s "$tmp/m.toml" ]; then
      names=$(grep -oE '^[[:space:]]*modId[[:space:]]*=[[:space:]]*"[^"]+"' "$tmp/m.toml" 2>/dev/null \
              | sed 's/.*"\(.*\)"/\1/' | tr '\n' ' ')
      [ -n "$names" ] && break
    fi
  done
  # Fabric: fabric.mod.json
  if [ -z "$names" ] && unzip -p "$j" fabric.mod.json > "$tmp/f.json" 2>/dev/null && [ -s "$tmp/f.json" ]; then
    id=$(grep -oE '"id"[[:space:]]*:[[:space:]]*"[^"]+"' "$tmp/f.json" | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
    names="$id"
  fi
  [ -n "$names" ] || names="(未识别)"
  for id in $names; do
    printf '%s\t%s\n' "$id" "$(basename "$j")" >> "$out"
  done
done

echo "== 扫描 $n 个 jar，提取到 $(wc -l < "$out") 条 modId =="
echo
dup=$(cut -f1 "$out" | sort | uniq -d)
if [ -z "$dup" ]; then
  echo "== 无重复 modId ✓"
else
  echo "== 发现重复 modId ✗"
  for id in $dup; do
    echo "-- $id"
    grep -P "^$id\t" "$out" | cut -f2 | sed 's/^/     /'
  done
fi
echo
echo "== 未识别元数据的 jar（可能是库/资源包）=="
grep -P "\(未识别\)" "$out" | cut -f2 | head -10 | sed 's/^/   /'
