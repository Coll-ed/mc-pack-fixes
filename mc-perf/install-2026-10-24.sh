#!/usr/bin/env bash
# 按 install-2026-10-24.conf 下载并替换模组：
#   1) 下载到临时目录，校验 jar 内 mods.toml 与版本号
#   2) 旧 jar 移到备份目录（不留在 mods 里，保持目录干净）
#   3) 新 jar 放进 mods
# 用法: install-2026-10-24.sh <mods 目录> <备份目录>
set -uo pipefail

MODS="${1:-}"; BAK="${2:-}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONF="$HERE/install-2026-10-24.conf"
[ -n "$MODS" ] && [ -d "$MODS" ] || { echo "[失败] 用法: $0 <mods 目录> <备份目录>"; exit 1; }
[ -n "$BAK" ] || { echo "[失败] 需要备份目录"; exit 1; }
[ -f "$CONF" ] || { echo "[失败] 缺配置 $CONF"; exit 1; }

mkdir -p "$BAK"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
TS="$(date +%Y%m%d-%H%M%S)"
fail=0

while IFS='|' read -r url name; do
  case "$name" in ''|\#*) continue ;; esac
  printf '== %s\n' "$name"
  dl="$TMP/$name"
  code=$(curl -sS -L -m 300 --connect-timeout 20 -o "$dl" -w '%{http_code}' "$url" 2>/dev/null)
  size=$(stat -c%s "$dl" 2>/dev/null || echo 0)
  echo "   下载 http=$code  $size 字节"
  if [ "$code" != "200" ] || [ "$size" -lt 100000 ]; then echo "   [失败] 下载不完整"; fail=1; continue; fi

  # 校验：必须是带 mods.toml 的 jar
  if ! unzip -l "$dl" 2>/dev/null | grep -q "META-INF/neoforge.mods.toml"; then
    echo "   [失败] 不是 NeoForge 模组 jar"; fail=1; continue
  fi
  v=$(unzip -p "$dl" META-INF/neoforge.mods.toml 2>/dev/null | grep -E '^version' | head -1 | tr -d ' ')
  mid=$(unzip -p "$dl" META-INF/neoforge.mods.toml 2>/dev/null | grep -E '^[[:space:]]*modId' | head -1 | tr -d ' ')
  echo "   校验: $mid  $v"

  # 旧同 modId 的 jar 移到备份（按 modId 前缀匹配文件名）
  base="$(echo "$mid" | sed 's/.*"\([^"]*\)"/\1/')"
  case "$base" in
    accessories) pat="accessories-neoforge-*.jar" ;;
    owo)         pat="owo-lib-neoforge-*.jar" ;;
    *)           pat="${base}*.jar" ;;
  esac
  for old in "$MODS"/$pat; do
    [ -f "$old" ] || continue
    case "$old" in *.disabled*|*.old|*.bak) continue ;; esac
    mv "$old" "$BAK/$(basename "$old")" && echo "   旧版移入备份: $(basename "$old")"
  done

  cp "$dl" "$MODS/$name" && echo "   已安装: $name ($(stat -c%s "$MODS/$name") 字节)"
done < "$CONF"

echo
echo "== mods 里现在生效的这两个 =="
ls -1 "$MODS" | grep -iE "^(accessories|owo-lib)" | sed 's/^/   /'
echo
echo "== 备份目录 =="
ls -1 "$BAK" | sed 's/^/   /'
exit "$fail"
