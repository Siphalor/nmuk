package de.siphalor.nmuk.impl.mixin;

import net.minecraft.client.options.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
	@Accessor
	static Map<String, KeyBinding> getKeysById() {
		return null;
	}

	@Accessor
	void setTranslationKey(String id);
	@Accessor
	void setCategory(String category);
	@Accessor
	int getTimesPressed();
	@Accessor
	void setTimesPressed(int timesPressed);
}
