package de.siphalor.nmuk.api;

import de.siphalor.nmuk.impl.NMUKKeyBindingHelper;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class NMUKAlternatives {
	public static void create(KeyBinding base, int code) {
		create(base, InputUtil.Type.KEYSYM, code);
	}

	public static void create(KeyBinding base, InputUtil.Type inputType, int code) {
		KeyBinding alternative = NMUKKeyBindingHelper.createAlternativeKeyBinding(base, inputType, code);
		NMUKKeyBindingHelper.registerKeyBinding(alternative);
		NMUKKeyBindingHelper.defaultAlternatives.put(base, alternative);
	}
}
