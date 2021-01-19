package de.siphalor.nmuk.api;

import de.siphalor.nmuk.impl.IKeyBinding;
import de.siphalor.nmuk.impl.NMUKKeyBindingHelper;
import de.siphalor.nmuk.impl.mixin.KeyBindingAccessor;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;

/**
 * Main API class of NMUK (No More Useless Keys).<br />
 * The methods defined in this class allow you to define alternatives to existing keybindings.
 */
public class NMUKAlternatives {
	/**
	 * Create an alternative keybinding with the given code and {@link InputUtil.Type#KEYSYM}.
	 * @param base The base keybinding to create an alternative for
	 * @param code The keycode to use as default for the alternative
	 */
	public static void create(KeyBinding base, int code) {
		create(base, InputUtil.Type.KEYSYM, code);
	}

	/**
	 * Create an alternative keybinding with the given code and input type.
	 * @param base The base keybinding to create an alternative for
	 * @param inputType The {@link InputUtil.Type} that defines the type of the code
	 * @param code The input code
	 */
	public static void create(KeyBinding base, InputUtil.Type inputType, int code) {
		KeyBinding alternative = NMUKKeyBindingHelper.createAlternativeKeyBinding(base, inputType, code);
		NMUKKeyBindingHelper.registerKeyBinding(alternative);
		NMUKKeyBindingHelper.defaultAlternatives.put(base, alternative);
	}

	/**
	 * Register and add the latter keybinding to the former.<br />
	 * This is useful when using more complex keybinding trigger, e.g. in use with Amces.<br />
	 * The translation key and the category of the alternative keybinding will be rewritten
	 * and as such it must not be registered yet.
	 * @param base The base keybinding to create an alternative for
	 * @param alternative The alternative keybinding. This keybinding MUST NOT be registered yet
	 */
	public static void create(KeyBinding base, KeyBinding alternative) {
		((KeyBindingAccessor) alternative).setId(base.getId() + "%" + ((IKeyBinding) base).nmuk_getNextChildId());
		((KeyBindingAccessor) alternative).setCategory(base.getCategory());
		((IKeyBinding) base).nmuk_addAlternative(alternative);
		((IKeyBinding) alternative).nmuk_setParent(base);
		NMUKKeyBindingHelper.registerKeyBinding(alternative);
		NMUKKeyBindingHelper.defaultAlternatives.put(base, alternative);
	}
}
