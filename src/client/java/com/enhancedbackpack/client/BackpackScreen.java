package com.enhancedbackpack.client;

import com.enhancedbackpack.item.PluginType;
import com.enhancedbackpack.menu.BackpackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
	private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

	private static final int MAIN_WIDTH = 176;
	private static final int PANEL_X = 178;
	private static final int PANEL_WIDTH = 106;

	public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, PANEL_X + PANEL_WIDTH, 168);
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int x = this.leftPos;
		int y = this.topPos;

		// Parte principal: mismo fondo que un cofre de 3 filas
		graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y, 0.0F, 0.0F, MAIN_WIDTH, 71, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y + 71, 0.0F, 126.0F, MAIN_WIDTH, 96, 256, 256);

		// Panel lateral de plugins
		drawPanel(graphics, x + PANEL_X, y, PANEL_WIDTH, this.imageHeight);
		for (Slot slot : this.menu.slots) {
			if (slot.x >= PANEL_X && slot.isActive()) {
				drawSlot(graphics, x + slot.x, y + slot.y);
			}
		}
		// Los 2 espacios de plugin siempre se ven
		for (int i : new int[] {BackpackMenu.PLUGIN_A, BackpackMenu.PLUGIN_B}) {
			Slot slot = this.menu.slots.get(i);
			drawSlot(graphics, x + slot.x, y + slot.y);
		}

		int sx = x + BackpackMenu.SIDE_X;

		if (this.menu.hasPlugin(PluginType.CRAFTING)) {
			// flecha grid -> resultado
			graphics.fill(sx + 58, y + 72, sx + 70, y + 74, 0xFF555555);
		}

		if (this.menu.hasPlugin(PluginType.HORN)) {
			// llama entre input y combustible
			int fx = sx + 1;
			int fy = y + 127;
			graphics.fill(fx, fy, fx + 14, fy + 14, 0xFF373737);
			int lit = this.menu.litProgress(14);
			if (lit > 0) graphics.fill(fx, fy + 14 - lit, fx + 14, fy + 14, 0xFFFF8A00);
			// flecha de progreso input -> output
			int ax = sx + 24;
			int ay = y + 130;
			graphics.fill(ax, ay, ax + 30, ay + 6, 0xFF373737);
			int cook = this.menu.cookProgress(30);
			if (cook > 0) graphics.fill(ax, ay, ax + cook, ay + 6, 0xFFFFFFFF);
		}
	}

	private static void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, 0xFF000000);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
		g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFFC6C6C6);
	}

	private static void drawSlot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
		g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
	}
}
