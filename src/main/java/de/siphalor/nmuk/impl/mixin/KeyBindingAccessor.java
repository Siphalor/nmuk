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

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Key;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
	@Accessor
	static Map<String, KeyBinding> getKeysById() {
		return null;
	}

	@Final
	@Mutable
	@Accessor
	void setTranslationKey(String id);

	@Final
	@Mutable
	@Accessor
	void setCategory(String category);

	@Accessor
	int getTimesPressed();

	@Accessor
	void setTimesPressed(int timesPressed);

	@Final
	@Accessor
	Key getBoundKey();
}
