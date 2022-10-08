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

import java.util.LinkedList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import de.siphalor.nmuk.impl.AlternativeKeyBinding;
import de.siphalor.nmuk.impl.IKeyBinding;
import de.siphalor.nmuk.impl.NMUKKeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;

@Mixin(value = KeyBinding.class, priority = 800)
public abstract class MixinKeyBinding implements IKeyBinding {
	@Shadow
	private boolean pressed;
	@Shadow
	@Final
	private String category;
	@Shadow
	@Final
	private String translationKey;

	@Unique
	private List<KeyBinding> children = null;
	@Unique
	private int nextChildId = 0;
	@Unique
	private int alternativeId = AlternativeKeyBinding.NO_ALTERNATIVE_ID;
	@Unique
	private KeyBinding parent = null;

	@Override
	public int nmuk_getNextChildId() {
		return nextChildId++;
	}

	@Override
	public void nmuk_setNextChildId(int nextChildId) {
		this.nextChildId = nextChildId;
	}

	@Override
	public boolean nmuk_isAlternative() {
		return parent != null; // at this point following should be the excat same: alternativeId != AlternativeKeyBinding.NO_ALTERNATIVE_ID
	}

	@Override
	public KeyBinding nmuk_getParent() {
		return parent;
	}

	@Override
	public void nmuk_setParent(KeyBinding binding) {
		parent = binding;
	}

	@Override
	public List<KeyBinding> nmuk_getAlternatives() {
		return children;
	}

	@Override
	public int nmuk_getAlternativesCount() {
		if (children == null) {
			return 0;
		} else {
			return children.size();
		}
	}

	@Override
	public int nmuk_removeAlternative(KeyBinding binding) {
		if (children != null) {
			int index = children.indexOf(binding);
			if (index == -1) {
				return -1;
			}
			children.remove(index);

			int bindingAltId = ((IKeyBinding) binding).nmuk_getAlternativeId();
			if (bindingAltId != AlternativeKeyBinding.NO_ALTERNATIVE_ID) {
				if ((nextChildId - 1) == bindingAltId) {
					nextChildId--;
				}
			}
			return index;
		}
		return -1;
	}

	@Override
	public void nmuk_addAlternative(KeyBinding binding) {
		if (children == null) {
			children = new LinkedList<>();
		}
		children.add(binding);
	}

	@Override
	public int nmuk_getIndexInParent() {
		if (parent == null) {
			return 0;
		}
		return ((IKeyBinding) parent).nmuk_getAlternatives().indexOf(this);
	}

	@Override
	public int nmuk_getAlternativeId() {
		return alternativeId;
	}

	@Override
	public void nmuk_setAlternativeId(int alternativeId) {
		this.alternativeId = alternativeId;
	}

	@Inject(method = "onKeyPressed", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/KeyBinding;timesPressed:I"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
	private static void onKeyPressed(InputUtil.Key key, CallbackInfo callbackInfo, KeyBinding binding) {
		KeyBinding parent = ((IKeyBinding) binding).nmuk_getParent();
		if (parent != null) {
			((KeyBindingAccessor) parent).setTimesPressed(((KeyBindingAccessor) parent).getTimesPressed() + 1);
			callbackInfo.cancel();
		}
	}

	@Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
	public void isPressedInjection(CallbackInfoReturnable<Boolean> cir) {
		if (!pressed && children != null && !children.isEmpty()) {
			for (KeyBinding child : children) {
				if (child.isPressed()) {
					cir.setReturnValue(true);
				}
			}
		}
	}

	@Inject(method = "reset", at = @At("RETURN"))
	private void resetInjection(CallbackInfo callbackInfo) {
		if (children != null && !children.isEmpty()) {
			for (KeyBinding child : children) {
				child.setPressed(false);
			}
		}
	}

	@Inject(method = "compareTo", at = @At("HEAD"), cancellable = true)
	public void compareToInjection(KeyBinding other, CallbackInfoReturnable<Integer> cir) {
		if (parent != null) {
			if (other == parent) {
				cir.setReturnValue(1);
			} else if (category.equals(other.getCategory())) {
				KeyBinding otherParent = ((IKeyBinding) other).nmuk_getParent();
				if (otherParent == parent) {
					cir.setReturnValue(Integer.compare(nmuk_getAlternativeId(), ((IKeyBinding) other).nmuk_getAlternativeId()));
				} else {
					cir.setReturnValue(
						I18n.translate(AlternativeKeyBinding.getBaseTranslationKey(translationKey))
							.compareTo(I18n.translate(AlternativeKeyBinding.getBaseTranslationKey(other.getTranslationKey()))));
				}
			}
		}
	}

	@Inject(method = "matchesKey", at = @At("HEAD"), cancellable = true)
	public void matchesKeyInjection(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> cir) {
		if (children != null && !children.isEmpty()) {
			for (KeyBinding child : children) {
				if (child.matchesKey(keyCode, scanCode)) {
					cir.setReturnValue(true);
					return;
				}
			}
		}
	}

	@Inject(method = "matchesMouse", at = @At("HEAD"), cancellable = true)
	public void matchesMouseInjection(int code, CallbackInfoReturnable<Boolean> cir) {
		if (children != null && !children.isEmpty()) {
			for (KeyBinding child : children) {
				if (child.matchesMouse(code)) {
					cir.setReturnValue(true);
					return;
				}
			}
		}
	}

	@Inject(method = "setBoundKey", at = @At("RETURN"))
	public void setBoundKeyInjection(InputUtil.Key boundKey, CallbackInfo callbackInfo) {
		if (nmuk_isAlternative() && boundKey.equals(InputUtil.UNKNOWN_KEY)) {
			NMUKKeyBindingHelper.removeAlternativeKeyBinding_OptionsScreen((KeyBinding) (Object) this, null, null);
		}
	}
}
