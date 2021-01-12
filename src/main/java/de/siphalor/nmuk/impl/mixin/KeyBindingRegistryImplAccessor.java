package de.siphalor.nmuk.impl.mixin;

import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.minecraft.client.options.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(KeyBindingRegistryImpl.class)
public interface KeyBindingRegistryImplAccessor {
	@Accessor
	static List<KeyBinding> getModdedKeyBindings() {
		return null;
	}
}
