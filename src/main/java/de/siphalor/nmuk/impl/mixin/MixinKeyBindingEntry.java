package de.siphalor.nmuk.impl.mixin;

import com.google.common.collect.ImmutableList;
import de.siphalor.nmuk.impl.IKeyBinding;
import de.siphalor.nmuk.impl.NMUKKeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(ControlsListWidget.KeyBindingEntry.class)
public class MixinKeyBindingEntry {
	private static final Text ENTRY_NAME = new LiteralText("    ->");
	private static final Text RESET_TOOLTIP = new TranslatableText("nmuk.options.controls.reset.tooltip");

	@Shadow @Final private ButtonWidget resetButton;
	@Shadow @Final private ButtonWidget editButton;
	@Mutable
	@Shadow @Final private Text bindingName;
	// This is a synthetic field containing the outer class instance
	@Shadow(aliases = "field_2742", remap = false)
	@Final private ControlsListWidget listWidget;
	@Unique
	private ButtonWidget alternativesButton;

	@Inject(method = "<init>", at = @At("RETURN"))
	public void onConstruct(ControlsListWidget outer, KeyBinding binding, Text text, CallbackInfo ci) {
		IKeyBinding iKeyBinding = (IKeyBinding) binding;
		if (iKeyBinding.nmuk_isAlternative()) {
			bindingName = ENTRY_NAME;
			alternativesButton = new ButtonWidget(0, 0, 20, 20, new LiteralText("x"), button -> {
				((IKeyBinding) iKeyBinding.nmuk_getParent()).nmuk_removeAlternative(binding);
				NMUKKeyBindingHelper.removeKeyBinding(binding);
				List<ControlsListWidget.KeyBindingEntry> entries = NMUKKeyBindingHelper.getControlsListWidgetEntries();
				if (entries != null) {
					//noinspection RedundantCast
					entries.remove((ControlsListWidget.KeyBindingEntry) (Object) this);
				}
			});
		} else {
			alternativesButton = new ButtonWidget(0, 0, 20, 20, new LiteralText("+"), button -> {
				KeyBinding altBinding = NMUKKeyBindingHelper.createAlternativeKeyBinding(binding);
				NMUKKeyBindingHelper.registerKeyBinding(altBinding);
				ControlsListWidget.KeyBindingEntry altEntry = NMUKKeyBindingHelper.createKeyBindingEntry(outer, altBinding, new LiteralText("..."));
				if (altEntry != null) {
					List<ControlsListWidget.KeyBindingEntry> entries = NMUKKeyBindingHelper.getControlsListWidgetEntries();
					if (entries != null) {
						for (int i = 0, entriesSize = entries.size(); i < entriesSize; i++) {
							//noinspection ConstantConditions,RedundantCast,RedundantCast
							if (entries.get(i) == (ControlsListWidget.KeyBindingEntry)(Object) this) {
								i += ((IKeyBinding) binding).nmuk_getAlternativesCount();
								entries.add(i, altEntry);
								break;
							}
						}
					}
				}
			});
			((ButtonWidgetAccessor) resetButton).setTooltipSupplier((button, matrices, mouseX, mouseY) ->
				MinecraftClient.getInstance().currentScreen.renderTooltip(matrices, RESET_TOOLTIP, mouseX, mouseY)
			);
		}
	}

	@SuppressWarnings("UnresolvedMixinReference")
	@Inject(method = "method_19870(Lnet/minecraft/client/option/KeyBinding;Lnet/minecraft/client/gui/widget/ButtonWidget;)V", at = @At("HEAD"))
	private void resetButtonPressed(KeyBinding keyBinding, ButtonWidget widget, CallbackInfo ci) {
		if (((IKeyBinding) keyBinding).nmuk_getParent() == null && Screen.hasShiftDown()) {
			List<KeyBinding> alternatives = ((IKeyBinding) keyBinding).nmuk_getAlternatives();
			List<KeyBinding> defaultAlternatives = new ArrayList<>(NMUKKeyBindingHelper.defaultAlternatives.get(keyBinding));
			List<ControlsListWidget.KeyBindingEntry> entries = NMUKKeyBindingHelper.getControlsListWidgetEntries();
			// noinspection ConstantConditions,RedundantCast
			int entryPos = entries.indexOf((ControlsListWidget.KeyBindingEntry)(Object) this);

			int index;
			for (Iterator<KeyBinding> iterator = alternatives.iterator(); iterator.hasNext(); ) {
				KeyBinding alternative = iterator.next();
				index = defaultAlternatives.indexOf(alternative);
				if (index == -1) {
					entries.remove(entryPos + 1 + ((IKeyBinding) alternative).nmuk_getIndexInParent());
					iterator.remove();
					NMUKKeyBindingHelper.removeKeyBinding(alternative);
					continue;
				}
				defaultAlternatives.remove(index);
				NMUKKeyBindingHelper.resetSingleKeyBinding(alternative);
			}
			entryPos += alternatives.size();

			ControlsListWidget.KeyBindingEntry entry;
			NMUKKeyBindingHelper.registerKeyBindings(MinecraftClient.getInstance().options, defaultAlternatives);
			for (KeyBinding defaultAlternative : defaultAlternatives) {
				entry = NMUKKeyBindingHelper.createKeyBindingEntry(listWidget, defaultAlternative, ENTRY_NAME);
				entries.add(++entryPos, entry);
				NMUKKeyBindingHelper.resetSingleKeyBinding(defaultAlternative);
			}
		}
	}

	@ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 2, argsOnly = true)
	public int adjustXPosition(int original) {
		return original - 30;
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
