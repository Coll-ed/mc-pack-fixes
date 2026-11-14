#!/usr/bin/env bash
# 按 restore-new.conf 把模组切回"新版"：
#   - 现有旧版 jar 改名成 <名>.prev-<时间戳>（保留，可回退）
#   - rolledback 里的新版复制回正式文件名
# 用法: restore-2026-10-24.sh <mods 目录>
set -uo pipefail

MODS="${1:-}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONF="$HERE/restore-new.conf"
[ -n "$MODS" ] && [ -d "$MODS" ] || { echo "[失败] 用法: $0 <mods 目录>"; exit 1; }
[ -f "$CONF" ] || { echo "[失败] 缺配置 $CONF"; exit 1; }

TS="$(date +%Y%m%d-%H%M%S)"
echo "== 模组目录: $MODS"
echo "== 时间戳:   $TS"
echo

fail=0
while IFS='|' read -r target src; do
  case "$target" in ''|\#*) continue ;; esac
  printf '== %s\n' "$target"
  if [ ! -f "$MODS/$src" ]; then echo "   [失败] 新版备份不存在: $src"; fail=1; continue; fi
  if [ -f "$MODS/$target" ]; then
    mv "$MODS/$target" "$MODS/$target.prev-$TS" && echo "   旧版保留 -> $target.prev-$TS"
  fi
  cp "$MODS/$src" "$MODS/$target" && echo "   新版放回 -> $target ($(stat -c%s "$MODS/$target") 字节)"
done < "$CONF"

echo
echo "== 现在的实际版本（从 jar 内读）=="
while IFS='|' read -r target _; do
  case "$target" in ''|\#*) continue ;; esac
  J="$MODS/$target"
  [ -f "$J" ] || continue
  v=$(unzip -p "$J" META-INF/neoforge.mods.toml 2>/dev/null | grep -E '^version' | head -1)
  printf '   %-42s %s\n' "$target" "${v:-?}"
done < "$CONF"
echo "   VanillaBackport 保持:"
ls -1 "$MODS" | grep -i "^VanillaBackport.*\.jar$" | sed 's/^/     /'

exit "$fail"
