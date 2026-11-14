#!/usr/bin/env python3
"""Perception：关掉「爆炸」的色差，保留震屏；条目必须存在（缺失会回退内置默认值）。

用户裁定：
  * 苦力怕 / TNT / 凋零头 爆炸 —— 色差关掉（strength 0.0）
  * 同一次爆炸的震屏 —— 保持出厂默认，不动
  * 其余所有音效条目 —— 一律不动

只改 chromatic_aberration.yaml 里 soundShakes.minecraft:entity.generic.explode 的 strength 字段。
若条目缺失，按出厂默认补回并把 strength 置 0（不能只删，否则会回落默认 0.1）。

用法：python patch_perception.py <config/perception 目录>
"""
import re
import shutil
import sys
import time
from pathlib import Path

SOUND = "minecraft:entity.generic.explode"
CHROMA_FILE = "modules/chromatic_aberration.yaml"

# 字段名单独提出来拼正则，避免把 "xxx:" 当成 Windows 路径
FIELD = "strength"
TARGET = "0.0"

FIELD_RE = re.compile(r"^(\s*)" + FIELD + r":\s*(\S+)\s*$")

# 出厂默认块（条目缺失时补回；strength 已按需求置 0）
DEFAULT_BLOCK_TEMPLATE = (
    "    {sound}:\n"
    "        duration: 10\n"
    "        fadeInTime: 2\n"
    "        fadeOutTime: -1\n"
    "        rangeMultiplier: 1.0\n"
    "        {field}: {target}\n"
)


def patch_chroma(path: Path) -> int:
    if not path.is_file():
        print(f"  跳过（文件不存在）: {path}")
        return 0
    text = path.read_text(encoding="utf-8")
    block = DEFAULT_BLOCK_TEMPLATE.format(sound=SOUND, field=FIELD, target=TARGET)

    if f"{SOUND}:" not in text:
        out, inserted = [], False
        for line in text.splitlines(keepends=True):
            out.append(line)
            if not inserted and line.strip() == "soundShakes:":
                out.append(block)
                inserted = True
        if not inserted:
            print("  未找到 soundShakes 段，放弃")
            return 0
        new_text = "".join(out)
        action = "条目缺失，已补回并置 0"
    else:
        out, in_block, base, done, before = [], False, None, False, None
        for line in text.splitlines(keepends=True):
            stripped = line.strip()
            indent = len(line) - len(line.lstrip())
            if not in_block and stripped == f"{SOUND}:":
                in_block, base = True, indent
                out.append(line)
                continue
            if in_block:
                if stripped and not stripped.startswith("#") and indent <= base:
                    in_block = False
                    out.append(line)
                    continue
                if not done:
                    m = FIELD_RE.match(line.rstrip("\n"))
                    if m:
                        before = m.group(2)
                        out.append(f"{m.group(1)}{FIELD}: {TARGET}\n")
                        done = True
                        continue
            out.append(line)
        new_text = "".join(out)
        if not done:
            print(f"  条目存在但没有 {FIELD} 字段，放弃（避免写坏结构）")
            return 0
        action = (f"{FIELD}: {before} → {TARGET}" if before != TARGET
                  else f"{FIELD} 已是目标值")

    if new_text == text:
        print(f"  {path.name}: {action}")
        return 0

    backup = path.with_suffix(path.suffix + f".{time.strftime('%Y%m%d-%H%M%S')}.bak")
    shutil.copy2(path, backup)
    path.write_text(new_text, encoding="utf-8", newline="\n")
    print(f"  {path.name}: {action}，备份 {backup.name}")
    return 1


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    root = Path(sys.argv[1])
    if not root.is_dir():
        print(f"目录不存在: {root}")
        return 1

    changed = patch_chroma(root / CHROMA_FILE)

    path = root / CHROMA_FILE
    if path.is_file():
        text = path.read_text(encoding="utf-8")
        assert f"{SOUND}:" in text, "条目缺失"
        tail = text.split(f"{SOUND}:", 1)[1]
        got = None
        for line in tail.splitlines()[:8]:
            m = FIELD_RE.match(line)
            if m:
                got = m.group(2)
                break
        assert got == TARGET, f"{FIELD} 未落地: {got}"
        print(f"  自检: {SOUND}.{FIELD} = {got}")
    print(f"完成：改动 {changed} 个文件。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
