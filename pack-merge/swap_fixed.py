#!/usr/bin/env python3
"""把预生成的 *.fixed 原子替换掉原文件（等待游戏释放文件锁）。

游戏进程名可能是 java 或 javaw，这里不判断进程，直接对文件做「可删除性」探测：
能删/能改名 = 锁已释放，立刻替换。重试窗口 3 小时。

用法: python swap_fixed.py <inst_dir>
"""
import os
import sys
import time

RETRY_SECONDS = 10
RETRY_WINDOW_SECONDS = 3 * 60 * 60


def pairs(inst):
    return [
        os.path.join(inst, "needsofnature", "Koriscene_NoN_Bottom_Extra_Animations_v1.34.zip"),
        os.path.join(inst, "mods", "atmospherics-2.6.6-mc-1.21.11.jar"),
    ]


def try_swap(target):
    fixed = target + ".fixed"
    if not os.path.exists(fixed):
        return "missing-fixed"
    try:
        os.replace(fixed, target)
        return "swapped"
    except PermissionError:
        return "locked"


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    inst = sys.argv[1]
    targets = pairs(inst)
    deadline = time.time() + RETRY_WINDOW_SECONDS
    pending = list(targets)
    while pending and time.time() < deadline:
        still = []
        for target in pending:
            result = try_swap(target)
            if result == "swapped":
                print(f"[{time.strftime('%H:%M:%S')}] 已替换 {os.path.basename(target)}", flush=True)
            elif result == "missing-fixed":
                print(f"[{time.strftime('%H:%M:%S')}] 缺少 .fixed，跳过 {os.path.basename(target)}", flush=True)
            else:
                still.append(target)
        pending = still
        if pending:
            time.sleep(RETRY_SECONDS)
    if pending:
        print(f"[{time.strftime('%H:%M:%S')}] 超时，仍未替换: " + ", ".join(os.path.basename(p) for p in pending), flush=True)
        return 1
    print(f"[{time.strftime('%H:%M:%S')}] 全部替换完成", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
