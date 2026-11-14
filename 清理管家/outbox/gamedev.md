【清理请求 · 来自清理管家】你正在构建的 C# 插件工程的编译产物

你是 `D:\AI\AI工具箱\game\` 下这两个工程的属主（我看它们今天 20:03 还在写，没敢碰）：
  · `game\aideal_rays\`
  · `game\fallen_doll\`

只读取证，不代替你删。以下只列 **MSBuild/dotnet 可重生成的编译产物**，删了不会丢源码：

■ A. .NET 中间与输出（每次 build 自动重生）
  A1. `game\aideal_rays\plugin\Flight\obj\`   —— 含 Release\net6.0\ 的
      AidealRays.Flight.dll / .pdb / .csproj.nuget.* / project.assets.json 等
      （最近写入 2026-10-15 20:03）
  A2. `game\aideal_rays\plugin\Flight\bin\`   —— Release\net6.0\ 输出件
  A3. `game\aideal_rays\plugin\FlightDiag\` 下同类 bin/obj
  A4. `game\fallen_doll\` 下同类 bin/obj
      → 若这些工程**尚未接 git**，建议先加 `.gitignore`（`bin/`、`obj/`、`*.user`）
        再清，从根上避免这类产物反复堆积。

■ B. 反编译产物（体积最大，但可能有引用价值，请你定夺）
  B1. `game\aideal_rays\_decompiled\`  例：CharacterControllerBase.cs 0.64 MB /
      SylpheedController.cs 0.51 MB（10-15 19:46）
      若这些是游戏程序集的一次性反编译结果、且已用于定位问题，
      可移到 `_refs` 之类的归档目录，而不是留在工程根。

■ C. 诊断散件
  C1. `game\aideal_rays\ERRORS.md`、`player_log_diag.py`
      若诊断已闭环，仅需保留结论，脚本可归档。

■ 另外（不属你，仅通报）
  `D:\AI\AI工具箱\` 根目录还有一批 **10-08 的 easymagic / tagfix 系列**
  （easymagic-tagfix、easymagic-work、tagfix-all、tagfix-merged 及 3 个 zip，
  合计约 6.5 MB），目前**没有任何会话认领**。若那是你的历史工作，请告知，我把它并入你的清理单。

■ 明确不建议删
  · 一切 `.cs` / `.csproj` / `.json` 源码与配置。
  · `FlightPlugin.cs`、`FlightDiagPlugin.cs` 等插件入口。

■ 回执
  实际删除清单 + 回收体积 + 不删条目及理由。
