package de.siphalor.nmuktestmod;

import de.siphalor.nmuk.api.NMUKAlternatives;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class NMUKTestMod implements ModInitializer {
	public static final String MOD_ID = "nmuk_testmod";

	@Override
	public void onInitialize() {
		KeyBinding kbd = KeyBindingHelper.registerKeyBinding(new KeyBinding(MOD_ID + ".test", InputUtil.Type.KEYSYM, 86, "key.categories.movement"));
		NMUKAlternatives.create(kbd, 85);
	}
}
