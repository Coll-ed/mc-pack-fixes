#!/usr/bin/env bash
# 从 Voxy SeedGen 的 mixin 清单里移除指定 mixin（默认 VoxyFarPlaneMixin），
# 其余 mixin 保持生效。原 jar 备份为 <jar>.orig-<时间戳>。
#
# 原因：SeedGen 0.3.0 的 VoxyFarPlaneMixin 用 @ModifyConstant 修改
# computeProjectionMat 里的常量，Voxy 0.5.0 改了实现 → Critical injection failure → 建世界崩溃。
# Voxy 0.5.0 自身已带远距离 LOD 读取，该 mixin 不再需要。
#
# 用法: drop-seedgen-mixin.sh <jar 路径> [mixin 名]
set -uo pipefail

JAR="${1:-}"
MIXIN="${2:-VoxyFarPlaneMixin}"
[ -n "$JAR" ] && [ -f "$JAR" ] || { echo "[失败] 用法: $0 <jar 路径> [mixin 名]"; exit 1; }

TS="$(date +%Y%m%d-%H%M%S)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "== 目标 jar: $(basename "$JAR")"
cp "$JAR" "$JAR.orig-$TS" && echo "   已备份 -> $(basename "$JAR").orig-$TS"

# 找 mixins.json（取第一个含 client 列表的）
JSON_NAME="$(unzip -l "$JAR" 2>/dev/null | grep -oE '[^ ]*mixins[^ ]*\.json' | head -1)"
[ -n "$JSON_NAME" ] || { echo "[失败] jar 内找不到 mixins json"; exit 1; }
echo "   配置: $JSON_NAME"

unzip -o -q "$JAR" "$JSON_NAME" -d "$WORK" || { echo "[失败] 解包失败"; exit 1; }
F="$WORK/$JSON_NAME"
[ -f "$F" ] || { echo "[失败] 文件不存在: $F"; exit 1; }

before=$(grep -c "\"$MIXIN\"" "$F" 2>/dev/null || echo 0)
echo "   移除前出现次数: $before"
if [ "$before" -eq 0 ]; then echo "[跳过] 清单里没有 $MIXIN"; exit 0; fi

# 删除该行（只删列表项，保留 JSON 结构）
grep -v "\"$MIXIN\"" "$F" > "$F.new" && mv "$F.new" "$F"
after=$(grep -c "\"$MIXIN\"" "$F" 2>/dev/null || echo 0)
echo "   移除后出现次数: $after"

# 用 jar --update 只替换这一个条目，其他条目原样保留
( cd "$WORK" && jar --update --file "$JAR" "$JSON_NAME" ) || { echo "[失败] jar 更新失败，正在还原"; cp "$JAR.orig-$TS" "$JAR"; exit 1; }

echo
echo "== 校验 =="
unzip -p "$JAR" "$JSON_NAME" 2>/dev/null | grep -c "\"$MIXIN\"" | sed 's/^/   jar 内剩余该 mixin: /'
unzip -p "$JAR" "$JSON_NAME" 2>/dev/null | grep -cE '"' | sed 's/^/   清单条目行数(含括号): /'
echo "   jar 大小: $(stat -c%s "$JAR") 字节（备份 $(stat -c%s "$JAR.orig-$TS")）"
