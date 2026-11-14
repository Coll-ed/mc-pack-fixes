@{
    # Easy Magic 改版部署配置 —— 机器相关的路径只在本文件出现一次，脚本通过符号引用。
    #
    # hardcode-ok: 下面 VersionsDir 是"要部署到哪台机器的哪个目录"这一**部署目标本身**，
    #              它就是配置值；若从环境变量取，脚本在无人值守时反而会静默失败。
    #              换机器/换整合包只需改这一行，代码里没有任何副本。
    VersionsDir = 'D:\GAME\MC\.minecraft\versions'

    # 实例目录名匹配片段（用 -like 匹配，避免把带中文的完整目录名再写死一遍）
    InstanceMatch = '*所留下的足迹*'

    # 模组 jar 文件名（NeoForge 原生模组，不走 Connector remap，直接替换即可）
    JarName = 'EasyMagic-v21.1.4-1.21.1-NeoForge.jar'
}
