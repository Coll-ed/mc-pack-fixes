#!/usr/bin/env python3
"""把补丁资源包启用进 options.txt（放在列表末尾 = 最高优先级）。

幂等：已存在则不动。只改 resourcePacks 这一行，其余行原样保留。
用法: python enable_pack.py <options.txt> <pack entry, 例如 file/VB-FA-Legacy-Fix.zip>
"""
import json
import sys

KEY = "resourcePacks:"


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    path, entry = sys.argv[1], sys.argv[2]
    with open(path, encoding="utf-8", errors="surrogateescape") as f:
        lines = f.readlines()

    changed = False
    for i, line in enumerate(lines):
        if not line.startswith(KEY):
            continue
        raw = line[len(KEY):].strip()
        packs = json.loads(raw)
        if entry in packs:
            print(f"已存在，跳过: {entry}")
            return 0
        packs.append(entry)
        lines[i] = KEY + json.dumps(packs, ensure_ascii=False) + "\n"
        changed = True
        break
    else:
        print("未找到 resourcePacks 行")
        return 1

    if changed:
        with open(path, "w", encoding="utf-8", errors="surrogateescape", newline="") as f:
            f.writelines(lines)
        print(f"已启用: {entry}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
