#!/usr/bin/env python3
"""按“先分配档位、再由档位成员换算 factor”的方式生成结构分档报告。

规则：
  · 输入：现有 sparsestructures.json5（旧档 + 旧 factor）、全量 structure_set 扫描结果（含 base spacing）
  · 档位阶梯：每档以「平均间距 L（区块）」定义，L 由 tier 名与 X整N次 反算
  · factor = L ÷ 该结构自己的 base spacing（spacing 为 0/缺失时跳过并标注）
用法: python build_tier_report.py <实例目录> <输出 md>
"""
import collections
import json
import math
import os
import re
import sys
import zipfile

# 档位阶梯：code -> (名称, X整, N次)；L = sqrt(((X+1)^2)/N)
TIERS = [
    ("T0", "装饰", 2, 2.0),      # 2整2次  → L 2.12
    ("T1", "寻常", 3, 1.5),      # 3整1.5次 → L 3.27
    ("T2", "常见", 7, 1.0),      # 8×8=64   → L 8
    ("T3", "普通", 13, 1.0),     # 14×14    → L 14
    ("T4", "少见", 21, 1.0),     # 22×22    → L 22
    ("T5", "稀少", 33, 1.0),     # 34×34    → L 34
    ("T6", "罕见", 51, 1.0),     # 52×52    → L 52
    ("T7", "传闻", 79, 1.0),     # 80×80    → L 80
    ("T8", "古籍", 119, 1.0),    # 120×120  → L 120
    ("T9", "传说", 179, 1.0),    # 180×180  → L 180
]

# 旧档 -> 新档
OLD_TO_NEW = {f"T{i}": f"T{i}" for i in range(7)}  # 保持原归档位

# 新增结构（旧配置未覆盖）的档位
NEW_TIER = {
    "minecraft:ancient_cities": "T9", "minecraft:woodland_mansions": "T9",
    "minecraft:strongholds": "T9", "minecraft:ocean_monuments": "T7",
    "minecraft:trial_chambers": "T7", "minecraft:desert_pyramids": "T6",
    "minecraft:jungle_temples": "T6", "minecraft:nether_complexes": "T6",
    "minecraft:swamp_huts": "T4", "minecraft:igloos": "T4",
    "minecraft:nether_fossils": "T4", "minecraft:mineshafts": "T2",
    "minecraft:ruined_portals": "T2", "minecraft:buried_treasures": "T2",
    "minecraft:trail_ruins": "T2", "betternether:mega_lava_lake": "T2",
    "iss_elden_ring:star_observation_platform": "T1", "unusualend:chorus_cane": "T1",
}

