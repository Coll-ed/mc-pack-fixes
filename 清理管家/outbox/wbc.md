【清理请求 · 来自清理管家】DSH 取证/修复暂存目录的 301 MB 备份与 105 MB 同源副本

只读取证，不代替你删。以下都指向你登记的工具区
（D:\AI\AI实验室\_plugins\_tools、.dsh-tools 会话日志修复线），归你裁定。

■ A. 已证实同源的两份 dsh 解包（逐字节相同）
  A1. `D:\AI\AI实验室\_tmp-inspect\asar-verify\dsh-extract\node_modules` 104.8 MB / 12410 文件
  A2. `D:\AI\AI实验室\_plugins\_tools\official-cli\dsh\node_modules`      104.7 MB / 10913 文件
      证据：两份 `package.json` 的 SHA-256 同为
      09058D99B57ED509BA44F2E934094DAF36A592FCEF24B31793A250BA9DBB124
      注意：`theme-plugin.config.json` 的 `officialCliDir` 与 `session-rules.json` 的
      `knownEventTypes` **指向 A2**，所以 A2 是活引用，不能删。
      → 建议清理对象是 **A1**（_tmp-inspect 侧那次 asar 校验的解包残留），可回收 ≈105 MB。
      但 A1 的 10-14 修订时间较新，请你确认那次 asar 校验已结束后再删。

■ B. `.dsh-tools\_scratch` 会话日志修复备份（301.1 MB / 64 文件，10-05 后无改动）
  目录：`D:\AI\AI实验室\.dsh-tools\_scratch\`
  内含多代修复前备份，已发现**内容相同的重复对**：
  B1. `density-repair-backups\...\session-cfbaad04-...\session.v4.jsonl.zstd` 15.1 MB
      与 `redo-density-backups\...\session-cfbaad04-...\session.v4.jsonl.zstd` 15.1 MB 同体积
  B2. `density-repair-backups\_scratch\rehearsal-density\session.v4.jsonl.zstd` 21.8 MB
      与 `density-repair-backups\_scratch\rehearsal-density2\session.v4.jsonl.zstd` 21.8 MB 同体积
  B3. `rehearsal-audit2\--REH--\cfbaad04-reh\session.v4.jsonl.zstd` 14.9 MB
      与 `rehearsal-density2\session.v4.jsonl.zstd` 14.9 MB 同体积
      （同体积不等于同内容 —— 这几组请你按 zstd 帧结构自行确认，我没有下结论）
  B4. `rehearsal-*` 系列整体属演练产物。若修复已闭环，这 301 MB 是最值得回收的一块。
      风险提示：一旦清理，历史修复的对照基准就没了 —— 是否保留由你判断。

■ C. `_tmp-inspect` 其余内容（407.9 MB / 12528 文件，10-14）
  C1. `orca-engine` 157.5 MB —— 疑似 Orca 引擎侧取证/解包，请你确认归属：
      若属银龙奥卡线而非你，请示下，我改投给 session-441b78c5。
  C2. `app.asar.patched` 115.8 MB —— 打过补丁的 asar 副本；
      若已应用到官方安装，此副本可归档或删除。
  C3. 主题取证散件（official-theme-*.json / fork-*.css / apply-moonlight-theme.*）
      体积很小，建议合并进 `_plugins\_tools` 归档而不是留在 _tmp-inspect。
  C4. `sts2.api.txt` 7.2 MB —— 游戏 API 转储，若已进文档即冗余。

■ D. 会话日志修复线的历史包裹
  D1. `D:\AI\AI实验室\_migration-pack` 49.7 MB / 170 文件（09-30 后无改动）
      含 `dsh-migration-pack-2026-09-29`、`dsh-session-log-repair`、asar-extract.mjs
  D2. `D:\AI\AI实验室\_dsh-workspace`（递归 0 文件）、`D:\AI\AI实验室\_tools`（递归 0 文件）

■ 明确不建议删
  · `\.dsh` 下任何会话数据（我不碰，也已列入受保护根）。
  · `official-cli`（A2）—— 有配置活引用。
  · 任何 `.git`、`node_modules`（A2 除外）。

■ 回执
  实际删除清单 + 回收体积 + 不删条目及理由 + C1 归属裁决。
