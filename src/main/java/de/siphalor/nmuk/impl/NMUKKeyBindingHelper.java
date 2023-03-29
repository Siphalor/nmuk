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

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.siphalor.nmuk.NMUK;
import de.siphalor.nmuk.impl.mixin.ControlsOptionsScreenAccessor;
import de.siphalor.nmuk.impl.mixin.EntryListWidgetAccessor;
import de.siphalor.nmuk.impl.mixin.GameOptionsAccessor;
import de.siphalor.nmuk.impl.mixin.KeyBindingRegistryImplAccessor;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.options.ControlsListWidget;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@ApiStatus.Internal
public class NMUKKeyBindingHelper {
	public static final Multimap<KeyBinding, KeyBinding> defaultAlternatives = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
	private static final boolean isAmecsLoaded = FabricLoader.getInstance().isModLoaded("amecsapi");

	public static void removeKeyBinding(KeyBinding binding) {
		List<KeyBinding> moddedKeyBindings = KeyBindingRegistryImplAccessor.getModdedKeyBindings();
		{
			//noinspection ConstantConditions
			boolean success = moddedKeyBindings.remove(binding);
			if (!success) {
				NMUK.log(Level.ERROR, "Failed to remove modded keybinding!");
			}
		}
		GameOptionsAccessor options = (GameOptionsAccessor) MinecraftClient.getInstance().options;
		KeyBinding[] keysAll = options.getKeysAll();
		int index = ArrayUtils.indexOf(keysAll, binding);
		KeyBinding[] newKeysAll = new KeyBinding[keysAll.length - 1];
		System.arraycopy(keysAll, 0, newKeysAll, 0, index);
		System.arraycopy(keysAll, index + 1, newKeysAll, index, keysAll.length - index - 1);
		options.setKeysAll(newKeysAll);
		KeyBinding.updateKeysByCode();
	}

	public static void registerKeyBinding(KeyBinding binding) {
		KeyBindingHelper.registerKeyBinding(binding);
		GameOptionsAccessor options = (GameOptionsAccessor) MinecraftClient.getInstance().options;
		if (options != null) { // Game is during initialization - this is handled by Fapi already
			KeyBinding[] keysAll = options.getKeysAll();
			KeyBinding[] newKeysAll = new KeyBinding[keysAll.length + 1];
			System.arraycopy(keysAll, 0, newKeysAll, 0, keysAll.length);
			newKeysAll[keysAll.length] = binding;
			options.setKeysAll(newKeysAll);
		}
		KeyBinding.updateKeysByCode();
	}

	public static void registerKeyBindings(GameOptions gameOptions, Collection<KeyBinding> bindings) {
		GameOptionsAccessor options = (GameOptionsAccessor) gameOptions;
		KeyBinding[] keysAll = options.getKeysAll();
		KeyBinding[] newKeysAll = new KeyBinding[keysAll.length + bindings.size()];
		System.arraycopy(keysAll, 0, newKeysAll, 0, keysAll.length);
		int i = keysAll.length;
		for (KeyBinding binding : bindings) {
			KeyBindingHelper.registerKeyBinding(binding);
			newKeysAll[i] = binding;
			i++;
		}
		options.setKeysAll(newKeysAll);
		KeyBinding.updateKeysByCode();
	}

	public static void resetSingleKeyBinding(KeyBinding keyBinding) {
		keyBinding.setKeyCode(keyBinding.getDefaultKeyCode());
		if (isAmecsLoaded) {
			AmecsProxy.resetKeyModifiers(keyBinding);
		}
	}

	public static KeyBinding createAlternativeKeyBinding(KeyBinding base) {
		return createAlternativeKeyBinding(base, -1);
	}

	public static KeyBinding createAlternativeKeyBinding(KeyBinding base, int code) {
		return createAlternativeKeyBinding(base, InputUtil.Type.KEYSYM, code);
	}

	public static KeyBinding createAlternativeKeyBinding(KeyBinding base, InputUtil.Type type, int code) {
		IKeyBinding parent = (IKeyBinding) base;
		KeyBinding alt = new AlternativeKeyBinding(base, base.getId() + "%" + parent.nmuk_getNextChildId(), type, code, base.getCategory());
		parent.nmuk_addAlternative(alt);
		return alt;
	}

	public static List<ControlsListWidget.KeyBindingEntry> getControlsListWidgetEntries() {
		Screen screen = MinecraftClient.getInstance().currentScreen;
		if (screen instanceof ControlsOptionsScreenAccessor) {
			//noinspection unchecked
			return (List<ControlsListWidget.KeyBindingEntry>) (Object)
					((EntryListWidgetAccessor) ((ControlsOptionsScreenAccessor) screen).getKeyBindingListWidget()).getChildren();
		}
		return null;
	}

	public static ControlsListWidget.KeyBindingEntry createKeyBindingEntry(ControlsListWidget listWidget, KeyBinding binding) {
		try {
			// noinspection JavaReflectionMemberAccess,JavaReflectionMemberAccess
			Constructor<ControlsListWidget.KeyBindingEntry> constructor = ControlsListWidget.KeyBindingEntry.class.getDeclaredConstructor(ControlsListWidget.class, KeyBinding.class);
			constructor.setAccessible(true);
			return constructor.newInstance(listWidget, binding);
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
		}
		return null;
	}
}