# 常见结构名中文（可补充，未收录的留空）
ZH = {
    "ancient_cities": "远古城市", "woodland_mansions": "林地府邸", "strongholds": "要塞",
    "ocean_monuments": "海底神殿", "trial_chambers": "试炼密室", "desert_pyramids": "沙漠神殿",
    "jungle_temples": "丛林神庙", "nether_complexes": "下界堡垒与要塞", "swamp_huts": "沼泽小屋",
    "igloos": "雪屋", "nether_fossils": "下界化石", "mineshafts": "废弃矿井",
    "ruined_portals": "废弃传送门", "buried_treasures": "埋藏的宝藏", "trail_ruins": "古迹废墟",
    "villages": "村庄", "mansion": "林地府邸", "shipwrecks": "沉船", "ocean_ruins": "海底废墟",
    "pillager_outposts": "掠夺者前哨站", "end_cities": "末地城",
    "final_castle": "终焉城堡", "hydra_lair": "九头蛇巢穴", "dark_tower": "暗黑之塔",
    "aurora_palace": "极光宫殿", "lich_tower": "巫妖塔", "knight_stronghold": "骑士要塞",
    "labyrinth": "牛头人迷宫", "yeti_cave": "雪怪洞窟", "troll_cave": "巨魔洞窟",
    "naga_courtyard": "娜迦庭院", "quest_grove": "任务林地", "giant_house": "巨人小屋",
    "hedge_maze": "树篱迷宫", "large_hollow_hill": "大型空心山",
    "medium_hollow_hill": "中型空心山", "small_hollow_hill": "小型空心山",
    "starwatch_tower_set": "观星塔", "starfall_garden_set": "星落花园",
    "stellar_endshore_set": "星辉末岸", "cherry_cabin": "樱花小屋",
    "end_bridge": "末地长桥", "end_lake": "末地湖", "end_lake_normal": "末地湖（普通）",
    "end_lake_rare": "末地湖（稀有）", "small_island": "末地小岛", "sulphuric_cave": "硫磺洞窟",
    "pirate_ship": "海盗船", "shipwreck": "深海沉船", "surface": "深海表层遗迹",
    "pirate_outpost": "海盗前哨", "pirate_shelter": "海盗避难所",
    "cog_chambers": "齿轮密室", "trench": "战壕", "aboveground_medium": "地表中型据点",
    "black_forest_camp": "黑森林营地", "sunflower_base": "向日葵基地", "kitchen": "林中厨房",
    "brass_dungeon": "黄铜地牢", "combiner_corridor": "合成者回廊",
    "altar_camp": "祭坛营地", "campfire": "营火遗迹",
    "monster_room/lower": "刷怪房·下层", "monster_room/upper": "刷怪房·上层",
    "end/common": "末地·常见遗迹", "mega_lava_lake": "巨型岩浆湖",
    "star_observation_platform": "观星平台", "chorus_cane": "紫颂蔗丛",
}

DECOR_FLOOR = 0.15  # 装饰档允许的最密倍数（= 1/6.7 默认间距）

MOD_ZH = {
    "minecraft": "原版", "twilightforest": "暮色森林", "betterend": "更好的末地",
    "betternether": "更好的下界", "deep_aether": "深空天境", "aquamirae": "深海",
    "unusualend": "奇异末地", "kaleidoscope_twilight": "万花筒·暮色",
    "scguns": "焦土枪械", "touhou_little_maid_spell": "东方女仆·法术",
    "iss_elden_ring": "铁魔法×艾尔登法环", "trek": "Trek", "repurposed_structures": "重制结构",
    "dungeons_arise": "地牢浮现", "dungeons_arise_seven_seas": "地牢浮现·七海",
    "irons_spellbooks": "铁之魔法", "iceandfire": "冰与火", "aether": "天境",
    "biomeswevegone": "群峦与彼方", "create": "机械动力",
}


def tier_spacing():
    out = {}
    for code, name, x, n in TIERS:
        out[code] = (name, math.sqrt(((x + 1) ** 2) / n), f"{x}整{n:g}次")
    return out


def scan_sets(inst):
    """返回 {id: base spacing or None}"""
    found = {}
    bases = [os.path.join(inst, "mods")] + [os.path.join(inst, f) for f in os.listdir(inst) if f.endswith(".jar")]
    for base in bases:
        files = [base] if base.endswith(".jar") else [
            os.path.join(base, f) for f in os.listdir(base) if f.endswith(".jar")]
        for jp in files:
            try:
                with zipfile.ZipFile(jp) as z:
                    for n in z.namelist():
                        m = re.match(r"data/([^/]+)/worldgen/structure_set/(.+)\.json$", n)
                        if not m:
                            continue
                        sid = f"{m.group(1)}:{m.group(2)}"
                        sp = None
                        try:
                            j = json.loads(z.read(n).decode("utf-8", "replace"))
                            sp = j.get("placement", {}).get("spacing")
                        except Exception:
                            sp = None
                        found[sid] = int(sp) if sp else None
            except Exception:
                continue
    return found


def scan_old(config_path):
    raw = open(config_path, encoding="utf-8").read()
    tier, old = None, {}
    for line in raw.splitlines():
        m = re.match(r"\s*//\s*=====\s*(T\d)", line)
        if m:
            tier = m.group(1)
            continue
        m2 = re.match(r'\s*\{\s*"structure":\s*"([^"]+)",\s*"factor":\s*([0-9.]+)', line)
        if m2 and tier:
            old[m2.group(1)] = (tier, float(m2.group(2)))
    return old


