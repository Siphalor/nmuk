/*
 * Copyright 2021 Siphalor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

package de.siphalor.nmuk.api;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import de.siphalor.nmuk.impl.AlternativeKeyBinding;
import de.siphalor.nmuk.impl.IKeyBinding;
import de.siphalor.nmuk.impl.NMUKKeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/**
 * Main API class of NMUK (No More Useless Keys).<br />
 * The methods defined in this class allow you to define alternatives to existing keybindings.
 */
public class NMUKAlternatives {
	/**
	 * Create an alternative keybinding with the given code and {@link InputUtil.Type#KEYSYM}.
	 *
	 * @param base The base keybinding to create an alternative for
	 * @param code The keycode to use as default for the alternative
	 * @return the new created keybinding
	 */
	public static KeyBinding createAndGet(KeyBinding base, int code) {
		return createAndGet(base, InputUtil.Type.KEYSYM, code);
	}

	/**
	 * Create an alternative keybinding with the given code and {@link InputUtil.Type#KEYSYM}.
	 *
	 * @param base The base keybinding to create an alternative for
	 * @param code The keycode to use as default for the alternative
	 */
	@Deprecated
	public static void create(KeyBinding base, int code) {
		createAndGet(base, code);
	}

	/**
	 * Create an alternative keybinding with the given code and input type.
	 *
	 * @param base The base keybinding to create an alternative for
	 * @param inputType The {@link InputUtil.Type} that defines the type of the code
	 * @param code The input code
	 * @return the new created keybinding
	 */
	public static KeyBinding createAndGet(KeyBinding base, InputUtil.Type inputType, int code) {
		KeyBinding alternative = NMUKKeyBindingHelper.createAndAddAlternativeKeyBinding(base, inputType, code);
		NMUKKeyBindingHelper.defaultAlternatives.put(base, alternative);
		NMUKKeyBindingHelper.registerKeyBindingGUI(alternative);
		return alternative;
	}

	/**
	 * Create an alternative keybinding with the given code and input type.
	 *
	 * @param base The base keybinding to create an alternative for
	 * @param inputType The {@link InputUtil.Type} that defines the type of the code
	 * @param code The input code
	 */
	@Deprecated
	public static void create(KeyBinding base, InputUtil.Type inputType, int code) {
		createAndGet(base, inputType, code);
	}

	/**
	 * Register and add the latter keybinding to the former.<br />
	 * This is useful when using more complex keybinding trigger, e.g. in use with Amces.<br />
	 * The translation key and the category of the alternative keybinding will be rewritten.
	 * The keybinding might already be registered. In that case it is unregistered and registered again with the new identity.
	 *
	 * @param base The base keybinding to create an alternative for
	 * @param alternative The alternative keybinding
	 * @return the alternative keybinding given
	 */
	public static <K extends KeyBinding> K createAndGet(KeyBinding base, K alternative) {
		// unregister the alternative keybinding as it is known until now
		NMUKKeyBindingHelper.unregisterKeyBindingQuerying(alternative);
		NMUKKeyBindingHelper.unregisterKeyBindingGUI(alternative);

		NMUKKeyBindingHelper.makeKeyBindingAlternativeOf(base, alternative, AlternativeKeyBinding.NO_ALTERNATIVE_ID, true);
		NMUKKeyBindingHelper.defaultAlternatives.put(base, alternative);

		NMUKKeyBindingHelper.registerKeyBindingQuerying(alternative);
		NMUKKeyBindingHelper.registerKeyBindingGUI(alternative);
		return alternative;
	}

	/**
	 * Register and add the latter keybinding to the former.<br />
	 * This is useful when using more complex keybinding trigger, e.g. in use with Amces.<br />
	 * The translation key and the category of the alternative keybinding will be rewritten.
	 * The keybinding might already be registered. In that case it is unregistered and registered again with the new identity.
	 *
	 * @param base The base keybinding to create an alternative for
	 * @param alternative The alternative keybinding
	 */
	@Deprecated
	public static void create(KeyBinding base, KeyBinding alternative) {
		createAndGet(base, alternative);
	}

	/**
	 * Remove the given {@code alternative} keybinding from the default alternatives list of the base.
	 * The {@code alternative} keybinding will be unregistered if it was a default alternative of the base.
	 *
	 * @param base
	 * @param alternative
	 * @return whether the alternative was removed from the base. It will fail if the base did not contain the given {@code alternative} keybinding as an alternative
	 */
	public static boolean removeDefaultAlternative(KeyBinding base, KeyBinding alternative) {
		boolean ret = NMUKKeyBindingHelper.defaultAlternatives.remove(base, alternative);
		if (ret) {
			NMUKKeyBindingHelper.unregisterKeyBindingQuerying(alternative);
			NMUKKeyBindingHelper.unregisterKeyBindingGUI(alternative);
		}
		return ret;
	}

	/**
	 * Returns whether the given keybinding is an alternative.
	 *
	 * @param binding A keybinding
	 * @return whether the given keybinding is an alternative
	 */
	public static boolean isAlternative(KeyBinding binding) {
		return ((IKeyBinding) binding).nmuk_isAlternative();
	}

	/**
	 * Gets all alternatives that are registered for a keybinding.
	 *
	 * @param binding A keyinding
	 * @return A list of alternatives or <code>null</code>
	 */
	@Nullable
	public static List<KeyBinding> getAlternatives(KeyBinding binding) {
		return ((IKeyBinding) binding).nmuk_getAlternatives();
	}

	/**
	 * Gets the base keybinding for an alternative keybinding.
	 *
	 * @param binding An alternative keybinding
	 * @return The base keyinding or <code>null</code> if the given keybinding is no alternative
	 */
	public static KeyBinding getBase(KeyBinding binding) {
		return ((IKeyBinding) binding).nmuk_getParent();
	}
}
