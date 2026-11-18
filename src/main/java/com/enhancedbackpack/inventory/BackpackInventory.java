package com.enhancedbackpack.inventory;

import com.enhancedbackpack.EnhancedBackpack;
import com.enhancedbackpack.ModComponents;
import com.enhancedbackpack.item.PluginItem;
import com.enhancedbackpack.item.PluginType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/**
 * Contenido en memoria de una mochila. Se carga desde el ItemStack y se guarda de vuelta con save().
 * Tambien contiene la logica del horno (sin BlockEntity).
 */
public class BackpackInventory {
	public static final int STORAGE_SIZE = 27;
	public static final int PLUGIN_A = 27;
	public static final int PLUGIN_B = 28;
	public static final int MAIN_SIZE = 29;
	public static final int GRID_SIZE = 9;
	public static final int FURNACE_SIZE = 3;
	public static final int TOTAL = MAIN_SIZE + GRID_SIZE + FURNACE_SIZE;

	/** 0-26 almacenamiento, 27-28 plugins */
	public final SimpleContainer main = new SimpleContainer(MAIN_SIZE) {
		@Override
		public void setChanged() {
			super.setChanged();
			BackpackInventory.this.save();
		}
	};
	/** rejilla 3x3 del plugin de crafteo */
	public List<ItemStack> grid = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
	/** 0 input, 1 combustible, 2 output */
	public final SimpleContainer furnace = new SimpleContainer(FURNACE_SIZE) {
		@Override
		public void setChanged() {
			super.setChanged();
			BackpackInventory.this.save();
		}
	};

	public int litTime;
	public int litDuration;
	public int cookTime;
	public int cookTotal;

	private ItemStack bound = ItemStack.EMPTY;

	// ---------- carga / guardado ----------

	public static BackpackInventory load(ItemStack stack) {
		BackpackInventory inv = new BackpackInventory();
		BackpackState state = stack.getOrDefault(ModComponents.BACKPACK_STATE, BackpackState.EMPTY);
		NonNullList<ItemStack> all = NonNullList.withSize(TOTAL, ItemStack.EMPTY);
		state.contents().copyInto(all);
		for (int i = 0; i < MAIN_SIZE; i++) inv.main.setItem(i, all.get(i));
		for (int i = 0; i < GRID_SIZE; i++) inv.grid.set(i, all.get(MAIN_SIZE + i));
		for (int i = 0; i < FURNACE_SIZE; i++) inv.furnace.setItem(i, all.get(MAIN_SIZE + GRID_SIZE + i));
		inv.litTime = state.litTime();
		inv.litDuration = state.litDuration();
		inv.cookTime = state.cookTime();
		return inv;
	}

	/** Despues de bind(), cada cambio se guarda automaticamente en el stack. */
	public BackpackInventory bind(ItemStack stack) {
		this.bound = stack;
		return this;
	}

	public ItemStack bound() {
		return bound;
	}

	public BackpackState toState() {
		List<ItemStack> list = new ArrayList<>(TOTAL);
		for (int i = 0; i < MAIN_SIZE; i++) list.add(main.getItem(i));
		list.addAll(grid);
		for (int i = 0; i < FURNACE_SIZE; i++) list.add(furnace.getItem(i));
		return new BackpackState(ItemContainerContents.fromItems(list), litTime, litDuration, cookTime);
	}

	public void save() {
		if (bound.isEmpty()) return;
		bound.set(ModComponents.BACKPACK_STATE, toState());
		bound.set(DataComponents.ITEM_MODEL, modelId());
	}

	// ---------- plugins ----------

	public boolean hasPlugin(PluginType type) {
		return isPlugin(main.getItem(PLUGIN_A), type) || isPlugin(main.getItem(PLUGIN_B), type);
	}

	private static boolean isPlugin(ItemStack stack, PluginType type) {
		return stack.getItem() instanceof PluginItem plugin && plugin.type() == type;
	}

