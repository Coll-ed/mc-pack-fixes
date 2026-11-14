#!/usr/bin/env python3
"""生成结构分档报告（含中文名与遗漏核对）。

数据来源：
  · config/sparsestructures.json5 —— 现有档位与 factor（保持原样，不自动改）
  · 全量 jar 扫描 —— structure_set 的 base spacing、以及未被配置覆盖的结构

用法: python render_report.py <实例目录> <输出 md>
"""

import collections
import json
import math
import os
import re
import sys
import zipfile

# 档位阶梯：code -> (名称, 记录, 平均间距 L 区块, L 格)
TIERS = [
    ("T0", "装饰", "2整2次", 2.12, 34),
    ("T1", "寻常", "3整1.5次", 3.27, 52),
    ("T2", "常见", "7整1次", 8.0, 128),
    ("T3", "普通", "13整1次", 14.0, 224),
    ("T4", "少见", "21整1次", 22.0, 352),
    ("T5", "稀少", "33整1次", 34.0, 544),
    ("T6", "罕见", "51整1次", 52.0, 832),
    ("T7", "传闻", "79整1次", 80.0, 1280),
    ("T8", "古籍", "119整1次", 120.0, 1920),
    ("T9", "传说", "179整1次", 180.0, 2880),
]

# 结构中文名（按 id 的 path 部分索引）
ZH = {
    # T0 装饰
    "betterend:mountain": "末地群山",
    "biomeswevegone:dripstone_arches": "滴水石拱门",
    "biomeswevegone:lush_arch": "繁茂拱门",
    "biomeswevegone:rugged_fossils": "崎岖化石",
    "biomeswevegone:sharpened_rocks": "尖岩群",
    "biomeswevegone:red_rock_arches": "红岩拱门",
    "biomeswevegone:ironwood_gour_plateau": "铁木台地",
    "biomeswevegone:large_cold_lake": "大型寒湖",
    "biomeswevegone:prairie_houses": "草原民居",
    "bakeries:well": "面包坊水井",
    # T1 寻常
    "betterend:megalake": "巨型湖泊",
    "betterend:giant_ice_star": "巨型冰晶",
    "betterend:giant_mossy_glowshroom": "巨型苔藓荧光菇",
    "unusualend:small_gloopy_ruins": "小型黏滑废墟",
    "unusualend:warped_station": "诡异车站",
    "twilightforest:fallen_trunk": "倒下的树干",
    "twilightforest:hollow_tree": "空心树",
    "aether:large_aercloud": "大型空云",
    "hopo:underwater_small_ruins": "水下小型废墟",
    "hopo:underwater_warm_small_ruins": "水下暖域小废墟",
    "hopo:underwater_fossils": "水下化石",
    "bettermineshafts:mineshafts": "强化废弃矿井",
    "repurposed_structures:mineshafts_overworld": "矿井（主世界）",
    "repurposed_structures:mineshafts_nether": "矿井（下界）",
    "repurposed_structures:mineshafts_end": "矿井（末地）",
    "repurposed_structures:mineshafts_ocean": "矿井（海洋）",
    # T2 普通
    "minecraft:ocean_ruins": "海底废墟",
    "minecraft:shipwrecks": "沉船",
    "minecraft:pillager_outposts": "掠夺者前哨站",
    "repurposed_structures:outposts_overworld": "前哨站（主世界）",
    "repurposed_structures:outposts_ocean": "前哨站（海洋）",
    "betternether:pillars": "下界石柱",
    "betternether:respawn_points": "重生点",
    "twilightforest:camp": "营地",
    "unusualend:gloopy_nest": "黏滑巢穴",
    "unusualend:warped_flying_end_ship": "诡异飞行末地船",
    "unusualend:firefly_nest": "萤火虫巢",
    "iceandfire:pixie_village": "小精灵村庄",
    "iceandfire:dragon_roost": "龙巢",
    "betterdungeons:small_dungeons": "小型地牢",
    "touhou_little_maid_spell:hidden_retreat_set": "隐秘别居",
    "touhou_little_maid_spell:hidden_cherry_tree_set": "隐秘樱树",
    "trek:overworld/very_common": "主世界·极常见遗迹",
    "trek:nether/very_common": "下界·极常见遗迹",
    "trek:end/very_common": "末地·极常见遗迹",
    "repurposed_structures:shipwrecks_nether": "沉船（下界）",
    "repurposed_structures:shipwrecks_end": "沉船（末地）",
    # T3 稀有
    "minecraft:villages": "村庄",
    "minecraft:end_cities": "末地城",
    "repurposed_structures:villages_overworld": "村庄（主世界）",
    "repurposed_structures:villages_mushroom": "村庄（蘑菇岛）",
    "repurposed_structures:villages_nether": "村庄（下界）",
    "repurposed_structures:pyramids_overworld": "金字塔（主世界）",
    "repurposed_structures:witch_huts_overworld": "女巫小屋（主世界）",
    "repurposed_structures:igloos_overworld": "雪屋（主世界）",
    "repurposed_structures:ruins_overworld": "废墟（主世界）",
    "repurposed_structures:outposts_nether": "前哨站（下界）",
    "repurposed_structures:ruins_nether": "废墟（下界）",
    "repurposed_structures:temples_overworld": "神庙（主世界）",
    "repurposed_structures:fortresses_overworld": "堡垒（主世界）",
    "biomeswevegone:villages": "群峦村庄",
    "betterend:end_village": "末地村庄",
    "betterdungeons:small_nether_dungeons": "小型下界地牢",
    "betterdungeons:spider_dungeons": "蜘蛛地牢",
    "betterdungeons:skeleton_dungeons": "骷髅地牢",
    "betterdungeons:zombie_dungeons": "僵尸地牢",
    "dungeons_arise:minor_structures": "地牢浮现·小型结构",
    "irons_spellbooks:mangrove_hut": "红树林小屋",
    "touhou_little_maid_spell:fairy_maid_cafe_set": "妖精女仆咖啡馆",
    "iceandfire:cyclops_cave": "独眼巨人洞窟",
    "trek:overworld/common": "主世界·常见遗迹",
    "trek:nether/common": "下界·常见遗迹",
    "trek:end/medium": "末地·中等遗迹",
    "unusualend:flying_end_ship": "飞行末地船",
    "unusualend:wanderer_island": "漫游者之岛",
    "unusualend:ancient_end_tower": "远古末地塔",
    "unusualend:end_house": "末地小屋",
    "aether:silver_and_gold_dungeons": "金银地牢",
    "aether:bronze_dungeon": "青铜地牢",
    "betterend:eternal_portal": "永恒传送门",
    "biomeswevegone:bog_trial": "沼泽试炼",
    "biomeswevegone:aspen_manors": "白杨庄园",
    # T4 珍稀
    "iceandfire:dragon_cave": "龙窟",
    "iceandfire:graveyard": "墓地",
    "iceandfire:siren_island": "海妖岛",
    "iceandfire:gorgon_temple": "戈耳工神庙",
    "iceandfire:mausoleum": "陵墓",
    "iceandfire:hydra_cave": "九头蛇洞窟",
    "betterfortresses:fortress": "强化堡垒",
    "betternether:pyramid": "下界金字塔",
    "irons_spellbooks:ancient_battleground": "远古战场",
    "touhou_little_maid_spell:relic_sanctum_set": "遗物圣所",
    "touhou_little_maid_spell:elven_realm_set": "精灵领域",
    "touhou_little_maid_spell:fallen_sanctum_set": "堕落圣所",
    "touhou_little_maid_spell:enchantress_footsteps_graveyard": "女巫足迹·墓地",
    "touhou_little_maid_spell:enchantress_footsteps_igloo": "女巫足迹·雪屋",
    "touhou_little_maid_spell:enchantress_footsteps_mushroom_fields": "女巫足迹·蘑菇原野",
    "touhou_little_maid_spell:enchantress_footsteps_outpost": "女巫足迹·前哨站",
    "touhou_little_maid_spell:woods_perch": "林间栖所",
    "touhou_little_maid_spell:yin_yang_altar_set": "阴阳祭坛",
    "dungeons_arise:major_structures": "地牢浮现·大型结构",
    "trek:overworld/medium": "主世界·中等遗迹",
    "trek:nether/medium": "下界·中等遗迹",
    "repurposed_structures:pyramids_nether": "金字塔（下界）",
    "repurposed_structures:outposts_end": "前哨站（末地）",
    "repurposed_structures:ruined_portals_end": "废弃传送门（末地）",
    "betternether:altars": "下界祭坛",
    "betternether:portals": "下界传送门",
    "betternether:spawn_altar_ladder": "出生祭坛阶梯",
    "betternether:gardens": "下界花园",
    "betternether:ghast_hive": "恶魂巢",
    "irons_spellbooks:mountain_tower": "山巅之塔",
    "irons_spellbooks:catacombs": "地下墓穴",
    "irons_spellbooks:impaled_icebreaker": "穿刺破冰船",
    "irons_spellbooks:evoker_fort": "唤魔者要塞",
    "irons_spellbooks:ice_spider_den": "冰蜘蛛巢",
    "irons_spellbooks:pyromancer_tower": "烈焰术士塔",
    "betteroceanmonuments:ocean_monument": "海底神殿（强化）",
    "repurposed_structures:monuments_overworld": "海底神殿（主世界）",
    "repurposed_structures:cities_nether": "城市（下界）",
    "repurposed_structures:strongholds_end": "要塞（末地）",
    "dungeons_arise_seven_seas:minor_structures": "七海·小型结构",
    # T5 重大发现
    "betterdeserttemples:desert_temples": "沙漠神殿（强化）",
    "hopo:underwater_city": "水下城市",
    "repurposed_structures:temples_nether": "神庙（下界）",
    "repurposed_structures:pyramids_mushroom": "金字塔（蘑菇岛）",
    "repurposed_structures:pyramids_end": "金字塔（末地）",
    "repurposed_structures:ancient_cities_end": "远古城市（末地）",
    "irons_spellbooks:citadel": "城堡要塞",
    "skyarena:ice_arena": "冰霜竞技场",
    "skyarena:sky_arena": "天空竞技场",
    "betternether:nether_city": "下界城市",
    "fdbosses:chesed_arena": "慈悲竞技场",
    "fdbosses:geburah_arena": "严厉竞技场",
    "fdbosses:malkuth_arena": "王国竞技场",
    "trek:overworld/rare": "主世界·稀有遗迹",
    "trek:nether/rare": "下界·稀有遗迹",
    "trek:end/rare": "末地·稀有遗迹",
    "repurposed_structures:mansions_mangrove": "林地府邸（红树林）",
    "repurposed_structures:mansions_overworld": "林地府邸（主世界）",
    "repurposed_structures:bastions_overworld": "堡垒遗迹（主世界）",
    "repurposed_structures:strongholds_nether": "要塞（下界）",
    "repurposed_structures:cities_overworld": "城市（主世界）",
    "repurposed_structures:ancient_cities_overworld": "远古城市（主世界）",
    "trek:nether/very_rare": "下界·极稀有遗迹",
    "trek:end/very_rare": "末地·极稀有遗迹",
    # T6 传说级
    "betterstrongholds:stronghold": "强化要塞",
    "trek:overworld/very_rare": "主世界·极稀有遗迹",
    "repurposed_structures:monuments_nether": "海底神殿（下界）",
    "repurposed_structures:ancient_cities_nether": "远古城市（下界）",
    # 未覆盖的 67 个（补充命名，便于后续加档）
    "minecraft:ancient_cities": "远古城市",
    "minecraft:woodland_mansions": "林地府邸",
    "minecraft:strongholds": "要塞",
    "minecraft:ocean_monuments": "海底神殿",
    "minecraft:trial_chambers": "试炼密室",
    "minecraft:desert_pyramids": "沙漠神殿",
    "minecraft:jungle_temples": "丛林神庙",
    "minecraft:nether_complexes": "下界堡垒与要塞",
    "minecraft:swamp_huts": "沼泽小屋",
    "minecraft:igloos": "雪屋",
    "minecraft:nether_fossils": "下界化石",
    "minecraft:mineshafts": "废弃矿井",
    "minecraft:ruined_portals": "废弃传送门",
    "minecraft:buried_treasures": "埋藏的宝藏",
    "minecraft:trail_ruins": "古迹废墟",
    "twilightforest:final_castle": "终焉城堡",
    "twilightforest:hydra_lair": "九头蛇巢穴",
    "twilightforest:dark_tower": "暗黑之塔",
    "twilightforest:aurora_palace": "极光宫殿",
    "twilightforest:lich_tower": "巫妖塔",
    "twilightforest:knight_stronghold": "骑士要塞",
    "twilightforest:labyrinth": "牛头人迷宫",
    "twilightforest:yeti_cave": "雪怪洞窟",
    "twilightforest:troll_cave": "巨魔洞窟",
    "twilightforest:naga_courtyard": "娜迦庭院",
    "twilightforest:quest_grove": "任务林地",
    "twilightforest:giant_house": "巨人小屋",
    "twilightforest:hedge_maze": "树篱迷宫",
    "twilightforest:large_hollow_hill": "大型空心山",
    "twilightforest:medium_hollow_hill": "中型空心山",
    "twilightforest:small_hollow_hill": "小型空心山",
    "touhou_little_maid_spell:starwatch_tower_set": "观星塔",
    "touhou_little_maid_spell:starfall_garden_set": "星落花园",
    "touhou_little_maid_spell:stellar_endshore_set": "星辉末岸",
    "touhou_little_maid_spell:cherry_cabin": "樱花小屋",
    "touhou_little_maid_spell:enchantress_footsteps_basalt_forge": "女巫足迹·玄武岩熔炉",
    "touhou_little_maid_spell:enchantress_footsteps_crimson_cabin": "女巫足迹·绯红小屋",
    "touhou_little_maid_spell:enchantress_footsteps_oasis": "女巫足迹·绿洲",
    "touhou_little_maid_spell:enchantress_footsteps_soulvalley_nook": "女巫足迹·灵魂山谷角落",
    "touhou_little_maid_spell:enchantress_footsteps_warped_perch": "女巫足迹·诡异栖木",
    "betterend:end_bridge": "末地长桥",
    "betterend:end_lake": "末地湖",
    "betterend:end_lake_normal": "末地湖（普通）",
    "betterend:end_lake_rare": "末地湖（稀有）",
    "betterend:small_island": "末地小岛",
    "betterend:sulphuric_cave": "硫磺洞窟",
    "aquamirae:pirate_ship": "海盗船",
    "aquamirae:shipwreck": "深海沉船",
    "aquamirae:surface": "深海表层遗迹",
    "aquamirae:pirate_outpost": "海盗前哨",
    "aquamirae:pirate_shelter": "海盗避难所",
    "scguns:cog_chambers": "齿轮密室",
    "scguns:trench": "战壕",
    "scguns:aboveground_medium": "地表中型据点",
    "kaleidoscope_twilight:black_forest_camp": "黑森林营地",
    "kaleidoscope_twilight:sunflower_base": "向日葵基地",
    "kaleidoscope_twilight:kitchen": "林中厨房",
    "deep_aether:brass_dungeon": "黄铜地牢",
    "deep_aether:combiner_corridor": "合成者回廊",
    "deep_aether:altar_camp": "祭坛营地",
    "deep_aether:campfire": "营火遗迹",
    "trek:end/common": "末地·常见遗迹",
    "trek:monster_room/lower": "刷怪房·下层",
    "trek:monster_room/upper": "刷怪房·上层",
    "betternether:mega_lava_lake": "巨型岩浆湖",
    "iss_elden_ring:star_observation_platform": "观星平台",
    "unusualend:chorus_cane": "紫颂蔗丛",
}

