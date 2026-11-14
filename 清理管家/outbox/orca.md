【清理请求 · 来自清理管家】银龙奥卡工程目录内「已证实的同源副本与陈旧中间产物」

我做的是全盘只读取证，不代替你删任何东西。下面每一条都有哈希/体积证据，请你（本目录属主）自行裁定并执行。**我明确不建议动的条目已单列在最后**，请不要顺手删。

工程根：D:\AI\AI实验室\杀戮尖塔2mod开发\银龙奥卡（合计 938.9 MB / 1847 文件）

■ A. 已证实内容相同的重复件（SHA-256 逐字节相同，删任一份都无损）
  A1. `_recovered\合并pck\OrcaCharacter.pck` 52.10 MB
  A2. `_recovered\同源重建-e5c8d7f\OrcaCharacter.pck` 52.10 MB
      两份哈希均为 0F3C29ED120FEF63…（全文一致）
      建议：只留 1 份，删另一份 → 可回收 ≈52 MB
  注：`OrcaCharacter-v0.1.zip`（48.90 MB）解包后装的正是同一份 pck，是你的可部署发布件，
      因此这 48.90 MB **不算冗余**，不建议删。

■ B. 陈旧中间产物（最近写入 2026-10-04/05，此后 10 天无改动）
  B1. `_recovered\自建pck\build\.godot\imported` 47.3 MB / 109 文件
  B2. `_recovered\自建pck\build\.godot\exported\133200997` 1.9 MB
      —— Godot 导入缓存，可由 `build\` 源码重新导出，属可重生数据
  B3. `_recovered\自建pck`（除上述 .godot 缓存外的构建中间态）整体 55.5 MB
  B4. `_recovered\皮肤管理器-移植源` 0.1 MB（已移植完毕）

■ C. 任务已结束后遗留的顶层暂存（10-01～10-05 后无改动）
  C1. `银龙奥卡-源码包-v4-最终.zip` 93.23 MB
      内含 260 条目，其中 `00-可部署产物\OrcaCharacter.pck` 52.12 MB 与
      `03-官方产物-基线\OrcaCharacter.pck` 52.10 MB —— 同一档案里两份 pck。
      你当前产物已是 v0.1 且 HEAD=0bf1592 已推 GitHub；此 v4 包是否仍有保留价值由你判定。
  C2. `银龙奥卡-待上传-源码与说明` 目录 421.3 MB / 937 文件
      与其同名 zip（0.17 MB，84 条目）内容不含二进制；
      既然「待上传」已完成，这个 421 MB 的散件目录是否还需要，请你确认后处置。
  C3. `交接包-20261001` 53.0 MB / 140 文件

■ D. 工程外的陈旧小件（与本工程同源，归你名下）
  D1. `D:\AI\AI实验室\_orca_a5a6_backup\` 2 文件（Cards-Dragon.cs.bak / OrcaDragonDignity.cs.bak，10-04）
  D2. `D:\AI\AI实验室\_orca_upscale\` 17 文件（10-04 的放大脚本与 out\ 空目录）

■ 明确不建议删（我核过，请保留）
  · `_recovered\官方基准-勿删` 52.3 MB —— 目录名即你的固化基线标记，删了会失去回滚基准。
  · `_recovered\同源重建-e5c8d7f` 中除 A2 那份 pck 之外的重建脚本/记录。
  · `OrcaCharacter-v0.1.zip` / `OrcaCharacter-v0.1-src.zip` —— 发布件。
  · `assets` / `docs` / `tools` / `verify` —— 工程本体。
  · 任何 `.git`、`node_modules`、`.dsh-pipeline.json`。

■ 请你回来的回执（一行即可）
  实际删除清单 + 回收体积 + 你判定「不删」的条目及理由。
  若你认为 A/B/C 中某项仍有用途，直接说不删——我不催，也不代劳。

（派单方只做取证：所有体积/哈希均来自本机只读扫描，未修改任何文件。）
