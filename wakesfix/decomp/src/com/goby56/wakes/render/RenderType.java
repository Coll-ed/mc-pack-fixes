/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_5944
 *  net.minecraft.class_757
 */
package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import java.util.function.Supplier;
import net.minecraft.class_310;
import net.minecraft.class_5944;
import net.minecraft.class_757;

public enum RenderType {
    AUTO(null),
    SOLID(class_757::method_34495),
    TRANSLUCENT(class_757::method_34498),
    CUTOUT(class_757::method_34497),
    ENTITY_SOLID(class_757::method_34502),
    ENTITY_TRANSLUCENT(class_757::method_34508),
    ENTITY_TRANSLUCENT_CULL(class_757::method_34507),
    ENTITY_CUTOUT(class_757::method_34503),
    ENTITY_CUTOUT_NO_CULL(class_757::method_34504),
    ENTITY_CUTOUT_NO_CULL_Z_OFFSET(class_757::method_34505);

    public final Supplier<class_5944> program;

    private RenderType(Supplier<class_5944> program) {
        this.program = program;
    }

    public static Supplier<class_5944> getProgram() {
        if (WakesClient.CONFIG_INSTANCE.renderType == AUTO) {
            if (class_310.method_29611()) {
                return RenderType.ENTITY_CUTOUT.program;
            }
            return RenderType.ENTITY_TRANSLUCENT_CULL.program;
        }
        return WakesClient.CONFIG_INSTANCE.renderType.program;
    }
}

