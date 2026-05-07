package com.pvpmod.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration — provides the config screen factory
 * so users can open PVP Reach Overlay settings from the Mods list.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ReachOverlayConfigScreen::new;
    }
}
