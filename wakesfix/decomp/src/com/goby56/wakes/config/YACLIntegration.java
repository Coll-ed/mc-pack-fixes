/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.isxander.yacl3.api.ConfigCategory
 *  dev.isxander.yacl3.api.ConfigCategory$Builder
 *  dev.isxander.yacl3.api.Option
 *  dev.isxander.yacl3.api.Option$Builder
 *  dev.isxander.yacl3.api.OptionDescription
 *  dev.isxander.yacl3.api.OptionDescription$Builder
 *  dev.isxander.yacl3.api.OptionGroup
 *  dev.isxander.yacl3.api.OptionGroup$Builder
 *  dev.isxander.yacl3.api.YetAnotherConfigLib
 *  dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder
 *  dev.isxander.yacl3.api.controller.EnumControllerBuilder
 *  dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder
 *  dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
 *  dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 */
package com.goby56.wakes.config;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.render.BlendingFunction;
import com.goby56.wakes.render.RenderType;
import com.goby56.wakes.utils.WakeColor;
import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import com.goby56.wakes.utils.WakesUtils;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_437;

public class YACLIntegration {
    public static class_437 createScreen(class_437 parent) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        Option wakeOpacityOption = YACLIntegration.optionOf(Integer.class, "wake_opacity").description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.wake_opacity")}).build()).binding((Object)100, () -> (int)(config.wakeOpacity * 100.0f), val -> {
            config.wakeOpacity = (float)val.intValue() / 100.0f;
        }).controller(opt -> YACLIntegration.integerSlider((Option<Integer>)opt, 0, 100)).available(config.blendMode.canVaryOpacity).build();
        return YetAnotherConfigLib.createBuilder().title((class_2561)WakesUtils.translatable("config", "title")).category(YACLIntegration.configCategory("wake_appearance").option(YACLIntegration.optionOf(WakesConfig.Resolution.class, "wake_resolution").description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.wake_resolution")}).build()).binding((Object)WakesConfig.Resolution.SIXTEEN, () -> config.wakeResolution, WakeHandler::scheduleResolutionChange).controller(opt -> EnumControllerBuilder.create((Option)opt).enumClass(WakesConfig.Resolution.class).valueFormatter(val -> WakesUtils.translatable("resolution", val.toString()))).build()).option(wakeOpacityOption).option(YACLIntegration.optionOf(BlendingFunction.class, "blend_mode").description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.blend_mode.default")}).text(new class_2561[]{class_2561.method_43471((String)"description.wakes.blend_mode.screen")}).build()).binding((Object)BlendingFunction.DEFAULT, () -> config.blendMode, val -> {
            config.blendMode = val;
            wakeOpacityOption.setAvailable(val.canVaryOpacity);
        }).controller(opt -> EnumControllerBuilder.create((Option)opt).enumClass(BlendingFunction.class).valueFormatter(val -> WakesUtils.translatable("blending_function", val.name().toLowerCase()))).available(!class_310.method_29611()).build()).option(YACLIntegration.booleanOption("use_water_blending").description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.use_water_blending")}).build()).binding((Object)true, () -> config.useWaterBlending, val -> {
            config.useWaterBlending = val;
        }).build()).group(YACLIntegration.group("splash_plane").option(YACLIntegration.optionOf(Float.class, "splash_plane_scale").binding((Object)Float.valueOf(1.0f), () -> Float.valueOf(config.splashPlaneScale), val -> {
            config.splashPlaneScale = val.floatValue();
        }).controller(opt -> YACLIntegration.floatSlider((Option<Float>)opt, 0.1f, 2.0f, 0.1f)).build()).option(YACLIntegration.optionOf(Float.class, "splash_plane_offset").binding((Object)Float.valueOf(0.0f), () -> Float.valueOf(config.splashPlaneOffset), val -> {
            config.splashPlaneOffset = val.floatValue();
        }).controller(opt -> YACLIntegration.floatSlider((Option<Float>)opt, -1.0f, 1.0f, 0.1f)).build()).option(YACLIntegration.optionOf(Float.class, "splash_plane_width").binding((Object)Float.valueOf(3.0f), () -> Float.valueOf(config.splashPlaneWidth), val -> {
            config.splashPlaneWidth = val.floatValue();
        }).controller(opt -> YACLIntegration.floatSlider((Option<Float>)opt, 0.0f, 10.0f, 0.1f)).build()).option(YACLIntegration.optionOf(Float.class, "splash_plane_height").binding((Object)Float.valueOf(1.5f), () -> Float.valueOf(config.splashPlaneHeight), val -> {
            config.splashPlaneHeight = val.floatValue();
        }).controller(opt -> YACLIntegration.floatSlider((Option<Float>)opt, 0.0f, 10.0f, 0.1f)).build()).option(YACLIntegration.optionOf(Float.class, "splash_plane_depth").binding((Object)Float.valueOf(2.0f), () -> Float.valueOf(config.splashPlaneDepth), val -> {
            config.splashPlaneDepth = val.floatValue();
        }).controller(opt -> YACLIntegration.floatSlider((Option<Float>)opt, 0.0f, 10.0f, 0.1f)).build()).build()).build()).category(YACLIntegration.configCategory("wake_behaviour").option(YACLIntegration.booleanOption("spawn_particles").binding((Object)true, () -> config.spawnParticles, val -> {
            config.spawnParticles = val;
        }).build()).option(YACLIntegration.booleanOption("render_splash_plane").binding((Object)true, () -> config.renderSplashPlane, val -> {
            config.renderSplashPlane = val;
        }).build()).option(YACLIntegration.optionOf(Float.class, "max_splash_plane_velocity").description(YACLIntegration.description("max_splash_plane_velocity").build()).binding((Object)Float.valueOf(0.5f), () -> Float.valueOf(config.maxSplashPlaneVelocity), val -> {
            config.maxSplashPlaneVelocity = val.floatValue();
        }).controller(opt -> YACLIntegration.floatSlider((Option<Float>)opt, 0.1f, 2.0f, 0.1f)).build()).group(YACLIntegration.group("wake_spawning").option(YACLIntegration.wakeSpawningRulesOption("boat_wake_rules")).option(YACLIntegration.wakeSpawningRulesOption("player_wake_rules")).option(YACLIntegration.wakeSpawningRulesOption("other_players_wake_rules")).option(YACLIntegration.wakeSpawningRulesOption("mobs_wake_rules")).option(YACLIntegration.wakeSpawningRulesOption("items_wake_rules")).option(YACLIntegration.booleanOption("wakes_in_running_water").description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.wakes_in_running_water")}).build()).binding((Object)false, () -> config.wakesInRunningWater, val -> {
            config.wakesInRunningWater = val;
        }).build()).build()).option(YACLIntegration.optionOf(Float.class, "wave_speed").description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.wave_speed")}).build()).binding((Object)Float.valueOf(0.95f), () -> Float.valueOf(config.waveSpeed), val -> {
            config.waveSpeed = val.floatValue();
            WakeNode.calculateAlpha();
        }).controller(opt -> YACLIntegration.floatSlider((Option<Float>)opt, 0.0f, 2.0f, 0.01f)).build()).option(YACLIntegration.optionOf(Integer.class, "initial_wave_strength").description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.initial_wave_strength")}).build()).binding((Object)20, () -> config.initialStrength, val -> {
            config.initialStrength = val;
        }).controller(opt -> YACLIntegration.integerSlider((Option<Integer>)opt, 0, 150)).build()).option(YACLIntegration.optionOf(Integer.class, "paddle_strength").description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.paddle_strength")}).build()).binding((Object)100, () -> config.paddleStrength, val -> {
            config.paddleStrength = val;
        }).controller(opt -> YACLIntegration.integerSlider((Option<Integer>)opt, 0, 150)).build()).option(YACLIntegration.optionOf(Integer.class, "splash_strength").description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.splash_strength")}).build()).binding((Object)100, () -> config.splashStrength, val -> {
            config.splashStrength = val;
        }).controller(opt -> YACLIntegration.integerSlider((Option<Integer>)opt, 0, 150)).build()).option(YACLIntegration.optionOf(Double.class, "minimum_producer_velocity").description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.minimum_producer_velocity")}).build()).binding((Object)0.1, () -> config.minimumProducerVelocity, val -> {
            config.minimumProducerVelocity = val;
        }).controller(DoubleFieldControllerBuilder::create).build()).option(YACLIntegration.optionOf(Float.class, "wave_decay").description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.wave_decay")}).build()).binding((Object)Float.valueOf(0.5f), () -> Float.valueOf(config.waveDecay), val -> {
            config.waveDecay = val.floatValue();
        }).controller(opt -> YACLIntegration.floatSlider((Option<Float>)opt, 0.0f, 1.0f, 0.01f)).build()).build()).category(YACLIntegration.configCategory("debug").option(YACLIntegration.optionOf(RenderType.class, "render_type").binding((Object)RenderType.AUTO, () -> config.renderType, val -> {
            config.renderType = val;
        }).controller(opt -> EnumControllerBuilder.create((Option)opt).enumClass(RenderType.class)).build()).option(YACLIntegration.optionOf(Integer.class, "flood_fill_distance").binding((Object)3, () -> config.floodFillDistance, val -> {
            config.floodFillDistance = val;
        }).controller(opt -> YACLIntegration.integerSlider((Option<Integer>)opt, 1, 5)).build()).option(YACLIntegration.optionOf(Integer.class, "ticks_before_fill").binding((Object)2, () -> config.ticksBeforeFill, val -> {
            config.ticksBeforeFill = val;
        }).controller(opt -> YACLIntegration.integerSlider((Option<Integer>)opt, 1, 5)).build()).option(YACLIntegration.booleanOption("use_9_point_stencil").binding((Object)true, () -> config.use9PointStencil, val -> {
            config.use9PointStencil = val;
        }).description(YACLIntegration.description("9_point_stencil").build()).build()).option(YACLIntegration.booleanOption("draw_debug_boxes").binding((Object)false, () -> config.drawDebugBoxes, val -> {
            config.drawDebugBoxes = val;
        }).build()).option(YACLIntegration.booleanOption("render_wakes").binding((Object)true, () -> config.renderWakes, val -> {
            config.renderWakes = val;
        }).build()).option(YACLIntegration.booleanOption("spawn_wakes").binding((Object)true, () -> config.spawnWakes, val -> {
            config.spawnWakes = val;
        }).build()).group(YACLIntegration.intervalGroup(0, WakeColor.TRANSPARENT, -50, -45)).group(YACLIntegration.intervalGroup(1, WakeColor.DARK_GRAY, -45, -35)).group(YACLIntegration.intervalGroup(2, WakeColor.GRAY, -35, -30)).group(YACLIntegration.intervalGroup(3, WakeColor.LIGHT_GRAY, -30, -15)).group(YACLIntegration.intervalGroup(4, WakeColor.TRANSPARENT, -15, 2)).group(YACLIntegration.intervalGroup(5, WakeColor.LIGHT_GRAY, 2, 10)).group(YACLIntegration.intervalGroup(6, WakeColor.WHITE, 10, 20)).group(YACLIntegration.intervalGroup(7, WakeColor.LIGHT_GRAY, 20, 40)).group(YACLIntegration.intervalGroup(8, WakeColor.GRAY, 40, 50)).build()).save(config::saveConfig).build().generateScreen(parent);
    }

    private static OptionDescription.Builder description(String name) {
        return OptionDescription.createBuilder().text(new class_2561[]{WakesUtils.translatable("description", name)});
    }

    private static ConfigCategory.Builder configCategory(String name) {
        return ConfigCategory.createBuilder().name((class_2561)WakesUtils.translatable("config_category", name));
    }

    private static OptionGroup.Builder group(String name) {
        return OptionGroup.createBuilder().name((class_2561)WakesUtils.translatable("option_group", name));
    }

    private static <T> Option.Builder<T> optionOf(Class<T> optionType, String name) {
        return Option.createBuilder().name((class_2561)WakesUtils.translatable("option", name));
    }

    private static IntegerSliderControllerBuilder integerSlider(Option<Integer> option, int min, int max) {
        return (IntegerSliderControllerBuilder)((IntegerSliderControllerBuilder)IntegerSliderControllerBuilder.create(option).range((Number)min, (Number)max)).step((Number)1);
    }

    private static FloatSliderControllerBuilder floatSlider(Option<Float> option, float min, float max, float step) {
        return (FloatSliderControllerBuilder)((FloatSliderControllerBuilder)FloatSliderControllerBuilder.create(option).range((Number)Float.valueOf(min), (Number)Float.valueOf(max))).step((Number)Float.valueOf(step));
    }

    private static Option.Builder<Boolean> booleanOption(String name) {
        return Option.createBuilder().name((class_2561)WakesUtils.translatable("option", name)).controller(TickBoxControllerBuilder::create);
    }

    private static Option<WakesConfig.WakeSpawningRule> wakeSpawningRulesOption(String name) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        return Option.createBuilder().name((class_2561)WakesUtils.translatable("option", name)).description(OptionDescription.createBuilder().text(new class_2561[]{class_2561.method_43471((String)"description.wakes.spawning_rules")}).build()).binding((Object)WakesConfig.WakeSpawningRule.WAKES_AND_SPLASHES, () -> config.wakeSpawningRules.get(name), val -> config.wakeSpawningRules.put(name, (WakesConfig.WakeSpawningRule)((Object)val))).controller(opt -> EnumControllerBuilder.create((Option)opt).enumClass(WakesConfig.WakeSpawningRule.class).valueFormatter(val -> WakesUtils.translatable("wake_spawn_rule", val.name().toLowerCase()))).build();
    }

    private static OptionGroup intervalGroup(int n, WakeColor defaultColor, int defaultLower, int defaultUpper) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        return OptionGroup.createBuilder().name((class_2561)WakesUtils.translatable("option_group", "interval" + (n + 1))).option(YACLIntegration.optionOf(Integer.class, "lower").binding((Object)defaultLower, () -> config.colorIntervals.get((int)n).lower, config.colorIntervals.get(n)::setLower).controller(opt -> YACLIntegration.integerSlider((Option<Integer>)opt, -50, 50)).build()).option(YACLIntegration.optionOf(Integer.class, "upper").binding((Object)defaultUpper, () -> config.colorIntervals.get((int)n).upper, config.colorIntervals.get(n)::setUpper).controller(opt -> YACLIntegration.integerSlider((Option<Integer>)opt, -50, 50)).build()).option(YACLIntegration.optionOf(WakeColor.class, "color").binding((Object)defaultColor, () -> config.colorIntervals.get((int)n).color, config.colorIntervals.get(n)::setColor).controller(opt -> EnumControllerBuilder.create((Option)opt).enumClass(WakeColor.class).valueFormatter(val -> WakesUtils.translatable("config", "color." + val.name().toLowerCase()))).build()).build();
    }
}

