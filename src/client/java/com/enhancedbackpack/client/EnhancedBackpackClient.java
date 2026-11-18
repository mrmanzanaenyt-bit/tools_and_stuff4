package com.enhancedbackpack.client;

import com.enhancedbackpack.EnhancedBackpack;
import com.enhancedbackpack.menu.ModMenus;
import com.enhancedbackpack.network.OpenBackpackPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;

public class EnhancedBackpackClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.BACKPACK, BackpackScreen::new);

		// La tecla se puede cambiar en Opciones > Controles > Teclas
		KeyMapping.Category category = KeyMapping.Category.register(EnhancedBackpack.id("main"));
		KeyMapping openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.enhancedbackpack.open",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_B,
				category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.consumeClick()) {
				if (client.player == null) return;
				ClientPlayNetworking.send(OpenBackpackPayload.INSTANCE);
			}
		});
	}
}
