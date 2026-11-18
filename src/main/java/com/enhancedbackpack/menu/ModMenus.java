package com.enhancedbackpack.menu;

import com.enhancedbackpack.EnhancedBackpack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	public static final MenuType<BackpackMenu> BACKPACK = Registry.register(
			BuiltInRegistries.MENU,
			EnhancedBackpack.id("backpack"),
			new MenuType<>(BackpackMenu::new, FeatureFlagSet.of()));

	private ModMenus() {}

	public static void init() {}
}
