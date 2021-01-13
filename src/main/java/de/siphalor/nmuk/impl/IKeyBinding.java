package de.siphalor.nmuk.impl;

import net.minecraft.client.options.KeyBinding;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public interface IKeyBinding {
	short nmuk_getNextChildId();
	void nmuk_setNextChildId(short nextChildId);
	boolean nmuk_isAlternative();
	KeyBinding nmuk_getParent();
	void nmuk_setParent(KeyBinding binding);
	List<KeyBinding> nmuk_getAlternatives();
	int nmuk_getAlternativesCount();
	void nmuk_removeAlternative(KeyBinding binding);
	void nmuk_addAlternative(KeyBinding binding);
	int nmuk_getIndexInParent();
}
