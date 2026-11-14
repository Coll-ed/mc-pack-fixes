#!/usr/bin/env python3
"""对运行中的 MC 进程做两次 class_histogram 采样并对比增长，用于定位活集泄漏点。

用法: python heap_diff.py <输出目录> [间隔秒数, 默认 600]
依赖 jcmd：优先取环境变量 JCMD，其次 JAVA_HOME/bin/jcmd，最后走 PATH。
"""
import os
import re
import shutil
import subprocess
import sys
import time

LINE = re.compile(r"^\s*\d+:\s+(\d+)\s+(\d+)\s+(.+?)\s*(?:\(([\w.\-]+)\))?$")


def resolve_jcmd() -> str:
    """按 JCMD → JAVA_HOME → PATH 的顺序定位 jcmd，找不到就报错。"""
    explicit = os.environ.get("JCMD")
    if explicit and os.path.isfile(explicit):
        return explicit
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = os.path.join(java_home, "bin", "jcmd.exe")
        if os.path.isfile(candidate):
            return candidate
    found = shutil.which("jcmd")
    if found:
        return found
    raise FileNotFoundError("找不到 jcmd：请设置 JCMD 或 JAVA_HOME 环境变量")


def find_pid() -> int | None:
    out = subprocess.run(
        ["powershell", "-NoProfile", "-Command",
         "Get-Process java,javaw -ErrorAction SilentlyContinue | "
         "Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | "
         "Select-Object -First 1 -ExpandProperty Id"],
        capture_output=True, text=True).stdout.strip()
    return int(out) if out.isdigit() else None


def histogram(jcmd: str, pid: int) -> dict[str, tuple[int, int]]:
    out = subprocess.run([jcmd, str(pid), "GC.class_histogram"],
                         capture_output=True, text=True).stdout
    table: dict[str, tuple[int, int]] = {}
    for raw in out.splitlines():
        m = LINE.match(raw)
        if not m:
            continue
        count, size, name = int(m.group(1)), int(m.group(2)), m.group(3).strip()
        table[name] = (count, size)
    return table


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    outdir = sys.argv[1]
    interval = int(sys.argv[2]) if len(sys.argv) > 2 else 600
    os.makedirs(outdir, exist_ok=True)
    jcmd = resolve_jcmd()

    pid = find_pid()
    if pid is None:
        print("未找到 Minecraft 进程")
        return 1

    before = histogram(jcmd, pid)
    print(f"第一次采样完成：{len(before)} 个类（PID {pid}）")
    time.sleep(interval)

    pid = find_pid() or pid
    after = histogram(jcmd, pid)
    print(f"第二次采样完成：{len(after)} 个类")

    growth = []
    for name, (count2, size2) in after.items():
        count1, size1 = before.get(name, (0, 0))
        growth.append((size2 - size1, count2 - count1, name, size1, size2))

    growth.sort(reverse=True)
    path = os.path.join(outdir, "heap-growth.txt")
    with open(path, "w", encoding="utf-8") as f:
        f.write(f"间隔 {interval}s 的堆增长（按字节增量排序，前 40）\n")
        f.write(f"{'dMB':>8} {'dInstances':>12} {'beforeMB':>10} {'afterMB':>9}  class\n")
        for dsize, dcount, name, s1, s2 in growth[:40]:
            f.write(f"{dsize / 1048576:8.1f} {dcount:12d} {s1 / 1048576:10.1f} {s2 / 1048576:9.1f}  {name}\n")
    print(f"结果: {path}")
    for dsize, dcount, name, s1, s2 in growth[:12]:
        print(f"  {dsize / 1048576:+8.1f}MB  {dcount:+10d}  {name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