	/** true si el plugin no se puede quitar porque su mini-interfaz aun tiene items. */
	public boolean pluginHasContents(PluginType type) {
		if (type == PluginType.CRAFTING) {
			for (ItemStack s : grid) if (!s.isEmpty()) return true;
		} else {
			for (int i = 0; i < FURNACE_SIZE; i++) if (!furnace.getItem(i).isEmpty()) return true;
		}
		return false;
	}

	/** Modelo de item segun los plugins instalados (items/*.json). */
	public Identifier modelId() {
		boolean horn = hasPlugin(PluginType.HORN);
		boolean craft = hasPlugin(PluginType.CRAFTING);
		String name = horn && craft ? "backpack_craft_horn" : craft ? "backpack_craft" : horn ? "backpack_horn" : "backpack";
		return EnhancedBackpack.id(name);
	}

	// ---------- datos sincronizados con el cliente (llama de la barra de progreso) ----------

	public ContainerData progressData() {
		return new ContainerData() {
			@Override
			public int get(int index) {
				return switch (index) {
					case 0 -> litTime;
					case 1 -> litDuration;
					case 2 -> cookTime;
					default -> cookTotal;
				};
			}

			@Override
			public void set(int index, int value) {
				switch (index) {
					case 0 -> litTime = value;
					case 1 -> litDuration = value;
					case 2 -> cookTime = value;
					default -> cookTotal = value;
				}
			}

			@Override
			public int getCount() {
				return 4;
			}
		};
	}

	// ---------- horno ----------

	/** Un tick de horno. Devuelve true si algo cambio. */
	public boolean tickFurnace(ServerLevel level) {
		if (!hasPlugin(PluginType.HORN)) {
			if (litTime > 0 || cookTime > 0) {
				litTime = 0;
				cookTime = 0;
				save();
				return true;
			}
			return false;
		}

		boolean changed = false;
		if (litTime > 0) {
			litTime--;
			changed = true;
		}

		ItemStack input = furnace.getItem(0);
		ItemStack fuel = furnace.getItem(1);
		ItemStack output = furnace.getItem(2);

		RecipeHolder<SmeltingRecipe> holder = null;
		ItemStack result = ItemStack.EMPTY;
		if (!input.isEmpty()) {
			SingleRecipeInput recipeInput = new SingleRecipeInput(input);
			Optional<RecipeHolder<SmeltingRecipe>> found =
					level.recipeAccess().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
			if (found.isPresent()) {
				holder = found.get();
				result = holder.value().assemble(recipeInput);
			}
		}

		boolean canSmelt = holder != null && canOutput(result, output);

		if (litTime == 0 && canSmelt && !fuel.isEmpty()) {
			int burn = level.fuelValues().burnDuration(fuel);
			if (burn > 0) {
				litTime = burn;
				litDuration = burn;
				ItemStack remainder = fuel.is(Items.LAVA_BUCKET) ? new ItemStack(Items.BUCKET) : ItemStack.EMPTY;
				fuel.shrink(1);
				if (fuel.isEmpty()) furnace.setItem(1, remainder);
				else furnace.setChanged();
				changed = true;
			}
		}

		if (litTime > 0 && canSmelt) {
			cookTotal = holder.value().cookingTime();
			cookTime++;
			changed = true;
			if (cookTime >= cookTotal) {
				cookTime = 0;
				if (output.isEmpty()) furnace.setItem(2, result.copy());
				else output.grow(result.getCount());
				input.shrink(1);
				if (input.isEmpty()) furnace.setItem(0, ItemStack.EMPTY);
				furnace.setChanged();
			}
		} else if (cookTime > 0) {
			cookTime = 0;
			changed = true;
		}

		if (changed) save();
		return changed;
	}

	private static boolean canOutput(ItemStack result, ItemStack output) {
		if (result.isEmpty()) return false;
		if (output.isEmpty()) return true;
		return ItemStack.isSameItemSameComponents(output, result)
				&& output.getCount() + result.getCount() <= output.getMaxStackSize();
	}
}
