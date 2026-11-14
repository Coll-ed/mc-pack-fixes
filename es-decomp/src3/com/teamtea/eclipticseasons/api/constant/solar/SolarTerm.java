/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.Holder
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.level.biome.Biome
 */
package com.teamtea.eclipticseasons.api.constant.solar;

import com.teamtea.eclipticseasons.EclipticSeasons;
import com.teamtea.eclipticseasons.api.constant.climate.BiomeClimateSettings;
import com.teamtea.eclipticseasons.api.constant.climate.BiomeRain;
import com.teamtea.eclipticseasons.api.constant.climate.FlatRain;
import com.teamtea.eclipticseasons.api.constant.climate.ISnowTerm;
import com.teamtea.eclipticseasons.api.constant.climate.MonsoonRain;
import com.teamtea.eclipticseasons.api.constant.climate.SnowTerm;
import com.teamtea.eclipticseasons.api.constant.climate.TemperateRain;
import com.teamtea.eclipticseasons.api.constant.climate.seasonal.ColdRain;
import com.teamtea.eclipticseasons.api.constant.climate.seasonal.HotRain;
import com.teamtea.eclipticseasons.api.constant.solar.ISolarTerm;
import com.teamtea.eclipticseasons.api.constant.solar.Season;
import com.teamtea.eclipticseasons.api.constant.solar.color.base.NoneSolarTermColors;
import com.teamtea.eclipticseasons.api.constant.solar.color.base.RainySolarTermColors;
import com.teamtea.eclipticseasons.api.constant.solar.color.base.SlightlySolarTermColors;
import com.teamtea.eclipticseasons.api.constant.solar.color.base.SolarTermColor;
import com.teamtea.eclipticseasons.api.constant.solar.color.base.TemperateSolarTermColors;
import com.teamtea.eclipticseasons.api.constant.solar.color.base.seasonal.ColdSolarTermColors;
import com.teamtea.eclipticseasons.api.constant.solar.color.base.seasonal.HotSolarTermColors;
import com.teamtea.eclipticseasons.api.constant.tag.ClimateTypeBiomeTags;
import com.teamtea.eclipticseasons.api.data.weather.CustomRain;
import com.teamtea.eclipticseasons.api.misc.ITranslatableWithPlaceholder;
import com.teamtea.eclipticseasons.common.core.biome.BiomeClimateManager;
import com.teamtea.eclipticseasons.common.misc.SimplePair;
import com.teamtea.eclipticseasons.config.CommonConfig;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public enum SolarTerm implements ITranslatableWithPlaceholder,
ISolarTerm
{
    BEGINNING_OF_SPRING(-0.25f, 10500),
    RAIN_WATER(-0.15f, 11000),
    INSECTS_AWAKENING(-0.1f, 11500),
    SPRING_EQUINOX(0.0f, 12000),
    FRESH_GREEN(0.0f, 12500),
    GRAIN_RAIN(0.05f, 13000),
    BEGINNING_OF_SUMMER(0.1f, 13500),
    LESSER_FULLNESS(0.15f, 14000),
    GRAIN_IN_EAR(0.15f, 14500),
    SUMMER_SOLSTICE(0.2f, 15000),
    LESSER_HEAT(0.2f, 14500),
    GREATER_HEAT(0.25f, 14000),
    BEGINNING_OF_AUTUMN(0.15f, 13500),
    END_OF_HEAT(0.1f, 13000),
    WHITE_DEW(0.05f, 12500),
    AUTUMNAL_EQUINOX(0.0f, 12000),
    COLD_DEW(-0.1f, 11500),
    FIRST_FROST(-0.2f, 11000),
    BEGINNING_OF_WINTER(-0.3f, 10500),
    LIGHT_SNOW(-0.35f, 10000),
    HEAVY_SNOW(-0.35f, 9500),
    WINTER_SOLSTICE(-0.4f, 9000),
    LESSER_COLD(-0.45f, 9500),
    GREATER_COLD(-0.4f, 10000),
    NONE(0.0f, 12000);

    private final float temperature;
    private final int dayTime;
    private static final SolarTerm[] solarTerms;
    private static final SolarTerm[] validSolarTerms;

    private SolarTerm(float temperature, int dayTime) {
        this.temperature = temperature;
        this.dayTime = dayTime;
    }

    public SolarTerm getNextSolarTerm() {
        if (!this.isValid()) {
            return NONE;
        }
        return SolarTerm.get((this.ordinal() + 1) % 24);
    }

    public SolarTerm getLastSolarTerm() {
        if (!this.isValid()) {
            return NONE;
        }
        return SolarTerm.get((this.ordinal() + 24 - 1) % 24);
    }

    @Override
    public String getName() {
        return this.toString().toLowerCase(Locale.ROOT);
    }

    @Override
    public MutableComponent getTranslation() {
        return Component.translatable((String)("info.eclipticseasons.environment.solar_term." + this.getName()));
    }

    @Override
    public MutableComponent getTittleTranslation() {
        return Component.translatable((String)"info.eclipticseasons.environment.solar_term.hint");
    }

    @Override
    public MutableComponent getPatternTranslation() {
        return this == NONE ? this.getTranslation() : Component.literal((String)(this.getTranslation().getString() + " (%s)".formatted(this.getSeason().getTranslation().getString())));
    }

    @Override
    public MutableComponent getAlternationText() {
        return Component.translatable((String)("info.eclipticseasons.environment.solar_term.alternation." + this.getName())).withStyle(this.getSeason().getColor());
    }

    @Override
    public ChatFormatting getColor() {
        return this.getSeason().getColor();
    }

    @Override
    public ResourceLocation getIconFont() {
        return EclipticSeasons.rl("solar_icons");
    }

    @Override
    public ResourceLocation getIcon() {
        return SolarTerm.getFullIcon();
    }

    public static ResourceLocation getFont() {
        return EclipticSeasons.rl("solar_icons");
    }

    @Override
    public String getFontLabel() {
        return String.valueOf((char)(this.ordinal() + 97));
    }

    public static ResourceLocation getFullIcon() {
        return EclipticSeasons.rl("font/seasons_icons");
    }

    public static ResourceLocation getFontIcon() {
        return EclipticSeasons.rl("font/seasons_icons_font");
    }

    @Override
    public SimplePair<Integer, Integer> getIconPosition() {
        return SimplePair.of(this.ordinal() % 6, this.ordinal() / 6);
    }

    @Override
    public int getIconAtlasSize() {
        return 30;
    }

    @Override
    public int getIconWidth() {
        return 180;
    }

    @Override
    public int getIconHeight() {
        return 120;
    }

    public static SolarTerm[] collectValues() {
        return solarTerms;
    }

    public static SolarTerm[] collectValidValues() {
        return validSolarTerms;
    }

    public static SolarTerm get(int index) {
        return SolarTerm.collectValues()[index];
    }

    public static SolarTerm getValid(int index) {
        return SolarTerm.collectValidValues()[index % 24];
    }

    public RainySolarTermColors getColorInfo() {
        return RainySolarTermColors.collectValues()[this.ordinal()];
    }

    public SolarTermColor getSolarTermColor(TagKey<Biome> biomeTagKey) {
        if (biomeTagKey == ClimateTypeBiomeTags.NONE_COLOR_CHANGE) {
            return NoneSolarTermColors.get(this.ordinal());
        }
        if (biomeTagKey == ClimateTypeBiomeTags.SLIGHTLY_COLOR_CHANGE) {
            return SlightlySolarTermColors.get(this.ordinal());
        }
        if (biomeTagKey == ClimateTypeBiomeTags.MONSOONAL_COLOR_CHANGE) {
            return RainySolarTermColors.collectValues()[this.ordinal()];
        }
        if (biomeTagKey == ClimateTypeBiomeTags.SEASONAL_HOT_COLOR_CHANGE) {
            return HotSolarTermColors.collectValues()[this.ordinal()];
        }
        if (biomeTagKey == ClimateTypeBiomeTags.SEASONAL_COLD_COLOR_CHANGE) {
            return ColdSolarTermColors.collectValues()[this.ordinal()];
        }
        if (biomeTagKey == ClimateTypeBiomeTags.SEASONAL_COLOR_CHANGE) {
            return TemperateSolarTermColors.collectValues()[this.ordinal()];
        }
        return NoneSolarTermColors.get(this.ordinal());
    }

    public float getTemperatureChange() {
        return this.temperature;
    }

    public int getDayTime() {
        if (CommonConfig.isUseDayTimes()) {
            return CommonConfig.getDayTimesForSeason()[this.ordinal()];
        }
        return this.getOriginalDayTime();
    }

    public int getOriginalDayTime() {
        return this.dayTime;
    }

    @Override
    public Season getSeason() {
        return Season.collectValues()[this.ordinal() / 6];
    }

    public BiomeRain getBiomeRain(Holder<Biome> biomeHolder) {
        Map<SolarTerm, CustomRain> customRainMap = BiomeClimateManager.getCustomRain((Biome)biomeHolder.value(), BiomeClimateManager.isServerInstance((Biome)biomeHolder.value()));
        CustomRain customRain = customRainMap.getOrDefault(this, null);
        if (customRain != null) {
            return customRain;
        }
        TagKey<Biome> tag = BiomeClimateManager.getTag((Biome)biomeHolder.value());
        if (tag == ClimateTypeBiomeTags.RAINLESS) {
            return FlatRain.RAINLESS;
        }
        if (tag == ClimateTypeBiomeTags.ARID) {
            return FlatRain.ARID;
        }
        if (tag == ClimateTypeBiomeTags.DROUGHTY) {
            return FlatRain.DROUGHTY;
        }
        if (tag == ClimateTypeBiomeTags.SOFT) {
            return FlatRain.SOFT;
        }
        if (tag == ClimateTypeBiomeTags.RAINY) {
            return FlatRain.RAINY;
        }
        if (tag == ClimateTypeBiomeTags.MONSOONAL) {
            return MonsoonRain.collectValues()[this.ordinal()];
        }
        if (tag == ClimateTypeBiomeTags.SEASONAL_HOT) {
            return HotRain.collectValues()[this.ordinal()];
        }
        if (tag == ClimateTypeBiomeTags.SEASONAL_COLD) {
            return ColdRain.collectValues()[this.ordinal()];
        }
        return TemperateRain.collectValues()[this.ordinal()];
    }

    public static ISnowTerm getSnowTerm(Biome biome) {
        if (biome == null) {
            return SnowTerm.T05;
        }
        boolean serverInstance = BiomeClimateManager.isServerInstance(biome);
        return SolarTerm.getSnowTerm(biome, serverInstance);
    }

    @Deprecated(forRemoval=true, since="0.12.0")
    public static ISnowTerm getSnowTerm(Biome biome, boolean isServer) {
        return SolarTerm.getSnowTerm(biome, isServer, 0.0f);
    }

    public static ISnowTerm getSnowTerm(Biome biome, boolean isServer, float tempChange) {
        if (biome == null) {
            return SnowTerm.T05;
        }
        ISnowTerm customSnowTerm = BiomeClimateManager.getCustomSnowTerm(biome, isServer);
        if (customSnowTerm != null) {
            return customSnowTerm.cast(tempChange);
        }
        float t = biome.getModifiedClimateSettings().temperature();
        BiomeClimateSettings biomeClimateSettings = BiomeClimateManager.getBiomeClimateSettings(biome, isServer);
        t = biomeClimateSettings == BiomeClimateManager.EMPTY ? t : biomeClimateSettings.getTemperature();
        return SnowTerm.get(t, tempChange);
    }

    public boolean isInTerms(SolarTerm start2, SolarTerm end) {
        if (start2 == NONE || end == NONE) {
            return false;
        }
        if (start2 == end) {
            return this == start2;
        }
        if (start2.ordinal() <= end.ordinal()) {
            return start2.ordinal() <= this.ordinal() && this.ordinal() <= end.ordinal();
        }
        return start2.ordinal() <= this.ordinal() || this.ordinal() <= end.ordinal();
    }

    @Override
    public boolean isValid() {
        return this != NONE;
    }

    static {
        solarTerms = SolarTerm.values();
        validSolarTerms = (SolarTerm[])Arrays.stream(SolarTerm.values()).filter(SolarTerm::isValid).toArray(SolarTerm[]::new);
    }
}

