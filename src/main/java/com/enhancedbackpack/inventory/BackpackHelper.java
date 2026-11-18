package com.enhancedbackpack.inventory;

import com.enhancedbackpack.ModComponents;
import com.enhancedbackpack.item.BackpackItem;
import com.enhancedbackpack.menu.BackpackMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Unico lugar donde se decide DONDE esta la mochila de un jugador.
 * Hoy: ranura de pecho y luego el inventario. Para integrar un mod de ranuras extra
 * (Trinkets / Accessories) solo hay que ampliar all().
 */
public final class BackpackHelper {
	private BackpackHelper() {}

	public static List<ItemStack> all(Player player) {
		List<ItemStack> result = new ArrayList<>();
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		if (chest.getItem() instanceof BackpackItem) result.add(chest);
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.getItem() instanceof BackpackItem && s != chest) result.add(s);
		}
		return result;
	}

	public static boolean isHeld(Player player, ItemStack stack) {
		for (ItemStack s : all(player)) {
			if (s == stack) return true;
		}
		return false;
	}

	public static void openFirst(ServerPlayer player) {
		List<ItemStack> list = all(player);
		if (!list.isEmpty()) open(player, list.get(0));
	}

	public static void open(ServerPlayer player, ItemStack stack) {
		if (player.containerMenu instanceof BackpackMenu) return;
		BackpackInventory inv = BackpackInventory.load(stack).bind(stack);
		player.openMenu(new SimpleMenuProvider(
				(id, playerInv, p) -> new BackpackMenu(id, playerInv, inv, stack),
				Component.translatable("container.enhancedbackpack.backpack")));
	}

	/** Horno en segundo plano: corre para todas las mochilas del jugador, este abierta o no. */
	public static void tickFurnaces(ServerPlayer player, int serverTick) {
		if (!(player.level() instanceof ServerLevel level)) return;
		for (ItemStack stack : all(player)) {
			BackpackState state = stack.getOrDefault(ModComponents.BACKPACK_STATE, BackpackState.EMPTY);
			boolean open = player.containerMenu instanceof BackpackMenu menu && menu.backpack() == stack;
			if (open) {
				((BackpackMenu) player.containerMenu).inventory().tickFurnace(level);
			} else if (state.litTime() > 0 || serverTick % 10 == 0) {
				// En reposo solo se revisa cada 10 ticks para no cargar el inventario completo cada tick
				BackpackInventory inv = BackpackInventory.load(stack).bind(stack);
				inv.tickFurnace(level);
			}
		}
	}
}
