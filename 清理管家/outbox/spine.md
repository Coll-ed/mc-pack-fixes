【清理请求 · 来自清理管家】你名下渲染/取证目录的陈旧中间产物

只读取证，不代替你删。工程目录最近仍在写（_comfy_bridge 10-14、_spine_render 10-09），
所以下面只列**10-04～10-09 之后无改动**的暂存/缓存，且都可由源重建。

■ A. Spine 渲染侧（D:\AI\AI实验室\_spine_render，399.1 MB / 411 文件）
  A1. `node_modules` 4.77 MB —— 依赖缓存，重装即可，可安全清除。
  A2. 渲染输出中的中间帧/对照图：若 setup 姿态与逐件渲染的**最终定版帧**已另有归档，
      这里的中间产物可清。此项请你按自己的流水线判断，我只提供体积坐标，不越权定夺。

■ B. 卡面/AI 放大侧（D:\AI\AI实验室\_orca_upscale，17 文件，10-04）
  B1. `_orca_upscale\out\`（空目录）
  B2. compare.py / fit_pad.py / verify.py —— 一次性放大比对脚本；
      若 Upscayl anime-x4 流程已固化到 `_comfy_bridge`，这三个可归档或删除。

■ C. 内容相同的重复件（同源证据）
  C1. `D:\AI\AI实验室\杀戮尖塔2mod开发\_refs` 45.3 MB / 44 文件
      与 Spine 渲染源可能重叠；请你确认 `_refs` 是否是 `_spine42-spec` 的下游副本。

■ D. 空壳
  D1. `D:\AI\AI实验室\_dsh-workspace`（递归后 0 文件）—— 疑似挂载点占位，请你确认后处置。
  D2. `D:\AI\AI实验室\_tools`（递归后 0 文件，10-09 创建后从未写入）

■ 明确不建议删
  · `_spine42-spec`（你的解析器与规格）。
  · `_comfy_bridge` 里 card_workflow.py / anima_probe.py / check_lora.py /
    collect_comfy_output.py / style_tags.json —— 你登记为单一来源的资产。
  · 任何 `.git`、`.dsh-pipeline.json`。

■ 回执
  实际删除清单 + 回收体积 + 你判定不删的条目及理由。
