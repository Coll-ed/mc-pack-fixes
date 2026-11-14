#!/usr/bin/env python3
"""修动画包里的内容级 JSON 问题（不改动画数据，只修格式错误）。

1) mimicmplayer.json: "animators": "Incarnadine"（字符串）-> ["Incarnadine"]（字符串数组）
2) skeletonxfplayer.animation-carrying.json -> skeletonxfplayer.animation-carrying.animation.json
   （NoN 只扫描 *.animation.json；改名后剥离 .animation 仍能配到同名 animdef）
3) Parmesan 4 个 animdef: 删掉与 surface_footprint 并存、被忽略的 surface_radius

用法: python fix_animation_json.py <inst_dir>
"""
import json
import os
import sys
import time
import zipfile

MIMIC_PACK = "NoNMimicPackV1.1.zip"
MIMIC_ENTRY = "data/needsofnature/afw_animdefs/mimicmplayer.json"

CARRY_PACK = "Koriscene_NoN_Bottom_Extra_Animations_v1.34.zip"
CARRY_OLD = "assets/animationframework/geckolib/animations/afw/skeletonxfplayer.animation-carrying.json"
CARRY_NEW = "assets/animationframework/geckolib/animations/afw/skeletonxfplayer.animation-carrying.animation.json"

PARMESAN_PACK = "ParmesanPack Remake v1.1.1.zip"
PARMESAN_ENTRIES = [
    "data/parmesan/afw_animdefs/bisandwich.json",
    "data/parmesan/afw_animdefs/defeat_bed.json",
    "data/parmesan/afw_animdefs/ff_pxp_rimming_from_behind.json",
    "data/parmesan/afw_animdefs/mf_pxp_rimming_from_behind.json",
]

RETRY_ATTEMPTS = 12
RETRY_DELAY_SECONDS = 5


def rewrite(path, handler):
    """handler(name, data) -> (new_name, new_data) 或 None。"""
    tmp = path + ".tmp"
    with zipfile.ZipFile(path) as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            result = handler(item.filename, data)
            name, data = result if result is not None else (item.filename, data)
            zout.writestr(name, data)

    for _ in range(RETRY_ATTEMPTS):
        try:
            os.replace(tmp, path)
            return True
        except PermissionError:
            time.sleep(RETRY_DELAY_SECONDS)
    raise PermissionError(f"无法替换（持续被占用）: {path}")


def fix_mimic(packs):
    path = os.path.join(packs, MIMIC_PACK)

    def handler(name, data):
        if name != MIMIC_ENTRY:
            return None
        meta = json.loads(data.decode("utf-8-sig"))
        animators = meta.get("animators")
        if isinstance(animators, str):
            meta["animators"] = [animators] if animators.strip() else []
        elif isinstance(animators, list):
            meta["animators"] = [a for a in animators if isinstance(a, str) and a.strip()]
        else:
            return None
        return name, json.dumps(meta, ensure_ascii=False, indent=2).encode("utf-8")

    return rewrite(path, handler)


def fix_carry(packs):
    path = os.path.join(packs, CARRY_PACK)

    def handler(name, data):
        if name == CARRY_OLD:
            return CARRY_NEW, data
        return None

    return rewrite(path, handler)


def fix_parmesan(packs):
    path = os.path.join(packs, PARMESAN_PACK)

    def handler(name, data):
        if name not in PARMESAN_ENTRIES:
            return None
        meta = json.loads(data.decode("utf-8-sig"))
        req = meta.get("block_requirements") or {}
        if "surface_radius" not in req:
            return None
        req.pop("surface_radius")
        meta["block_requirements"] = req
        return name, json.dumps(meta, ensure_ascii=False, indent=2).encode("utf-8")

    return rewrite(path, handler)


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    packs = os.path.join(sys.argv[1], "needsofnature")
    print("1) mimicmplayer animators:", "OK" if fix_mimic(packs) else "FAIL")
    print("2) 动画资产改名:", "OK" if fix_carry(packs) else "FAIL")
    print("3) Parmesan surface_radius:", "OK" if fix_parmesan(packs) else "FAIL")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
