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
# mixin 配置必须一起进包：toml 里 [[mixins]] config 指向它，缺了会在启动时报
# IllegalArgumentException: The specified resource 'xxx.mixins.json' was invalid or could not be read
shopt -s nullglob
MIXIN_CFGS=(res/*.mixins.json)
if [ ${#MIXIN_CFGS[@]} -eq 0 ]; then
  echo "ERROR: res/ 下没有 *.mixins.json，但 toml 声明了 mixins" >&2
  exit 1
fi
cp "${MIXIN_CFGS[@]}" "$CLASSES/"
echo "  已包含: ${MIXIN_CFGS[*]##*/}"

echo "== 打包 =="
JAR="$DIST/prvfix-$MOD_VERSION.jar"
jar --create --file "$JAR" -C "$CLASSES" .
ls -lh "$JAR" | awk '{print "  "$9"  "$5}'

echo "== 自检 =="
unzip -l "$JAR" | grep -E "neoforge.mods.toml|mixins.json|\.class" | awk '{print "  "$4}'
unzip -p "$JAR" META-INF/neoforge.mods.toml | grep -E "^modId|^version|^displayName|config =" | sed 's/^/  /'
# 逐条校验 toml 声明的 mixin 配置确实在包里
for cfg in $(unzip -p "$JAR" META-INF/neoforge.mods.toml | sed -n 's/.*config = "\(.*\)"/\1/p'); do
  if unzip -l "$JAR" | grep -q " $cfg$"; then
    echo "  mixin 配置 OK: $cfg"
  else
    echo "  ERROR: toml 声明的 $cfg 不在包里" >&2
    exit 1
  fi
done
