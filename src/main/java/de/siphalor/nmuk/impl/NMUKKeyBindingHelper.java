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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.ApiStatus;

import com.google.common.collect.ListMultimap;
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
import net.minecraft.client.util.InputUtil.Key;
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

	public static final ListMultimap<KeyBinding, KeyBinding> defaultAlternatives = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);

	// register/unregister keybindings
	public static void registerKeyBindingGUI(KeyBinding binding) {
		KeyBindingHelper.registerKeyBinding(binding); // this adds the keybinding to the moddedKeybindings list and adds the category to the options gui
		GameOptionsAccessor options = getGameOptionsAccessor();
		if (options != null) { // if options == null: Game is during initialization - this is handled by Fabric Api already
			// this adds the keybindings to the options gui
			changeKeysAll(options, keysAll -> ArrayUtils.add(keysAll, binding));
		}
	}

	public static void unregisterKeyBindingGUI(KeyBinding binding) {
		removeKeyBindingGUI(getGameOptionsAccessor(), binding);
	}

	public static void removeKeyBindingGUI(GameOptionsAccessor options, KeyBinding binding) {
		// remove it from fabrics moddedKeyBindings list. Or at least try to. Maybe it is not a modded keybinding
		List<KeyBinding> moddedKeyBindings = KeyBindingRegistryImplAccessor.getModdedKeyBindings();
		moddedKeyBindings.remove(binding);

		if (options != null) { // if options == null: Game is during initialization - this is handled by Fabric Api already
			// this removes the keybinding from the options gui
			changeKeysAll(options, keysAll -> ArrayUtils.removeElement(keysAll, binding));
		}
	}

	private static void changeKeysById(Consumer<Map<String, KeyBinding>> changeFunction) {
		// get the keybinding from the input query list
		Map<String, KeyBinding> keyBindings = KeyBindingAccessor.getKeysById();
		changeFunction.accept(keyBindings);
		// update keys from ids (this will result in the keybinding from now on/no longer being tiggered)
		KeyBinding.updateKeysByCode();
	}

	public static void registerKeyBindingQuerying(KeyBinding binding) {
		// TODO: use methods from amecs??
		changeKeysById(keyBindings -> keyBindings.put(binding.getTranslationKey(), binding));
	}

	public static void unregisterKeyBindingQuerying(KeyBinding binding) {
		// TODO: use methods from amecs??
		changeKeysById(keyBindings -> keyBindings.remove(binding.getTranslationKey(), binding));
	}

	public static void registerKeyBindingsBoth(GameOptions gameOptions, Collection<KeyBinding> bindings) {
		for (KeyBinding binding : bindings) {
			KeyBindingHelper.registerKeyBinding(binding);
			registerKeyBindingQuerying(binding);
		}
		GameOptionsAccessor options = (GameOptionsAccessor) gameOptions;
		if (options != null) { // if options == null: Game is during initialization - this is handled by Fabric Api already
			// this adds the keybindings to the options gui
			changeKeysAll(options, keysAll -> ArrayUtils.addAll(keysAll, bindings.toArray(KeyBinding[]::new)));
		}
	}
	// - register/unregister keybindings

	public static void resetSingleKeyBinding(KeyBinding keyBinding) {
		keyBinding.setBoundKey(keyBinding.getDefaultKey());
	}

	// used in GameOptions.load
	public static KeyBinding findMatchingAlternativeInBase(KeyBinding base, int alternativeId) {
		IKeyBinding parent = (IKeyBinding) base;
		List<KeyBinding> alternatives = parent.nmuk_getAlternatives();
		return findMatchingAlternative(alternatives, base.getTranslationKey(), alternativeId);
	}

	// used in GameOptions.load
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

	/**
	 *
	 * @param base
	 * @return the keybinding (get or new). It is registered for input querying and is added to the parent
	 */
	public static KeyBinding getOrCreateAlternativeKeyBinding(KeyBinding base) {
		IKeyBinding parent = (IKeyBinding) base;

		// get the next default alternative if available
		List<KeyBinding> defaultAlternatives = NMUKKeyBindingHelper.defaultAlternatives.get(base);
		if (defaultAlternatives.size() > parent.nmuk_getAlternativesCount()) {
			KeyBinding defaultAlternative = defaultAlternatives.get(parent.nmuk_getAlternativesCount());
			makeKeyBindingAlternativeOf(base, defaultAlternative, AlternativeKeyBinding.NO_ALTERNATIVE_ID, false);
			registerKeyBindingQuerying(defaultAlternative);

			parent.nmuk_addAlternative(defaultAlternative);
			return defaultAlternative;
		}

		// if not we create a new alternative keybinding
		KeyBinding alternative = new AlternativeKeyBinding(base, base.getTranslationKey(), parent.nmuk_getNextChildId(), base.getCategory());
		parent.nmuk_addAlternative(alternative);
		return alternative;
	}

	/**
	 *
	 * @param base
	 * @param type
	 * @param code
	 * @return the newly created keybinding. It is registered for input querying
	 */
	public static KeyBinding createAndAddAlternativeKeyBinding(KeyBinding base, InputUtil.Type type, int code) {
		IKeyBinding parent = (IKeyBinding) base;
		KeyBinding alternative = new AlternativeKeyBinding(base, base.getTranslationKey(), parent.nmuk_getNextChildId(), type, code, base.getCategory());
		parent.nmuk_addAlternative(alternative);
		return alternative;
	}

	/**
	 * The keybinding {@code alternative} is NEITHER registered for querying NOR in the gui
	 *
	 * @param base
	 * @param alternative
	 * @param alternativeId
	 * @param addToBase
	 */
	public static void makeKeyBindingAlternativeOf(KeyBinding base, KeyBinding alternative, int alternativeId, boolean addToBase) {
		IKeyBinding parent = (IKeyBinding) base;

		// now the keybinding is a complete ghost and we can give it a new identity
		// this code here should set all the values in the same way than the AlternativeKeyBinding contructor does
		if (alternativeId == AlternativeKeyBinding.NO_ALTERNATIVE_ID) {
			alternativeId = parent.nmuk_getNextChildId();
		}
		String newTranslationKey = AlternativeKeyBinding.makeAlternativeKeyTranslationKey(base.getTranslationKey(), alternativeId);
		((KeyBindingAccessor) alternative).setTranslationKey(newTranslationKey);
		((KeyBindingAccessor) alternative).setCategory(base.getCategory());
		((IKeyBinding) alternative).nmuk_setAlternativeId(alternativeId);
		((IKeyBinding) alternative).nmuk_setParent(base);

		if (addToBase) {
			// and finally we give the parent its new child
			parent.nmuk_addAlternative(alternative);
		}
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

		KeyBinding base = ((IKeyBinding) keyBinding).nmuk_getParent();
		int indexInBase = ((IKeyBinding) base).nmuk_removeAlternative(keyBinding);
		unregisterKeyBindingQuerying(keyBinding);
		unregisterKeyBindingGUI(keyBinding);

		List<Entry<?>> entries = getControlsListWidgetEntries(listWidget);
		int indexInEntries = entries.indexOf(entry);
		entries.remove(indexInEntries);
		updateDefaultAlternativesInBase(base, indexInBase, listWidget, entries, indexInEntries);

		// do it like vanilla and save directly
		saveOptions();
	}

	private static void updateDefaultAlternativesInBase(KeyBinding base, int indexInBase, ControlsListWidget listWidget, List<Entry<?>> entries, int indexInEntries) {
		if (indexInBase == -1) {
			// not removed: nothing changed and maybe even the list is empty
			return;
		}
		List<KeyBinding> defaultAlternatives = NMUKKeyBindingHelper.defaultAlternatives.get(base);
		if (indexInBase >= defaultAlternatives.size()) {
			// the removed keybinding was after all the default ones
			return;
		}
		// update the defaults
		List<KeyBinding> alternatives = ((IKeyBinding) base).nmuk_getAlternatives();
		int minSize = Math.min(alternatives.size(), defaultAlternatives.size());
		for (int i = indexInBase; i < minSize; i++) {
			KeyBinding currentAlt = alternatives.get(i);
			KeyBinding newAlt = defaultAlternatives.get(i);
			if (currentAlt == newAlt) {
				continue;
			}
			Key boundKey = ((KeyBindingAccessor) currentAlt).getBoundKey();
			makeKeyBindingAlternativeOf(base, newAlt, ((IKeyBinding) currentAlt).nmuk_getAlternativeId(), false);
			newAlt.setBoundKey(boundKey);
			// important to first unregister the current one before registering the new one
			unregisterKeyBindingQuerying(currentAlt);
			unregisterKeyBindingGUI(currentAlt);

			registerKeyBindingQuerying(newAlt);
			registerKeyBindingGUI(newAlt);
			alternatives.set(i, newAlt);

			// update gui entries
			int iRelToStart = (i - indexInBase);
			int indexEntry = indexInEntries + iRelToStart;
			entries.remove(indexEntry);
			ControlsListWidget.KeyBindingEntry newEntry = createKeyBindingEntry(listWidget, newAlt, ENTRY_NAME);
			entries.add(indexEntry, newEntry);
		}
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

		KeyBinding altBinding = getOrCreateAlternativeKeyBinding(baseKeyBinding);
		registerKeyBindingGUI(altBinding);
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
		// we make a copy of the defaultAlternatives here because we remove some elements for calculation
		List<KeyBinding> defaultAlternatives = new ArrayList<>(NMUKKeyBindingHelper.defaultAlternatives.get(baseKeyBinding));
		List<Entry<?>> entries = getControlsListWidgetEntries(listWidget);
		int childrenStartEntryPos = entries.indexOf(entry) + 1;

		int alternativesSize = alternatives.size();
		boolean changed = false;
		Iterator<KeyBinding> iterator = alternatives.iterator();
		while (iterator.hasNext()) {
			KeyBinding alternative = iterator.next();
			if (!defaultAlternatives.contains(alternative)) {
				// if alternative is not a default alternative
				// unregister it
				unregisterKeyBindingQuerying(alternative);
				unregisterKeyBindingGUI(alternative);
				iterator.remove();
				changed = true;
			}
		}
		// alternatives now only contains the default alternatives we already knew
		List<KeyBinding> defaultAlternativesAlreadyKnown = new ArrayList<>(alternatives);

		// remove all entries from gui
		entries.subList(childrenStartEntryPos, childrenStartEntryPos + alternativesSize).clear();
		// clear alternatives of base
		alternatives.clear();

		for (KeyBinding defaultAlternative : defaultAlternatives) {
			ControlsListWidget.KeyBindingEntry newEntry = createKeyBindingEntry(listWidget, defaultAlternative, ENTRY_NAME);
			entries.add(childrenStartEntryPos++, newEntry);
			resetSingleKeyBinding(defaultAlternative);
			alternatives.add(defaultAlternative);
			changed = true;
		}

		defaultAlternatives.removeAll(defaultAlternativesAlreadyKnown);
		registerKeyBindingsBoth(MinecraftClient.getInstance().options, defaultAlternatives);

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
