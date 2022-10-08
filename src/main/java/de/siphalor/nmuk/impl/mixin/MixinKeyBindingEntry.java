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

import java.util.Collection;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;

import de.siphalor.nmuk.impl.IKeyBinding;
import de.siphalor.nmuk.impl.NMUKKeyBindingHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@Mixin(ControlsListWidget.KeyBindingEntry.class)
public class MixinKeyBindingEntry {
	@Shadow
	@Final
	private KeyBinding binding;
	@Shadow
	@Final
	private ButtonWidget resetButton;
	@Shadow
	@Final
	private ButtonWidget editButton;
	@Mutable
	@Shadow
	@Final
	private Text bindingName;
	// This is a synthetic field containing the outer class instance
	@Shadow(aliases = "field_2742", remap = false)
	@Final
	private ControlsListWidget listWidget;
	@Unique
	private ButtonWidget alternativesButton;

	@Inject(method = "<init>", at = @At("RETURN"))
	public void onConstruct(ControlsListWidget outer, KeyBinding binding, Text text, CallbackInfo ci) {
		IKeyBinding iKeyBinding = (IKeyBinding) binding;
		if (iKeyBinding.nmuk_isAlternative()) {
			bindingName = NMUKKeyBindingHelper.ENTRY_NAME;
			alternativesButton = new ButtonWidget(0, 0, 20, 20, NMUKKeyBindingHelper.REMOVE_ALTERNATIVE_TEXT, button -> {
				NMUKKeyBindingHelper.removeAlternativeKeyBinding_OptionsScreen(binding, outer, (ControlsListWidget.KeyBindingEntry) (Object) this);
			});
		} else {
			alternativesButton = new ButtonWidget(0, 0, 20, 20, NMUKKeyBindingHelper.ADD_ALTERNATIVE_TEXT, button -> {
				ControlsListWidget.KeyBindingEntry newAltEntry = NMUKKeyBindingHelper.addNewAlternativeKeyBinding_OptionsScreen(binding, outer, (ControlsListWidget.KeyBindingEntry) (Object) this);
				if (newAltEntry != null) {
					((KeyBindingEntryAccessor) newAltEntry).getEditButton().onPress();
				}
			});
		}
	}

	@SuppressWarnings("UnresolvedMixinReference")
	@Inject(method = "method_19870(Lnet/minecraft/client/option/KeyBinding;Lnet/minecraft/client/gui/widget/ButtonWidget;)V", at = @At("HEAD"))
	private void resetButtonPressed(KeyBinding keyBinding, ButtonWidget widget, CallbackInfo ci) {
		if (((IKeyBinding) keyBinding).nmuk_getParent() == null && Screen.hasShiftDown()) {
			NMUKKeyBindingHelper.resetAlternativeKeyBindings_OptionsScreen(keyBinding, listWidget, (ControlsListWidget.KeyBindingEntry) (Object) this);
		}
	}

	@ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 2, argsOnly = true)
	private int adjustXPosition(int original) {
		return original - 30;
	}

	@Inject(method = "render", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/gui/widget/ButtonWidget;active:Z", shift = Shift.AFTER))
	private void setResetButtonActive(CallbackInfo callbackInfo) {
		IKeyBinding iKeyBinding = (IKeyBinding) binding;
		if (!iKeyBinding.nmuk_isAlternative()) {
			if (resetButton.active) {
				((ButtonWidgetAccessor) resetButton)
					.setTooltipSupplier((button, matrices, mouseX, mouseY) -> ((ControlsListWidgetAccessor) listWidget).getParent().renderTooltip(matrices, NMUKKeyBindingHelper.RESET_TOOLTIP, mouseX, mouseY));
			} else {
				((ButtonWidgetAccessor) resetButton).setTooltipSupplier(ButtonWidget.EMPTY);
			}
		}
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isDefault()Z"))
	private boolean isDefaultOnRender(KeyBinding keyBinding) {
		IKeyBinding iKeyBinding = (IKeyBinding) keyBinding;
		if (iKeyBinding.nmuk_getParent() == null) {
			Collection<KeyBinding> defaults = NMUKKeyBindingHelper.defaultAlternatives.get(keyBinding);
			int childrenCount = iKeyBinding.nmuk_getAlternativesCount();

			if (defaults.size() == childrenCount) {
				List<KeyBinding> children = iKeyBinding.nmuk_getAlternatives();
				if (childrenCount > 0) {
					for (KeyBinding child : children) {
						if (!defaults.contains(child)) {
							return false;
						}
						if (!child.isDefault()) {
							return false;
						}
					}
				}
			} else {
				return false;
			}
		} else {
			if (keyBinding.getDefaultKey().equals(InputUtil.UNKNOWN_KEY)) {
				return true;
			}
		}
		return keyBinding.isDefault();
	}

	@Inject(method = "render", at = @At("RETURN"))
	public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo callbackInfo) {
		alternativesButton.y = resetButton.y;
		alternativesButton.x = resetButton.x + resetButton.getWidth() + 10;
		alternativesButton.render(matrices, mouseX, mouseY, tickDelta);
	}

	@Inject(method = "children", at = @At("RETURN"), cancellable = true)
	public void children(CallbackInfoReturnable<List<? extends Element>> callbackInfoReturnable) {
		callbackInfoReturnable.setReturnValue(ImmutableList.of(editButton, resetButton, alternativesButton));
	}

	// ordinal 2 is required because in the byte code the second return statement is unfolded to a condition with two constant returns
	@Inject(method = "mouseClicked", at = @At(value = "RETURN", ordinal = 2), require = 1, cancellable = true)
	public void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (alternativesButton.mouseClicked(mouseX, mouseY, button)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseReleased", at = @At("RETURN"), cancellable = true)
	public void mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (alternativesButton.mouseReleased(mouseX, mouseY, button)) {
			cir.setReturnValue(true);
		}
	}
}
