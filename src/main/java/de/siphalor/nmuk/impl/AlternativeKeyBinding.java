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

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

@ApiStatus.Internal
public class AlternativeKeyBinding extends KeyBinding {
	public static final String ALTERNATIVE_ID_TRANSLATION_KEY_DELIMITER = "%";

	public static String makeAlternativeKeyTranslationKey(String translationKey, int alternativeId) {
		return translationKey + ALTERNATIVE_ID_TRANSLATION_KEY_DELIMITER + alternativeId;
	}

	private final int alternativeId;

	public AlternativeKeyBinding(KeyBinding parent, String translationKey, int alternativeId, int code, String category) {
		this(parent, translationKey, alternativeId, InputUtil.Type.KEYSYM, code, category);
	}

	public AlternativeKeyBinding(KeyBinding parent, String translationKey, int alternativeId, InputUtil.Type type, int code, String category) {
		super(makeAlternativeKeyTranslationKey(translationKey, alternativeId), type, code, category);
		this.alternativeId = alternativeId;
		((IKeyBinding) this).nmuk_setParent(parent);
	}

	public int getAlternativeId() {
		return alternativeId;
	}
}
