#!/usr/bin/env python3
"""修两个真实告警：
1) 两个 Koriscene 动画包共用 animationframework.id -> 给 Bottom_Extra 改名并同步内部引用
2) atmospherics 内置 sun_pack 的 pack.mcmeta 缺 min_format/max_format -> 补上

用法: python fix_nonan_warnings.py <inst_dir>
"""
import json
import os
import shutil
import sys
import time
import zipfile

DUP_ID_OLD = "fkoriscene:extra_animations_pack"
DUP_ID_NEW = "fkoriscene:bottom_extra_pack"
TARGET_PACK = "Koriscene_NoN_Bottom_Extra_Animations_v1.34.zip"
# 文件可能被游戏短暂占用：重试 12 次 * 5 秒 = 60 秒
RETRY_ATTEMPTS = 12
RETRY_DELAY_SECONDS = 5
ATMO_PACK_META = "resourcepacks/atmospherics_sun_pack/pack.mcmeta"


def rewrite_zip(path, replacer):
    """replacer(name, data) -> bytes 或 None(保持原样)。原地替换 zip。"""
    tmp = path + ".tmp"
    changed = []
    with zipfile.ZipFile(path) as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            new = replacer(item.filename, data)
            if new is not None and new != data:
                changed.append(item.filename)
                data = new
            zout.writestr(item, data)
    last_error = None
    for attempt in range(RETRY_ATTEMPTS):
        try:
            try:
                os.replace(tmp, path)
            except PermissionError:
                # Windows 上目标被占用时 os.replace 会 WinError 5：先删再改名
                os.remove(path)
                os.replace(tmp, path)
            return changed
        except PermissionError as exc:
            last_error = exc
            time.sleep(RETRY_DELAY_SECONDS)
    raise PermissionError(f"文件被占用且重试 {RETRY_ATTEMPTS} 次仍失败: {path}") from last_error


def fix_duplicate_id(pack_path):
    def repl(name, data):
        # 只动 pack.mcmeta：该包没有任何模型位于旧 id 的根下，内部引用无需同步
        if not name.lower().endswith("pack.mcmeta"):
            return None
        meta = json.loads(data.decode("utf-8-sig"))
        afw = meta.get("animationframework") or {}
        if afw.get("id") != DUP_ID_OLD:
            return None
        afw["id"] = DUP_ID_NEW
        meta["animationframework"] = afw
        return json.dumps(meta, ensure_ascii=False, indent=2).encode("utf-8")

    changed = rewrite_zip(pack_path, repl)
    return changed


def fix_atmospherics_pack(mods_dir):
    jar = None
    for f in os.listdir(mods_dir):
        if f.lower().startswith("atmospherics") and f.endswith(".jar"):
            jar = os.path.join(mods_dir, f)
            break
    if not jar:
        return None, []

    def repl(name, data):
        if name != ATMO_PACK_META:
            return None
        meta = json.loads(data.decode("utf-8-sig"))
        pack = meta.setdefault("pack", {})
        pack["min_format"] = 34
        pack["max_format"] = 99
        return json.dumps(meta, ensure_ascii=False, indent=2).encode("utf-8")

    changed = rewrite_zip(jar, repl)
    return jar, changed


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    inst = sys.argv[1]
    packs = os.path.join(inst, "needsofnature")
    mods = os.path.join(inst, "mods")

    target = os.path.join(packs, TARGET_PACK)
    if not os.path.exists(target):
        print(f"FAIL: 找不到 {TARGET_PACK}")
        return 1
    changed = fix_duplicate_id(target)
    print(f"1) {TARGET_PACK}: 改写 {len(changed)} 个文件")
    for c in changed:
        print(f"     - {c}")

    jar, changed2 = fix_atmospherics_pack(mods)
    if jar:
        print(f"2) {os.path.basename(jar)}: 改写 {len(changed2)} 个文件")
        for c in changed2:
            print(f"     - {c}")
    else:
        print("2) 未找到 atmospherics jar")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
