package de.siphalor.nmuk.impl;

import de.siphalor.nmuk.NMUK;
import de.siphalor.nmuk.impl.mixin.ControlsOptionsScreenAccessor;
import de.siphalor.nmuk.impl.mixin.EntryListWidgetAccessor;
import de.siphalor.nmuk.impl.mixin.GameOptionsAccessor;
import de.siphalor.nmuk.impl.mixin.KeyBindingRegistryImplAccessor;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.options.ControlsListWidget;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.text.Text;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public class NMUKKeyBindingHelper {
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
		KeyBinding[] keysAll = options.getKeysAll();
		KeyBinding[] newKeysAll = new KeyBinding[keysAll.length + 1];
		System.arraycopy(keysAll, 0, newKeysAll, 0, keysAll.length);
		newKeysAll[keysAll.length] = binding;
		options.setKeysAll(newKeysAll);
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

	public static KeyBinding createAlternativeKeyBinding(KeyBinding base) {
		IKeyBinding parent = (IKeyBinding) base;
		KeyBinding alt = new AlternativeKeyBinding(base, base.getTranslationKey() + "%" + parent.nmuk_getAlternativesCount(), -1, base.getCategory());
		parent.nmuk_addAlternative(alt);
		return alt;
	}

	public static List<ControlsListWidget.KeyBindingEntry> getControlsListWidgetEntries() {
		Screen screen = MinecraftClient.getInstance().currentScreen;
		if (screen instanceof ControlsOptionsScreenAccessor) {
			//noinspection unchecked
			return (List<ControlsListWidget.KeyBindingEntry>)(Object)
					((EntryListWidgetAccessor) ((ControlsOptionsScreenAccessor) screen).getKeyBindingListWidget()).getChildren();
		}
		return null;
	}

	public static ControlsListWidget.KeyBindingEntry createKeyBindingEntry(ControlsListWidget listWidget, KeyBinding binding, Text text) {
		try {
			//noinspection rawtypes
			Constructor constructor = ControlsListWidget.KeyBindingEntry.class.getDeclaredConstructors()[0];
			constructor.setAccessible(true);
			return (ControlsListWidget.KeyBindingEntry) constructor.newInstance(listWidget, binding, text);
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
}
