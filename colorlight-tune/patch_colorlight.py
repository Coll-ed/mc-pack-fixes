#!/usr/bin/env python3
"""ColorLight 配置微调（幂等）。

只改下面 TUNING 里列出的键，其余（1139 条方块取色、所有其它设置）原样保留。
用法：python patch_colorlight.py <config/colorlight.json>
"""
import io
import json
import shutil
import sys
import time
from pathlib import Path

# 目标值 —— 依据 ColorLight 自身文档语义：
#   GPU_PIPELINE=false        → ShaderChunkRendererMixin 的 gpuMode() 门控不成立，不再替换
#                               sodium 地形 shader，Just Like Rays 的区块光照数据重新生效
#   BRIGHTNESS_WEIGHT 100→55  → 不再专挑最亮像素（发光贴图最亮处通常是近白的火芯）
#   REGION_WEIGHT 0→100       → 偏好大片均匀色区，避免孤立高光被当作代表色
#   LOCAL_WEIGHT 100→70       → 降低局部亮斑权重
#   TINT_GAMMA 0.55→0.85      → 颜色“晚一点”显现（文档：lower = 颜色更早显现）
TUNING = {
    "GPU_PIPELINE": False,
    "BRIGHTNESS_WEIGHT": 55,
    "REGION_WEIGHT": 100,
    "LOCAL_WEIGHT": 70,
    "TINT_GAMMA": 0.85,
}


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    path = Path(sys.argv[1])
    if not path.is_file():
        print(f"配置不存在: {path}")
        return 1

    original = json.loads(io.open(path, encoding="utf-8").read())
    patched = dict(original)

    changed = []
    for key, value in TUNING.items():
        if key not in patched:
            print(f"  跳过（配置里没有这个键）: {key}")
            continue
        before = patched[key]
        if before == value:
            print(f"  已是目标值: {key} = {value!r}")
            continue
        patched[key] = value
        changed.append((key, before, value))

    if not changed:
        print("没有需要改的项。")
        return 0

    stamp = time.strftime("%Y%m%d-%H%M%S")
    backup = path.with_suffix(path.suffix + f".{stamp}.bak")
    shutil.copy2(path, backup)
    print(f"  已备份: {backup.name}")

    io.open(path, "w", encoding="utf-8", newline="\n").write(
        json.dumps(patched, ensure_ascii=False, indent=2) + "\n")

    # 复读校验：JSON 合法 + 方块条目数不变 + 目标值已落地
    check = json.loads(io.open(path, encoding="utf-8").read())
    assert len(check.get("blocks", [])) == len(original.get("blocks", [])), "blocks 条目数变了"
    for key, _, value in changed:
        assert check[key] == value, f"{key} 未落地"
        print(f"  {key}: {original[key]!r} → {check[key]!r}")

    print(f"完成：改了 {len(changed)} 项，blocks 条目数 {len(check.get('blocks', []))} 保持不变。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
