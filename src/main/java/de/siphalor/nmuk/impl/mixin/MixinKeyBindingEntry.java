package de.siphalor.nmuk.impl.mixin;

import com.google.common.collect.ImmutableList;
import de.siphalor.nmuk.impl.IKeyBinding;
import de.siphalor.nmuk.impl.NMUKKeyBindingHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.options.ControlsListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ControlsListWidget.KeyBindingEntry.class)
public class MixinKeyBindingEntry {
	private static final Text ENTRY_NAME = new LiteralText("    ->");

	@Shadow @Final private ButtonWidget resetButton;
	@Shadow @Final private ButtonWidget editButton;
	@Mutable
	@Shadow @Final private Text bindingName;
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
								i += ((IKeyBinding) altBinding).nmuk_getIndexInParent();
								entries.add(i + 1, altEntry);
								break;
							}
						}
					}
				}
			});
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
