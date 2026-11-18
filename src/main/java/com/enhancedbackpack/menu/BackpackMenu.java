package com.enhancedbackpack.menu;

import com.enhancedbackpack.inventory.BackpackHelper;
import com.enhancedbackpack.inventory.BackpackInventory;
import com.enhancedbackpack.item.BackpackItem;
import com.enhancedbackpack.item.PluginItem;
import com.enhancedbackpack.item.PluginType;
import java.util.Optional;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

public class BackpackMenu extends AbstractContainerMenu {
	// indices de slot del menu
	public static final int STORAGE_END = 27;
	public static final int PLUGIN_A = 27;
	public static final int PLUGIN_B = 28;
	public static final int GRID_START = 29;
	public static final int RESULT = 38;
	public static final int FURNACE_IN = 39;
	public static final int FURNACE_FUEL = 40;
	public static final int FURNACE_OUT = 41;
	public static final int PLAYER_START = 42;
	public static final int PLAYER_END = PLAYER_START + Inventory.INVENTORY_SIZE;

	// posiciones (relativas a la esquina del GUI). BackpackScreen usa las mismas.
	public static final int SIDE_X = 188;

	private final BackpackInventory inv;
	private final ItemStack backpack;
	private final Player player;
	private final TransientCraftingContainer grid;
	private final ResultContainer result = new ResultContainer();

	/** Constructor del cliente: contenedores vacios que el servidor sincroniza. */
	public BackpackMenu(int containerId, Inventory playerInv) {
		this(containerId, playerInv, new BackpackInventory(), ItemStack.EMPTY);
	}

