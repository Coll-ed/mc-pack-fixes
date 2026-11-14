#!/usr/bin/env bash
# biomelink：纯数据模组（无 Java 源码），把 res/ 下的 data 打包成 jar。
set -euo pipefail
cd "$(dirname "$0")"

read_prop() { grep -E "^$1=" gradle.properties | head -1 | cut -d= -f2-; }
MOD_VERSION="$(read_prop modVersion)"

CLASSES="build/classes"
DIST="build/dist"
rm -rf "$CLASSES" "$DIST"
mkdir -p "$CLASSES/META-INF" "$DIST"

sed -e "s/\${version}/$MOD_VERSION/" res/META-INF/neoforge.mods.toml > "$CLASSES/META-INF/neoforge.mods.toml"
cp -r res/data "$CLASSES/data"
cp res/pack.mcmeta "$CLASSES/pack.mcmeta"

JAR="$DIST/biomelink-$MOD_VERSION.jar"
jar --create --file "$JAR" -C "$CLASSES" .
ls -lh "$JAR" | awk '{print "  "$9"  "$5}'

echo "== 自检 =="
unzip -l "$JAR" | grep -cE "has_structure/.*\.json" | sed 's/^/  has_structure 标签数: /'
unzip -p "$JAR" META-INF/neoforge.mods.toml | grep -E "^modId|^version|^displayName" | sed 's/^/  /'
