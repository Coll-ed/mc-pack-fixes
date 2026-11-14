/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.teamtea.eclipticseasons.api.event.SolarTermChangeEvent
 *  net.minecraft.client.Minecraft
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent$LoggingOut
 */
package me.cortex.voxy.client.core.compat.eclipticseasons;

import com.teamtea.eclipticseasons.api.event.SolarTermChangeEvent;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.client.core.compat.eclipticseasons.SeasonalMeshView;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public class VoxyEsHandler {
    public static final VoxyEsHandler INSTANCE = new VoxyEsHandler();

    @SubscribeEvent
    public void onSolarTermChangeEvent(SolarTermChangeEvent event) {
        if (event.getLevel() != Minecraft.getInstance().level) {
            return;
        }
        SeasonalMeshView.clearJudgementCaches();
        if (VoxyConfig.CONFIG.eclipticSeasonsReloadOnSeasonChange) {
            try {
                IGetVoxyRenderSystem levelRenderer = (IGetVoxyRenderSystem)Minecraft.getInstance().levelRenderer;
                levelRenderer.voxy$shutdownRenderer();
                levelRenderer.voxy$createRenderer();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SeasonalMeshView.clearJudgementCaches();
    }
}

