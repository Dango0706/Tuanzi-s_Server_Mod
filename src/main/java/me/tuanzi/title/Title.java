package me.tuanzi.title;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record Title(String id, Component displayName) {
    public static final Codec<Title> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(Title::id),
                    ComponentSerialization.CODEC.fieldOf("displayName").forGetter(Title::displayName)
            ).apply(instance, Title::new)
    );
}
