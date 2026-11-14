#!/usr/bin/env python3
"""修 NoN 动画包的元数据问题（全部可修，不删除任何包）。

1) Bunnibee_xtra_male_pack：model_imports 里移掉悬空的 kaceyk:kpig（该包两个整合包都没有）
2) ChristmasPuffin_MM_Slime_Addon_Pack：移除已废弃的 animationframework.model_scope
3) kaceyk_non_animal_pack：同上

用法: python fix_pack_metadata.py <inst_dir>
"""
import json
import os
import sys
import zipfile

BUNNI = "Bunnibee_xtra_male_pack_v1.4.zip"
MM_SLIME = "ChristmasPuffin_MM_Slime_Addon_Pack_v1.1.zip"
KACEYK = "kaceyk_non_animal_pack_v0.0.9.zip"
DROPPED_IMPORT = "kaceyk:kpig"
RETRY_ATTEMPTS = 12
RETRY_DELAY_SECONDS = 5


def rewrite_pack(path, mutator):
    """mutator(meta) -> bool(是否改动)；改动后原子替换（带占用重试）。"""
    tmp = path + ".tmp"
    with zipfile.ZipFile(path) as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename.lower().endswith("pack.mcmeta"):
                meta = json.loads(data.decode("utf-8-sig"))
                if mutator(meta):
                    data = json.dumps(meta, ensure_ascii=False, indent=2).encode("utf-8")
            zout.writestr(item, data)

    for _ in range(RETRY_ATTEMPTS):
        try:
            os.replace(tmp, path)
            return True
        except PermissionError:
            import time

            time.sleep(RETRY_DELAY_SECONDS)
    raise PermissionError(f"无法替换（持续被占用）: {path}")


def drop_missing_import(meta):
    afw = meta.get("animationframework") or {}
    imports = afw.get("model_imports") or []
    kept = [i for i in imports if (i.get("pack") != DROPPED_IMPORT)]
    if len(kept) == len(imports):
        return False
    if kept:
        afw["model_imports"] = kept
    else:
        afw.pop("model_imports", None)
    meta["animationframework"] = afw
    return True


def drop_model_scope(meta):
    afw = meta.get("animationframework") or {}
    if "model_scope" not in afw:
        return False
    afw.pop("model_scope")
    meta["animationframework"] = afw
    return True


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    packs = os.path.join(sys.argv[1], "needsofnature")
    jobs = [
        (BUNNI, drop_missing_import, f"移除悬空导入 {DROPPED_IMPORT}"),
        (MM_SLIME, drop_model_scope, "移除废弃字段 model_scope"),
        (KACEYK, drop_model_scope, "移除废弃字段 model_scope"),
    ]
    for name, fn, desc in jobs:
        path = os.path.join(packs, name)
        if not os.path.exists(path):
            print(f"SKIP {name}: 不存在")
            continue
        changed = rewrite_pack(path, fn)
        print(f"{'OK  ' if changed else 'NOOP'} {name}: {desc}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
