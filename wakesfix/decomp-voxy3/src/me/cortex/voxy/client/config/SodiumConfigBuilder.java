/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.api.config.ConfigState
 *  net.caffeinemc.mods.sodium.api.config.StorageEventHandler
 *  net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter
 *  net.caffeinemc.mods.sodium.api.config.option.FlagHook
 *  net.caffeinemc.mods.sodium.api.config.option.OptionImpact
 *  net.caffeinemc.mods.sodium.api.config.option.Range
 *  net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder
 *  net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder
 *  net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder
 *  net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder
 *  net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder
 *  net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder
 *  net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder
 *  net.caffeinemc.mods.sodium.api.config.structure.PageBuilder
 *  net.caffeinemc.mods.sodium.api.config.structure.StatefulOptionBuilder
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.ComponentContents
 *  net.minecraft.network.chat.contents.TranslatableContents
 *  net.minecraft.resources.ResourceLocation
 *  org.jetbrains.annotations.Nullable
 */
package me.cortex.voxy.client.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.option.FlagHook;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.PageBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.StatefulOptionBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class SodiumConfigBuilder {
    private static <F, T> T[] map(F[] from, Function<F, T> mapper, Function<Integer, T[]> factory) {
        T[] arr = factory.apply(from.length);
        for (int i = 0; i < from.length; ++i) {
            arr[i] = mapper.apply(from[i]);
        }
        return arr;
    }

    private static ResourceLocation[] mapIds(String[] strings) {
        return SodiumConfigBuilder.map(strings, ResourceLocation::parse, ResourceLocation[]::new);
    }

    public static void buildToSodium(ConfigBuilder builder, ModOptionsBuilder options, StorageEventHandler saveHandler, Consumer<PostApplyOps> registerOps, Page ... pages) {
        BuildCtx ctx = new BuildCtx();
        registerOps.accept(ctx.postRunner);
        ctx.saveHandler = saveHandler;
        for (Page page : pages) {
            options.addPage((PageBuilder)page.create(builder, ctx));
        }
        options.registerFlagHook((FlagHook)ctx.postRunner.build());
    }

    private static final class BuildCtx {
        public PostApplyOps postRunner = new PostApplyOps();
        public StorageEventHandler saveHandler;

        private BuildCtx() {
        }
    }

    public static class PostApplyOps
    implements FlagHook {
        private Map<ResourceLocation, Hook> hooks = new LinkedHashMap<ResourceLocation, Hook>();

        public PostApplyOps register(String name, Runnable postRunner, String ... conflicts) {
            return this.register(ResourceLocation.parse((String)name), postRunner, SodiumConfigBuilder.mapIds(conflicts));
        }

        public PostApplyOps register(ResourceLocation name, Runnable postRunner, ResourceLocation ... conflicts) {
            this.hooks.put(name, new Hook(name, postRunner, new LinkedHashSet<ResourceLocation>(List.of(conflicts))));
            return this;
        }

        protected PostApplyOps build() {
            boolean changed = false;
            do {
                changed = false;
                for (Hook hook : this.hooks.values()) {
                    for (ResourceLocation ref : new LinkedHashSet<ResourceLocation>(hook.conflicts)) {
                        Hook other = this.hooks.getOrDefault(ref, null);
                        if (other == null) continue;
                        changed |= hook.conflicts.addAll(other.conflicts);
                    }
                }
            } while (changed);
            return this;
        }

        public Collection<ResourceLocation> getTriggers() {
            return this.hooks.keySet();
        }

        public void accept(Collection<ResourceLocation> identifiers, ConfigState configState) {
            for (ResourceLocation id : identifiers) {
                Hook hook = this.hooks.get(id);
                if (hook == null || !Collections.disjoint(identifiers, hook.conflicts)) continue;
                hook.runnable.run();
            }
        }

        private record Hook(ResourceLocation name, Runnable runnable, Set<ResourceLocation> conflicts) {
        }
    }

    public static class Page
    extends Enableable<Page> {
        protected Component name;
        protected Group[] groups;

        public Page(Component name, Group ... groups) {
            this.name = name;
            this.groups = groups;
        }

        protected OptionPageBuilder create(ConfigBuilder builder, BuildCtx ctx) {
            OptionPageBuilder page = builder.createOptionPage();
            page.setName(this.name);
            for (Group group : this.groups) {
                page.addOptionGroup(group.create(builder, ctx));
            }
            return page;
        }

        @Override
        protected Enableable[] getEnablerChildren() {
            return this.groups;
        }
    }

    public static class EnumOption<T extends Enum<T>>
    extends Option<T, EnumOption<T>, EnumOptionBuilder<T>> {
        private final Class<T> theEnum;
        private Function<T, Component> nameProvider = value -> Component.literal((String)(value == null ? "NULL" : value.toString()));

        public EnumOption(String id, Class<T> theEnum, Component name, Component tooltip, Supplier<T> getter, Consumer<T> setter) {
            super(id, name, tooltip, getter, setter);
            this.theEnum = theEnum;
        }

        public EnumOption(String id, Class<T> theEnum, Component name, Supplier<T> getter, Consumer<T> setter) {
            super(id, name, getter, setter);
            this.theEnum = theEnum;
        }

        public EnumOption<T> setNameProvider(Function<T, Component> provider) {
            this.nameProvider = provider;
            return this;
        }

        @Override
        protected EnumOptionBuilder<T> createType(ConfigBuilder builder) {
            return builder.createEnumOption(ResourceLocation.parse((String)this.id), this.theEnum);
        }

        @Override
        protected EnumOptionBuilder<T> create(ConfigBuilder builder, BuildCtx ctx) {
            EnumOptionBuilder option = (EnumOptionBuilder)super.create(builder, ctx);
            option.setElementNameProvider(this.nameProvider);
            return option;
        }
    }

    public static class BoolOption
    extends Option<Boolean, BoolOption, BooleanOptionBuilder> {
        public BoolOption(String id, Component name, Component tooltip, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            super(id, name, tooltip, getter, setter);
        }

        public BoolOption(String id, Component name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            super(id, name, getter, setter);
        }

        @Override
        protected BooleanOptionBuilder createType(ConfigBuilder builder) {
            return builder.createBooleanOption(ResourceLocation.parse((String)this.id));
        }
    }

    public static class IntOption
    extends Option<Integer, IntOption, IntegerOptionBuilder> {
        protected Function<ConfigState, Range> rangeProvider;
        protected String[] rangeDependencies;
        protected ControlValueFormatter formatter = v -> Component.literal((String)Integer.toString(v));

        public IntOption(String id, Component name, Component tooltip, Supplier<Integer> getter, Consumer<Integer> setter, Range range) {
            super(id, name, tooltip, getter, setter);
            this.rangeProvider = s -> range;
        }

        public IntOption(String id, Component name, Supplier<Integer> getter, Consumer<Integer> setter, Range range) {
            super(id, name, getter, setter);
            this.rangeProvider = s -> range;
        }

        public IntOption setFormatter(IntFunction<Component> formatter) {
            this.formatter = v -> (Component)formatter.apply(v);
            return this;
        }

        @Override
        protected IntegerOptionBuilder createType(ConfigBuilder builder) {
            return builder.createIntegerOption(ResourceLocation.parse((String)this.id));
        }

        @Override
        protected IntegerOptionBuilder create(ConfigBuilder builder, BuildCtx ctx) {
            IntegerOptionBuilder option = (IntegerOptionBuilder)super.create(builder, ctx);
            if (this.rangeDependencies == null || this.rangeDependencies.length == 0) {
                option.setRange(this.rangeProvider.apply(null));
            } else {
                option.setRangeProvider(this.rangeProvider, SodiumConfigBuilder.mapIds(this.rangeDependencies));
            }
            option.setValueFormatter(this.formatter);
            return option;
        }
    }

    public static abstract class Option<TYPE, OPTION extends Option<TYPE, OPTION, STYPE>, STYPE extends StatefulOptionBuilder<TYPE>>
    extends Enableable<Option<TYPE, OPTION, STYPE>> {
        protected String id;
        protected Component name;
        protected Component tooltip;
        protected Function<TYPE, Component> tooltipSupplier;
        protected Supplier<TYPE> getter;
        protected Consumer<TYPE> setter;
        protected OptionImpact impact;
        private TYPE defaultValue;
        private boolean explicitDefault;
        protected Consumer<TYPE> postRunner;
        protected ResourceLocation[] postRunnerConflicts;
        protected ResourceLocation[] postChangeFlags;

        public OPTION setDefault(TYPE value) {
            this.defaultValue = value;
            this.explicitDefault = true;
            return (OPTION)this;
        }

        public Option(String id, Component name, Component tooltip, Supplier<TYPE> getter, Consumer<TYPE> setter) {
            this.id = id;
            this.name = name;
            this.tooltip = tooltip;
            this.getter = getter;
            this.setter = setter;
        }

        public Option(String id, Component name, Supplier<TYPE> getter, Consumer<TYPE> setter) {
            this.id = id;
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            ComponentContents componentContents = name.getContents();
            if (componentContents instanceof TranslatableContents) {
                TranslatableContents tc = (TranslatableContents)componentContents;
                this.tooltip = Component.translatable((String)(tc.getKey() + ".tooltip"));
            } else {
                this.tooltip = name;
            }
        }

        public OPTION setTooltipSupplier(Function<TYPE, Component> supplier) {
            this.tooltipSupplier = supplier;
            return (OPTION)this;
        }

        public OPTION setImpact(OptionImpact impact) {
            this.impact = impact;
            return (OPTION)this;
        }

        public OPTION setPostChangeRunner(Consumer<TYPE> postRunner, String ... dontRunIfChangedVars) {
            this.postRunner = postRunner;
            this.postRunnerConflicts = SodiumConfigBuilder.mapIds(dontRunIfChangedVars);
            return (OPTION)this;
        }

        public OPTION setPostChangeFlags(String ... flags) {
            this.postChangeFlags = SodiumConfigBuilder.mapIds(flags);
            return (OPTION)this;
        }

        protected abstract STYPE createType(ConfigBuilder var1);

        protected STYPE create(ConfigBuilder builder, BuildCtx ctx) {
            STYPE option = this.createType(builder);
            option.setName(this.name);
            option.setTooltip(this.tooltip);
            LinkedHashSet<ResourceLocation> flags = new LinkedHashSet<ResourceLocation>();
            if (this.postRunner != null) {
                ResourceLocation id = ResourceLocation.parse((String)this.id);
                Consumer runner = this.postRunner;
                Supplier getter = this.getter;
                ctx.postRunner.register(id, () -> runner.accept(getter.get()), this.postRunnerConflicts);
                flags.add(id);
            }
            if (this.postChangeFlags != null) {
                flags.addAll(List.of(this.postChangeFlags));
            }
            if (!flags.isEmpty()) {
                option.setFlags((ResourceLocation[])flags.toArray(ResourceLocation[]::new));
            }
            option.setBinding(this.setter, this.getter);
            if (this.enabler != null) {
                Predicate<ConfigState> pred = this.enabler.tester;
                option.setEnabledProvider(s -> pred.test((ConfigState)s), this.enabler.dependencies);
            }
            option.setStorageHandler(ctx.saveHandler);
            option.setDefaultValue(this.explicitDefault ? this.defaultValue : this.getter.get());
            if (this.tooltipSupplier != null) {
                option.setTooltip(this.tooltipSupplier);
            }
            if (this.impact != null) {
                option.setImpact(this.impact);
            }
            return option;
        }
    }

    public static class Group
    extends Enableable<Group> {
        @Nullable
        protected Component name;
        protected Option[] options;

        public Group(Option ... options) {
            this.options = options;
        }

        public Group(Component name, Option ... options) {
            this.name = name;
            this.options = options;
        }

        protected OptionGroupBuilder create(ConfigBuilder builder, BuildCtx ctx) {
            OptionGroupBuilder group = builder.createOptionGroup();
            if (this.name != null) {
                group.setName(this.name);
            }
            for (Option option : this.options) {
                group.addOption(option.create(builder, ctx));
            }
            return group;
        }

        @Override
        protected Enableable[] getEnablerChildren() {
            return this.options;
        }
    }

    public static abstract class Enableable<TYPE extends Enableable<TYPE>> {
        @Nullable
        private Enabler prevEnabler;
        @Nullable
        protected Enabler enabler;

        private TYPE setEnabler0(Enabler enabler) {
            this.prevEnabler = this.enabler;
            this.enabler = enabler;
            this.updateChildren();
            return (TYPE)this;
        }

        private void updateChildren() {
            Enableable[] children = this.getEnablerChildren();
            if (children != null) {
                for (Enableable child : children) {
                    child.parentEnablerUpdate(this);
                }
            }
        }

        private TYPE parentEnablerUpdate(Enableable parent) {
            if (this.enabler == null) {
                this.setEnabler0(parent.enabler);
            } else if (this.enabler == parent.prevEnabler) {
                this.setEnabler0(parent.enabler);
            } else if (this.enabler.inheritedEnabler != null && this.enabler.inheritedEnabler == parent.prevEnabler) {
                this.setEnabler0(this.enabler.baseEnabler.joinAnd(parent.enabler));
            } else if (this.enabler.inheritedEnabler == null && this.enabler.joinParent) {
                this.setEnabler0(this.enabler.joinAnd(parent.enabler));
            }
            return (TYPE)this;
        }

        public TYPE setEnabler(Predicate<ConfigState> enabler, String ... dependencies) {
            return this.setEnabler0(new Enabler(enabler, dependencies, false));
        }

        public TYPE setEnablerInherit(Predicate<ConfigState> enabler, String ... dependencies) {
            return this.setEnabler0(new Enabler(enabler, dependencies, true));
        }

        public TYPE setEnablerInherit(Predicate<ConfigState> enabler) {
            return this.setEnabler0(new Enabler(enabler, new ResourceLocation[0], true));
        }

        public TYPE setEnablerInherit(Predicate<ConfigState> enabler, ResourceLocation ... dependencies) {
            return this.setEnabler0(new Enabler(enabler, dependencies, true));
        }

        public TYPE setEnabler(String enabler) {
            if (enabler == null) {
                return this.setEnabler(s -> true, new String[0]);
            }
            ResourceLocation id = ResourceLocation.parse((String)enabler);
            return this.setEnabler(s -> s.readBooleanOption(id), enabler);
        }

        public TYPE setEnablerInherit(String enabler) {
            ResourceLocation id = ResourceLocation.parse((String)enabler);
            return this.setEnablerInherit((ConfigState s) -> s.readBooleanOption(id), enabler);
        }

        public TYPE setEnablerAND(String ... enablers) {
            ResourceLocation[] enablersId = SodiumConfigBuilder.mapIds(enablers);
            return this.setEnabler0(new Enabler(s -> {
                for (ResourceLocation id : enablersId) {
                    if (s.readBooleanOption(id)) continue;
                    return false;
                }
                return true;
            }, enablersId));
        }

        protected Enableable[] getEnablerChildren() {
            return null;
        }
    }

    private static class Enabler {
        public final Predicate<ConfigState> tester;
        public final ResourceLocation[] dependencies;
        public final boolean joinParent;
        public Enabler inheritedEnabler;
        public Enabler baseEnabler;

        public Enabler(Predicate<ConfigState> tester, ResourceLocation[] dependencies, boolean joinParent) {
            this.tester = tester;
            this.dependencies = dependencies;
            this.joinParent = joinParent;
        }

        public Enabler(Predicate<ConfigState> tester, String[] dependencies) {
            this(tester, dependencies, false);
        }

        public Enabler(Predicate<ConfigState> tester, ResourceLocation[] dependencies) {
            this(tester, dependencies, false);
        }

        public Enabler(Predicate<ConfigState> tester, String[] dependencies, boolean joinParent) {
            this(tester, SodiumConfigBuilder.mapIds(dependencies), joinParent);
        }

        public Enabler joinAnd(Enabler parent) {
            HashSet<ResourceLocation> identifiers = new HashSet<ResourceLocation>();
            for (ResourceLocation i : this.dependencies) {
                identifiers.add(i);
            }
            for (ResourceLocation i : parent.dependencies) {
                identifiers.add(i);
            }
            Predicate<ConfigState> tester = state -> {
                if (!this.tester.test((ConfigState)state)) {
                    return false;
                }
                return parent.tester.test((ConfigState)state);
            };
            Enabler newEnabler = new Enabler(tester, (ResourceLocation[])identifiers.toArray(ResourceLocation[]::new));
            newEnabler.baseEnabler = this;
            newEnabler.inheritedEnabler = parent;
            return newEnabler;
        }
    }
}

