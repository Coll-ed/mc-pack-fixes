/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.terraformersmc.modmenu.api.ConfigScreenFactory
 *  com.terraformersmc.modmenu.api.ModMenuApi
 *  net.minecraft.class_156
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_410
 *  net.minecraft.class_5244
 */
package com.goby56.wakes.config;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.YACLIntegration;
import com.goby56.wakes.utils.WakesUtils;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import java.net.URI;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_410;
import net.minecraft.class_5244;

public class ModMenuIntegration
implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (WakesClient.isYACLLoaded()) {
            return YACLIntegration::createScreen;
        }
        return parent -> new class_410(result -> {
            if (result) {
                class_156.method_668().method_673(URI.create("https://modrinth.com/mod/yacl/versions"));
            }
            class_310.method_1551().method_1507(parent);
        }, (class_2561)WakesUtils.translatable("config", "yacl_missing"), (class_2561)WakesUtils.translatable("config", "yacl_missing.message"), class_5244.field_24336, class_5244.field_24337);
    }
}

