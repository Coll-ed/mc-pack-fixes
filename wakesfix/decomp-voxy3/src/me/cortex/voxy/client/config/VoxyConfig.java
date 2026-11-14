/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.FieldNamingPolicy
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.neoforged.fml.loading.FMLPaths
 */
package me.cortex.voxy.client.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Locale;
import me.cortex.voxy.client.core.SSAO;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.util.cpu.CpuLayout;
import me.cortex.voxy.common.world.WorldSection;
import me.cortex.voxy.commonImpl.VoxyCommon;
import me.cortex.voxy.commonImpl.compat.create.DistantTrainConfig;
import me.cortex.voxy.commonImpl.compat.sable.SableContraptionRenderDistance;
import net.neoforged.fml.loading.FMLPaths;

public class VoxyConfig {
    public static final int MIN_REQUEST_DISTANCE = 8;
    public static final int MAX_REQUEST_DISTANCE = 48;
    public static final int MAX_CLOUD_DISTANCE = 128;
    public static final float MIN_SUBDIVISION_SIZE = 28.0f;
    public static final float MAX_SUBDIVISION_SIZE = 1024.0f;
    public static final int DEFAULT_RENDER_QUALITY_LEVEL = 3;
    private static final float[] RENDER_QUALITY_SIZES = new float[]{1024.0f, 768.0f, 512.0f, 256.0f, 123.0f, 64.0f, 28.0f};
    private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().excludeFieldsWithModifiers(new int[]{2, 8, 128}).create();
    public static final VoxyConfig CONFIG = VoxyConfig.loadOrCreate();
    public boolean enabled = true;
    public boolean enableRendering = true;
    public boolean ingestEnabled = true;
    public float sectionRenderDistance = 16.0f;
    public boolean sableLodRendering = true;
    public int aeronauticsContraptionMaxChunks = 0;
    public boolean distantTrains = true;
    public boolean distantTracks = true;
    public boolean distantContraptions = true;
    public boolean distantBeacons = true;
    public int distantBeaconMaxChunks = 0;
    public boolean distantKinetics = true;
    public boolean kineticEnclosedCulling = true;
    public int distantTrainMaxChunks = 0;
    public int distantTrackMaxChunks = 0;
    public int distantContraptionMaxChunks = 0;
    public int distantKineticMaxChunks = 0;
    public boolean distantPowerGridWires = true;
    public int distantPowerGridWireMaxChunks = 0;
    public boolean distantCopycats = true;
    public int distantCopycatsMaxChunks = 0;
    public boolean distantSimulatedLasers = true;
    public int distantSimulatedLaserMaxChunks = 0;
    public boolean distantFramedBlocks = true;
    public int distantFramedBlocksMaxChunks = 0;
    public boolean distantLittleTiles = true;
    public int distantLittleTilesMaxChunks = 0;
    public boolean distantDomum = true;
    public int distantDomumMaxChunks = 0;
    public int distantContraptionGpuBudgetMiB = 48;
    public int distantKineticGpuBudgetMiB = 32;
    public boolean lodLiteShading = false;
    public int biomeBlendRadius = 2;
    public String biomeBlendScope = "water";
    public int serviceThreads = (int)Math.max((double)CpuLayout.getCoreCount() / 1.5, 1.0);
    public float subDivisionSize = RENDER_QUALITY_SIZES[3];
    public int skyFogDistance = 96;
    public float fogIntensity = 1.0f;
    public float fogDensity = 0.0f;
    public int fogDistancePercent = 100;
    public boolean adaptCloudDistance = true;
    public int cloudDistance = 0;
    public boolean dontUseSodiumBuilderThreads = false;
    public int renderPressure = 2;
    public int lodBoundaryBuffer = 1;
    public boolean enableLodBoundaryFade = true;
    public int lodBoundaryFadeLength = 16;
    public int lodBoundaryInset = 8;
    public int earthCurveRatio = 0;
    public boolean enableExtendedRequestDistance = false;
    public int requestDistance = 48;
    public String ssaoMode;
    public boolean useEnvironmentalFog = true;
    public String leafLodMode = "balanced";
    public boolean enableFarPlayerRendering = true;
    public boolean enableFarVehicleRendering = true;
    public boolean renderFarPlayerNames = true;
    public int farPlayerAnimationDistance = 0;
    public boolean shareFarPlayerPosition = true;
    public boolean joinMessageShown = false;
    public boolean upgradeCleanupNoticeShown = false;
    public boolean experimentalHiZCompute = false;
    public boolean experimentalCmdListHold = false;
    public int cmdListHoldMaxFrames = 4;
    public boolean experimentalChunkMaskReuse = false;
    public int sectionArrayPoolMiB = 100;
    public boolean experimentalOpaqueNearFirst = false;
    public boolean experimentalChunkMaskHalfRes = false;
    public boolean eclipticSeasonsSnowLod = true;
    public boolean eclipticSeasonsLodAutoReload = false;
    public boolean eclipticSeasonsReloadOnSeasonChange = true;
    public boolean showJoinMessage = true;

