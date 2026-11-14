#!/usr/bin/env bash
# 编译 + 打包。依赖清单在 build.env，版本号在 gradle.properties（单一来源）。
set -euo pipefail
cd "$(dirname "$0")"
# shellcheck disable=SC1091
source build.env

read_prop() { grep -E "^$1=" gradle.properties | head -1 | cut -d= -f2-; }

MOD_VERSION="$(read_prop modVersion)"
SHINE_VERSION="$(read_prop shineVersion)"
LDD_VERSION="$(read_prop lddVersion)"
SHINE_JAR="$(read_prop shineJar)"
LDD_JAR="$(read_prop lddJar)"

CLASSES="build/classes"
DIST="build/dist"
rm -rf "$CLASSES" "$DIST"
mkdir -p "$CLASSES" "$DIST"

CP="$DEV_JAR;$FML_JAR;$BUS_JAR;$DIST_JAR;$MIXIN_JAR;$FASTUTIL_JAR;$DFU_JAR;$BRIGADIER_JAR;$LOGGING_JAR;$SLF4J_JAR;$GSON_JAR;libs/$SHINE_JAR;libs/$LDD_JAR"

find src -name '*.java' | sort > build/sources.txt
echo "== 编译 $(wc -l < build/sources.txt) 个源文件 =="
javac --release 21 -encoding UTF-8 -proc:none -nowarn -cp "$CP" -d "$CLASSES" @build/sources.txt

echo "== 资源 + 版本占位替换 =="
cp res/lddshine.mixins.json "$CLASSES/"
cp -r res/assets "$CLASSES/"
mkdir -p "$CLASSES/META-INF"
sed -e "s/\${version}/$MOD_VERSION/" \
    -e "s/\${shineVersion}/$SHINE_VERSION/" \
    -e "s/\${lddVersion}/$LDD_VERSION/" \
    res/META-INF/neoforge.mods.toml > "$CLASSES/META-INF/neoforge.mods.toml"

echo "== 打包 =="
JAR="$DIST/lddshine-$MOD_VERSION.jar"
jar --create --file "$JAR" -C "$CLASSES" .
ls -lh "$JAR" | awk '{print "  "$9"  "$5}'

echo "== 自检 =="
unzip -l "$JAR" | grep -E "lddshine.mixins.json|neoforge.mods.toml|item_colors.json|LambDynLightsAccessor|ShineColoredLightRendererMixin|LddShine.class|DynamicLightSamples|ShineActiveLightFactory|ItemLightColors|DefaultItemColors" | awk '{print "  "$4}'
echo "  --- toml 关键行 ---"
unzip -p "$JAR" META-INF/neoforge.mods.toml | grep -E "^modId|^version|^displayName|config =|modId = \"(shine|lambdynlights)\"|versionRange" | sed 's/^/    /'
