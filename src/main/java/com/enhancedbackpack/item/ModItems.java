package com.enhancedbackpack.item;

import com.enhancedbackpack.EnhancedBackpack;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.Equippable;

public final class ModItems {
	public static final Item BACKPACK = register("backpack",
			new Item.Properties()
					.stacksTo(1)
					.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST).build()),
			BackpackItem::new);

	public static final Item HORN_PLUGIN = register("horn_plugin", new Item.Properties(),
			props -> new PluginItem(props, PluginType.HORN));

	public static final Item CRAFTING_PLUGIN = register("crafting_plugin", new Item.Properties(),
			props -> new PluginItem(props, PluginType.CRAFTING));

	private ModItems() {}

	private static Item register(String name, Item.Properties properties, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, EnhancedBackpack.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void init() {}
}
