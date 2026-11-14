/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  blue.endless.jankson.Jankson
 *  blue.endless.jankson.JsonObject
 *  blue.endless.jankson.api.SyntaxError
 *  com.google.gson.Gson
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_1542
 *  net.minecraft.class_1657
 *  net.minecraft.class_1690
 *  net.minecraft.class_310
 *  net.minecraft.class_3532
 *  net.minecraft.class_746
 */
package com.goby56.wakes.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.BlendingFunction;
import com.goby56.wakes.render.RenderType;
import com.goby56.wakes.utils.WakeColor;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1542;
import net.minecraft.class_1657;
import net.minecraft.class_1690;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_746;

public class WakesConfig {
    public Map<String, WakeSpawningRule> wakeSpawningRules = new HashMap<String, WakeSpawningRule>(Map.of("boat_wake_rules", WakeSpawningRule.WAKES_AND_SPLASHES, "player_wake_rules", WakeSpawningRule.WAKES_AND_SPLASHES, "other_players_wake_rules", WakeSpawningRule.WAKES_AND_SPLASHES, "mobs_wake_rules", WakeSpawningRule.WAKES_AND_SPLASHES, "items_wake_rules", WakeSpawningRule.WAKES_AND_SPLASHES));
    public boolean wakesInRunningWater = false;
    public float waveSpeed = 0.95f;
    public int initialStrength = 20;
    public int paddleStrength = 100;
    public int splashStrength = 100;
    public double minimumProducerVelocity = 0.1;
    public float waveDecay = 0.5f;
    public boolean spawnParticles = true;
    public int floodFillDistance = 3;
    public boolean use9PointStencil = true;
    public int ticksBeforeFill = 2;
    public boolean drawDebugBoxes = false;
    public boolean renderWakes = true;
    public boolean spawnWakes = true;
    public RenderType renderType = RenderType.AUTO;
    public Resolution wakeResolution = Resolution.SIXTEEN;
    public float wakeOpacity = 1.0f;
    public boolean useWaterBlending = true;
    public BlendingFunction blendMode = BlendingFunction.DEFAULT;
    public List<ColorInterval> colorIntervals = List.of(new ColorInterval(WakeColor.TRANSPARENT, -50, -45), new ColorInterval(WakeColor.DARK_GRAY, -45, -35), new ColorInterval(WakeColor.GRAY, -35, -30), new ColorInterval(WakeColor.LIGHT_GRAY, -30, -15), new ColorInterval(WakeColor.TRANSPARENT, -15, 2), new ColorInterval(WakeColor.LIGHT_GRAY, 2, 10), new ColorInterval(WakeColor.WHITE, 10, 20), new ColorInterval(WakeColor.LIGHT_GRAY, 20, 40), new ColorInterval(WakeColor.GRAY, 40, 50));
    public boolean renderSplashPlane = true;
    public float splashPlaneWidth = 3.0f;
    public float splashPlaneHeight = 1.5f;
    public float splashPlaneDepth = 2.0f;
    public int splashPlaneResolution = 5;
    public float maxSplashPlaneVelocity = 0.5f;
    public float splashPlaneScale = 1.0f;
    public float splashPlaneOffset = 0.0f;

    public WakeSpawningRule getSpawningRule(class_1297 producer) {
        if (producer instanceof class_1690) {
            class_1690 boat = (class_1690)producer;
            if (this.wakeSpawningRules.get("boat_wake_rules") == WakeSpawningRule.WAKES_AND_SPLASHES && !boat.method_5626((class_1297)class_310.method_1551().field_1724)) {
                return this.wakeSpawningRules.get("other_players_wake_rules");
            }
            return this.wakeSpawningRules.get("boat_wake_rules");
        }
        if (producer instanceof class_1657) {
            class_1657 player = (class_1657)producer;
            if (player.method_7325()) {
                return WakeSpawningRule.DISABLED;
            }
            if (player instanceof class_746) {
                return this.wakeSpawningRules.get("player_wake_rules");
            }
            return this.wakeSpawningRules.get("other_players_wake_rules");
        }
        if (producer instanceof class_1309) {
            return this.wakeSpawningRules.get("mobs_wake_rules");
        }
        if (producer instanceof class_1542) {
            return this.wakeSpawningRules.get("items_wake_rules");
        }
        return WakeSpawningRule.DISABLED;
    }

    public static WakesConfig loadConfig() {
        Jankson jankson = Jankson.builder().build();
        try {
            File configFile = new File(WakesClient.CONFIG_PATH);
            if (!configFile.exists()) {
                WakesClient.LOGGER.info(String.format("No config file found for wakes-%s. Creating one...", WakesClient.METADATA.getVersion().getFriendlyString()));
                WakesConfig config = new WakesConfig();
                config.saveConfig();
                return config;
            }
            JsonObject configJson = jankson.load(configFile);
            String normalized = configJson.toJson(false, false);
            return (WakesConfig)new Gson().fromJson(normalized, WakesConfig.class);
        }
        catch (SyntaxError | IOException e) {
            e.printStackTrace();
            return new WakesConfig();
        }
    }

    public void saveConfig() {
        File configFile = new File(WakesClient.CONFIG_PATH);
        Jankson jankson = Jankson.builder().build();
        String result = jankson.toJson((Object)this).toJson(true, true);
        try {
            boolean usable;
            boolean bl = usable = configFile.exists() || configFile.createNewFile();
            if (!usable) {
                return;
            }
            FileOutputStream out = new FileOutputStream(configFile, false);
            out.write(result.getBytes());
            out.flush();
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static enum WakeSpawningRule {
        WAKES_AND_SPLASHES(true, true),
        ONLY_WAKES(true, false),
        ONLY_SPLASHES(false, true),
        DISABLED(false, false);

        public final boolean spawnsWake;
        public final boolean spawnsSplashes;

        private WakeSpawningRule(boolean spawnsWake, boolean spawnsSplashes) {
            this.spawnsWake = spawnsWake;
            this.spawnsSplashes = spawnsSplashes;
        }
    }

    public static enum Resolution {
        EIGHT(8),
        SIXTEEN(16),
        THIRTYTWO(32);

        public final int res;
        public final int power;

        private Resolution(int res) {
            this.res = res;
            this.power = class_3532.method_15351((int)res);
        }

        public String toString() {
            return String.valueOf(this.res);
        }
    }

    public static class ColorInterval {
        public WakeColor color;
        public int lower;
        public int upper;

        public ColorInterval(WakeColor color, int lower, int upper) {
            this.color = color;
            this.lower = lower;
            this.upper = upper;
        }

        public void setColor(WakeColor color) {
            this.color = color;
        }

        public void setLower(int lower) {
            this.lower = lower;
        }

        public void setUpper(int upper) {
            this.upper = upper;
        }
    }
}

