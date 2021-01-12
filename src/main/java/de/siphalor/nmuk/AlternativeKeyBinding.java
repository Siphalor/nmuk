package de.siphalor.nmuk;

import de.siphalor.nmuk.util.IKeyBinding;
import net.minecraft.client.options.KeyBinding;

public class AlternativeKeyBinding extends KeyBinding {
	public AlternativeKeyBinding(KeyBinding parent, String translationKey, int code, String category) {
		super(translationKey, code, category);
		((IKeyBinding) this).nmuk_setParent(parent);
	}
}
