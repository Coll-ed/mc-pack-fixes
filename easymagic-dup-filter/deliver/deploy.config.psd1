@{
    # EasyMagic「附魔刷新重复」修复 —— 部署配置
    # 机器相关路径/文件名只在本文件出现一次，脚本只引用符号。

    # hardcode-ok: VersionsDir 是"部署到哪台机器的哪个实例"这一部署目标本身，它就是配置值；
    #              换机器/换整合包只改这一行，代码里没有副本。
    VersionsDir = 'D:\GAME\MC\.minecraft\versions'

    # 实例目录名匹配片段（避免把带中文的完整目录名再写死一遍）
    InstanceMatch = '*所留下的足迹*'

    # 目标模组文件名（脚本会覆写它，覆写前先备份）
    ModJarName = 'EasyMagic-v21.1.4-1.21.1-NeoForge.jar'

    # 备份后缀（加在目标文件名之后）
    BackupSuffix = '.dupfilter-bak'

    # 修好的 jar（相对本脚本所在目录）
    # v2：修掉 v1「任何槽位凑不出就清池」的副作用（实测 pool=55 就被清空，而全集约 98）
    PatchedJar = 'build\EasyMagic-v21.1.4-1.21.1-NeoForge-norepeat.jar'

    # 修好的 jar 的 SHA256 前 16 位 —— 防止部署到错的产物
    # hardcode-ok: 这是构建产物指纹，用于部署前校验；产物变了必须同步更新（脚本会报错提醒）。
    ExpectedPatchedSha16 = 'C37DF41415C0C57E'

    # 被替换掉的旧版 jar 的 SHA256 前 16 位（部署前校验，防止覆盖非预期版本）
    # hardcode-ok: 同上，这是"当前部署版本"的指纹，用于确认替换对象正确。
    #             注：备份文件仍是更早的原始版 ECB00732C2A1F3BC（脚本不会覆盖已有备份），
    #             所以 rollback 会一路回到未打补丁的原始 jar。
    ExpectedCurrentSha16 = '58FBAD115A1295FE'
}
