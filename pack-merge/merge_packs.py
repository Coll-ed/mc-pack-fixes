#!/usr/bin/env python3
"""把「上方包」的更新内容合并进「下方包」（原地修改，生成变更清单）。

合并范围（用户裁定）：
  A 组性能 7 个 + B 组视觉 7 个模组
  needsofnature 1.5.1 -> 1.5.3、ysm_non_bridge 1.0.0 -> 2.0.2
  动画包：新增上方 24 个 + 升级 6 个旧版（保留下方独有的 6 个）
  配置：只补 1.5.3 新增的键，不覆盖用户已调值

用法：python merge_packs.py <MC_HOME> <TOP_PACK_DIR> <BOTTOM_PACK_DIR>
"""
import json
import os
import shutil
import sys
import time

# ---- 合并清单（单一来源，改这里即可）----
GROUP_A = [
    "threadtweak", "scalablelux", "sodium-extra", "sspb",
    "reeses-sodium-options", "dynamic_fps", "entity-view-distance",
]
GROUP_B = [
    "bettergrass", "skyboxify", "realcamera", "dynamiccrosshair",
    "held-item-info", "dark-loading-screen", "resourcify",
]
# 需要整体升级的模组：modId -> (旧版本前缀用于定位旧 jar, 说明)
UPGRADE = {
    "needsofnature": "升级 1.5.1 -> 1.5.3",
    "ysm_non_bridge": "升级 1.0.0 -> 2.0.2",
}
# 需要被新版本取代的下方旧动画包（精确文件名）
SUPERSEDED_PACKS = [
    "ChristmasPuffins_Barnyard_Addon_Pack_v5.1.2.zip",
    "ChristmasPuffins_Player_Animations_v1.2.1.zip",
    "NoN_MartyRnull_Addon_Packv1.0.5.zip",
    "needs_of_nature_default_packv1.4.1.zip",
    "needs_of_nature_default_packv1.5.0-fix.1.zip",
]
# 需要补进配置的新键（值取上方包）
NEW_CONFIG_KEYS = [
    "heatVignetteMaxVisibilityPercent",
    "mcaFamilyIntegrationEnabled",
    "mcaPreventAttacksEnabled",
    "mcaRelationshipEffectsEnabled",
    "mcaRelationshipEligibilityEnabled",
    "mobPlayerDCullingEnabled",
    "playerPlayerDCullingEnabled",
    "uiAnimationLabelVisible",
]


def read_mod_meta(path):
    import zipfile
    try:
        with zipfile.ZipFile(path) as z:
            if "fabric.mod.json" in z.namelist():
                return json.loads(z.read("fabric.mod.json").decode("utf-8", "replace"))
    except Exception:
        return {}
    return {}


def index_mods(mods_dir):
    """modId -> 文件名"""
    out = {}
    for name in os.listdir(mods_dir):
        if not name.endswith(".jar"):
            continue
        meta = read_mod_meta(os.path.join(mods_dir, name))
        mid = meta.get("id")
        if mid:
            out[mid] = name
    return out


