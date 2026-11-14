# Wakes 模组修复

唤醒生成、粒子、配置界面（YACL/ModMenu 集成）与 mixin。

来源：`_sessions/wakesfix`。只收代码与文档，不含 jar / 备份 / 解包资源。

**文件（55）**：

- `decomp/src/com/goby56/wakes/command/DebugCommand.java`
- `decomp/src/com/goby56/wakes/config/ModMenuIntegration.java`
- `decomp/src/com/goby56/wakes/config/WakesConfig.java`
- `decomp/src/com/goby56/wakes/config/YACLIntegration.java`
- `decomp/src/com/goby56/wakes/duck/ProducesWake.java`
- `decomp/src/com/goby56/wakes/event/WakeTicker.java`
- `decomp/src/com/goby56/wakes/mixin/WakeSpawnerMixin.java`
- `decomp/src/com/goby56/wakes/particle/custom/SplashCloudParticle.java`
- `decomp/src/com/goby56/wakes/particle/custom/SplashPlaneParticle.java`
- `decomp/src/com/goby56/wakes/particle/ModParticles.java`
- `decomp/src/com/goby56/wakes/particle/WithOwnerParticleType.java`
- `decomp/src/com/goby56/wakes/render/BlendingFunction.java`
- `decomp/src/com/goby56/wakes/render/debug/DebugUtils.java`
- `decomp/src/com/goby56/wakes/render/debug/WakeDebugRenderer.java`
- `decomp/src/com/goby56/wakes/render/FoamOutlineRenderer.java`
- `decomp/src/com/goby56/wakes/render/model/WakeModel.java`
- `decomp/src/com/goby56/wakes/render/RenderType.java`
- `decomp/src/com/goby56/wakes/render/SplashPlaneRenderer.java`
- `decomp/src/com/goby56/wakes/render/WakeTextureRenderer.java`
- `decomp/src/com/goby56/wakes/tests/GenericNode.java`
- `decomp/src/com/goby56/wakes/tests/QuadTreeTest.java`
- `decomp/src/com/goby56/wakes/tests/ResolutionTest.java`
- `decomp/src/com/goby56/wakes/tests/WakesUtilsTest.java`
- `decomp/src/com/goby56/wakes/utils/Age.java`
- `decomp/src/com/goby56/wakes/utils/Position.java`
- `decomp/src/com/goby56/wakes/utils/QuadTree.java`
- `decomp/src/com/goby56/wakes/utils/QueueSet.java`
- `decomp/src/com/goby56/wakes/utils/WakeColor.java`
- `decomp/src/com/goby56/wakes/utils/WakeHandler.java`
- `decomp/src/com/goby56/wakes/utils/WakeNode.java`
- `decomp/src/com/goby56/wakes/utils/WakesUtils.java`
- `decomp/src/com/goby56/wakes/WakesClient.java`
- `decomp/src/summary.txt`
- `decomp-voxy/src/me/cortex/voxy/client/core/compat/eclipticseasons/EsCompatGate.java`
- `decomp-voxy/src/me/cortex/voxy/client/core/compat/eclipticseasons/IVoxyAboveLightingSupplier.java`
- `decomp-voxy/src/me/cortex/voxy/client/core/compat/eclipticseasons/SeasonalLod.java`
- `decomp-voxy/src/me/cortex/voxy/client/core/compat/eclipticseasons/SeasonalMeshView.java`
- `decomp-voxy/src/me/cortex/voxy/client/core/compat/eclipticseasons/SeasonalSnowRefresher.java`
- `decomp-voxy/src/me/cortex/voxy/client/core/compat/eclipticseasons/VoxyClientTool.java`
- `decomp-voxy/src/me/cortex/voxy/client/core/compat/eclipticseasons/VoxyEsHandler.java`
- …… 另有 15 个
