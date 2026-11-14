@{
    # MC 性能补强 —— 配置（单一来源）
    # 所有路径 / URL / 期望哈希 / 数值都在本文件，脚本只引用符号（不写死）。

    # hardcode-ok: VersionsDir 是"装到哪台机器的哪个实例"这一部署目标本身，它就是配置值。
    VersionsDir = 'D:\GAME\MC\.minecraft\versions'
    InstanceMatch = '*所留下的足迹*'
    # 全局数据包目录（.minecraft\datapacks，对所有存档生效）
    GlobalDatapackDir = 'D:\GAME\MC\.minecraft\datapacks'

    DownloadDir = 'downloads'
    BuildDir = 'build'
    ManifestName = 'installed.json'

    # ───────────── 预生成：Genesis（推荐）─────────────
    # 为什么是它（按 NeoForge 1.21.1 逐个筛过）：
    #   真正的"自动预生成"模组（World Pregen / ChunkMill / Auto Chunky）要么只支持 1.21.9+/26.1+、
    #   要么仅 Fabric、要么语义是"没人在线时才跑"（单人游戏永远不跑）→ 都不适用。
    #   Genesis 支持 NeoForge 1.21.1，且语义正对需求：
    #   · 进世界后**在后台继续跑**（ENTER WORLD to keep it running in the background）
    #   · 进度 HUD（维度/区块数/百分比/ETA）、可 PAUSE / RESUME / CANCEL
    #   · pregen all 一次覆盖所有维度；已有存档也能用命令指过去
    #   · 前置 Architectury API ≥13.0.8 —— 你已装 13.0.11 ✓ 无需再装
    # 备选（若更想要"零操作自动续跑"）：Chunky + make-autopregen.ps1 生成的数据包，见 README。
    #   两者只需装其一，不要同时装两个预生成器。
    Pregen = @{
        DatapackName = 'autopregen'
        Namespace = 'autopregen'
        OnLoadFunction = 'on_load'
        FirstSetupFunction = 'first_setup'
        # 数据包版本号：1.21 / 1.21.1 记作 48（来源：Minecraft Wiki「Data pack version」）
        PackFormat = 48
        # 计分板名（Minecraft 上限 16 字符）
        ScoreboardObjective = 'autopregen'
        ScoreboardHolder = '#autopregen'
        # 预生成半径（方块）—— "要多大已生成地图"是产品决定，可调
        Radius = 2000
        Shape = 'square'
        Worlds = @('minecraft:overworld')
        CenterOnSpawn = $true
    }

    # ───────────── 模组配置的内存调优（apply-memory-tuning.ps1 用）─────────────
    # 只列"确实有内存收益"的键，每项都注明依据；不做无依据的改动。
    # Section 必须写对：c2me.toml 里同名键可能出现在多个节，脚本按节精确定位。
    # 值的写法要跟文件里一致（c2me 用带引号的字符串，如 "default"）。
    Tuning = @(
        @{
            # Voxy 的 LOD 数据随探索累积，是"越玩越卡"最大的堆外内存来源。
            # sectionRenderDistance 单位是"×32 区块"：16 ⇒ 512 区块，10 ⇒ 320 区块（依据 voxy 配置注释）。
            File = 'voxy-client.toml'
            Section = ''    # 该文件是扁平结构，没有节名
            Keys = @{
                sectionRenderDistance = '10'
                # 处理线程 10 → 6：16 核机器上 6 条仍富余，减少每个线程各自持有的缓冲
                serviceThreads = '6'
            }
        },
        @{
            # c2me 官方注释：(Default: false) Whether to enable low memory mode
            #   "This option will attempt to aggressively unload unused chunks."
            # ⇒ 正对"走过的区域越攒越多"；它只在 useLegacyScheduling 关闭时生效（默认关闭，满足条件）。
            # 另核对过 [ioSystem] 的 chunkDataCacheLimit 默认 32768（≈32 MB），不是内存问题，故不动。
            File = 'c2me.toml'
            Section = 'chunkSystem'
            Keys = @{
                lowMemoryMode = '"true"'
            }
        }
    )

    # ───────────── 模组（**需你同意后才安装**）─────────────
    # 内存问题**不需要模组**：见同目录 JVM-方案A-修G1.txt / JVM-方案B-ZGC.txt
    # 已装且够用、不要重复装：modernfix、ferritecore、byepregen（区块保存/GC 压力优化）、
    #                          architectury 13.0.11（Genesis 前置，已满足）
    Mods = @(
        @{
            Purpose = '预生成：进世界后后台持续跑，带进度 HUD，pregen all 覆盖所有维度'
            Name    = 'Genesis - World Previewer & Pregenerator'
            File    = 'genesis-1.1.1-1.21.1-NEOFORGE.jar'
            Url     = 'https://cdn.modrinth.com/data/xXuFelru/versions/3gRxgOI9/genesis-1.1.1-1.21.1-NEOFORGE.jar'
            Sha1    = 'be9e619ff5271ecad5b409478fae3884da7beb32'
            Size    = 158181
        }
    )
}
