package de.siphalor.nmuk.mixin;

import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameOptions.class)
public interface GameOptionsAccessor {
	@Accessor
	KeyBinding[] getKeysAll();
	@Accessor
	void setKeysAll(KeyBinding[] bindings);
}
