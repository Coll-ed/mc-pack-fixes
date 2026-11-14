#!/usr/bin/env python3
"""Wither Reincarnated 客户端效果降载（幂等，按行改，保留原文件格式与注释）。

只改 TUNING 里列出的键，其余键与注释原样保留。
用法：python patch_wither.py <config/witherreincarnated/client.toml>
"""
import re
import shutil
import sys
import time
from pathlib import Path

# 键名必须与 mod 写出的完全一致（含 "(default: ...)" 后缀）
# 取值理由（用户裁定“其余 6 项略微加强”，故每项只回补到默认值的 1/3 ~ 1/2）：
#   Max Distant Fire 256→48      远处火焰 billboard 批次；远景观感由「数量 x 不透明度」决定，
#                                用户反馈 96 仍嫌远处显眼，故减半并把不透明度压到 0.03
#   Distant Fire Opacity 0.1→0.03  该键此前从未改过，远处那片橙光的主因就是它（96 层 0.1 叠加）
#   Distant Fire Detail 1.0→0.55 细节档位回到一半略多
#   Max Smoke Columns 256→80     烟柱回补到默认 31%
#   Falling Ash → true           灰烬粒子层；用户裁定保留（接受这点性能换观感）
#   Wither Shine Opacity 0.1→0.03 / Detail 1.0→0.3   ← 用户裁定：首轮这套值最棒，不再随其它项回补
#   Wither Death Flare Detail 1.0→0.7                死亡闪光接近原样（瞬时效果，成本低）
#   Camera Shake Intensity 1.0→0.1                   用户另外裁定，保持 0.1 不动
TUNING = {
    '"Max Distant Fire (default: 256)"': "48",
    '"Distant Fire Opacity (default: 0.1)"': "0.03",
    '"Distant Fire Detail (default: 1.0)"': "0.55",
    '"Max Smoke Columns (default: 256)"': "80",
    '"Falling Ash Near Smoke Columns (default: true)"': "true",
    '"Wither Shine Opacity (default: 0.1)"': "0.03",
    '"Wither Shine Detail (default: 1.0)"': "0.3",
    '"Wither Death Flare Detail (default: 1.0)"': "0.7",
    '"Camera Shake Intensity (default: 1.0)"': "0.1",
}


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    path = Path(sys.argv[1])
    if not path.is_file():
        print(f"配置不存在: {path}")
        return 1

    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    changed = []
    out = []
    seen = set()

    for line in lines:
        match = re.match(r'^(\s*)(".*?")\s*=\s*(.*?)(\s*)$', line.rstrip("\n"))
        if match and match.group(2) in TUNING:
            key = match.group(2)
            seen.add(key)
            before = match.group(3)
            after = TUNING[key]
            if before != after:
                changed.append((key, before, after))
                out.append(f"{match.group(1)}{key} = {after}\n")
                continue
        out.append(line)

    missing = [k for k in TUNING if k not in seen]
    for key in missing:
        print(f"  跳过（配置里没有这个键）: {key}")

    if not changed:
        print("没有需要改的项。")
        return 0

    stamp = time.strftime("%Y%m%d-%H%M%S")
    backup = path.with_suffix(path.suffix + f".{stamp}.bak")
    shutil.copy2(path, backup)
    print(f"  已备份: {backup.name}")

    path.write_text("".join(out), encoding="utf-8", newline="\n")

    # 复读校验：目标值全部落地，且总行数不变
    check = path.read_text(encoding="utf-8").splitlines()
    assert len(check) == len(lines), "行数发生变化，检查写入逻辑"
    for key, _, after in changed:
        hit = [l for l in check if l.startswith(f"{key} = ")]
        assert hit and hit[0].split(" = ", 1)[1] == after, f"{key} 未落地"
        print(f"  {key}: {dict((k, b) for k, b, _ in changed)[key]} → {after}")

    print(f"完成：改了 {len(changed)} 项，总行数 {len(check)} 不变。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
