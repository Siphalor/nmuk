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

package de.siphalor.nmuktestmod;

import de.siphalor.nmuk.api.NMUKAlternatives;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("nmuk_testmod")
public class NMUKTestMod {
	public static final String MOD_ID = "nmuk_testmod";

	@SubscribeEvent
	public void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
		KeyBinding test = new KeyBinding(MOD_ID + ".test", InputUtil.Type.KEYSYM, 86, "key.categories.movement");
		event.register(test);

		NMUKAlternatives.create(test, 85);

		KeyBinding withModifier = new KeyBinding(
				"key." + (new Identifier(MOD_ID, "").toTranslationKey()),
				KeyConflictContext.UNIVERSAL,
				KeyModifier.ALT,
				InputUtil.Type.KEYSYM,
				86,
				""
		);
		NMUKAlternatives.create(test, withModifier);
	}
}
