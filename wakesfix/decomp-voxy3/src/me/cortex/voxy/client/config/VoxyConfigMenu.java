/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint
 *  net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge
 *  net.caffeinemc.mods.sodium.api.config.ConfigState
 *  net.caffeinemc.mods.sodium.api.config.option.OptionFlag
 *  net.caffeinemc.mods.sodium.api.config.option.OptionImpact
 *  net.caffeinemc.mods.sodium.api.config.option.Range
 *  net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder
 *  net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder
 *  net.minecraft.client.Minecraft
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.neoforged.fml.ModList
 */
package me.cortex.voxy.client.config;

import java.util.Locale;
import me.cortex.voxy.client.ClientSessionEvents;
import me.cortex.voxy.client.ServerCapabilities;
import me.cortex.voxy.client.config.SodiumConfigBuilder;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.client.core.SSAO;
import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.client.core.compat.eclipticseasons.EsCompatGate;
import me.cortex.voxy.client.core.util.IrisUtil;
import me.cortex.voxy.client.iris.LiteShaderStatus;
import me.cortex.voxy.common.util.cpu.CpuLayout;
import me.cortex.voxy.commonImpl.VoxyCommon;
import me.cortex.voxy.commonImpl.VoxyInstance;
import me.cortex.voxy.compat.far.FarEntityClient;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

