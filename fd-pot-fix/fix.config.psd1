@{
    # 农夫乐事煮锅顶面（水面）修复 —— 部署配置
    # 机器相关路径/文件名只在本文件出现一次，脚本只引用符号。
    #
    # hardcode-ok: VersionsDir 是"修哪台机器的哪个实例"这一**部署目标本身**，它就是配置值；
    #              换机器/换整合包只需改这一行，代码里没有任何副本。
    VersionsDir = 'D:\GAME\MC\.minecraft\versions'

    # 实例目录名匹配片段（避免把带中文的完整目录名再写死一遍）
    InstanceMatch = '*所留下的足迹*'

    # 出问题的资源包（就地修改它，脚本会先备份）
    ResourcePackName = "SPBR-Farmer's Delight_1.1.zip"

    # 提供原始纹理的模组 jar（用通配符匹配，避免写死版本号）
    SourceModJarPattern = 'FarmersDelight-*.jar'

    # 要替换的纹理条目（资源包内路径）
    TextureEntry = 'assets/farmersdelight/textures/block/cooking_pot_top.png'
}
