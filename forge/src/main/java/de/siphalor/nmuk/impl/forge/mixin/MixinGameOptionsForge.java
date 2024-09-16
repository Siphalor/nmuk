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

package de.siphalor.nmuk.impl.forge.mixin;

import de.siphalor.nmuk.impl.NMUKKeyBinding;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(value = GameOptions.class, priority = 800)
public class MixinGameOptionsForge {
	@Unique
	private KeyBinding[] tempKeysAll;

	@Mutable
	@Shadow
	@Final
	public KeyBinding[] allKeys;

	// Prevent nmuk keybindings from getting saved to the Vanilla options file
	@Inject(
			method = "processOptionsForge",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;allKeys:[Lnet/minecraft/client/option/KeyBinding;")
	)
	public void removeNMUKBindings(CallbackInfo ci) {
		tempKeysAll = allKeys;
		allKeys = Arrays.stream(allKeys).filter(binding -> !((NMUKKeyBinding) binding).nmuk_isAlternative()).toArray(KeyBinding[]::new);
	}

	@Inject(
			method = "processOptionsForge",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/sound/SoundCategory;values()[Lnet/minecraft/sound/SoundCategory;")
	)
	public void resetAllKeys(CallbackInfo ci) {
		allKeys = tempKeysAll;
	}
}
