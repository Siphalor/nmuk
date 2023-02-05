package de.siphalor.nmuk.impl;

import de.siphalor.amecs.api.KeyBindingUtils;
import net.minecraft.client.options.KeyBinding;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class AmecsProxy {
	public static void resetKeyModifiers(KeyBinding keyBinding) {
		KeyBindingUtils.resetBoundModifiers(keyBinding);
	}
}
