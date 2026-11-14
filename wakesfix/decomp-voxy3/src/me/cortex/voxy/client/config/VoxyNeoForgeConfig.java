/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.ModContainer
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.common.EventBusSubscriber$Bus
 *  net.neoforged.fml.config.IConfigSpec
 *  net.neoforged.fml.config.ModConfig$Type
 *  net.neoforged.fml.event.config.ModConfigEvent$Loading
 *  net.neoforged.fml.event.config.ModConfigEvent$Reloading
 *  net.neoforged.neoforge.common.ModConfigSpec
 *  net.neoforged.neoforge.common.ModConfigSpec$BooleanValue
 *  net.neoforged.neoforge.common.ModConfigSpec$Builder
 *  net.neoforged.neoforge.common.ModConfigSpec$DoubleValue
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 */
package me.cortex.voxy.client.config;

import me.cortex.voxy.client.RenderStatistics;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.common.util.cpu.CpuLayout;
import me.cortex.voxy.compat.far.FarEntityClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid="voxy", bus=EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public class VoxyNeoForgeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue ENABLED = BUILDER.comment("Enable Voxy LOD rendering system").define("enabled", true);
    private static final ModConfigSpec.BooleanValue ENABLE_RENDERING = BUILDER.comment("Enable LOD terrain rendering (can be disabled while keeping data ingestion)").define("enableRendering", true);
    private static final ModConfigSpec.BooleanValue INGEST_ENABLED = BUILDER.comment("Enable automatic chunk data ingestion for LOD generation").define("ingestEnabled", true);
    private static final ModConfigSpec.DoubleValue SECTION_RENDER_DISTANCE = BUILDER.comment(new String[]{"LOD section render distance (multiplied by 32 for actual chunk distance)", "Example: 16 = 512 chunks render distance"}).defineInRange("sectionRenderDistance", 16.0, 0.625, 64.0);
    private static final ModConfigSpec.IntValue SERVICE_THREADS = BUILDER.comment(new String[]{"Number of background threads for LOD processing", "Default is based on CPU core count."}).defineInRange("serviceThreads", Math.max((int)((double)CpuLayout.getCoreCount() / 1.5), 1), 1, CpuLayout.getCoreCount());
    private static final ModConfigSpec.DoubleValue SUB_DIVISION_SIZE = BUILDER.comment("Render precision presets: 1024, 768, 512, 256 (default), 123, 64, 28. Lower values give more detail.").defineInRange("subDivisionSize", 256.0, 28.0, 1024.0);
    private static final ModConfigSpec.BooleanValue USE_ENVIRONMENTAL_FOG = BUILDER.comment("Apply environmental fog to LOD terrain").define("useEnvironmentalFog", true);
    private static final ModConfigSpec.BooleanValue DONT_USE_SODIUM_BUILDER_THREADS = BUILDER.comment("Don't share threads with Sodium's chunk builder").define("dontUseSodiumBuilderThreads", false);
    private static final ModConfigSpec.IntValue LOD_BOUNDARY_BUFFER = BUILDER.comment("LOD boundary safety overlap in blocks").defineInRange("lodBoundaryBuffer", 1, 0, 4);
    private static final ModConfigSpec.BooleanValue ENABLE_LOD_BOUNDARY_FADE = BUILDER.comment(new String[]{"Use a camera-centred circular handoff between vanilla terrain and Voxy LOD.", "Disable this when the active shader pack already supplies an LOD fade (for example Photon)."}).define("enableLodBoundaryFade", true);
    private static final ModConfigSpec.IntValue LOD_BOUNDARY_FADE_LENGTH = BUILDER.comment("Width of the stable dithered terrain handoff, in blocks").defineInRange("lodBoundaryFadeLength", 16, 8, 64);
    private static final ModConfigSpec.IntValue LOD_BOUNDARY_INSET = BUILDER.comment("Safety distance between the handoff outer edge and vanilla render distance, in blocks").defineInRange("lodBoundaryInset", 8, 8, 32);
    private static final ModConfigSpec.BooleanValue ENABLE_EXTENDED_REQUEST_DISTANCE = BUILDER.comment("Enable FakeSight-style extended chunk requests for Voxy ingestion").define("enableExtendedRequestDistance", false);
    private static final ModConfigSpec.IntValue REQUEST_DISTANCE = BUILDER.comment("Requested chunk radius. Higher values increase world-generation load.").defineInRange("requestDistance", 48, 8, 48);
    private static final ModConfigSpec.IntValue EARTH_CURVE_RATIO = BUILDER.comment(new String[]{"World curvature effect - simulates standing on a spherical planet", "0 = disabled (flat world)", "1 = real Earth curvature (6371km radius)", "Higher values = more extreme curvature (smaller planet effect)", "Valid range: 0 (off), or 50-5000. Values 1-49 are auto-corrected to 50.", "Inspired by Distant Horizons' earth curvature feature"}).defineInRange("earthCurveRatio", 0, 0, 5000);
    private static final ModConfigSpec.BooleanValue ENABLE_FAR_PLAYER_RENDERING = BUILDER.comment(new String[]{"Render far players with lightweight server snapshots.", "Multiplayer requires Voxy on the server; standalone SeeU takes precedence when installed."}).define("enableFarPlayerRendering", true);
    private static final ModConfigSpec.BooleanValue ENABLE_FAR_VEHICLE_RENDERING = BUILDER.comment("Render ridden vehicles and mounts from far-entity snapshots independently of players.").define("enableFarVehicleRendering", true);
    private static final ModConfigSpec.BooleanValue RENDER_FAR_PLAYER_NAMES = BUILDER.comment("Render name tags above far-player proxies.").define("renderFarPlayerNames", true);
    private static final ModConfigSpec.IntValue FAR_PLAYER_ANIMATION_DISTANCE = BUILDER.comment(new String[]{"Maximum distance in blocks for far-player walk animation.", "Set to 0 to follow the LOD render distance."}).defineInRange("farPlayerAnimationDistance", 0, 0, 32768);
    private static final ModConfigSpec.BooleanValue SHARE_FAR_PLAYER_POSITION = BUILDER.comment("Allow other Voxy clients on the same server to receive your far-player snapshot.").define("shareFarPlayerPosition", true);
    private static final ModConfigSpec.BooleanValue RENDER_STATISTICS = BUILDER.comment(new String[]{"Show render statistics in F3 debug screen", "Displays LOD traversal counts, visible sections, and quad counts"}).define("renderStatistics", false);
    private static final ModConfigSpec.BooleanValue DISTANT_TRAINS = BUILDER.comment("Render Create trains beyond the vanilla view distance, in the LOD").define("distantTrains", true);
    private static final ModConfigSpec.IntValue DISTANT_TRAIN_MAX_CHUNKS = BUILDER.comment(new String[]{"Max distance to render distant trains, in chunks. 0 = follow the LOD radius.", "Lower renders trains nearer; on the integrated server it also shrinks the", "server's train pose-stream window, cutting bandwidth."}).defineInRange("distantTrainMaxChunks", 0, 0, 192);
    private static final ModConfigSpec.BooleanValue DISTANT_TRACKS = BUILDER.comment("Render the Create track network beyond the view distance, in the LOD").define("distantTracks", true);
    private static final ModConfigSpec.IntValue DISTANT_TRACK_MAX_CHUNKS = BUILDER.comment("Max distance to render the distant track network, in chunks. 0 = follow the LOD radius.").defineInRange("distantTrackMaxChunks", 0, 0, 192);
    private static final ModConfigSpec.BooleanValue DISTANT_CONTRAPTIONS = BUILDER.comment(new String[]{"Render snapshots of Create contraptions (bearings/pistons/gantries/mounted)", "beyond the view distance, in the LOD"}).define("distantContraptions", true);
    private static final ModConfigSpec.IntValue DISTANT_CONTRAPTION_MAX_CHUNKS = BUILDER.comment("Max distance to render distant contraptions, in chunks. 0 = follow the LOD radius.").defineInRange("distantContraptionMaxChunks", 0, 0, 192);
    private static final ModConfigSpec.BooleanValue DISTANT_BEACONS = BUILDER.comment("Draw beacon beams past vanilla's block-entity render range").define("distantBeacons", true);
    private static final ModConfigSpec.IntValue DISTANT_BEACON_MAX_CHUNKS = BUILDER.comment("Maximum beacon-beam distance in chunks. 0 follows Voxy's LOD radius.").defineInRange("distantBeaconMaxChunks", 0, 0, 512);
    private static final ModConfigSpec.BooleanValue DISTANT_KINETICS = BUILDER.comment(new String[]{"Cull placed kinetic machine moving parts (rotating shafts/gears) beyond the render", "distance so they stop floating over the LOD. Off = Create draws them natively."}).define("distantKinetics", true);
    private static final ModConfigSpec.BooleanValue DISTANT_POWERGRID_WIRES = BUILDER.comment("Render cached PowerGrid hanging wires beyond their entity tracking range").define("distantPowerGridWires", true);
    private static final ModConfigSpec.IntValue DISTANT_POWERGRID_WIRE_MAX_CHUNKS = BUILDER.comment("Maximum PowerGrid wire LOD distance in chunks. 0 follows Voxy's LOD radius.").defineInRange("distantPowerGridWireMaxChunks", 0, 0, 192);
    private static final ModConfigSpec.BooleanValue DISTANT_COPYCATS = BUILDER.comment("Render Create and Copycats+ camouflage blocks with dedicated LOD meshes").define("distantCopycats", true);
    private static final ModConfigSpec.IntValue DISTANT_COPYCATS_MAX_CHUNKS = BUILDER.comment("Maximum Copycats+ LOD distance in chunks. 0 follows Voxy's LOD radius.").defineInRange("distantCopycatsMaxChunks", 0, 0, 192);
    private static final ModConfigSpec.BooleanValue DISTANT_SIMULATED_LASERS = BUILDER.comment("Render Simulated laser-pointer beams beyond block-entity render distance").define("distantSimulatedLasers", true);
    private static final ModConfigSpec.IntValue DISTANT_SIMULATED_LASER_MAX_CHUNKS = BUILDER.comment("Maximum Simulated laser LOD distance in chunks. 0 follows Voxy's LOD radius.").defineInRange("distantSimulatedLaserMaxChunks", 0, 0, 192);
    private static final ModConfigSpec.BooleanValue DISTANT_FRAMED_BLOCKS = BUILDER.comment("Render FramedBlocks camouflaged models in the LOD").define("distantFramedBlocks", true);
    private static final ModConfigSpec.IntValue DISTANT_FRAMED_BLOCKS_MAX_CHUNKS = BUILDER.comment("Maximum FramedBlocks LOD distance in chunks. 0 follows Voxy's LOD radius.").defineInRange("distantFramedBlocksMaxChunks", 0, 0, 192);
    private static final ModConfigSpec.BooleanValue DISTANT_LITTLETILES = BUILDER.comment("Render cached LittleTiles microblock meshes beyond vanilla view distance").define("distantLittleTiles", true);
    private static final ModConfigSpec.IntValue DISTANT_LITTLETILES_MAX_CHUNKS = BUILDER.comment("Maximum LittleTiles LOD distance in chunks. 0 follows Voxy's LOD radius.").defineInRange("distantLittleTilesMaxChunks", 0, 0, 192);
    private static final ModConfigSpec.BooleanValue DISTANT_DOMUM = BUILDER.comment("Render detailed Domum Ornamentum models in the LOD").define("distantDomum", true);
    private static final ModConfigSpec.IntValue DISTANT_DOMUM_MAX_CHUNKS = BUILDER.comment("Maximum Domum Ornamentum LOD distance in chunks. 0 follows Voxy's LOD radius.").defineInRange("distantDomumMaxChunks", 0, 0, 192);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private VoxyNeoForgeConfig() {
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, (IConfigSpec)SPEC, "voxy-client.toml");
    }

    private static void syncToVoxyConfig() {
        VoxyConfig.CONFIG.enabled = (Boolean)ENABLED.get();
        VoxyConfig.CONFIG.enableRendering = (Boolean)ENABLE_RENDERING.get();
        VoxyConfig.CONFIG.ingestEnabled = (Boolean)INGEST_ENABLED.get();
        VoxyConfig.CONFIG.sectionRenderDistance = ((Double)SECTION_RENDER_DISTANCE.get()).floatValue();
        VoxyConfig.CONFIG.serviceThreads = (Integer)SERVICE_THREADS.get();
        VoxyConfig.CONFIG.subDivisionSize = ((Double)SUB_DIVISION_SIZE.get()).floatValue();
        VoxyConfig.CONFIG.useEnvironmentalFog = (Boolean)USE_ENVIRONMENTAL_FOG.get();
        VoxyConfig.CONFIG.dontUseSodiumBuilderThreads = (Boolean)DONT_USE_SODIUM_BUILDER_THREADS.get();
        VoxyConfig.CONFIG.lodBoundaryBuffer = (Integer)LOD_BOUNDARY_BUFFER.get();
        VoxyConfig.CONFIG.enableLodBoundaryFade = (Boolean)ENABLE_LOD_BOUNDARY_FADE.get();
        VoxyConfig.CONFIG.lodBoundaryFadeLength = (Integer)LOD_BOUNDARY_FADE_LENGTH.get();
        VoxyConfig.CONFIG.lodBoundaryInset = (Integer)LOD_BOUNDARY_INSET.get();
        VoxyConfig.CONFIG.enableExtendedRequestDistance = (Boolean)ENABLE_EXTENDED_REQUEST_DISTANCE.get();
        VoxyConfig.CONFIG.requestDistance = (Integer)REQUEST_DISTANCE.get();
        VoxyConfig.CONFIG.earthCurveRatio = (Integer)EARTH_CURVE_RATIO.get();
        VoxyConfig.CONFIG.distantTrains = (Boolean)DISTANT_TRAINS.get();
        VoxyConfig.CONFIG.distantTrainMaxChunks = (Integer)DISTANT_TRAIN_MAX_CHUNKS.get();
        VoxyConfig.CONFIG.distantTracks = (Boolean)DISTANT_TRACKS.get();
        VoxyConfig.CONFIG.distantTrackMaxChunks = (Integer)DISTANT_TRACK_MAX_CHUNKS.get();
        VoxyConfig.CONFIG.distantContraptions = (Boolean)DISTANT_CONTRAPTIONS.get();
        VoxyConfig.CONFIG.distantContraptionMaxChunks = (Integer)DISTANT_CONTRAPTION_MAX_CHUNKS.get();
        VoxyConfig.CONFIG.distantBeacons = (Boolean)DISTANT_BEACONS.get();
        VoxyConfig.CONFIG.distantBeaconMaxChunks = (Integer)DISTANT_BEACON_MAX_CHUNKS.get();
        VoxyConfig.CONFIG.distantKinetics = (Boolean)DISTANT_KINETICS.get();
        VoxyConfig.CONFIG.distantPowerGridWires = (Boolean)DISTANT_POWERGRID_WIRES.get();
        VoxyConfig.CONFIG.distantPowerGridWireMaxChunks = (Integer)DISTANT_POWERGRID_WIRE_MAX_CHUNKS.get();
        VoxyConfig.CONFIG.distantCopycats = (Boolean)DISTANT_COPYCATS.get();
        VoxyConfig.CONFIG.distantCopycatsMaxChunks = (Integer)DISTANT_COPYCATS_MAX_CHUNKS.get();
        VoxyConfig.CONFIG.distantSimulatedLasers = (Boolean)DISTANT_SIMULATED_LASERS.get();
        VoxyConfig.CONFIG.distantSimulatedLaserMaxChunks = (Integer)DISTANT_SIMULATED_LASER_MAX_CHUNKS.get();
        VoxyConfig.CONFIG.distantFramedBlocks = (Boolean)DISTANT_FRAMED_BLOCKS.get();
        VoxyConfig.CONFIG.distantFramedBlocksMaxChunks = (Integer)DISTANT_FRAMED_BLOCKS_MAX_CHUNKS.get();
        VoxyConfig.CONFIG.distantLittleTiles = (Boolean)DISTANT_LITTLETILES.get();
        VoxyConfig.CONFIG.distantLittleTilesMaxChunks = (Integer)DISTANT_LITTLETILES_MAX_CHUNKS.get();
        VoxyConfig.CONFIG.distantDomum = (Boolean)DISTANT_DOMUM.get();
        VoxyConfig.CONFIG.distantDomumMaxChunks = (Integer)DISTANT_DOMUM_MAX_CHUNKS.get();
        VoxyConfig.CONFIG.enableFarPlayerRendering = (Boolean)ENABLE_FAR_PLAYER_RENDERING.get();
        VoxyConfig.CONFIG.enableFarVehicleRendering = (Boolean)ENABLE_FAR_VEHICLE_RENDERING.get();
        VoxyConfig.CONFIG.renderFarPlayerNames = (Boolean)RENDER_FAR_PLAYER_NAMES.get();
        VoxyConfig.CONFIG.farPlayerAnimationDistance = (Integer)FAR_PLAYER_ANIMATION_DISTANCE.get();
        VoxyConfig.CONFIG.shareFarPlayerPosition = (Boolean)SHARE_FAR_PLAYER_POSITION.get();
        VoxyConfig.CONFIG.sanitize();
        RenderStatistics.enabled = (Boolean)RENDER_STATISTICS.get();
        VoxyConfig.CONFIG.save();
    }

    private static void syncFromVoxyConfig() {
        VoxyConfig.CONFIG.sanitize();
        ENABLED.set((Object)VoxyConfig.CONFIG.enabled);
        ENABLE_RENDERING.set((Object)VoxyConfig.CONFIG.enableRendering);
        INGEST_ENABLED.set((Object)VoxyConfig.CONFIG.ingestEnabled);
        SECTION_RENDER_DISTANCE.set((Object)VoxyConfig.CONFIG.sectionRenderDistance);
        SERVICE_THREADS.set((Object)VoxyConfig.CONFIG.serviceThreads);
        SUB_DIVISION_SIZE.set((Object)VoxyConfig.CONFIG.subDivisionSize);
        USE_ENVIRONMENTAL_FOG.set((Object)VoxyConfig.CONFIG.useEnvironmentalFog);
        DONT_USE_SODIUM_BUILDER_THREADS.set((Object)VoxyConfig.CONFIG.dontUseSodiumBuilderThreads);
        LOD_BOUNDARY_BUFFER.set((Object)VoxyConfig.CONFIG.lodBoundaryBuffer);
        ENABLE_LOD_BOUNDARY_FADE.set((Object)VoxyConfig.CONFIG.enableLodBoundaryFade);
        LOD_BOUNDARY_FADE_LENGTH.set((Object)VoxyConfig.CONFIG.lodBoundaryFadeLength);
        LOD_BOUNDARY_INSET.set((Object)VoxyConfig.CONFIG.lodBoundaryInset);
        ENABLE_EXTENDED_REQUEST_DISTANCE.set((Object)VoxyConfig.CONFIG.enableExtendedRequestDistance);
        REQUEST_DISTANCE.set((Object)VoxyConfig.CONFIG.requestDistance);
        EARTH_CURVE_RATIO.set((Object)VoxyConfig.CONFIG.earthCurveRatio);
        DISTANT_TRAINS.set((Object)VoxyConfig.CONFIG.distantTrains);
        DISTANT_TRAIN_MAX_CHUNKS.set((Object)VoxyConfig.CONFIG.distantTrainMaxChunks);
        DISTANT_TRACKS.set((Object)VoxyConfig.CONFIG.distantTracks);
        DISTANT_TRACK_MAX_CHUNKS.set((Object)VoxyConfig.CONFIG.distantTrackMaxChunks);
        DISTANT_CONTRAPTIONS.set((Object)VoxyConfig.CONFIG.distantContraptions);
        DISTANT_CONTRAPTION_MAX_CHUNKS.set((Object)VoxyConfig.CONFIG.distantContraptionMaxChunks);
        DISTANT_BEACONS.set((Object)VoxyConfig.CONFIG.distantBeacons);
        DISTANT_BEACON_MAX_CHUNKS.set((Object)VoxyConfig.CONFIG.distantBeaconMaxChunks);
        DISTANT_KINETICS.set((Object)VoxyConfig.CONFIG.distantKinetics);
        DISTANT_POWERGRID_WIRES.set((Object)VoxyConfig.CONFIG.distantPowerGridWires);
        DISTANT_POWERGRID_WIRE_MAX_CHUNKS.set((Object)VoxyConfig.CONFIG.distantPowerGridWireMaxChunks);
        DISTANT_COPYCATS.set((Object)VoxyConfig.CONFIG.distantCopycats);
        DISTANT_COPYCATS_MAX_CHUNKS.set((Object)VoxyConfig.CONFIG.distantCopycatsMaxChunks);
        DISTANT_SIMULATED_LASERS.set((Object)VoxyConfig.CONFIG.distantSimulatedLasers);
        DISTANT_SIMULATED_LASER_MAX_CHUNKS.set((Object)VoxyConfig.CONFIG.distantSimulatedLaserMaxChunks);
        DISTANT_FRAMED_BLOCKS.set((Object)VoxyConfig.CONFIG.distantFramedBlocks);
        DISTANT_FRAMED_BLOCKS_MAX_CHUNKS.set((Object)VoxyConfig.CONFIG.distantFramedBlocksMaxChunks);
        DISTANT_LITTLETILES.set((Object)VoxyConfig.CONFIG.distantLittleTiles);
        DISTANT_LITTLETILES_MAX_CHUNKS.set((Object)VoxyConfig.CONFIG.distantLittleTilesMaxChunks);
        DISTANT_DOMUM.set((Object)VoxyConfig.CONFIG.distantDomum);
        DISTANT_DOMUM_MAX_CHUNKS.set((Object)VoxyConfig.CONFIG.distantDomumMaxChunks);
        ENABLE_FAR_PLAYER_RENDERING.set((Object)VoxyConfig.CONFIG.enableFarPlayerRendering);
        ENABLE_FAR_VEHICLE_RENDERING.set((Object)VoxyConfig.CONFIG.enableFarVehicleRendering);
        RENDER_FAR_PLAYER_NAMES.set((Object)VoxyConfig.CONFIG.renderFarPlayerNames);
        FAR_PLAYER_ANIMATION_DISTANCE.set((Object)VoxyConfig.CONFIG.farPlayerAnimationDistance);
        SHARE_FAR_PLAYER_POSITION.set((Object)VoxyConfig.CONFIG.shareFarPlayerPosition);
        RenderStatistics.enabled = (Boolean)RENDER_STATISTICS.get();
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            VoxyNeoForgeConfig.syncFromVoxyConfig();
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            VoxyNeoForgeConfig.syncToVoxyConfig();
            FarEntityClient.sendHello();
        }
    }

    public static boolean isEnabled() {
        return (Boolean)ENABLED.get();
    }

    public static boolean isRenderingEnabled() {
        return (Boolean)ENABLE_RENDERING.get();
    }

    public static boolean isIngestEnabled() {
        return (Boolean)INGEST_ENABLED.get();
    }

    public static float getSectionRenderDistance() {
        return ((Double)SECTION_RENDER_DISTANCE.get()).floatValue();
    }

    public static int getServiceThreads() {
        return (Integer)SERVICE_THREADS.get();
    }

    public static float getSubDivisionSize() {
        return ((Double)SUB_DIVISION_SIZE.get()).floatValue();
    }

    public static boolean useEnvironmentalFog() {
        return (Boolean)USE_ENVIRONMENTAL_FOG.get();
    }

    public static boolean dontUseSodiumBuilderThreads() {
        return (Boolean)DONT_USE_SODIUM_BUILDER_THREADS.get();
    }

    public static boolean isRenderStatisticsEnabled() {
        return (Boolean)RENDER_STATISTICS.get();
    }

    public static int getEarthCurveRatio() {
        return (Integer)EARTH_CURVE_RATIO.get();
    }
}

