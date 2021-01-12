package de.siphalor.nmuk.impl;

import net.minecraft.client.options.KeyBinding;

public class AlternativeKeyBinding extends KeyBinding {
	public AlternativeKeyBinding(KeyBinding parent, String translationKey, int code, String category) {
		super(translationKey, code, category);
		((IKeyBinding) this).nmuk_setParent(parent);
	}
}