MOD_ZH = {
    "minecraft": "原版", "twilightforest": "暮色森林", "betterend": "更好的末地",
    "betternether": "更好的下界", "deep_aether": "深空天境", "aquamirae": "深海",
    "unusualend": "奇异末地", "kaleidoscope_twilight": "万花筒·暮色",
    "scguns": "焦土枪械", "touhou_little_maid_spell": "东方女仆·法术",
    "iss_elden_ring": "铁魔法×艾尔登法环", "trek": "Trek",
    "repurposed_structures": "重制结构", "dungeons_arise": "地牢浮现",
    "dungeons_arise_seven_seas": "地牢浮现·七海", "irons_spellbooks": "铁之魔法",
    "iceandfire": "冰与火", "aether": "天境", "biomeswevegone": "群峦与彼方",
    "bakeries": "烘焙坊", "hopo": "Hopo 水下", "betterdungeons": "更好的地牢",
    "bettermineshafts": "更好的矿井", "betterstrongholds": "更好的要塞",
    "betterfortresses": "更好的堡垒", "betterdeserttemples": "更好的沙漠神殿",
    "betteroceanmonuments": "更好的海底神殿", "skyarena": "天空竞技场",
    "fdbosses": "BOSS 竞技场",
}


def render(inst: str, out_path: str) -> None:
    raw = open(os.path.join(inst, "config", "sparsestructures.json5"), encoding="utf-8").read()
    tier, entries = None, []
    for line in raw.splitlines():
        m = re.match(r"\s*//\s*=====\s*(T\d)(.*?)=====", line)
        if m:
            tier = m.group(1)
            continue
        m2 = re.match(r'\s*\{\s*"structure":\s*"([^"]+)",\s*"factor":\s*([0-9.]+)', line)
        if m2 and tier:
            entries.append((tier, m2.group(1), float(m2.group(2))))
    configured = {e[1] for e in entries}

    sets = {}
    for base in [os.path.join(inst, "mods")] + [
            os.path.join(inst, f) for f in os.listdir(inst) if f.endswith(".jar")]:
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
                        sets[sid] = int(sp) if sp else None
            except Exception:
                continue

    # 档位名（按原始 T 编号对应阶梯前 7 档）
    tier_meta = {t[0]: t for t in TIERS}
    out = ["# 结构分档表（含中文名）\n",
           f"现有配置：**{len(entries)}** 条；包内 structure_set：**{len(sets)}** 个；"
           f"未覆盖：**{len(set(sets) - configured)}** 个\n",
           "| 档 | 名称 | 记录 | L(区块) | L(格) | 条数 |", "|---|---|---|---:|---:|---:|"]
    cnt = collections.Counter(e[0] for e in entries)
    for code, name, rec, lch, lblock in TIERS:
        if cnt.get(code):
            out.append(f"| {code} | {name} | {rec} | {lch:.2f} | {lblock} | {cnt[code]} |")
    out.append("")

    for code, name, rec, lch, lblock in TIERS:
        rows = [e for e in entries if e[0] == code]
        if not rows:
            continue
        out.append(f"## {code} {name}（{rec}）\n")
        out.append("| 结构 ID | 中文名 | 所属模组 | base spacing | factor |")
        out.append("|---|---|---|---:|---:|")
        for _c, sid, fac in rows:
            zh = ZH.get(sid, "")
            mod = MOD_ZH.get(sid.split(":")[0], sid.split(":")[0])
            base = sets.get(sid)
            out.append(f"| `{sid}` | {zh or '（缺译）'} | {mod} | {base if base else '?'} | {fac:.2f} |")
        out.append("")

    missing = sorted(set(sets) - configured)
    out.append(f"\n## 附：未被配置覆盖的结构（{len(missing)} 个）\n")
    out.append("| 结构 ID | 中文名 | 所属模组 | base spacing |")
    out.append("|---|---|---|---:|")
    for sid in missing:
        zh = ZH.get(sid, "")
        mod = MOD_ZH.get(sid.split(":")[0], sid.split(":")[0])
        base = sets.get(sid)
        out.append(f"| `{sid}` | {zh or '（缺译）'} | {mod} | {base if base else '?'} |")
    out.append("")

    stale = sorted(configured - set(sets))
    out.append(f"\n## 附：配置里有、包里没有（{len(stale)} 个）\n")
    out.append("无\n" if not stale else "\n".join(f"- `{s}`" for s in stale))

    no_zh = [e[1] for e in entries if e[1] not in ZH]
    out.append(f"\n## 附：缺中文名的条目（{len(no_zh)} 个）\n")
    out.append("无\n" if not no_zh else "\n".join(f"- `{s}`" for s in no_zh))

    open(out_path, "w", encoding="utf-8").write("\n".join(out))
    print("写入:", out_path)
    print("已配置:", len(entries), " 缺译:", len(no_zh), " 未覆盖:", len(missing), " 失效:", len(stale))


if __name__ == "__main__":
    render(sys.argv[1], sys.argv[2])