    public int getRequestDistance() {
        return Math.clamp((long)this.requestDistance, 8, 48);
    }

    public int getFarEntityRenderDistanceBlocks() {
        return Math.clamp((long)Math.round(this.sectionRenderDistance * 32.0f * 16.0f), 64, 32768);
    }

    public int getRenderPressureLevel() {
        if (this.renderPressure < 0 || this.renderPressure > 4) {
            this.renderPressure = 2;
        }
        return this.renderPressure;
    }

    public int getRenderQualityLevel() {
        if (!Float.isFinite(this.subDivisionSize) || this.subDivisionSize <= 0.0f) {
            return 3;
        }
        int closest = 0;
        double distance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < RENDER_QUALITY_SIZES.length; ++i) {
            double candidate = Math.abs(Math.log(this.subDivisionSize / RENDER_QUALITY_SIZES[i]));
            if (!(candidate < distance)) continue;
            closest = i;
            distance = candidate;
        }
        return closest;
    }

    public void setRenderQualityLevel(int level) {
        this.subDivisionSize = RENDER_QUALITY_SIZES[Math.clamp((long)level, 0, RENDER_QUALITY_SIZES.length - 1)];
    }

    public LeafLodMode getLeafLodMode() {
        if (this.leafLodMode == null) {
            return LeafLodMode.BALANCED;
        }
        try {
            return LeafLodMode.valueOf(this.leafLodMode.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ignored) {
            return LeafLodMode.BALANCED;
        }
    }

    public void setLeafLodMode(LeafLodMode mode) {
        this.leafLodMode = mode.name().toLowerCase(Locale.ROOT);
    }

    public SSAO.SSAOMode getSSAOMode() {
        if (this.ssaoMode == null) {
            return SSAO.SSAOMode.AUTO;
        }
        try {
            return SSAO.SSAOMode.valueOf(this.ssaoMode.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ignored) {
            return SSAO.SSAOMode.AUTO;
        }
    }

    public void setSSAOMode(SSAO.SSAOMode mode) {
        this.ssaoMode = mode.name().toLowerCase(Locale.ROOT);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static VoxyConfig loadOrCreate() {
        if (!VoxyCommon.isAvailable()) {
            VoxyConfig config = new VoxyConfig();
            config.enabled = false;
            config.enableRendering = false;
            return config;
        }
        Path path = VoxyConfig.getConfigPath();
        if (Files.exists(path, new LinkOption[0])) {
            try (FileReader reader = new FileReader(path.toFile());){
                VoxyConfig config = (VoxyConfig)GSON.fromJson((Reader)reader, VoxyConfig.class);
                if (config != null) {
                    config.sanitize();
                    VoxyConfig voxyConfig = config;
                    return voxyConfig;
                }
                Logger.error("Failed to load Voxy config; resetting it");
            }
            catch (IOException | RuntimeException e) {
                Logger.error("Could not load Voxy config; resetting it", e);
                VoxyConfig.backupInvalidConfig(path);
            }
        }
        Logger.info("Config does not exist; creating a new one");
        VoxyConfig config = new VoxyConfig();
        config.save();
        return config;
    }

    public void sanitize() {
        this.cmdListHoldMaxFrames = Math.clamp((long)this.cmdListHoldMaxFrames, 2, 60);
        this.sectionArrayPoolMiB = Math.clamp((long)this.sectionArrayPoolMiB, 25, 1024);
        WorldSection.setArrayPoolCapMiB(this.sectionArrayPoolMiB);
        this.sectionRenderDistance = Math.clamp(this.sectionRenderDistance, 0.625f, 64.0f);
        this.setRenderQualityLevel(this.getRenderQualityLevel());
        this.requestDistance = Math.clamp((long)this.requestDistance, 8, 48);
        if (this.fogDistancePercent > 200) {
            this.fogDistancePercent = Math.max(5, Math.round((float)this.fogDistancePercent / 16.0f));
        }
        this.fogDistancePercent = Math.clamp((long)this.fogDistancePercent, 5, 200);
        this.skyFogDistance = Math.clamp((long)this.skyFogDistance, 0, 1024);
        this.cloudDistance = Math.clamp((long)this.cloudDistance, 0, 128);
        this.fogIntensity = Math.clamp(this.fogIntensity, 0.0f, 1.0f);
        this.fogDensity = Math.clamp(this.fogDensity, 0.0f, 1.0f);
        this.lodBoundaryBuffer = Math.clamp((long)this.lodBoundaryBuffer, 0, 4);
        this.lodBoundaryFadeLength = Math.clamp((long)this.lodBoundaryFadeLength, 8, 64);
        this.lodBoundaryInset = Math.clamp((long)this.lodBoundaryInset, 8, 32);
        this.setLeafLodMode(this.getLeafLodMode());
        this.farPlayerAnimationDistance = Math.clamp((long)this.farPlayerAnimationDistance, 0, 32768);
        this.aeronauticsContraptionMaxChunks = Math.clamp((long)this.aeronauticsContraptionMaxChunks, 0, 192);
        this.biomeBlendRadius = Math.clamp((long)this.biomeBlendRadius, 0, 7);
        if (!"water".equals(this.biomeBlendScope) && !"water_grass".equals(this.biomeBlendScope)) {
            this.biomeBlendScope = "water";
        }
    }

    public void save() {
        if (!VoxyCommon.isAvailable()) {
            Logger.info("Not saving config since voxy is unavailable");
            this.syncSableContraptionRenderDistance();
            this.syncDistantTrainConfig();
            return;
        }
        this.sanitize();
        Path path = VoxyConfig.getConfigPath();
        Path temporary = path.resolveSibling(String.valueOf(path.getFileName()) + ".tmp");
        try {
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            Files.writeString(temporary, (CharSequence)GSON.toJson((Object)this), new OpenOption[0]);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e) {
            Logger.error("Failed to write Voxy config", e);
            try {
                Files.deleteIfExists(temporary);
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        this.syncSableContraptionRenderDistance();
        this.syncDistantTrainConfig();
    }

    private static void backupInvalidConfig(Path path) {
        try {
            Path backup = path.resolveSibling(String.valueOf(path.getFileName()) + ".invalid");
            Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            Logger.error("Failed to back up invalid Voxy config", e);
        }
    }

    public void syncSableContraptionRenderDistance() {
        SableContraptionRenderDistance.updateClientConfig(this.isRenderingEnabled() && this.sableLodRendering, this.sectionRenderDistance, this.aeronauticsContraptionMaxChunks);
    }

    public void syncDistantTrainConfig() {
        DistantTrainConfig.updateClientConfig(this.isRenderingEnabled() && this.distantTrains, this.createRenderDistance(this.distantTrainMaxChunks));
        DistantTrainConfig.updateClientContraptionConfig(this.isRenderingEnabled() && this.distantContraptions, this.createRenderDistance(this.distantContraptionMaxChunks));
    }

    public double createLodRadius() {
        return 512.0 * (double)this.sectionRenderDistance;
    }

    public double createRenderDistance(int maxChunks) {
        double lod = this.createLodRadius();
        return maxChunks > 0 ? Math.min((double)maxChunks * 16.0, lod) : lod;
    }

    private static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve("voxy-config.json");
    }

    public boolean isRenderingEnabled() {
        return VoxyCommon.isAvailable() && this.enabled && this.enableRendering;
    }

    public static enum LeafLodMode {
        FAST,
        BALANCED,
        QUALITY;

    }
}

