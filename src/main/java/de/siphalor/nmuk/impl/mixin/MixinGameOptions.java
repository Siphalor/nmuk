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

package de.siphalor.nmuk.impl.mixin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.logging.log4j.Level;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.siphalor.nmuk.NMUK;
import de.siphalor.nmuk.impl.AlternativeKeyBinding;
import de.siphalor.nmuk.impl.IKeyBinding;
import de.siphalor.nmuk.impl.NMUKKeyBindingHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

@Mixin(value = GameOptions.class, priority = 800)
public class MixinGameOptions {
	@Unique
	private File nmukOptionsFile;
	@Unique
	private KeyBinding[] tempKeysAll;

	@Shadow
	protected MinecraftClient client;

	@Mutable
	@Shadow
	@Final
	public KeyBinding[] keysAll;

	// Prevent nmuk keybindings from getting saved to the Vanilla options file
	@Inject(method = "accept", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;keysAll:[Lnet/minecraft/client/option/KeyBinding;"))
	public void removeNMUKBindings(CallbackInfo ci) {
		tempKeysAll = keysAll;
		keysAll = Arrays.stream(keysAll).filter(binding -> !((IKeyBinding) binding).nmuk_isAlternative()).toArray(KeyBinding[]::new);
	}

	@Inject(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/sound/SoundCategory;values()[Lnet/minecraft/sound/SoundCategory;"))
	public void resetAllKeys(CallbackInfo ci) {
		keysAll = tempKeysAll;
	}

	@Inject(method = "write", at = @At("RETURN"))
	public void save(CallbackInfo ci) {
		try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nmukOptionsFile), StandardCharsets.UTF_8))) {
			for (KeyBinding binding : keysAll) {
				if (((IKeyBinding) binding).nmuk_isAlternative() && !binding.isUnbound()) {
					printWriter.println("key_" + binding.getTranslationKey() + ":" + binding.getBoundKeyTranslationKey());
				}
			}
		} catch (FileNotFoundException e) {
			NMUK.log(Level.ERROR, "Encountered an issue whilst writing nmuk options file!");
			e.printStackTrace();
		}
	}

	private static final String KEY_PREFIX = "key_";
	private static final String KEY_ENTRY_DELIMITER = ":";

	@Inject(method = "load", at = @At("RETURN"))
	public void load(CallbackInfo ci) {
		if (nmukOptionsFile == null) {
			nmukOptionsFile = new File(client.runDirectory, "options." + NMUK.MOD_ID + ".txt");
		}

		if (!nmukOptionsFile.exists()) {
			return;
		}
		Map<String, KeyBinding> keyBindings = KeyBindingAccessor.getKeysById();
		Object2IntMap<KeyBinding> highestAlternativeIdMap = new Object2IntOpenHashMap<>();
		List<KeyBinding> newAlternatives = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(nmukOptionsFile))) {
			String line;
			while ((line = reader.readLine()) != null) {
				try {
					int stringIndex = line.lastIndexOf(KEY_ENTRY_DELIMITER);
					if (stringIndex <= 0) {
						NMUK.log(Level.WARN, "Invalid nmuk options line: " + line);
						continue;
					}
					String id = line.substring(0, stringIndex);
					String keyId = line.substring(stringIndex + KEY_ENTRY_DELIMITER.length());
					if (!id.startsWith(KEY_PREFIX)) {
						NMUK.log(Level.WARN, "Invalid nmuk options entry: " + id);
						continue;
					}
					id = id.substring(KEY_PREFIX.length());
					int altId = AlternativeKeyBinding.getAlternativeIdFromTranslationKey(id);
					if (altId == AlternativeKeyBinding.NO_ALTERNATIVE_ID) {
						NMUK.log(Level.WARN, "Nmuk options entry is missing an valid alternative id");
						continue;
					}
					id = AlternativeKeyBinding.getBaseTranslationKey(id);
					InputUtil.Key boundKey = InputUtil.fromTranslationKey(keyId);
					KeyBinding base = keyBindings.get(id);
					if (base != null) {

						KeyBinding alternative = NMUKKeyBindingHelper.findMatchingAlternativeInBase(base, altId);
						if (alternative == null) {
							((IKeyBinding) base).nmuk_setNextChildId(altId);
							alternative = NMUKKeyBindingHelper.createAndAddAlternativeKeyBinding(base, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN);
							newAlternatives.add(alternative);
						}
						alternative.setBoundKey(boundKey);

						int newHighestAltId = Math.max(altId, highestAlternativeIdMap.getOrDefault(base, 0));
						((IKeyBinding) base).nmuk_setNextChildId(newHighestAltId + 1);
						highestAlternativeIdMap.put(base, newHighestAltId);
					} else {
						NMUK.log(Level.WARN, "Nmuk options entry has a base key which is not registered: " + id);
					}
				} catch (Throwable e) {
					NMUK.log(Level.ERROR, "Encountered an issue whilst reading a line from nmuk options file!");
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			NMUK.log(Level.ERROR, "Encountered an issue whilst loading nmuk options file!");
			e.printStackTrace();
		}

		// here we remove default keybindings that the user deleted but are still present from the initialization at the moment
		for (Entry<KeyBinding> binding : highestAlternativeIdMap.object2IntEntrySet()) {
			int addedAlts = binding.getIntValue() + 1;
			// defaultCount at this point should be the current highestAltId + 1
			int defaultCount = ((IKeyBinding) binding.getKey()).nmuk_getAlternativesCount();
			if (defaultCount > addedAlts) {
				List<KeyBinding> alternatives = ((IKeyBinding) binding.getKey()).nmuk_getAlternatives();
				ListIterator<KeyBinding> iterator = alternatives.subList(addedAlts, defaultCount).listIterator();
				while (iterator.hasNext()) {
					KeyBinding defKB = iterator.next();
					// we also need to remove the old keybinding from the two internal lists
					NMUKKeyBindingHelper.removeKeyBindingGUI((GameOptionsAccessor) this, defKB);
					NMUKKeyBindingHelper.unregisterKeyBindingQuerying(defKB);
					// ... and remove it from the children list
					iterator.remove();
				}
			}
		}

		NMUKKeyBindingHelper.registerKeyBindingsBoth((GameOptions) (Object) this, newAlternatives);
	}
}
