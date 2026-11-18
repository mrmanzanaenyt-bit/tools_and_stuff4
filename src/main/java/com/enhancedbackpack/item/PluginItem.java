package com.enhancedbackpack.item;

import net.minecraft.world.item.Item;

public class PluginItem extends Item {
	private final PluginType type;

	public PluginItem(Properties properties, PluginType type) {
		super(properties);
		this.type = type;
	}

	public PluginType type() {
		return type;
	}
}
