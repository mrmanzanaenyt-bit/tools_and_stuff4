package com.enhancedbackpack.network;

import com.enhancedbackpack.EnhancedBackpack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenBackpackPayload() implements CustomPacketPayload {
	public static final OpenBackpackPayload INSTANCE = new OpenBackpackPayload();
	public static final CustomPacketPayload.Type<OpenBackpackPayload> TYPE =
			new CustomPacketPayload.Type<>(EnhancedBackpack.id("open_backpack"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenBackpackPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
