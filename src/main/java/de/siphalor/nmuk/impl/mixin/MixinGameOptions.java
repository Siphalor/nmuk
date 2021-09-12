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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.siphalor.nmuk.NMUK;
import de.siphalor.nmuk.impl.AlternativeKeyBinding;
import de.siphalor.nmuk.impl.IKeyBinding;
import de.siphalor.nmuk.impl.NMUKKeyBindingHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
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

	@Inject(method = "load", at = @At("RETURN"))
	public void load(CallbackInfo ci) {
		if (nmukOptionsFile == null) {
			nmukOptionsFile = new File(MinecraftClient.getInstance().runDirectory, "options." + NMUK.MOD_ID + ".txt");
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
					int stringIndex = line.lastIndexOf(':');
					if (stringIndex <= 0) {
						NMUK.log(Level.WARN, "Invalid nmuk options line: " + line);
						continue;
					}
					String id = line.substring(0, stringIndex);
					String keyId = line.substring(stringIndex + 1);
					if (!id.startsWith("key_")) {
						NMUK.log(Level.WARN, "Invalid nmuk options entry: " + id);
						continue;
					}
					id = id.substring(4);
					stringIndex = id.indexOf(AlternativeKeyBinding.ALTERNATIVE_ID_TRANSLATION_KEY_DELIMITER);
					if (stringIndex <= 0) {
						NMUK.log(Level.WARN, "Nmuk options entry is missing an alternative id");
						continue;
					}
					int altId = Integer.parseInt(id.substring(stringIndex + AlternativeKeyBinding.ALTERNATIVE_ID_TRANSLATION_KEY_DELIMITER.length()));
					id = id.substring(0, stringIndex);
					InputUtil.Key boundKey = InputUtil.fromTranslationKey(keyId);
					// noinspection ConstantConditions
					KeyBinding base = keyBindings.get(id);
					if (base != null) {

						KeyBinding alternative = NMUKKeyBindingHelper.findMatchingAlternativeInBase(base, altId);
						if (alternative == null) {
							((IKeyBinding) base).nmuk_setNextChildId(altId);
							alternative = NMUKKeyBindingHelper.createAlternativeKeyBinding(base);
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

		NMUKKeyBindingHelper.registerKeyBindings((GameOptions) (Object) this, newAlternatives);
	}
}
