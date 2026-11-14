/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
 *  net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
 *  net.minecraft.class_1163
 *  net.minecraft.class_1920
 *  net.minecraft.class_2338
 *  net.minecraft.class_2394
 *  net.minecraft.class_243
 *  net.minecraft.class_2561
 *  net.minecraft.class_5253$class_5254
 *  net.minecraft.class_638
 *  net.minecraft.class_7157
 *  net.minecraft.class_761
 */
package com.goby56.wakes.command;

import com.goby56.wakes.particle.ModParticles;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.class_1163;
import net.minecraft.class_1920;
import net.minecraft.class_2338;
import net.minecraft.class_2394;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_5253;
import net.minecraft.class_638;
import net.minecraft.class_7157;
import net.minecraft.class_761;

public class DebugCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, class_7157 registryAccess) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal((String)"wakes_debug").then(ClientCommandManager.literal((String)"light").executes(DebugCommand::lightCoordinate))).then(ClientCommandManager.literal((String)"color").executes(DebugCommand::waterColor))).then(ClientCommandManager.literal((String)"spawn").then(ClientCommandManager.literal((String)"splash_cloud_particle").executes(DebugCommand::spawnSplashCloudParticle))));
    }

    public static int lightCoordinate(CommandContext<FabricClientCommandSource> cmdCtx) throws CommandSyntaxException {
        class_638 world = ((FabricClientCommandSource)cmdCtx.getSource()).getWorld();
        class_2338 blockPos = ((FabricClientCommandSource)cmdCtx.getSource()).getPlayer().method_24515();
        ((FabricClientCommandSource)cmdCtx.getSource()).sendFeedback(class_2561.method_30163((String)String.valueOf(class_761.method_23794((class_1920)world, (class_2338)blockPos))));
        return 1;
    }

    public static int waterColor(CommandContext<FabricClientCommandSource> cmdCtx) throws CommandSyntaxException {
        class_638 world = ((FabricClientCommandSource)cmdCtx.getSource()).getWorld();
        class_2338 blockPos = ((FabricClientCommandSource)cmdCtx.getSource()).getPlayer().method_24515();
        int col = class_1163.method_4961((class_1920)world, (class_2338)blockPos);
        ((FabricClientCommandSource)cmdCtx.getSource()).sendFeedback(class_2561.method_30163((String)String.format("(%d, %d, %d)", class_5253.class_5254.method_27765((int)col), class_5253.class_5254.method_27766((int)col), class_5253.class_5254.method_27767((int)col))));
        return 1;
    }

    public static int spawnSplashCloudParticle(CommandContext<FabricClientCommandSource> cmdCtx) throws CommandSyntaxException {
        class_243 pos = ((FabricClientCommandSource)cmdCtx.getSource()).getPosition();
        ((FabricClientCommandSource)cmdCtx.getSource()).getWorld().method_8406((class_2394)ModParticles.SPLASH_CLOUD, pos.field_1352, pos.field_1351, pos.field_1350, 0.0, 0.0, 0.0);
        return 1;
    }
}

