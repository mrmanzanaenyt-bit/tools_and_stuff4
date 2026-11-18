package com.enhancedbackpack.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Datos guardados dentro del ItemStack de la mochila.
 * contents: 0-26 almacenamiento, 27-28 plugins, 29-37 rejilla de crafteo, 38-40 horno (input, combustible, output).
 */
public record BackpackState(ItemContainerContents contents, int litTime, int litDuration, int cookTime) {
	public static final BackpackState EMPTY = new BackpackState(ItemContainerContents.EMPTY, 0, 0, 0);

	public static final Codec<BackpackState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemContainerContents.CODEC.optionalFieldOf("items", ItemContainerContents.EMPTY).forGetter(BackpackState::contents),
			Codec.INT.optionalFieldOf("lit_time", 0).forGetter(BackpackState::litTime),
			Codec.INT.optionalFieldOf("lit_duration", 0).forGetter(BackpackState::litDuration),
			Codec.INT.optionalFieldOf("cook_time", 0).forGetter(BackpackState::cookTime)
	).apply(instance, BackpackState::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, BackpackState> STREAM_CODEC = StreamCodec.composite(
			ItemContainerContents.STREAM_CODEC, BackpackState::contents,
			ByteBufCodecs.VAR_INT, BackpackState::litTime,
			ByteBufCodecs.VAR_INT, BackpackState::litDuration,
			ByteBufCodecs.VAR_INT, BackpackState::cookTime,
			BackpackState::new);
}