def main():
    inst, out_path = sys.argv[1], sys.argv[2]
    sp = tier_spacing()
    sets = scan_sets(inst)
    old = scan_old(os.path.join(inst, "config", "sparsestructures.json5"))

    assign = {}
    for sid, (t, _f) in old.items():
        assign[sid] = OLD_TO_NEW.get(t, "T3")
    for sid in ():  # 新增结构暂不自动纳入，等用户手动加
        if sid in assign:
            continue
        if sid in NEW_TIER:
            assign[sid] = NEW_TIER[sid]
            continue
        ns = sid.split(":")[0]
        if ns == "twilightforest":
            assign[sid] = "T9" if any(k in sid for k in ("final_castle", "hydra_lair", "dark_tower", "aurora_palace")) \
                else "T7" if any(k in sid for k in ("lich_tower", "knight_stronghold", "labyrinth", "yeti_cave", "troll_cave")) else "T6"
        elif ns == "touhou_little_maid_spell":
            assign[sid] = "T7" if "tower" in sid else "T5"
        elif ns == "betterend":
            assign[sid] = "T4" if any(k in sid for k in ("bridge", "rare", "sulphuric")) else "T2"
        elif ns == "aquamirae":
            assign[sid] = "T4" if any(k in sid for k in ("ship", "surface")) else "T3"
        elif ns == "scguns":
            assign[sid] = "T6" if "cog" in sid else "T4"
        elif ns == "deep_aether":
            assign[sid] = "T6" if "brass" in sid else "T2"
        else:
            assign[sid] = "T3"

    by_tier = collections.defaultdict(list)
    for sid, t in assign.items():
        by_tier[t].append(sid)

    L = []
    L.append("# 结构分档与 factor 换算\n")
    L.append("| 档 | 名称 | 记录 | 平均间距 L(区块) | L(格) | 该档结构数 |")
    L.append("|---|---|---|---:|---:|---:|")
    for code, name, _x, _n in TIERS:
        nm, val, rec = sp[code]
        L.append(f"| {code} | {nm} | {rec} | {val:.2f} | {val * 16:.0f} | {len(by_tier.get(code, []))} |")
    L.append("\n> `factor = L ÷ base spacing`；`base` 缺失的结构无法换算，标注为 `-`（保持默认）。\n")

    for code, name, _x, _n in TIERS:
        members = sorted(by_tier.get(code, []))
        if not members:
            continue
        nm, val, rec = sp[code]
        L.append(f"\n## {code} {nm}（{rec}，L={val:.2f} 区块 / {val * 16:.0f} 格）\n")
        L.append("| 结构 | 中文名 | 所属模组 | base spacing | 当前 factor | 建议 factor |")
        L.append("|---|---|---|---:|---:|---:|")
        for sid in members:
            ns, path = sid.split(":", 1)
            base = sets.get(sid)
            cur = old.get(sid, (None, None))[1]
            if base:
                raw = val / base
                # 只稀不密：T2 以上不允许比默认更密；T0/T1 允许加密但有下限
                factor = max(1.0, raw) if code not in ("T0", "T1") else min(1.0, max(DECOR_FLOOR, raw))
                new = f"{factor:.2f}"
            else:
                new = "-"
            zh = ZH.get(path, ZH.get(path.split("/")[-1], ""))
            mod = MOD_ZH.get(ns, ns)
            L.append(f"| `{sid}` | {zh or '-'} | {mod} | {base if base else '-'} | {cur if cur else '-'} | {new} |")

    open(out_path, "w", encoding="utf-8").write("\n".join(L))
    print("写入:", out_path)
    print("各档结构数:", {c: len(by_tier.get(c, [])) for c, *_ in TIERS})


if __name__ == "__main__":
    main()
