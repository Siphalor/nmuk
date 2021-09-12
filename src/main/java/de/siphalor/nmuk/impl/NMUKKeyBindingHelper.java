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

package de.siphalor.nmuk.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.ApiStatus;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import de.siphalor.nmuk.NMUK;
import de.siphalor.nmuk.impl.ErrorMessageToast.Type;
import de.siphalor.nmuk.impl.mixin.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.widget.EntryListWidget.Entry;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

@ApiStatus.Internal
public class NMUKKeyBindingHelper {

	private static final Constructor<ControlsListWidget.KeyBindingEntry> KeyBindingEntry_contructor;

	static {
		Constructor<ControlsListWidget.KeyBindingEntry> local_KeyBindingEntry_contructor = null;
		try {
			// noinspection JavaReflectionMemberAccess
			local_KeyBindingEntry_contructor = ControlsListWidget.KeyBindingEntry.class.getDeclaredConstructor(
				ControlsListWidget.class, KeyBinding.class, Text.class);
			local_KeyBindingEntry_contructor.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			NMUK.log(Level.ERROR, "Failed to load constructor from class \"KeyBindingEntry\" with reflection");
			e.printStackTrace();
		}

		KeyBindingEntry_contructor = local_KeyBindingEntry_contructor;
	}

	private static void changeKeysAll(GameOptionsAccessor options, Function<KeyBinding[], KeyBinding[]> changeFunction) {
		options.setKeysAll(changeFunction.apply(options.getKeysAll()));
	}

	private static void changeKeysAll(Function<KeyBinding[], KeyBinding[]> changeFunction) {
		changeKeysAll(getGameOptionsAccessor(), changeFunction);
	}

	private static GameOptionsAccessor getGameOptionsAccessor() {
		return (GameOptionsAccessor) MinecraftClient.getInstance().options;
	}

