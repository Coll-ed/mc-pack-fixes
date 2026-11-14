#!/usr/bin/env python3
"""修复 sandworm_mod 里 `effeks/Texture/...` 的大写资源路径。

背景：aaa_particles 会遍历 assets/<ns>/effeks/** 并为每个文件构造
ResourceLocation；MC 只允许 [a-z0-9/._-]，而该模组把目录和贴图命名成
`Texture/Fire_Blur.png` 这类大写形式，导致整次资源重载抛
ResourceLocationException，进而 MC 清空所有资源包并重载，最终卡在加载界面。

做法（不改模组逻辑，只把资源路径全小写并同步改写引用）：
  1) assets/sandworm_mod/effeks/ 下所有条目路径转小写；
  2) 所有 .efkefc 等二进制里出现的相对引用（UTF-16LE 与 UTF-8 两种编码）同步转小写；
  3) 重新打包，保留其余条目原样。

用法: python fix_sandworm_paths.py <原始 jar> <输出 jar>
"""
import io
import os
import re
import sys
import zipfile

PREFIX = "assets/sandworm_mod/effeks/"


def collect_renames(names):
    """收集需要改名的条目：原路径 -> 小写路径（仅 effeks 前缀下）。"""
    mapping = {}
    for n in names:
        if not n.startswith(PREFIX):
            continue
        low = n.lower()
        if low != n:
            mapping[n] = low
    return mapping


def build_token_map(mapping):
    """从改名表提取需要替换的引用文本（去重、按长度降序，避免前缀误替换）。"""
    tokens = set()
    for old, new in mapping.items():
        rel_old = old[len(PREFIX):]
        rel_new = new[len(PREFIX):]
        if not rel_old:
            continue
        tokens.add((rel_old, rel_new))
        # 单独的文件名/目录名也替换（efkefc 里常用相对名，如 Texture/Fire.png）
        for a, b in zip(rel_old.split("/"), rel_new.split("/")):
            if a != b:
                tokens.add((a, b))
    return sorted(tokens, key=lambda t: len(t[0]), reverse=True)


def rewrite_bytes(data, tokens):
    changed = False
    for old, new in tokens:
        # UTF-8
        ob, nb = old.encode("utf-8"), new.encode("utf-8")
        if ob in data:
            data = data.replace(ob, nb)
            changed = True
        # UTF-16LE（.efkefc 里存的是宽字符）
        ou, nu = old.encode("utf-16-le"), new.encode("utf-16-le")
        if ou in data:
            data = data.replace(ou, nu)
            changed = True
    return data, changed


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    src, dst = sys.argv[1], sys.argv[2]

    with zipfile.ZipFile(src) as zin:
        names = zin.namelist()
        mapping = collect_renames(names)
        if not mapping:
            print("没有需要改名的条目，退出")
            return 1
        tokens = build_token_map(mapping)
        print(f"  待改名条目 {len(mapping)} 个，替换 token {len(tokens)} 组")

        with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
            for item in zin.infolist():
                data = zin.read(item.filename)
                name = item.filename

                # 资源引用同步小写（只动资源类文件，避免误改 class 里无关字符串）
                if not name.endswith((".png", ".ogg", ".class")) and not name.endswith("/"):
                    data, changed = rewrite_bytes(data, tokens)
                    if changed:
                        print(f"  references updated: {name}")

                new_name = mapping.get(name)
                if new_name:
                    name = new_name

                info = zipfile.ZipInfo(name, date_time=item.date_time)
                info.compress_type = zipfile.ZIP_DEFLATED
                info.external_attr = item.external_attr
                zout.writestr(info, data)

    print(f"  输出: {dst}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
