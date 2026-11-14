#!/usr/bin/env bash
# 静态自证：把 mixin 依赖的每个符号，逐个在真实 jar 里核对一遍。
# 任何一条对不上就非零退出 —— 比进游戏才发现 mixin 没生效便宜得多。
set -uo pipefail
cd "$(dirname "$0")"
# shellcheck disable=SC1091
source build.env

: "${MODS_DIR:?build.env 里缺少 MODS_DIR}"
MODS="$MODS_DIR"
WORK="build/verify"
rm -rf "$WORK"; mkdir -p "$WORK/shine" "$WORK/ldd"

SHINE="$(grep -E '^shineJar=' gradle.properties | cut -d= -f2-)"
LDD_NESTED="$(grep -E '^lddJar=' gradle.properties | cut -d= -f2-)"

unzip -o -q "$MODS/$SHINE" 'com/bloom/client/coloredlight/*' -d "$WORK/shine"
unzip -o -q "libs/$LDD_NESTED" -d "$WORK/ldd"

FAIL=0
check() { # check <描述> <期望出现的 grep 模式> <javap 输出文件>
  if grep -qE "$2" "$3"; then
    echo "  PASS  $1"
  else
    echo "  FAIL  $1   (期望匹配: $2)"
    FAIL=1
  fi
}

echo "== Shine: ColoredLightRenderer =="
javap -p -classpath "$WORK/shine" com.bloom.client.coloredlight.ColoredLightRenderer > "$WORK/clr.txt" 2>&1
check "refreshUploadLights(Vec3) 存在且为 private static" \
      'private static void refreshUploadLights\(net\.minecraft\.world\.phys\.Vec3\)' "$WORK/clr.txt"
check "UPLOAD_LIGHTS 为 List<ActiveLight> 字段" \
      'private static final java\.util\.List<com\.bloom\.client\.coloredlight\.ColoredLightRenderer\$ActiveLight> UPLOAD_LIGHTS;' "$WORK/clr.txt"

echo "== Shine: ActiveLight =="
javap -p -classpath "$WORK/shine" 'com.bloom.client.coloredlight.ColoredLightRenderer$ActiveLight' > "$WORK/al.txt" 2>&1
check "11 参构造器 (III DDD FFFFF)" \
      'ActiveLight\(int, int, int, double, double, double, float, float, float, float, float\)' "$WORK/al.txt"

echo "== Shine: ColoredLightConfig / Manager =="
javap -p -classpath "$WORK/shine" com.bloom.client.coloredlight.ColoredLightConfig > "$WORK/cfg.txt" 2>&1
check "配置项 enabled 为 public boolean" 'public boolean enabled;' "$WORK/cfg.txt"
check "lightForState(BlockState) 公开" 'public com\.bloom\.client\.coloredlight\.ColoredLightConfig\$BlockLight lightForState\(net\.minecraft\.world\.level\.block\.state\.BlockState\)' "$WORK/cfg.txt"
javap -p -classpath "$WORK/shine" com.bloom.client.coloredlight.ColoredLightConfigManager > "$WORK/mgr.txt" 2>&1
check "ColoredLightConfigManager.get() 公开静态" 'public static synchronized com\.bloom\.client\.coloredlight\.ColoredLightConfig get\(\)' "$WORK/mgr.txt"

echo "== LDD: LambDynLights 内部状态 =="
javap -p -classpath "$WORK/ldd" dev.lambdaurora.lambdynlights.LambDynLights > "$WORK/lddmain.txt" 2>&1
check "public static final INSTANCE" 'public static final dev\.lambdaurora\.lambdynlights\.LambDynLights INSTANCE' "$WORK/lddmain.txt"
check "dynamicLightSources 字段" 'private final java\.util\.Set<dev\.lambdaurora\.lambdynlights\.engine\.source\.DynamicLightSource> dynamicLightSources;' "$WORK/lddmain.txt"
check "lightSourcesLock 字段" 'private final java\.util\.concurrent\.locks\.ReentrantReadWriteLock lightSourcesLock;' "$WORK/lddmain.txt"

echo "== LDD: EntityDynamicLightSource =="
javap -p -classpath "$WORK/ldd" dev.lambdaurora.lambdynlights.engine.source.EntityDynamicLightSource > "$WORK/edls.txt" 2>&1
check "getLuminance()" 'public abstract int getLuminance\(\)' "$WORK/edls.txt"
check "getDynamicLightX()" 'public abstract double getDynamicLightX\(\)' "$WORK/edls.txt"
check "isDynamicLightEnabled()" 'public default boolean isDynamicLightEnabled\(\)' "$WORK/edls.txt"

echo "== LDD: 谁实现了 EntityDynamicLightSource =="
javap -p -classpath "$WORK/ldd" dev.lambdaurora.lambdynlights.mixin.lightsource.PlayerEntityMixin > "$WORK/pem.txt" 2>&1
check "PlayerEntityMixin implements EntityDynamicLightSource" \
      'implements dev\.lambdaurora\.lambdynlights\.engine\.source\.EntityDynamicLightSource' "$WORK/pem.txt"

echo
if [ "$FAIL" -ne 0 ]; then
  echo "结论：有假设不成立，别装 —— 先改 BridgeTargets。"
  exit 1
fi
echo "结论：全部假设成立（${0##*/} 通过）。"
