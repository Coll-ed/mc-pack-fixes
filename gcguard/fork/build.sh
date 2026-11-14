#!/usr/bin/env bash
# 编译 + 打包（javac 直编，本机无 Gradle）。版本号来自 gradle.properties。
set -euo pipefail
cd "$(dirname "$0")"
# shellcheck disable=SC1091
source build.env

read_prop() { grep -E "^$1=" gradle.properties | head -1 | cut -d= -f2-; }
MOD_VERSION="$(read_prop modVersion)"

CLASSES="build/classes"
DIST="build/dist"
rm -rf "$CLASSES" "$DIST"
mkdir -p "$CLASSES" "$DIST"

CP="$DEV_JAR;$FML_JAR;$BUS_JAR;$DIST_JAR;$MIXIN_JAR;$FASTUTIL_JAR;$DFU_JAR;$BRIGADIER_JAR;$LOGGING_JAR;$SLF4J_JAR;$GSON_JAR"

find src -name '*.java' | sort > build/sources.txt
echo "== 编译 $(wc -l < build/sources.txt) 个源文件 =="
javac --release 21 -encoding UTF-8 -proc:none -nowarn -cp "$CP" -d "$CLASSES" @build/sources.txt

echo "== 资源 =="
mkdir -p "$CLASSES/META-INF"
sed -e "s/\${version}/$MOD_VERSION/" res/META-INF/neoforge.mods.toml > "$CLASSES/META-INF/neoforge.mods.toml"

echo "== 打包 =="
JAR="$DIST/gcguard-$MOD_VERSION.jar"
jar --create --file "$JAR" -C "$CLASSES" .
ls -lh "$JAR" | awk '{print "  "$9"  "$5}'

echo "== 自检 =="
unzip -l "$JAR" | grep -E "neoforge.mods.toml|\.class" | awk '{print "  "$4}'
unzip -p "$JAR" META-INF/neoforge.mods.toml | grep -E "^modId|^version|^displayName" | sed 's/^/  /'