@ConfigEntryPointForge(value="voxy")
public class VoxyConfigMenu
implements ConfigEntryPoint {
    public void registerConfigLate(ConfigBuilder builder) {
        if (!VoxyCommon.isAvailable()) {
            return;
        }
        VoxyConfig cfg = VoxyConfig.CONFIG;
        boolean sableInstalled = ModList.get().isLoaded("sable");
        boolean createInstalled = ModList.get().isLoaded("create");
        boolean powerGridInstalled = ModList.get().isLoaded("powergrid");
        boolean copycatsInstalled = ModList.get().isLoaded("copycats");
        boolean simulatedInstalled = ModList.get().isLoaded("simulated");
        boolean framedBlocksInstalled = ModList.get().isLoaded("framedblocks");
        boolean littleTilesInstalled = ModList.get().isLoaded("littletiles");
        boolean domumInstalled = ModList.get().isLoaded("domum_ornamentum");
        boolean seasonsInstalled = EsCompatGate.shouldArm();
        ModOptionsBuilder options = builder.registerModOptions("voxy", VoxyCommon.displayName(), VoxyCommon.MOD_VERSION).setIcon(ResourceLocation.parse((String)"voxy:icon.png"));
        String renderReload = OptionFlag.REQUIRES_RENDERER_RELOAD.getId().toString();
        SodiumConfigBuilder.buildToSodium(builder, options, cfg::save, VoxyConfigMenu::registerPostApplyOps, VoxyConfigMenu.generalPage(cfg, renderReload), VoxyConfigMenu.renderingPage(cfg, renderReload), VoxyConfigMenu.experimentalPage(cfg, renderReload), VoxyConfigMenu.fakesightPage(cfg), VoxyConfigMenu.compatibilityPage(cfg, renderReload, sableInstalled, createInstalled, powerGridInstalled, copycatsInstalled, simulatedInstalled, framedBlocksInstalled, littleTilesInstalled, domumInstalled, seasonsInstalled));
    }

    private static void registerPostApplyOps(SodiumConfigBuilder.PostApplyOps postOp) {
        postOp.register("voxy:update_threads", () -> {
            VoxyInstance instance = VoxyCommon.getInstance();
            if (instance != null) {
                instance.updateDedicatedThreads();
            }
        }, "voxy:enabled").register("voxy:iris_reload", IrisUtil::reload, new String[0]).register("voxy:refresh_far_entities", FarEntityClient::sendHello, new String[0]).register("voxy:refresh_chunk_request", () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null) {
                minecraft.options.broadcastOptions();
            }
        }, new String[0]);
    }

    private static SodiumConfigBuilder.Page generalPage(VoxyConfig cfg, String renderReload) {
        return (SodiumConfigBuilder.Page)new SodiumConfigBuilder.Page((Component)Component.translatable((String)"voxy.config.general"), new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.general"), (SodiumConfigBuilder.Option)((SodiumConfigBuilder.BoolOption)((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:enabled", (Component)Component.translatable((String)"voxy.config.general.enabled"), () -> cfg.enabled, v -> {
            cfg.enabled = v;
            if (v.booleanValue() && ClientSessionEvents.inSession) {
                VoxyCommon.createInstance();
            }
        }).setPostChangeRunner(c -> {
            if (!c.booleanValue()) {
                IGetVoxyRenderSystem vrsh = (IGetVoxyRenderSystem)Minecraft.getInstance().levelRenderer;
                if (vrsh != null) {
                    vrsh.voxy$shutdownRenderer();
                }
                VoxyCommon.shutdownInstance();
            }
        }, new String[0])).setPostChangeFlags(renderReload, "voxy:iris_reload")).setEnabler(null)), new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.threads"), new SodiumConfigBuilder.Option[]{new SodiumConfigBuilder.IntOption("voxy:thread_count", (Component)Component.translatable((String)"voxy.config.general.serviceThreads"), () -> cfg.serviceThreads, v -> {
            cfg.serviceThreads = v;
        }, new Range(1, CpuLayout.getCoreCount(), 1)).setPostChangeFlags("voxy:update_threads"), new SodiumConfigBuilder.BoolOption("voxy:use_sodium_threads", (Component)Component.translatable((String)"voxy.config.general.useSodiumBuilder"), () -> !cfg.dontUseSodiumBuilderThreads, v -> {
            cfg.dontUseSodiumBuilderThreads = v == false;
        }).setPostChangeFlags("voxy:update_threads", renderReload)}), new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.data"), new SodiumConfigBuilder.BoolOption("voxy:ingest_enabled", (Component)Component.translatable((String)"voxy.config.general.ingest"), () -> cfg.ingestEnabled, v -> {
            cfg.ingestEnabled = v;
        }), new SodiumConfigBuilder.BoolOption("voxy:show_join_message", (Component)Component.translatable((String)"voxy.config.general.showJoinMessage"), () -> cfg.showJoinMessage, v -> {
            cfg.showJoinMessage = v;
        }))).setEnabler("voxy:enabled");
    }

    private static SodiumConfigBuilder.Page renderingPage(VoxyConfig cfg, String renderReload) {
        SodiumConfigBuilder.Group[] groupArray = new SodiumConfigBuilder.Group[8];
        groupArray[0] = new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.activation"), (SodiumConfigBuilder.Option)((SodiumConfigBuilder.BoolOption)((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:rendering", (Component)Component.translatable((String)"voxy.config.general.rendering"), () -> cfg.enableRendering, v -> {
            cfg.enableRendering = v;
        }).setPostChangeRunner(c -> {
            IGetVoxyRenderSystem vrsh = (IGetVoxyRenderSystem)Minecraft.getInstance().levelRenderer;
            if (vrsh != null) {
                if (c.booleanValue()) {
                    vrsh.voxy$createRenderer();
                } else {
                    vrsh.voxy$shutdownRenderer();
                }
            }
        }, "voxy:enabled", renderReload)).setPostChangeFlags("voxy:iris_reload")).setEnabler("voxy:enabled"));
        SodiumConfigBuilder.Option[] optionArray = new SodiumConfigBuilder.Option[5];
        optionArray[0] = ((SodiumConfigBuilder.IntOption)new SodiumConfigBuilder.IntOption("voxy:subdivsize", (Component)Component.translatable((String)"voxy.config.general.subDivisionSize"), cfg::getRenderQualityLevel, cfg::setRenderQualityLevel, new Range(0, 6, 1)).setFormatter(v -> Component.translatable((String)("voxy.config.general.renderQuality." + v))).setDefault(3)).setImpact(OptionImpact.HIGH);
        optionArray[1] = ((SodiumConfigBuilder.IntOption)new SodiumConfigBuilder.IntOption("voxy:render_distance", (Component)Component.translatable((String)"voxy.config.general.renderDistance"), () -> Math.round(cfg.sectionRenderDistance * 16.0f), v -> {
            cfg.sectionRenderDistance = (float)v.intValue() / 16.0f;
        }, new Range(10, 1024, 1)).setFormatter(v -> Component.literal((String)Integer.toString(v * 2))).setPostChangeRunner(c -> {
            VoxyRenderSystem vrs;
            IGetVoxyRenderSystem vrsh = (IGetVoxyRenderSystem)Minecraft.getInstance().levelRenderer;
            if (vrsh != null && (vrs = vrsh.voxy$getRenderSystem()) != null) {
                vrs.setRenderDistance(cfg.sectionRenderDistance);
            }
        }, "voxy:rendering", renderReload)).setImpact(OptionImpact.MEDIUM);
        optionArray[2] = new SodiumConfigBuilder.IntOption("voxy:render_pressure", (Component)Component.translatable((String)"voxy.config.general.renderPressure"), () -> cfg.getRenderPressureLevel(), v -> {
            cfg.renderPressure = v;
        }, new Range(0, 4, 1)).setFormatter(v -> Component.translatable((String)("voxy.config.general.renderPressure." + v))).setImpact(OptionImpact.HIGH);
        optionArray[3] = ((SodiumConfigBuilder.IntOption)((SodiumConfigBuilder.IntOption)new SodiumConfigBuilder.IntOption("voxy:leaf_lod_mode", (Component)Component.translatable((String)"voxy.config.general.leafLodMode"), () -> cfg.getLeafLodMode().ordinal(), v -> cfg.setLeafLodMode(VoxyConfig.LeafLodMode.values()[v]), new Range(0, 2, 1)).setFormatter(v -> Component.translatable((String)("voxy.config.general.leafLodMode." + VoxyConfig.LeafLodMode.values()[v].name().toLowerCase(Locale.ROOT)))).setDefault(VoxyConfig.LeafLodMode.BALANCED.ordinal())).setPostChangeFlags(renderReload)).setImpact(OptionImpact.MEDIUM);
        optionArray[4] = ((SodiumConfigBuilder.IntOption)new SodiumConfigBuilder.IntOption("voxy:earth_curve_ratio", (Component)Component.translatable((String)"voxy.config.general.earthCurveRatio"), () -> cfg.earthCurveRatio, v -> {
            cfg.earthCurveRatio = v > 0 && v < 50 ? 50 : v;
        }, new Range(0, 10000, 50)).setPostChangeFlags(renderReload)).setImpact(OptionImpact.LOW);
        groupArray[1] = new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.quality"), optionArray);
        groupArray[2] = (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.vanillaEffects"), new SodiumConfigBuilder.Option[]{new SodiumConfigBuilder.BoolOption("voxy:environmental_fog", (Component)Component.translatable((String)"voxy.config.general.environmental_fog"), () -> cfg.useEnvironmentalFog, v -> {
            cfg.useEnvironmentalFog = v;
        }), ((SodiumConfigBuilder.EnumOption)new SodiumConfigBuilder.EnumOption<SSAO.SSAOMode>("voxy:ssao_mode", SSAO.SSAOMode.class, (Component)Component.translatable((String)"voxy.config.general.ssao_mode"), () -> cfg.getSSAOMode(), v -> cfg.setSSAOMode((SSAO.SSAOMode)((Object)v))).setImpact(OptionImpact.MEDIUM)).setPostChangeFlags(renderReload)}).setEnablerInherit(s -> !IrisUtil.irisShaderPackEnabled(), ConfigState.UPDATE_ON_REBUILD);
        groupArray[3] = (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.clouds"), new SodiumConfigBuilder.Option[]{new SodiumConfigBuilder.BoolOption("voxy:adapt_cloud_distance", (Component)Component.translatable((String)"voxy.config.general.adaptCloudDistance"), () -> cfg.adaptCloudDistance, v -> {
            cfg.adaptCloudDistance = v;
        }), new SodiumConfigBuilder.IntOption("voxy:cloud_distance", (Component)Component.translatable((String)"voxy.config.general.cloudDistance"), () -> cfg.cloudDistance, v -> {
            cfg.cloudDistance = v;
        }, new Range(0, 128, 1)).setImpact(OptionImpact.LOW)}).setEnablerInherit(s -> !IrisUtil.irisShaderPackEnabled(), ConfigState.UPDATE_ON_REBUILD);
        groupArray[4] = (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.fog"), new SodiumConfigBuilder.Option[]{new SodiumConfigBuilder.IntOption("voxy:fog_intensity", (Component)Component.translatable((String)"voxy.config.general.fogIntensity"), () -> Math.round(cfg.fogIntensity * 100.0f), v -> {
            cfg.fogIntensity = (float)v.intValue() / 100.0f;
        }, new Range(0, 100, 1)).setImpact(OptionImpact.LOW), new SodiumConfigBuilder.IntOption("voxy:fog_density", (Component)Component.translatable((String)"voxy.config.general.fogDensity"), () -> Math.round(cfg.fogDensity * 100.0f), v -> {
            cfg.fogDensity = (float)v.intValue() / 100.0f;
        }, new Range(0, 100, 1)).setImpact(OptionImpact.LOW), ((SodiumConfigBuilder.IntOption)new SodiumConfigBuilder.IntOption("voxy:sky_fog_distance", (Component)Component.translatable((String)"voxy.config.general.skyFogDistance"), () -> cfg.skyFogDistance, v -> {
            cfg.skyFogDistance = v;
        }, new Range(0, 1024, 1)).setImpact(OptionImpact.LOW)).setPostChangeFlags(renderReload), new SodiumConfigBuilder.IntOption("voxy:fog_distance", (Component)Component.translatable((String)"voxy.config.general.fog_distance"), () -> cfg.fogDistancePercent, v -> {
            cfg.fogDistancePercent = v;
        }, new Range(5, 200, 5)).setFormatter(v -> Component.literal((String)(v + "%"))).setImpact(OptionImpact.LOW)}).setEnablerInherit(s -> !IrisUtil.irisShaderPackEnabled(), ConfigState.UPDATE_ON_REBUILD);
        groupArray[5] = new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.biomeColours"), new SodiumConfigBuilder.Option[]{((SodiumConfigBuilder.IntOption)new SodiumConfigBuilder.IntOption("voxy:biome_blend_radius", (Component)Component.translatable((String)"voxy.config.general.biomeBlendRadius"), () -> cfg.biomeBlendRadius, v -> {
            cfg.biomeBlendRadius = v;
        }, new Range(0, 7, 1)).setFormatter(v -> v == 0 ? Component.translatable((String)"voxy.config.general.biomeBlendRadius.off") : Component.literal((String)Integer.toString(v))).setPostChangeFlags(renderReload)).setImpact(OptionImpact.MEDIUM), ((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:biome_blend_grass", (Component)Component.translatable((String)"voxy.config.general.biomeBlendGrass"), () -> "water_grass".equals(cfg.biomeBlendScope), v -> {
            cfg.biomeBlendScope = v != false ? "water_grass" : "water";
        }).setPostChangeFlags(renderReload)).setImpact(OptionImpact.MEDIUM)});
        groupArray[6] = (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.farEntities"), new SodiumConfigBuilder.Option[]{new SodiumConfigBuilder.BoolOption("voxy:far_players", (Component)Component.translatable((String)"voxy.config.farEntities.players"), () -> cfg.enableFarPlayerRendering, v -> {
            cfg.enableFarPlayerRendering = v;
        }).setPostChangeFlags("voxy:refresh_far_entities"), new SodiumConfigBuilder.BoolOption("voxy:far_vehicles", (Component)Component.translatable((String)"voxy.config.farEntities.vehicles"), () -> cfg.enableFarVehicleRendering, v -> {
            cfg.enableFarVehicleRendering = v;
        }).setPostChangeFlags("voxy:refresh_far_entities"), (SodiumConfigBuilder.Option)new SodiumConfigBuilder.BoolOption("voxy:far_player_names", (Component)Component.translatable((String)"voxy.config.farEntities.names"), () -> cfg.renderFarPlayerNames, v -> {
            cfg.renderFarPlayerNames = v;
        }).setEnablerInherit("voxy:far_players"), ((SodiumConfigBuilder.Option)new SodiumConfigBuilder.IntOption("voxy:far_player_animation_distance", (Component)Component.translatable((String)"voxy.config.farEntities.animationDistance"), () -> cfg.farPlayerAnimationDistance, v -> {
            cfg.farPlayerAnimationDistance = v;
        }, new Range(0, 32768, 64)).setFormatter(v -> v == 0 ? Component.translatable((String)"voxy.config.compat.distanceFollowLod") : Component.translatable((String)"voxy.config.unit.blocks", (Object[])new Object[]{v})).setEnablerInherit("voxy:far_players")).setImpact(OptionImpact.LOW), new SodiumConfigBuilder.BoolOption("voxy:share_far_player_position", (Component)Component.translatable((String)"voxy.config.farEntities.sharePosition"), () -> cfg.shareFarPlayerPosition, v -> {
            cfg.shareFarPlayerPosition = v;
        }).setPostChangeFlags("voxy:refresh_far_entities")}).setEnablerInherit(s -> ServerCapabilities.canConfigureFarEntities(), ConfigState.UPDATE_ON_REBUILD);
        groupArray[7] = new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.vanillaExtensions"), new SodiumConfigBuilder.Option[]{new SodiumConfigBuilder.BoolOption("voxy:distant_beacons", (Component)Component.translatable((String)"voxy.config.compat.distantBeacons"), () -> cfg.distantBeacons, v -> {
            cfg.distantBeacons = v;
        }).setImpact(OptionImpact.LOW), new SodiumConfigBuilder.IntOption("voxy:distant_beacon_distance", (Component)Component.translatable((String)"voxy.config.compat.distantBeaconDistance"), () -> cfg.distantBeaconMaxChunks, v -> {
            cfg.distantBeaconMaxChunks = v;
        }, new Range(0, 512, 16)).setFormatter(VoxyConfigMenu::formatCreateDistance).setImpact(OptionImpact.LOW)});
        return (SodiumConfigBuilder.Page)new SodiumConfigBuilder.Page((Component)Component.translatable((String)"voxy.config.rendering"), groupArray).setEnablerAND("voxy:enabled", "voxy:rendering");
    }

    private static SodiumConfigBuilder.Page experimentalPage(VoxyConfig cfg, String renderReload) {
        return (SodiumConfigBuilder.Page)new SodiumConfigBuilder.Page((Component)Component.translatable((String)"voxy.config.experimental"), new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.experimentalStill"), new SodiumConfigBuilder.Option[]{((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:experimental_cmd_list_hold", (Component)Component.translatable((String)"voxy.config.experimental.cmdListHold"), () -> cfg.experimentalCmdListHold, v -> {
            cfg.experimentalCmdListHold = v;
        }).setDefault(false)).setImpact(OptionImpact.MEDIUM), ((SodiumConfigBuilder.Option)((SodiumConfigBuilder.IntOption)new SodiumConfigBuilder.IntOption("voxy:cmd_list_hold_max_frames", (Component)Component.translatable((String)"voxy.config.experimental.cmdListHoldMaxFrames"), () -> cfg.cmdListHoldMaxFrames, v -> {
            cfg.cmdListHoldMaxFrames = v;
        }, new Range(2, 60, 1)).setDefault(4)).setEnablerInherit("voxy:experimental_cmd_list_hold")).setImpact(OptionImpact.LOW), ((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:experimental_chunk_mask_reuse", (Component)Component.translatable((String)"voxy.config.experimental.chunkMaskReuse"), () -> cfg.experimentalChunkMaskReuse, v -> {
            cfg.experimentalChunkMaskReuse = v;
        }).setDefault(false)).setImpact(OptionImpact.LOW)}), new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.experimentalGpu"), new SodiumConfigBuilder.Option[]{((SodiumConfigBuilder.BoolOption)((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:experimental_opaque_near_first", (Component)Component.translatable((String)"voxy.config.experimental.opaqueNearFirst"), () -> cfg.experimentalOpaqueNearFirst, v -> {
            cfg.experimentalOpaqueNearFirst = v;
        }).setDefault(false)).setImpact(OptionImpact.MEDIUM)).setPostChangeFlags(renderReload), ((SodiumConfigBuilder.BoolOption)((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:experimental_chunk_mask_half_res", (Component)Component.translatable((String)"voxy.config.experimental.chunkMaskHalfRes"), () -> cfg.experimentalChunkMaskHalfRes, v -> {
            cfg.experimentalChunkMaskHalfRes = v;
        }).setDefault(false)).setImpact(OptionImpact.MEDIUM)).setPostChangeFlags(renderReload), ((SodiumConfigBuilder.BoolOption)((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:experimental_hiz_compute", (Component)Component.translatable((String)"voxy.config.experimental.hiZCompute"), () -> cfg.experimentalHiZCompute, v -> {
            cfg.experimentalHiZCompute = v;
        }).setDefault(false)).setImpact(OptionImpact.LOW)).setPostChangeFlags(renderReload)}), new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.experimentalMemory"), new SodiumConfigBuilder.Option[]{((SodiumConfigBuilder.IntOption)new SodiumConfigBuilder.IntOption("voxy:section_array_pool_mib", (Component)Component.translatable((String)"voxy.config.experimental.sectionArrayPoolMiB"), () -> cfg.sectionArrayPoolMiB, v -> {
            cfg.sectionArrayPoolMiB = v;
        }, new Range(25, 1024, 1)).setDefault(100)).setFormatter(v -> Component.literal((String)(v + " MiB"))).setImpact(OptionImpact.MEDIUM)}), new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.experimentalTransition"), new SodiumConfigBuilder.Option[]{new SodiumConfigBuilder.BoolOption("voxy:lod_boundary_fade", (Component)Component.translatable((String)"voxy.config.general.lodBoundaryFade"), () -> cfg.enableLodBoundaryFade, v -> {
            cfg.enableLodBoundaryFade = v;
        }).setImpact(OptionImpact.LOW), ((SodiumConfigBuilder.Option)new SodiumConfigBuilder.IntOption("voxy:lod_boundary_fade_length", (Component)Component.translatable((String)"voxy.config.general.lodBoundaryFadeLength"), () -> cfg.lodBoundaryFadeLength, v -> {
            cfg.lodBoundaryFadeLength = v;
        }, new Range(8, 64, 1)).setFormatter(v -> Component.translatable((String)"voxy.config.unit.blocks", (Object[])new Object[]{v})).setEnablerInherit("voxy:lod_boundary_fade")).setImpact(OptionImpact.LOW), ((SodiumConfigBuilder.Option)new SodiumConfigBuilder.IntOption("voxy:lod_boundary_inset", (Component)Component.translatable((String)"voxy.config.general.lodBoundaryInset"), () -> cfg.lodBoundaryInset, v -> {
            cfg.lodBoundaryInset = v;
        }, new Range(8, 32, 1)).setFormatter(v -> Component.translatable((String)"voxy.config.unit.blocks", (Object[])new Object[]{v})).setEnablerInherit("voxy:lod_boundary_fade")).setImpact(OptionImpact.LOW)}), new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.experimentalShaders"), new SodiumConfigBuilder.Option[]{((SodiumConfigBuilder.Option)((SodiumConfigBuilder.BoolOption)((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:lod_lite_shading", (Component)Component.translatable((String)"voxy.config.general.lodLiteShading"), () -> cfg.lodLiteShading, v -> {
            cfg.lodLiteShading = v;
        }).setTooltipSupplier(v -> VoxyConfigMenu.liteShaderTooltip())).setImpact(OptionImpact.HIGH)).setEnablerInherit(s -> IrisUtil.irisShaderPackEnabled(), ConfigState.UPDATE_ON_REBUILD)).setPostChangeFlags(renderReload, "voxy:iris_reload")})).setEnablerAND("voxy:enabled", "voxy:rendering");
    }

    private static SodiumConfigBuilder.Page fakesightPage(VoxyConfig cfg) {
        SodiumConfigBuilder.Group[] groupArray = new SodiumConfigBuilder.Group[1];
        SodiumConfigBuilder.Option[] optionArray = new SodiumConfigBuilder.Option[2];
        optionArray[0] = ((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:fakesight_enabled", (Component)Component.translatable((String)"voxy.config.fakesight.enabled"), () -> cfg.enableExtendedRequestDistance, v -> {
            cfg.enableExtendedRequestDistance = v;
        }).setPostChangeFlags("voxy:refresh_chunk_request")).setImpact(OptionImpact.HIGH);
        optionArray[1] = ((SodiumConfigBuilder.Option)((SodiumConfigBuilder.IntOption)new SodiumConfigBuilder.IntOption("voxy:fakesight_request_distance", (Component)Component.translatable((String)"voxy.config.fakesight.distance"), cfg::getRequestDistance, v -> {
            cfg.requestDistance = v;
        }, new Range(8, 48, 1)).setFormatter(v -> Component.literal((String)Integer.toString(v))).setPostChangeFlags("voxy:refresh_chunk_request")).setEnablerInherit("voxy:fakesight_enabled")).setImpact(OptionImpact.HIGH);
        groupArray[0] = (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.chunkRequests"), optionArray).setEnablerInherit(s -> Minecraft.getInstance().getConnection() == null || Minecraft.getInstance().hasSingleplayerServer(), ConfigState.UPDATE_ON_REBUILD);
        return (SodiumConfigBuilder.Page)new SodiumConfigBuilder.Page((Component)Component.translatable((String)"voxy.config.fakesight"), groupArray).setEnablerAND("voxy:enabled", "voxy:rendering");
    }

    private static SodiumConfigBuilder.Page compatibilityPage(VoxyConfig cfg, String renderReload, boolean sableInstalled, boolean createInstalled, boolean powerGridInstalled, boolean copycatsInstalled, boolean simulatedInstalled, boolean framedBlocksInstalled, boolean littleTilesInstalled, boolean domumInstalled, boolean seasonsInstalled) {
        return (SodiumConfigBuilder.Page)new SodiumConfigBuilder.Page((Component)Component.translatable((String)"voxy.config.compat"), (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.create"), new SodiumConfigBuilder.Option[]{((SodiumConfigBuilder.Option)new SodiumConfigBuilder.BoolOption("voxy:distant_trains", (Component)Component.translatable((String)"voxy.config.compat.distantTrains"), () -> cfg.distantTrains, v -> {
            cfg.distantTrains = v;
        }).setEnablerInherit(s -> ServerCapabilities.canConfigureTrains(), ConfigState.UPDATE_ON_REBUILD)).setImpact(OptionImpact.LOW), ((SodiumConfigBuilder.Option)new SodiumConfigBuilder.IntOption("voxy:distant_train_distance", (Component)Component.translatable((String)"voxy.config.compat.distantTrainDistance"), () -> cfg.distantTrainMaxChunks, v -> {
            cfg.distantTrainMaxChunks = v;
        }, new Range(0, 192, 8)).setFormatter(VoxyConfigMenu::formatCreateDistance).setEnablerInherit(s -> ServerCapabilities.canConfigureTrains(), ConfigState.UPDATE_ON_REBUILD)).setImpact(OptionImpact.LOW), new SodiumConfigBuilder.BoolOption("voxy:distant_tracks", (Component)Component.translatable((String)"voxy.config.compat.distantTracks"), () -> cfg.distantTracks, v -> {
            cfg.distantTracks = v;
        }).setImpact(OptionImpact.LOW), new SodiumConfigBuilder.IntOption("voxy:distant_track_distance", (Component)Component.translatable((String)"voxy.config.compat.distantTrackDistance"), () -> cfg.distantTrackMaxChunks, v -> {
            cfg.distantTrackMaxChunks = v;
        }, new Range(0, 192, 8)).setFormatter(VoxyConfigMenu::formatCreateDistance).setImpact(OptionImpact.LOW), new SodiumConfigBuilder.BoolOption("voxy:distant_contraptions", (Component)Component.translatable((String)"voxy.config.compat.distantContraptions"), () -> cfg.distantContraptions, v -> {
            cfg.distantContraptions = v;
        }).setImpact(OptionImpact.LOW), new SodiumConfigBuilder.IntOption("voxy:distant_contraption_distance", (Component)Component.translatable((String)"voxy.config.compat.distantContraptionDistance"), () -> cfg.distantContraptionMaxChunks, v -> {
            cfg.distantContraptionMaxChunks = v;
        }, new Range(0, 192, 8)).setFormatter(VoxyConfigMenu::formatCreateDistance).setImpact(OptionImpact.LOW), new SodiumConfigBuilder.BoolOption("voxy:distant_kinetics", (Component)Component.translatable((String)"voxy.config.compat.distantKinetics"), () -> cfg.distantKinetics, v -> {
            cfg.distantKinetics = v;
        }).setImpact(OptionImpact.LOW), new SodiumConfigBuilder.BoolOption("voxy:kinetic_enclosed_culling", (Component)Component.translatable((String)"voxy.config.compat.kineticEnclosedCulling"), () -> cfg.kineticEnclosedCulling, v -> {
            cfg.kineticEnclosedCulling = v;
        }).setImpact(OptionImpact.LOW)}).setEnablerInherit(s -> createInstalled), (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.aeronautics"), (SodiumConfigBuilder.Option)new SodiumConfigBuilder.BoolOption("voxy:sable_lod", (Component)Component.translatable((String)"voxy.config.compat.sableLod"), () -> cfg.sableLodRendering, v -> {
            cfg.sableLodRendering = v;
        }).setEnablerInherit(s -> sableInstalled), (SodiumConfigBuilder.Option)((SodiumConfigBuilder.IntOption)new SodiumConfigBuilder.IntOption("voxy:sable_lod_distance", (Component)Component.translatable((String)"voxy.config.compat.sableLodDistance"), () -> cfg.aeronauticsContraptionMaxChunks, v -> {
            cfg.aeronauticsContraptionMaxChunks = v;
        }, new Range(0, 192, 8)).setFormatter(VoxyConfigMenu::formatCreateDistance).setImpact(OptionImpact.MEDIUM)).setEnablerInherit(s -> sableInstalled), (SodiumConfigBuilder.Option)((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:distant_simulated_lasers", (Component)Component.translatable((String)"voxy.config.compat.distantSimulatedLasers"), () -> cfg.distantSimulatedLasers, v -> {
            cfg.distantSimulatedLasers = v;
        }).setImpact(OptionImpact.LOW)).setEnablerInherit(s -> simulatedInstalled), (SodiumConfigBuilder.Option)((SodiumConfigBuilder.IntOption)new SodiumConfigBuilder.IntOption("voxy:distant_simulated_laser_distance", (Component)Component.translatable((String)"voxy.config.compat.distantSimulatedLaserDistance"), () -> cfg.distantSimulatedLaserMaxChunks, v -> {
            cfg.distantSimulatedLaserMaxChunks = v;
        }, new Range(0, 192, 8)).setFormatter(VoxyConfigMenu::formatCreateDistance).setImpact(OptionImpact.LOW)).setEnablerInherit(s -> simulatedInstalled)).setEnablerInherit(s -> sableInstalled || simulatedInstalled), (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.powergrid"), new SodiumConfigBuilder.Option[]{new SodiumConfigBuilder.BoolOption("voxy:distant_powergrid_wires", (Component)Component.translatable((String)"voxy.config.compat.distantPowerGridWires"), () -> cfg.distantPowerGridWires, v -> {
            cfg.distantPowerGridWires = v;
        }).setImpact(OptionImpact.LOW), new SodiumConfigBuilder.IntOption("voxy:distant_powergrid_wire_distance", (Component)Component.translatable((String)"voxy.config.compat.distantPowerGridWireDistance"), () -> cfg.distantPowerGridWireMaxChunks, v -> {
            cfg.distantPowerGridWireMaxChunks = v;
        }, new Range(0, 192, 8)).setFormatter(VoxyConfigMenu::formatCreateDistance).setImpact(OptionImpact.LOW)}).setEnablerInherit(s -> powerGridInstalled), (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.copycats"), new SodiumConfigBuilder.Option[]{((SodiumConfigBuilder.BoolOption)new SodiumConfigBuilder.BoolOption("voxy:distant_copycats", (Component)Component.translatable((String)"voxy.config.compat.distantCopycats"), () -> cfg.distantCopycats, v -> {
            cfg.distantCopycats = v;
        }).setPostChangeFlags(renderReload)).setImpact(OptionImpact.MEDIUM), new SodiumConfigBuilder.IntOption("voxy:distant_copycats_distance", (Component)Component.translatable((String)"voxy.config.compat.distantCopycatsDistance"), () -> cfg.distantCopycatsMaxChunks, v -> {
            cfg.distantCopycatsMaxChunks = v;
        }, new Range(0, 192, 8)).setFormatter(VoxyConfigMenu::formatCreateDistance).setImpact(OptionImpact.MEDIUM)}).setEnablerInherit(s -> copycatsInstalled), (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.framedblocks"), new SodiumConfigBuilder.Option[]{new SodiumConfigBuilder.BoolOption("voxy:distant_framedblocks", (Component)Component.translatable((String)"voxy.config.compat.distantFramedBlocks"), () -> cfg.distantFramedBlocks, v -> {
            cfg.distantFramedBlocks = v;
        }).setImpact(OptionImpact.LOW), new SodiumConfigBuilder.IntOption("voxy:distant_framedblocks_distance", (Component)Component.translatable((String)"voxy.config.compat.distantFramedBlocksDistance"), () -> cfg.distantFramedBlocksMaxChunks, v -> {
            cfg.distantFramedBlocksMaxChunks = v;
        }, new Range(0, 192, 8)).setFormatter(VoxyConfigMenu::formatCreateDistance).setImpact(OptionImpact.LOW)}).setEnablerInherit(s -> framedBlocksInstalled), (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.littletiles"), new SodiumConfigBuilder.Option[]{new SodiumConfigBuilder.BoolOption("voxy:distant_littletiles", (Component)Component.translatable((String)"voxy.config.compat.distantLittleTiles"), () -> cfg.distantLittleTiles, v -> {
            cfg.distantLittleTiles = v;
        }).setImpact(OptionImpact.MEDIUM), new SodiumConfigBuilder.IntOption("voxy:distant_littletiles_distance", (Component)Component.translatable((String)"voxy.config.compat.distantLittleTilesDistance"), () -> cfg.distantLittleTilesMaxChunks, v -> {
            cfg.distantLittleTilesMaxChunks = v;
        }, new Range(0, 192, 8)).setFormatter(VoxyConfigMenu::formatCreateDistance).setImpact(OptionImpact.MEDIUM)}).setEnablerInherit(s -> littleTilesInstalled), (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.domum"), new SodiumConfigBuilder.Option[]{new SodiumConfigBuilder.BoolOption("voxy:distant_domum", (Component)Component.translatable((String)"voxy.config.compat.distantDomum"), () -> cfg.distantDomum, v -> {
            cfg.distantDomum = v;
        }).setImpact(OptionImpact.MEDIUM), new SodiumConfigBuilder.IntOption("voxy:distant_domum_distance", (Component)Component.translatable((String)"voxy.config.compat.distantDomumDistance"), () -> cfg.distantDomumMaxChunks, v -> {
            cfg.distantDomumMaxChunks = v;
        }, new Range(0, 192, 8)).setFormatter(VoxyConfigMenu::formatCreateDistance).setImpact(OptionImpact.MEDIUM)}).setEnablerInherit(s -> domumInstalled), (SodiumConfigBuilder.Group)new SodiumConfigBuilder.Group((Component)Component.translatable((String)"voxy.config.group.seasons"), new SodiumConfigBuilder.BoolOption("voxy:es_snow_lod", (Component)Component.translatable((String)"voxy.config.compat.esSnowLod"), () -> cfg.eclipticSeasonsSnowLod, v -> {
            cfg.eclipticSeasonsSnowLod = v;
        }), new SodiumConfigBuilder.BoolOption("voxy:es_lod_auto_reload", (Component)Component.translatable((String)"voxy.config.compat.esLodAutoReload"), () -> cfg.eclipticSeasonsLodAutoReload, v -> {
            cfg.eclipticSeasonsLodAutoReload = v;
        }), new SodiumConfigBuilder.BoolOption("voxy:es_reload_on_season_change", (Component)Component.translatable((String)"voxy.config.compat.esReloadOnSeasonChange"), () -> cfg.eclipticSeasonsReloadOnSeasonChange, v -> {
            cfg.eclipticSeasonsReloadOnSeasonChange = v;
        })).setEnablerInherit(s -> seasonsInstalled)).setEnabler("voxy:enabled");
    }

    private static Component formatCreateDistance(int chunks) {
        return chunks == 0 ? Component.translatable((String)"voxy.config.compat.distanceFollowLod") : Component.translatable((String)"voxy.config.compat.distanceChunks", (Object[])new Object[]{chunks});
    }

    private static Component liteShaderTooltip() {
        LiteShaderStatus.Snapshot state = LiteShaderStatus.get();
        MutableComponent status = switch (state.code()) {
            default -> throw new MatchException(null, null);
            case LiteShaderStatus.Code.ACTIVE -> Component.translatable((String)"voxy.config.general.lodLiteShading.status.active", (Object[])new Object[]{state.packName(), state.testedVersions()});
            case LiteShaderStatus.Code.REQUESTED -> Component.translatable((String)"voxy.config.general.lodLiteShading.status.requested");
            case LiteShaderStatus.Code.NO_VOXY_PATCH -> Component.translatable((String)"voxy.config.general.lodLiteShading.status.noPatch");
            case LiteShaderStatus.Code.MISSING_PROGRAMS -> Component.translatable((String)"voxy.config.general.lodLiteShading.status.missingPrograms");
            case LiteShaderStatus.Code.MISSING_CONTRACT -> Component.translatable((String)"voxy.config.general.lodLiteShading.status.missingContract");
            case LiteShaderStatus.Code.CONTRACT_MISMATCH -> Component.translatable((String)"voxy.config.general.lodLiteShading.status.contractMismatch");
            case LiteShaderStatus.Code.API_MISMATCH -> Component.translatable((String)"voxy.config.general.lodLiteShading.status.apiMismatch", (Object[])new Object[]{state.detail()});
            case LiteShaderStatus.Code.TRANSITION_REQUIRED -> Component.translatable((String)"voxy.config.general.lodLiteShading.status.transitionRequired");
            case LiteShaderStatus.Code.ERROR -> Component.translatable((String)"voxy.config.general.lodLiteShading.status.error", (Object[])new Object[]{state.detail()});
            case LiteShaderStatus.Code.DISABLED -> Component.translatable((String)"voxy.config.general.lodLiteShading.status.disabled");
        };
        return Component.translatable((String)"voxy.config.general.lodLiteShading.tooltip").append("\n").append((Component)status);
    }
}

