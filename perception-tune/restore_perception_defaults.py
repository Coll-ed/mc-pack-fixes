#!/usr/bin/env python3
"""把 Perception 的 `minecraft:entity.generic.explode` 条目恢复成 mod 出厂的默认值。

背景：
  * 苦力怕 / TNT / 凋零头 共用同一个音效键 entity.generic.explode，Perception 只能按音效键配置，
    因此无法「只去掉凋零爆炸的震屏」。
  * 条目不能删：删掉等于不配置，mod 会退回内置默认值（与显式写默认等价，但不可控且难维护）。
  * 结论：这一条恢复成原版默认（苦力怕/TNT 保持原样），凋零的连爆改为从 wither 侧限流。

用法：python restore_perception_defaults.py <config/perception 目录>
"""
import re
import shutil
import sys
import time
from pathlib import Path

SOUND = "minecraft:entity.generic.explode"

# 出厂默认值（来自 Perception 首次生成的配置文件，逐行核对过）
DEFAULT_BLOCKS = {
    "modules/shake.yaml": (
        "    {sound}:\n"
        "        duration: 5\n"
        "        fadeInTime: 0\n"
        "        fadeOutTime: -1\n"
        "        fovAmplitude: 0.15\n"
        "        fovSpeed: 10.0\n"
        "        offsetAmplitude: 0.35\n"
        "        offsetSpeed: 3.5\n"
        "        rangeMultiplier: 0.1\n"
        "        rotationAmplitude: 0.1\n"
        "        rotationSpeed: 5.0\n"
    ),
    "modules/chromatic_aberration.yaml": (
        "    {sound}:\n"
        "        duration: 10\n"
        "        fadeInTime: 2\n"
        "        fadeOutTime: -1\n"
        "        rangeMultiplier: 1.0\n"
        "        strength: 0.1\n"
    ),
}


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    root = Path(sys.argv[1])
    if not root.is_dir():
        print(f"目录不存在: {root}")
        return 1

    changed = 0
    for rel, template in DEFAULT_BLOCKS.items():
        path = root / rel
        if not path.is_file():
            print(f"  跳过（文件不存在）: {rel}")
            continue
        text = path.read_text(encoding="utf-8")
        block = template.format(sound=SOUND)

        if f"{SOUND}:" in text:
            # 已存在：整块替换成默认值（保留块前注释与其它条目）
            lines = text.splitlines(keepends=True)
            out, skipping, target_indent, replaced = [], False, None, False
            for line in lines:
                stripped = line.strip()
                indent = len(line) - len(line.lstrip())
                if not skipping and stripped == f"{SOUND}:":
                    skipping = True
                    target_indent = indent
                    out.append(block)
                    replaced = True
                    continue
                if skipping:
                    if not stripped or stripped.startswith("#") or indent > target_indent:
                        continue
                    skipping = False
                out.append(line)
            new_text = "".join(out)
            action = "已整块改回默认值" if replaced else "未定位到块"
        else:
            # 不存在：在 soundShakes: 之后插入（放在段首，避免落进别的条目里）
            lines = text.splitlines(keepends=True)
            out, inserted = [], False
            for line in lines:
                out.append(line)
                if not inserted and line.strip() == "soundShakes:":
                    out.append(block)
                    inserted = True
            new_text = "".join(out)
            action = "已补回默认块" if inserted else "未找到 soundShakes 段"

        if new_text == text:
            print(f"  {rel}: 无需修改")
            continue
        backup = path.with_suffix(path.suffix + f".{time.strftime('%Y%m%d-%H%M%S')}.bak")
        shutil.copy2(path, backup)
        path.write_text(new_text, encoding="utf-8", newline="\n")
        print(f"  {rel}: {action}，备份 {backup.name}")
        changed += 1

    # 自检：条目存在且字段数与默认块一致
    for rel, template in DEFAULT_BLOCKS.items():
        path = root / rel
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8")
        assert f"{SOUND}:" in text, f"{rel} 缺少 {SOUND} 条目"
        want = len([l for l in template.format(sound=SOUND).splitlines() if ":" in l])
        got = 0
        in_block = False
        for line in text.splitlines():
            stripped = line.strip()
            indent = len(line) - len(line.lstrip())
            if not in_block and stripped == f"{SOUND}:":
                in_block, base = True, indent
                got = 1
                continue
            if in_block:
                if not stripped or stripped.startswith("#") or indent > base:
                    if ":" in stripped:
                        got += 1
                    continue
                in_block = False
        print(f"  自检 {rel}: 字段 {got}/{want}")
    print(f"完成：改动 {changed} 个文件。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
