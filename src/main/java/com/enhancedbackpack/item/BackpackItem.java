package com.enhancedbackpack.item;

import com.enhancedbackpack.inventory.BackpackHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class BackpackItem extends Item {
	public BackpackItem(Properties properties) {
		super(properties);
	}

	/** Click derecho abre la mochila. Con shift se equipa en la ranura de pecho (comportamiento de Equippable). */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.isShiftKeyDown()) {
			return super.use(level, player, hand);
		}
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			BackpackHelper.open(serverPlayer, player.getItemInHand(hand));
		}
		return InteractionResult.SUCCESS;
	}
}