	public static final Multimap<KeyBinding, KeyBinding> defaultAlternatives = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);

	public static void removeKeyBinding(KeyBinding binding) {
		List<KeyBinding> moddedKeyBindings = KeyBindingRegistryImplAccessor.getModdedKeyBindings();
		{
			// noinspection ConstantConditions
			boolean success = moddedKeyBindings.remove(binding);
			if (!success) {
				NMUK.log(Level.ERROR, "Failed to remove modded keybinding!");
			}
		}

		changeKeysAll(keysAll -> ArrayUtils.removeElement(keysAll, binding));
		KeyBinding.updateKeysByCode();
	}

	public static void registerKeyBinding(KeyBinding binding) {
		KeyBindingHelper.registerKeyBinding(binding);
		GameOptionsAccessor options = getGameOptionsAccessor();
		if (options != null) { // Game is during initialization - this is handled by Fapi already
			changeKeysAll(options, keysAll -> ArrayUtils.add(keysAll, binding));
		}
		KeyBinding.updateKeysByCode();
	}

	public static void registerKeyBindings(GameOptions gameOptions, Collection<KeyBinding> bindings) {
		for (KeyBinding binding : bindings) {
			KeyBindingHelper.registerKeyBinding(binding);
		}
		changeKeysAll((GameOptionsAccessor) gameOptions, keysAll -> ArrayUtils.addAll(keysAll, bindings.toArray(KeyBinding[]::new)));
		KeyBinding.updateKeysByCode();
	}

	public static void resetSingleKeyBinding(KeyBinding keyBinding) {
		keyBinding.setBoundKey(keyBinding.getDefaultKey());
	}

	public static KeyBinding createAlternativeKeyBinding(KeyBinding base) {
		return createAlternativeKeyBinding(base, -1);
	}

	public static KeyBinding createAlternativeKeyBinding(KeyBinding base, int code) {
		return createAlternativeKeyBinding(base, InputUtil.Type.KEYSYM, code);
	}

	public static KeyBinding findMatchingAlternativeInBase(KeyBinding base, int alternativeId) {
		IKeyBinding parent = (IKeyBinding) base;
		List<KeyBinding> alternatives = parent.nmuk_getAlternatives();
		return findMatchingAlternative(alternatives, base.getTranslationKey(), alternativeId);
	}

	public static KeyBinding findMatchingAlternative(List<KeyBinding> alternatives, String tanslationKey, int alternativeId) {
		if (alternatives == null) {
			return null;
		}
		String searchTranslationKey = AlternativeKeyBinding.makeAlternativeKeyTranslationKey(tanslationKey, alternativeId);
		for (KeyBinding alternative : alternatives) {
			if (alternative.getTranslationKey().equals(searchTranslationKey)) {
				return alternative;
			}
		}
		return null;
	}

	public static KeyBinding createAlternativeKeyBinding(KeyBinding base, InputUtil.Type type, int code) {
		IKeyBinding parent = (IKeyBinding) base;
		KeyBinding alt = new AlternativeKeyBinding(base, base.getTranslationKey(), parent.nmuk_getNextChildId(), type, code, base.getCategory());
		parent.nmuk_addAlternative(alt);
		return alt;
	}

	// for options gui
	public static final Text ENTRY_NAME = new LiteralText("    ->");
	public static final Text ADD_ALTERNATIVE_TEXT = new LiteralText("+");
	public static final Text REMOVE_ALTERNATIVE_TEXT = new LiteralText("x");
	public static final Text RESET_TOOLTIP = new TranslatableText("nmuk.options.controls.reset.tooltip");
	public static final Text DEFAULT_KEYBINDING_ENTRY_TEXT = new LiteralText("...");

	private static void saveOptions() {
		MinecraftClient.getInstance().options.write();
	}

	public static ControlsListWidget getControlsListWidgetFromCurrentScreen(Predicate<ControlsOptionsScreen> ifFunction) {
		Screen screen = MinecraftClient.getInstance().currentScreen;
		if (screen instanceof ControlsOptionsScreen && ifFunction.test((ControlsOptionsScreen) screen)) {
			return ((ControlsOptionsScreenAccessor) screen).getKeyBindingListWidget();
		}
		return null;
	}

	public static ControlsListWidget.KeyBindingEntry findKeyBindingEntry(KeyBinding keyBinding, ControlsListWidget listWidget) {
		List<Entry<?>> entries = getControlsListWidgetEntries(listWidget);
		for (Entry<?> e : entries) {
			if (e instanceof ControlsListWidget.KeyBindingEntry) {
				if (((KeyBindingEntryAccessor) e).getBinding() == keyBinding) {
					return (ControlsListWidget.KeyBindingEntry) e;
				}
			}
		}
		return null;
	}

	public static void removeAlternativeKeyBinding_OptionsScreen(KeyBinding keyBinding, ControlsListWidget listWidget, ControlsListWidget.KeyBindingEntry entry) {
		// if this method is called outside of the gui code and search the gui
		if (listWidget == null) {
			listWidget = getControlsListWidgetFromCurrentScreen(controlsOptionScreen -> controlsOptionScreen.focusedBinding == keyBinding);
			// not found
			if (listWidget == null) {
				return;
			}
		}

		// if the entry is not given search the entry with this keyBinding
		if (entry == null) {
			entry = findKeyBindingEntry(keyBinding, listWidget);
			// not found
			if (entry == null) {
				return;
			}
		}

		((IKeyBinding) ((IKeyBinding) keyBinding).nmuk_getParent()).nmuk_removeAlternative(keyBinding);
		removeKeyBinding(keyBinding);
		// do it like vanilla and save directly
		saveOptions();

		List<Entry<?>> entries = getControlsListWidgetEntries(listWidget);
		entries.remove(entry);
	}

	private static int getExistingKeyIsUnboundIndex(KeyBinding binding) {
		if (binding.isUnbound()) {
			return -1;
		}
		List<KeyBinding> alternatives = ((IKeyBinding) binding).nmuk_getAlternatives();
		if (alternatives != null) {
			Optional<KeyBinding> unboundAlternative = alternatives.stream().filter(alternative -> alternative.isUnbound()).findFirst();
			if (unboundAlternative.isPresent()) {
				return ((IKeyBinding) unboundAlternative.get()).nmuk_getIndexInParent();
			}
		}
		return -2;
	}

	private static boolean showToastIfExistingKeyIsUnbound(KeyBinding binding) {
		int index = getExistingKeyIsUnboundIndex(binding);
		if (index != -2) {
			MinecraftClient client = MinecraftClient.getInstance();
			boolean isMainKey = index == -1;
			Object[] args = new Object[isMainKey ? 0 : 1];
			if (!isMainKey) {
				args[0] = index;
			}
			ErrorMessageToast.show(client.getToastManager(), isMainKey ? Type.MAIN_KEY_UNBOUND : Type.CHILDREN_KEY_UNBOUND_TRANSLATION_KEY, args);
			return true;
		}
		return false;
	}

	public static ControlsListWidget.KeyBindingEntry addNewAlternativeKeyBinding_OptionsScreen(KeyBinding baseKeyBinding, ControlsListWidget listWidget, ControlsListWidget.KeyBindingEntry entry) {
		if (showToastIfExistingKeyIsUnbound(baseKeyBinding)) {
			return null;
		}

		KeyBinding altBinding = createAlternativeKeyBinding(baseKeyBinding);
		registerKeyBinding(altBinding);
		ControlsListWidget.KeyBindingEntry altEntry = createKeyBindingEntry(listWidget, altBinding, DEFAULT_KEYBINDING_ENTRY_TEXT);
		if (altEntry != null) {
			List<Entry<?>> entries = getControlsListWidgetEntries(listWidget);
			for (int i = 0, entriesSize = entries.size(); i < entriesSize; i++) {
				if (entries.get(i) == entry) {
					i += ((IKeyBinding) baseKeyBinding).nmuk_getAlternativesCount();
					entries.add(i, altEntry);
					return altEntry;
				}
			}
		}
		NMUK.log(Level.ERROR, "Failed to create new KeyBindingEntry in options GUI!!");
		return null;
	}

	public static void resetAlternativeKeyBindings_OptionsScreen(KeyBinding baseKeyBinding, ControlsListWidget listWidget, ControlsListWidget.KeyBindingEntry entry) {
		List<KeyBinding> alternatives = ((IKeyBinding) baseKeyBinding).nmuk_getAlternatives();
		List<KeyBinding> defaultAlternatives = new ArrayList<>(NMUKKeyBindingHelper.defaultAlternatives.get(baseKeyBinding));
		List<Entry<?>> entries = getControlsListWidgetEntries(listWidget);
		int entryPos = entries.indexOf(entry);

		boolean changed = false;
		int index;
		for (Iterator<KeyBinding> iterator = alternatives.iterator(); iterator.hasNext();) {
			KeyBinding alternative = iterator.next();
			index = defaultAlternatives.indexOf(alternative);
			if (index == -1) {
				entries.remove(entryPos + 1 + ((IKeyBinding) alternative).nmuk_getIndexInParent());
				iterator.remove();
				removeKeyBinding(alternative);
			} else {
				defaultAlternatives.remove(index);
				resetSingleKeyBinding(alternative);
			}
			changed = true;
		}
		entryPos += alternatives.size();

		registerKeyBindings(MinecraftClient.getInstance().options, defaultAlternatives);
		for (KeyBinding defaultAlternative : defaultAlternatives) {
			ControlsListWidget.KeyBindingEntry newEntry = createKeyBindingEntry(listWidget, defaultAlternative, ENTRY_NAME);
			entries.add(++entryPos, newEntry);
			resetSingleKeyBinding(defaultAlternative);
			changed = true;
		}
		if (changed) {
			// do it like vanilla and save directly
			saveOptions();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Entry<?>> getControlsListWidgetEntries(ControlsListWidget controlsList) {
		return ((EntryListWidgetAccessor) controlsList).getChildren();
	}

	public static ControlsListWidget.KeyBindingEntry createKeyBindingEntry(ControlsListWidget listWidget, KeyBinding binding, Text text) {
		try {
			return KeyBindingEntry_contructor.newInstance(listWidget, binding, text);
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			NMUK.log(Level.ERROR, "Failed to create new instance of \"KeyBindingEntry\"");
			e.printStackTrace();
		}
		return null;
	}
}
