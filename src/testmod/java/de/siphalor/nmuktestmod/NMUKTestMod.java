package de.siphalor.nmuktestmod;

import de.siphalor.amecs.api.AmecsKeyBinding;
import de.siphalor.amecs.api.KeyModifiers;
import de.siphalor.nmuk.api.NMUKAlternatives;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class NMUKTestMod implements ModInitializer {
	public static final String MOD_ID = "nmuk_testmod";

	@Override
	public void onInitialize() {
		KeyBinding kbd = KeyBindingHelper.registerKeyBinding(new KeyBinding(MOD_ID + ".test", InputUtil.Type.KEYSYM, 86, "key.categories.movement"));
		NMUKAlternatives.create(kbd, 85);
		NMUKAlternatives.create(kbd, new AmecsKeyBinding(new Identifier(MOD_ID, ""), InputUtil.Type.KEYSYM, 86, "", new KeyModifiers(false, true, true)));
	}
}
