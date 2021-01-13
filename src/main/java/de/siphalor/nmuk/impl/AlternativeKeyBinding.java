package de.siphalor.nmuk.impl;

import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class AlternativeKeyBinding extends KeyBinding {
	public AlternativeKeyBinding(KeyBinding parent, String translationKey, int code, String category) {
		super(translationKey, code, category);
		((IKeyBinding) this).nmuk_setParent(parent);
	}

	public AlternativeKeyBinding(KeyBinding parent, String translationKey, InputUtil.Type type, int code, String category) {
		super(translationKey, type, code, category);
		((IKeyBinding) this).nmuk_setParent(parent);
	}

	@Override
	public boolean isDefault() {
		if (getDefaultKey() == InputUtil.UNKNOWN_KEY) {
			return true;
		}
		return super.isDefault();
	}
}
