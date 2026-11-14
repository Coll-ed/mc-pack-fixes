#!/usr/bin/env bash
# 按 revert-2026-10-24.conf 把指定模组回退到 .old 版本。
# 用法: revert-2026-10-24.sh <mods 目录>
#   当前 jar  -> 改成 .rolledback-<时间戳>（保留，不删）
#   .old      -> 复制回 .jar（原 .old 保留，便于再切回）
set -uo pipefail

MODS="${1:-}"
CONF="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/revert-2026-10-24.conf"
[ -n "$MODS" ] && [ -d "$MODS" ] || { echo "[失败] 用法: $0 <mods 目录>"; exit 1; }
[ -f "$CONF" ] || { echo "[失败] 缺配置 $CONF"; exit 1; }

TS="$(date +%Y%m%d-%H%M%S)"
echo "== 模组目录: $MODS"
echo "== 时间戳:   $TS"
echo

fail=0
while IFS='|' read -r cur bak; do
  case "$cur" in ''|\#*) continue ;; esac
  cur_f="$MODS/$cur"
  bak_f="$MODS/$bak"

  printf '== %s\n' "$cur"
  if [ ! -f "$cur_f" ]; then echo "   [跳过] 当前版不存在"; continue; fi
  if [ ! -f "$bak_f" ]; then echo "   [失败] 备份不存在: $bak"; fail=1; continue; fi

  if mv "$cur_f" "$cur_f.rolledback-$TS"; then
    echo "   已移出当前版 -> $(basename "$cur").rolledback-$TS"
  else
    echo "   [失败] 无法移出当前版"; fail=1; continue
  fi

  if cp "$bak_f" "$MODS/$cur"; then
    echo "   已恢复备份   -> $cur ($(stat -c%s "$MODS/$cur") 字节)"
  else
    echo "   [失败] 无法恢复备份"; fail=1
  fi
done < "$CONF"

echo
echo "== 回退后的这四个 =="
while IFS='|' read -r cur _; do
  case "$cur" in ''|\#*) continue ;; esac
  [ -f "$MODS/$cur" ] && printf '   %-52s %s\n' "$cur" "$(date -r "$MODS/$cur" '+%m-%d %H:%M')"
done < "$CONF"

exit "$fail"
