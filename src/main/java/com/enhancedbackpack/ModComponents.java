package com.enhancedbackpack;

import com.enhancedbackpack.inventory.BackpackState;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModComponents {
	public static final DataComponentType<BackpackState> BACKPACK_STATE = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			EnhancedBackpack.id("backpack_state"),
			DataComponentType.<BackpackState>builder()
					.persistent(BackpackState.CODEC)
					.networkSynchronized(BackpackState.STREAM_CODEC)
					.build());

	private ModComponents() {}

	public static void init() {}
}