def main() -> int:
    if len(sys.argv) != 4:
        print(__doc__)
        return 2
    mc_home, top_dir, bot_dir = sys.argv[1:4]
    top_mods = os.path.join(top_dir, "mods")
    bot_mods = os.path.join(bot_dir, "mods")
    top_packs = os.path.join(top_dir, "needsofnature")
    bot_packs = os.path.join(bot_dir, "needsofnature")

    actions = []

    # ---- 1) 依赖预检 ----
    bot_index = index_mods(bot_mods)
    top_index = index_mods(top_mods)
    print(f"top mods={len(top_index)}  bottom mods={len(bot_index)}")

    # ---- 2) 新增 A/B 组 ----
    for mid in GROUP_A + GROUP_B:
        if mid in bot_index:
            actions.append(f"跳过（已存在）{mid}")
            continue
        src_name = top_index.get(mid)
        if not src_name:
            actions.append(f"失败（上方包没有）{mid}")
            continue
        shutil.copy2(os.path.join(top_mods, src_name), os.path.join(bot_mods, src_name))
        actions.append(f"新增 {mid}  {src_name}")

    # ---- 3) 升级指定模组 ----
    for mid, note in UPGRADE.items():
        src_name = top_index.get(mid)
        old_name = bot_index.get(mid)
        if not src_name:
            actions.append(f"失败（上方包没有）{mid}")
            continue
        if old_name:
            os.remove(os.path.join(bot_mods, old_name))
            actions.append(f"移除旧版 {mid}  {old_name}")
        shutil.copy2(os.path.join(top_mods, src_name), os.path.join(bot_mods, src_name))
        actions.append(f"{note}: {src_name}")

    # ---- 4) 动画包 ----
    copied, removed = [], []
    for name in sorted(os.listdir(top_packs)):
        if not name.lower().endswith(".zip"):
            continue
        shutil.copy2(os.path.join(top_packs, name), os.path.join(bot_packs, name))
        copied.append(name)
    for name in SUPERSEDED_PACKS:
        p = os.path.join(bot_packs, name)
        if os.path.exists(p):
            os.remove(p)
            removed.append(name)
    actions.append(f"动画包：新增/覆盖 {len(copied)} 个")
    for n in removed:
        actions.append(f"动画包：移除旧版 {n}")

    # ---- 5) 配置：只补新键 ----
    top_cfg = os.path.join(top_dir, "config", "needsofnature.json")
    bot_cfg = os.path.join(bot_dir, "config", "needsofnature.json")

    def flatten(obj, pre="", out=None):
        if out is None:
            out = {}
        if isinstance(obj, dict):
            for k, v in obj.items():
                flatten(v, f"{pre}.{k}" if pre else k, out)
        else:
            out[pre] = obj
        return out

    def set_path(obj, dotted, value):
        parts = dotted.split(".")
        cur = obj
        for p in parts[:-1]:
            cur = cur.setdefault(p, {})
        cur[parts[-1]] = value

    with open(top_cfg, encoding="utf-8") as f:
        t = json.load(f)
    with open(bot_cfg, encoding="utf-8") as f:
        b = json.load(f)
    t_flat, b_flat = flatten(t), flatten(b)

    added_keys = []
    for key in NEW_CONFIG_KEYS:
        if key in t_flat and key not in b_flat:
            set_path(b, key, t_flat[key])
            added_keys.append(key)
    # entityProfilesByEntity.* 与 uiDescriptionCardExpanded.* 这类嵌套键整体补
    for key, value in t_flat.items():
        if key not in b_flat and (
            key.startswith("entityProfilesByEntity.minecraft:pig.genderSpawn.")
            or key.startswith("uiDescriptionCardExpanded.player_skin_model.")
        ):
            set_path(b, key, value)
            added_keys.append(key)

    if added_keys:
        with open(bot_cfg, "w", encoding="utf-8", newline="\n") as f:
            json.dump(b, f, ensure_ascii=False, indent=2)
            f.write("\n")
    actions.append(f"配置：补入 {len(added_keys)} 个新键（未覆盖任何已有值）")

    # ---- 6) 变更清单 ----
    stamp = time.strftime("%Y%m%d-%H%M%S")
    manifest = os.path.join(mc_home, "pack-merge-manifest-" + stamp + ".txt")
    with open(manifest, "w", encoding="utf-8") as f:
        f.write(f"# 合并变更清单 {stamp}\n")
        f.write(f"# 目标: {bot_dir}\n# 来源: {top_dir}\n\n")
        for a in actions:
            f.write(a + "\n")
        f.write("\n# 新增配置键\n")
        for k in added_keys:
            f.write(f"+ {k}\n")
        f.write("\n# 动画包清单\n")
        for n in copied:
            f.write(f"pack {n}\n")
    print("\n".join(actions))
    print(f"\nmanifest: {manifest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
