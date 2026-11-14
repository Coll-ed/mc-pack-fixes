/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  javax.annotation.Nullable
 *  lombok.Generated
 *  net.minecraft.core.Holder
 *  net.minecraft.core.HolderSet
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.teamtea.eclipticseasons.api.data.season;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamtea.eclipticseasons.api.misc.util.HolderMappable;
import com.teamtea.eclipticseasons.api.misc.util.MapFiller;
import com.teamtea.eclipticseasons.api.util.codec.CodecUtil;
import com.teamtea.eclipticseasons.common.core.snow.ClientModelDefinitions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SnowDefinition
implements MapFiller<Block, SnowDefinition>,
HolderMappable<HolderSet<Block>, SnowDefinition> {
    public static final Codec<SnowDefinition> CODEC = RecordCodecBuilder.create(ins -> ins.group((App)CodecUtil.holderSetCodec(Registries.BLOCK).fieldOf("blocks").forGetter(o -> o.blocks), (App)PropertyTester.CODEC.listOf().optionalFieldOf("properties", List.of()).forGetter(o -> o.map), (App)Info.MAP_CODEC.forGetter(o -> o.info)).apply((Applicative)ins, SnowDefinition::new));
    @NotNull
    private final HolderSet<Block> blocks;
    private final List<PropertyTester> map;
    private final Info info;

    @Override
    public void fillMap(Map<Block, List<SnowDefinition>> map) {
        for (Holder block : this.blocks) {
            List definitionList = map.computeIfAbsent((Block)block.value(), b -> new ArrayList());
            definitionList.add(this);
        }
    }

    @Override
    public Pair<HolderSet<Block>, SnowDefinition> asHolderMapping() {
        return Pair.of(this.blocks, (Object)this);
    }

    @Generated
    private static List<PropertyTester> $default$map() {
        return new ArrayList<PropertyTester>();
    }

    @Generated
    private static Info $default$info() {
        return Info.builder().build();
    }

    @Generated
    SnowDefinition(@NotNull HolderSet<Block> blocks, List<PropertyTester> map, Info info) {
        if (blocks == null) {
            throw new NullPointerException("blocks is marked non-null but is null");
        }
        this.blocks = blocks;
        this.map = map;
        this.info = info;
    }

    @Generated
    public static SnowDefinitionBuilder builder() {
        return new SnowDefinitionBuilder();
    }

    @NotNull
    @Generated
    public HolderSet<Block> getBlocks() {
        return this.blocks;
    }

    @Generated
    public List<PropertyTester> getMap() {
        return this.map;
    }

    @Generated
    public Info getInfo() {
        return this.info;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SnowDefinition)) {
            return false;
        }
        SnowDefinition other = (SnowDefinition)o;
        if (!other.canEqual(this)) {
            return false;
        }
        HolderSet<Block> this$blocks = this.getBlocks();
        HolderSet<Block> other$blocks = other.getBlocks();
        if (this$blocks == null ? other$blocks != null : !this$blocks.equals(other$blocks)) {
            return false;
        }
        List<PropertyTester> this$map = this.getMap();
        List<PropertyTester> other$map = other.getMap();
        if (this$map == null ? other$map != null : !((Object)this$map).equals(other$map)) {
            return false;
        }
        Info this$info = this.getInfo();
        Info other$info = other.getInfo();
        return !(this$info == null ? other$info != null : !((Object)this$info).equals(other$info));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SnowDefinition;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        HolderSet<Block> $blocks = this.getBlocks();
        result = result * 59 + ($blocks == null ? 43 : $blocks.hashCode());
        List<PropertyTester> $map = this.getMap();
        result = result * 59 + ($map == null ? 43 : ((Object)$map).hashCode());
        Info $info = this.getInfo();
        result = result * 59 + ($info == null ? 43 : ((Object)$info).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SnowDefinition(blocks=" + String.valueOf(this.getBlocks()) + ", map=" + String.valueOf(this.getMap()) + ", info=" + String.valueOf(this.getInfo()) + ")";
    }

    public static class Info {
        public static final MapCodec<Info> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group((App)Codec.INT.optionalFieldOf("flag", (Object)1000).forGetter(o -> o.flag), (App)Codec.BOOL.optionalFieldOf("snow_passable", (Object)false).forGetter(o -> o.snowPassable), (App)Codec.BOOL.optionalFieldOf("ignore_offset", (Object)false).forGetter(o -> o.ignoreOffset), (App)Codec.INT.optionalFieldOf("offset", (Object)0).forGetter(o -> o.offset), (App)ResourceLocation.CODEC.optionalFieldOf("mid", (Object)ClientModelDefinitions.EMPTY).forGetter(o -> o.mid), (App)ResourceLocation.CODEC.optionalFieldOf("mid2", (Object)ClientModelDefinitions.EMPTY).forGetter(o -> o.mid2)).apply((Applicative)ins, Info::new));
        public static final Codec<Info> CODEC = MAP_CODEC.codec();
        public static final Info EMPTY = Info.builder().flag(-1).build();
        private final int flag;
        private final boolean snowPassable;
        private final boolean ignoreOffset;
        private final int offset;
        private final ResourceLocation mid;
        private final ResourceLocation mid2;

        public boolean isValid() {
            return this.flag != -1;
        }

        @Generated
        private static int $default$flag() {
            return 1000;
        }

        @Generated
        private static boolean $default$snowPassable() {
            return false;
        }

        @Generated
        private static boolean $default$ignoreOffset() {
            return false;
        }

        @Generated
        private static int $default$offset() {
            return 0;
        }

        @Generated
        private static ResourceLocation $default$mid() {
            return ClientModelDefinitions.OVERLAY;
        }

        @Generated
        private static ResourceLocation $default$mid2() {
            return ClientModelDefinitions.EMPTY;
        }

        @Generated
        Info(int flag, boolean snowPassable, boolean ignoreOffset, int offset, ResourceLocation mid, ResourceLocation mid2) {
            this.flag = flag;
            this.snowPassable = snowPassable;
            this.ignoreOffset = ignoreOffset;
            this.offset = offset;
            this.mid = mid;
            this.mid2 = mid2;
        }

        @Generated
        public static InfoBuilder builder() {
            return new InfoBuilder();
        }

        @Generated
        public int getFlag() {
            return this.flag;
        }

        @Generated
        public boolean isSnowPassable() {
            return this.snowPassable;
        }

        @Generated
        public boolean isIgnoreOffset() {
            return this.ignoreOffset;
        }

        @Generated
        public int getOffset() {
            return this.offset;
        }

        @Generated
        public ResourceLocation getMid() {
            return this.mid;
        }

        @Generated
        public ResourceLocation getMid2() {
            return this.mid2;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Info)) {
                return false;
            }
            Info other = (Info)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.getFlag() != other.getFlag()) {
                return false;
            }
            if (this.isSnowPassable() != other.isSnowPassable()) {
                return false;
            }
            if (this.isIgnoreOffset() != other.isIgnoreOffset()) {
                return false;
            }
            if (this.getOffset() != other.getOffset()) {
                return false;
            }
            ResourceLocation this$mid = this.getMid();
            ResourceLocation other$mid = other.getMid();
            if (this$mid == null ? other$mid != null : !this$mid.equals(other$mid)) {
                return false;
            }
            ResourceLocation this$mid2 = this.getMid2();
            ResourceLocation other$mid2 = other.getMid2();
            return !(this$mid2 == null ? other$mid2 != null : !this$mid2.equals(other$mid2));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof Info;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + this.getFlag();
            result = result * 59 + (this.isSnowPassable() ? 79 : 97);
            result = result * 59 + (this.isIgnoreOffset() ? 79 : 97);
            result = result * 59 + this.getOffset();
            ResourceLocation $mid = this.getMid();
            result = result * 59 + ($mid == null ? 43 : $mid.hashCode());
            ResourceLocation $mid2 = this.getMid2();
            result = result * 59 + ($mid2 == null ? 43 : $mid2.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "SnowDefinition.Info(flag=" + this.getFlag() + ", snowPassable=" + this.isSnowPassable() + ", ignoreOffset=" + this.isIgnoreOffset() + ", offset=" + this.getOffset() + ", mid=" + String.valueOf(this.getMid()) + ", mid2=" + String.valueOf(this.getMid2()) + ")";
        }

        @Generated
        public static class InfoBuilder {
            @Generated
            private boolean flag$set;
            @Generated
            private int flag$value;
            @Generated
            private boolean snowPassable$set;
            @Generated
            private boolean snowPassable$value;
            @Generated
            private boolean ignoreOffset$set;
            @Generated
            private boolean ignoreOffset$value;
            @Generated
            private boolean offset$set;
            @Generated
            private int offset$value;
            @Generated
            private boolean mid$set;
            @Generated
            private ResourceLocation mid$value;
            @Generated
            private boolean mid2$set;
            @Generated
            private ResourceLocation mid2$value;

            @Generated
            InfoBuilder() {
            }

            @Generated
            public InfoBuilder flag(int flag) {
                this.flag$value = flag;
                this.flag$set = true;
                return this;
            }

            @Generated
            public InfoBuilder snowPassable(boolean snowPassable) {
                this.snowPassable$value = snowPassable;
                this.snowPassable$set = true;
                return this;
            }

            @Generated
            public InfoBuilder ignoreOffset(boolean ignoreOffset) {
                this.ignoreOffset$value = ignoreOffset;
                this.ignoreOffset$set = true;
                return this;
            }

            @Generated
            public InfoBuilder offset(int offset) {
                this.offset$value = offset;
                this.offset$set = true;
                return this;
            }

            @Generated
            public InfoBuilder mid(ResourceLocation mid) {
                this.mid$value = mid;
                this.mid$set = true;
                return this;
            }

            @Generated
            public InfoBuilder mid2(ResourceLocation mid2) {
                this.mid2$value = mid2;
                this.mid2$set = true;
                return this;
            }

            @Generated
            public Info build() {
                int flag$value = this.flag$value;
                if (!this.flag$set) {
                    flag$value = Info.$default$flag();
                }
                boolean snowPassable$value = this.snowPassable$value;
                if (!this.snowPassable$set) {
                    snowPassable$value = Info.$default$snowPassable();
                }
                boolean ignoreOffset$value = this.ignoreOffset$value;
                if (!this.ignoreOffset$set) {
                    ignoreOffset$value = Info.$default$ignoreOffset();
                }
                int offset$value = this.offset$value;
                if (!this.offset$set) {
                    offset$value = Info.$default$offset();
                }
                ResourceLocation mid$value = this.mid$value;
                if (!this.mid$set) {
                    mid$value = Info.$default$mid();
                }
                ResourceLocation mid2$value = this.mid2$value;
                if (!this.mid2$set) {
                    mid2$value = Info.$default$mid2();
                }
                return new Info(flag$value, snowPassable$value, ignoreOffset$value, offset$value, mid$value, mid2$value);
            }

            @Generated
            public String toString() {
                return "SnowDefinition.Info.InfoBuilder(flag$value=" + this.flag$value + ", snowPassable$value=" + this.snowPassable$value + ", ignoreOffset$value=" + this.ignoreOffset$value + ", offset$value=" + this.offset$value + ", mid$value=" + String.valueOf(this.mid$value) + ", mid2$value=" + String.valueOf(this.mid2$value) + ")";
            }
        }
    }

    @Generated
    public static class SnowDefinitionBuilder {
        @Generated
        private HolderSet<Block> blocks;
        @Generated
        private boolean map$set;
        @Generated
        private List<PropertyTester> map$value;
        @Generated
        private boolean info$set;
        @Generated
        private Info info$value;

        @Generated
        SnowDefinitionBuilder() {
        }

        @Generated
        public SnowDefinitionBuilder blocks(@NotNull HolderSet<Block> blocks) {
            if (blocks == null) {
                throw new NullPointerException("blocks is marked non-null but is null");
            }
            this.blocks = blocks;
            return this;
        }

        @Generated
        public SnowDefinitionBuilder map(List<PropertyTester> map) {
            this.map$value = map;
            this.map$set = true;
            return this;
        }

        @Generated
        public SnowDefinitionBuilder info(Info info) {
            this.info$value = info;
            this.info$set = true;
            return this;
        }

        @Generated
        public SnowDefinition build() {
            List<PropertyTester> map$value = this.map$value;
            if (!this.map$set) {
                map$value = SnowDefinition.$default$map();
            }
            Info info$value = this.info$value;
            if (!this.info$set) {
                info$value = SnowDefinition.$default$info();
            }
            return new SnowDefinition(this.blocks, map$value, info$value);
        }

        @Generated
        public String toString() {
            return "SnowDefinition.SnowDefinitionBuilder(blocks=" + String.valueOf(this.blocks) + ", map$value=" + String.valueOf(this.map$value) + ", info$value=" + String.valueOf(this.info$value) + ")";
        }
    }

    public static class PropertyTester {
        public static final Codec<PropertyTester> CODEC = RecordCodecBuilder.create(ins -> ins.group((App)Codec.STRING.fieldOf("name").forGetter(o -> o.name), (App)Codec.BOOL.optionalFieldOf("reverse", (Object)false).forGetter(o -> o.reverse), (App)CodecUtil.either(ExactMatcher.CODEC, RangeMatcher.CODEC, ExactMatcher.class, RangeMatcher.class).fieldOf("matcher").forGetter(o -> o.matcher)).apply((Applicative)ins, PropertyTester::new));
        @NotNull
        private final String name;
        private final boolean reverse;
        @NotNull
        private final PropertyMatcher matcher;

        public boolean matches(BlockState blockState) {
            Property test = null;
            for (Property property : blockState.getProperties()) {
                if (!property.getName().equals(this.name)) continue;
                test = property;
            }
            return this.matcher.matches(test, blockState);
        }

        @Generated
        private static boolean $default$reverse() {
            return false;
        }

        @Generated
        PropertyTester(@NotNull String name, boolean reverse, @NotNull PropertyMatcher matcher) {
            if (name == null) {
                throw new NullPointerException("name is marked non-null but is null");
            }
            if (matcher == null) {
                throw new NullPointerException("matcher is marked non-null but is null");
            }
            this.name = name;
            this.reverse = reverse;
            this.matcher = matcher;
        }

        @Generated
        public static PropertyTesterBuilder builder() {
            return new PropertyTesterBuilder();
        }

        @NotNull
        @Generated
        public String getName() {
            return this.name;
        }

        @Generated
        public boolean isReverse() {
            return this.reverse;
        }

        @NotNull
        @Generated
        public PropertyMatcher getMatcher() {
            return this.matcher;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof PropertyTester)) {
                return false;
            }
            PropertyTester other = (PropertyTester)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.isReverse() != other.isReverse()) {
                return false;
            }
            String this$name = this.getName();
            String other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
                return false;
            }
            PropertyMatcher this$matcher = this.getMatcher();
            PropertyMatcher other$matcher = other.getMatcher();
            return !(this$matcher == null ? other$matcher != null : !this$matcher.equals(other$matcher));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof PropertyTester;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + (this.isReverse() ? 79 : 97);
            String $name = this.getName();
            result = result * 59 + ($name == null ? 43 : $name.hashCode());
            PropertyMatcher $matcher = this.getMatcher();
            result = result * 59 + ($matcher == null ? 43 : $matcher.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "SnowDefinition.PropertyTester(name=" + this.getName() + ", reverse=" + this.isReverse() + ", matcher=" + String.valueOf(this.getMatcher()) + ")";
        }

        @Generated
        public static class PropertyTesterBuilder {
            @Generated
            private String name;
            @Generated
            private boolean reverse$set;
            @Generated
            private boolean reverse$value;
            @Generated
            private PropertyMatcher matcher;

            @Generated
            PropertyTesterBuilder() {
            }

            @Generated
            public PropertyTesterBuilder name(@NotNull String name) {
                if (name == null) {
                    throw new NullPointerException("name is marked non-null but is null");
                }
                this.name = name;
                return this;
            }

            @Generated
            public PropertyTesterBuilder reverse(boolean reverse) {
                this.reverse$value = reverse;
                this.reverse$set = true;
                return this;
            }

            @Generated
            public PropertyTesterBuilder matcher(@NotNull PropertyMatcher matcher) {
                if (matcher == null) {
                    throw new NullPointerException("matcher is marked non-null but is null");
                }
                this.matcher = matcher;
                return this;
            }

            @Generated
            public PropertyTester build() {
                boolean reverse$value = this.reverse$value;
                if (!this.reverse$set) {
                    reverse$value = PropertyTester.$default$reverse();
                }
                return new PropertyTester(this.name, reverse$value, this.matcher);
            }

            @Generated
            public String toString() {
                return "SnowDefinition.PropertyTester.PropertyTesterBuilder(name=" + this.name + ", reverse$value=" + this.reverse$value + ", matcher=" + String.valueOf(this.matcher) + ")";
            }
        }
    }

    public static interface PropertyMatcher {
        public boolean matches(@javax.annotation.Nullable Property<?> var1, BlockState var2);
    }

    public static class RangeMatcher
    implements PropertyMatcher {
        public static final Codec<RangeMatcher> CODEC = RecordCodecBuilder.create(ins -> ins.group((App)Codec.BOOL.optionalFieldOf("ignore_min", (Object)false).forGetter(o -> o.ignoreMin), (App)Codec.INT.fieldOf("minTime").forGetter(o -> o.min), (App)Codec.BOOL.optionalFieldOf("ignore_max", (Object)false).forGetter(o -> o.ignoreMax), (App)Codec.INT.fieldOf("maxTime").forGetter(o -> o.max)).apply((Applicative)ins, RangeMatcher::new));
        private final boolean ignoreMin;
        private final int min;
        private final boolean ignoreMax;
        private final int max;

        @Override
        public boolean matches(@Nullable Property<?> property, BlockState blockState) {
            Comparable comparable;
            if (property != null && (comparable = blockState.getValue(property)) instanceof Integer) {
                Integer integer = (Integer)comparable;
                return !(!this.ignoreMin && integer < this.min || !this.ignoreMax && integer > this.max);
            }
            return false;
        }

        @Generated
        private static boolean $default$ignoreMin() {
            return false;
        }

        @Generated
        private static boolean $default$ignoreMax() {
            return false;
        }

        @Generated
        RangeMatcher(boolean ignoreMin, int min, boolean ignoreMax, int max) {
            this.ignoreMin = ignoreMin;
            this.min = min;
            this.ignoreMax = ignoreMax;
            this.max = max;
        }

        @Generated
        public static RangeMatcherBuilder builder() {
            return new RangeMatcherBuilder();
        }

        @Generated
        public boolean isIgnoreMin() {
            return this.ignoreMin;
        }

        @Generated
        public int getMin() {
            return this.min;
        }

        @Generated
        public boolean isIgnoreMax() {
            return this.ignoreMax;
        }

        @Generated
        public int getMax() {
            return this.max;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof RangeMatcher)) {
                return false;
            }
            RangeMatcher other = (RangeMatcher)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.isIgnoreMin() != other.isIgnoreMin()) {
                return false;
            }
            if (this.getMin() != other.getMin()) {
                return false;
            }
            if (this.isIgnoreMax() != other.isIgnoreMax()) {
                return false;
            }
            return this.getMax() == other.getMax();
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof RangeMatcher;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + (this.isIgnoreMin() ? 79 : 97);
            result = result * 59 + this.getMin();
            result = result * 59 + (this.isIgnoreMax() ? 79 : 97);
            result = result * 59 + this.getMax();
            return result;
        }

        @Generated
        public String toString() {
            return "SnowDefinition.RangeMatcher(ignoreMin=" + this.isIgnoreMin() + ", min=" + this.getMin() + ", ignoreMax=" + this.isIgnoreMax() + ", max=" + this.getMax() + ")";
        }

        @Generated
        public static class RangeMatcherBuilder {
            @Generated
            private boolean ignoreMin$set;
            @Generated
            private boolean ignoreMin$value;
            @Generated
            private int min;
            @Generated
            private boolean ignoreMax$set;
            @Generated
            private boolean ignoreMax$value;
            @Generated
            private int max;

            @Generated
            RangeMatcherBuilder() {
            }

            @Generated
            public RangeMatcherBuilder ignoreMin(boolean ignoreMin) {
                this.ignoreMin$value = ignoreMin;
                this.ignoreMin$set = true;
                return this;
            }

            @Generated
            public RangeMatcherBuilder min(int min) {
                this.min = min;
                return this;
            }

            @Generated
            public RangeMatcherBuilder ignoreMax(boolean ignoreMax) {
                this.ignoreMax$value = ignoreMax;
                this.ignoreMax$set = true;
                return this;
            }

            @Generated
            public RangeMatcherBuilder max(int max) {
                this.max = max;
                return this;
            }

            @Generated
            public RangeMatcher build() {
                boolean ignoreMin$value = this.ignoreMin$value;
                if (!this.ignoreMin$set) {
                    ignoreMin$value = RangeMatcher.$default$ignoreMin();
                }
                boolean ignoreMax$value = this.ignoreMax$value;
                if (!this.ignoreMax$set) {
                    ignoreMax$value = RangeMatcher.$default$ignoreMax();
                }
                return new RangeMatcher(ignoreMin$value, this.min, ignoreMax$value, this.max);
            }

            @Generated
            public String toString() {
                return "SnowDefinition.RangeMatcher.RangeMatcherBuilder(ignoreMin$value=" + this.ignoreMin$value + ", min=" + this.min + ", ignoreMax$value=" + this.ignoreMax$value + ", max=" + this.max + ")";
            }
        }
    }

    public static class ExactMatcher
    implements PropertyMatcher {
        public static final Codec<ExactMatcher> CODEC = RecordCodecBuilder.create(ins -> ins.group((App)Codec.STRING.fieldOf("value").forGetter(o -> o.value)).apply((Applicative)ins, ExactMatcher::new));
        @NotNull
        private final String value;

        @Override
        public boolean matches(@javax.annotation.Nullable Property<?> property, BlockState blockState) {
            return property != null && blockState.getValue(property).toString().equals(this.value);
        }

        @Generated
        ExactMatcher(@NotNull String value) {
            if (value == null) {
                throw new NullPointerException("value is marked non-null but is null");
            }
            this.value = value;
        }

        @Generated
        public static ExactMatcherBuilder builder() {
            return new ExactMatcherBuilder();
        }

        @NotNull
        @Generated
        public String getValue() {
            return this.value;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof ExactMatcher)) {
                return false;
            }
            ExactMatcher other = (ExactMatcher)o;
            if (!other.canEqual(this)) {
                return false;
            }
            String this$value = this.getValue();
            String other$value = other.getValue();
            return !(this$value == null ? other$value != null : !this$value.equals(other$value));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof ExactMatcher;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            String $value = this.getValue();
            result = result * 59 + ($value == null ? 43 : $value.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "SnowDefinition.ExactMatcher(value=" + this.getValue() + ")";
        }

        @Generated
        public static class ExactMatcherBuilder {
            @Generated
            private String value;

            @Generated
            ExactMatcherBuilder() {
            }

            @Generated
            public ExactMatcherBuilder value(@NotNull String value) {
                if (value == null) {
                    throw new NullPointerException("value is marked non-null but is null");
                }
                this.value = value;
                return this;
            }

            @Generated
            public ExactMatcher build() {
                return new ExactMatcher(this.value);
            }

            @Generated
            public String toString() {
                return "SnowDefinition.ExactMatcher.ExactMatcherBuilder(value=" + this.value + ")";
            }
        }
    }
}

