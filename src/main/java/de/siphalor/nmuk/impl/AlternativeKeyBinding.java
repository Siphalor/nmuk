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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

@ApiStatus.Internal
public class AlternativeKeyBinding extends KeyBinding {
	public static final String ALTERNATIVE_ID_TRANSLATION_KEY_DELIMITER = "%";
	public static final int NO_ALTERNATIVE_ID = -1;

	public static String makeAlternativeKeyTranslationKey(String translationKey, int alternativeId) {
		return translationKey + ALTERNATIVE_ID_TRANSLATION_KEY_DELIMITER + alternativeId;
	}

	public static String getBaseTranslationKey(String translationKey) {
		return StringUtils.substringBeforeLast(translationKey, ALTERNATIVE_ID_TRANSLATION_KEY_DELIMITER);
	}

	public static int getAlternativeIdFromTranslationKey(String translationKey) {
		int stringIndex = translationKey.indexOf(AlternativeKeyBinding.ALTERNATIVE_ID_TRANSLATION_KEY_DELIMITER);
		if (stringIndex == -1) {
			// not found
			return NO_ALTERNATIVE_ID;
		}
		return Integer.parseInt(translationKey.substring(stringIndex + AlternativeKeyBinding.ALTERNATIVE_ID_TRANSLATION_KEY_DELIMITER.length()));
	}

	public AlternativeKeyBinding(KeyBinding parent, String translationKey, int alternativeId, String category) {
		this(parent, translationKey, alternativeId, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category);
	}

	public AlternativeKeyBinding(KeyBinding parent, String translationKey, int alternativeId, int code, String category) {
		this(parent, translationKey, alternativeId, InputUtil.Type.KEYSYM, code, category);
	}

	public AlternativeKeyBinding(KeyBinding parent, String translationKey, int alternativeId, InputUtil.Type type, int code, String category) {
		super(makeAlternativeKeyTranslationKey(translationKey, alternativeId), type, code, category);
		((IKeyBinding) this).nmuk_setAlternativeId(alternativeId);
		((IKeyBinding) this).nmuk_setParent(parent);
	}
}
