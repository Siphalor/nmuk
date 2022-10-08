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

import de.siphalor.amecs.api.AmecsKeyBinding;
import de.siphalor.amecs.api.KeyModifiers;
import de.siphalor.nmuk.api.NMUKAlternatives;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class NMUKTestMod implements ModInitializer {
	public static final String MOD_ID = "nmuk_testmod";

	@Override
	public void onInitialize() {
		KeyBinding kbd = KeyBindingHelper.registerKeyBinding(new KeyBinding(MOD_ID + ".test", InputUtil.Type.KEYSYM, 86, "key.categories.movement"));
		NMUKAlternatives.createAndGet(kbd, 85);
		NMUKAlternatives.createAndGet(kbd, new AmecsKeyBinding(new Identifier(MOD_ID, ""), InputUtil.Type.KEYSYM, 86, "", new KeyModifiers(false, true, true)));
	}
}