	/** Constructor del servidor. */
	public BackpackMenu(int containerId, Inventory playerInv, BackpackInventory inv, ItemStack backpack) {
		super(ModMenus.BACKPACK, containerId);
		this.inv = inv;
		this.backpack = backpack;
		this.player = playerInv.player;
		this.grid = new TransientCraftingContainer(this, 3, 3);
		// La rejilla del inventario pasa a compartir la lista interna del contenedor,
		// para que lo que se haga en la mesa se guarde en la mochila.
		java.util.List<ItemStack> gridItems = this.grid.getItems();
		for (int i = 0; i < BackpackInventory.GRID_SIZE; i++) {
			gridItems.set(i, inv.grid.get(i));
		}
		inv.grid = gridItems;

		// 27 espacios de almacenamiento
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inv.main, col + row * 9, 8 + col * 18, 18 + row * 18) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return !(stack.getItem() instanceof BackpackItem);
					}
				});
			}
		}

		// 2 espacios de plugin
		addSlot(new PluginSlot(BackpackInventory.PLUGIN_A, SIDE_X, 18));
		addSlot(new PluginSlot(BackpackInventory.PLUGIN_B, SIDE_X + 22, 18));

		// mini interfaz de crafteo
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				addSlot(new FeatureSlot(grid, col + row * 3, SIDE_X + col * 18, 46 + row * 18, PluginType.CRAFTING, false));
			}
		}
		addSlot(new CraftResultSlot(player, grid, result, 0, SIDE_X + 74, 64));

		// mini interfaz de horno
		addSlot(new FeatureSlot(inv.furnace, 0, SIDE_X, 108, PluginType.HORN, false));
		addSlot(new FeatureSlot(inv.furnace, 1, SIDE_X, 144, PluginType.HORN, false));
		addSlot(new FeatureSlot(inv.furnace, 2, SIDE_X + 64, 126, PluginType.HORN, true));

		addStandardInventorySlots(playerInv, 8, 85);
		addDataSlots(inv.progressData());

		if (!player.level().isClientSide()) {
			result.setItem(0, computeResult());
		}
	}

	public ItemStack backpack() {
		return backpack;
	}

	public BackpackInventory inventory() {
		return inv;
	}

	public boolean hasPlugin(PluginType type) {
		return inv.hasPlugin(type);
	}

	/** progreso de llama (0..px) */
	public int litProgress(int px) {
		int duration = inv.litDuration == 0 ? 200 : inv.litDuration;
		return Math.min(px, inv.litTime * px / duration);
	}

	/** progreso de flecha (0..px) */
	public int cookProgress(int px) {
		return inv.cookTotal == 0 ? 0 : Math.min(px, inv.cookTime * px / inv.cookTotal);
	}

	// ---------- crafteo ----------

	private ItemStack computeResult() {
		if (!(player.level() instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
			return ItemStack.EMPTY;
		}
		if (!inv.hasPlugin(PluginType.CRAFTING)) return ItemStack.EMPTY;
		CraftingInput input = grid.asCraftInput();
		Optional<RecipeHolder<CraftingRecipe>> recipe =
				level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
		if (recipe.isPresent() && result.setRecipeUsed(serverPlayer, recipe.get())) {
			return recipe.get().value().assemble(input);
		}
		return ItemStack.EMPTY;
	}

	private void updateResult() {
		if (!(player instanceof ServerPlayer serverPlayer)) return;
		ItemStack out = computeResult();
		result.setItem(0, out);
		serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT, out));
	}

	@Override
	public void slotsChanged(Container container) {
		if (!player.level().isClientSide() && (container == grid || container == inv.main)) {
			updateResult();
		}
		inv.save();
		super.slotsChanged(container);
	}

	// ---------- ciclo de vida ----------

	@Override
	public boolean stillValid(Player player) {
		return backpack.isEmpty() || BackpackHelper.isHeld(player, backpack);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		inv.save();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;

		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();

		if (index == RESULT) {
			if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
			slot.onQuickCraft(stack, copy);
		} else if (index < PLAYER_START) {
			if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
		} else if (stack.getItem() instanceof PluginItem && moveItemStackTo(stack, PLUGIN_A, PLUGIN_B + 1, false)) {
			// movido a un espacio de plugin
		} else if (!moveItemStackTo(stack, 0, STORAGE_END, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();

		if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;

		slot.onTake(player, stack);
		if (index == RESULT) player.drop(stack, false);
		return copy;
	}

	// ---------- slots ----------

	/** Espacio de plugin: solo acepta PluginItem y nunca dos del mismo tipo. */
	private class PluginSlot extends Slot {
		PluginSlot(int index, int x, int y) {
			super(inv.main, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			if (!(stack.getItem() instanceof PluginItem plugin)) return false;
			int other = getContainerSlot() == BackpackInventory.PLUGIN_A ? BackpackInventory.PLUGIN_B : BackpackInventory.PLUGIN_A;
			return !(inv.main.getItem(other).getItem() instanceof PluginItem otherPlugin && otherPlugin.type() == plugin.type());
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		/** No se puede quitar un plugin mientras su mini interfaz tenga items (evita perderlos). */
		@Override
		public boolean mayPickup(Player player) {
			return !(getItem().getItem() instanceof PluginItem plugin) || !inv.pluginHasContents(plugin.type());
		}
	}

	/** Slot de una mini interfaz: solo activo si el plugin correspondiente esta instalado. */
	private class FeatureSlot extends Slot {
		private final PluginType type;
		private final boolean outputOnly;

		FeatureSlot(Container container, int index, int x, int y, PluginType type, boolean outputOnly) {
			super(container, index, x, y);
			this.type = type;
			this.outputOnly = outputOnly;
		}

		@Override
		public boolean isActive() {
			return inv.hasPlugin(type);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return !outputOnly && isActive();
		}

		@Override
		public boolean mayPickup(Player player) {
			return isActive();
		}
	}

	private class CraftResultSlot extends ResultSlot {
		CraftResultSlot(Player player, TransientCraftingContainer grid, Container result, int index, int x, int y) {
			super(player, grid, result, index, x, y);
		}

		@Override
		public boolean isActive() {
			return inv.hasPlugin(PluginType.CRAFTING);
		}

		@Override
		public boolean mayPickup(Player player) {
			return isActive() && super.mayPickup(player);
		}
	}
}
