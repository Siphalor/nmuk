package de.siphalor.nmuk.impl.mixin;

import de.siphalor.nmuk.impl.IKeyBinding;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.LinkedList;
import java.util.List;

@Mixin(value = KeyBinding.class, priority = 800)
public abstract class MixinKeyBinding implements IKeyBinding {
	@Shadow private boolean pressed;
	@Shadow @Final private String category;
	@Shadow @Final private String translationKey;

	@Unique
	private List<KeyBinding> children = null;
	@Unique
	private KeyBinding parent = null;

	@Override
	public boolean nmuk_isAlternative() {
		return parent != null;
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
	public void nmuk_removeAlternative(KeyBinding binding) {
		if (children != null) {
			children.remove(binding);
		}
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
		return Integer.parseInt(StringUtils.substringAfterLast(translationKey, "%"));
	}

	@Inject(
			method = "onKeyPressed",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/KeyBinding;timesPressed:I"),
			cancellable = true,
			locals = LocalCapture.CAPTURE_FAILSOFT
	)
	private static void onKeyPressed(InputUtil.Key key, CallbackInfo callbackInfo, KeyBinding binding) {
		KeyBinding parent = ((IKeyBinding) binding).nmuk_getParent();
		if (parent != null) {
			((KeyBindingAccessor) parent).setTimesPressed(((KeyBindingAccessor) parent).getTimesPressed() + 1);
			callbackInfo.cancel();
		}
	}

	@Inject(
			method = "isPressed",
			at = @At("RETURN"),
			cancellable = true
	)
	public void isPressedInjection(CallbackInfoReturnable<Boolean> cir) {
		if (!pressed && children != null && !children.isEmpty()) {
			for (KeyBinding child : children) {
				if (child.isPressed()) {
					cir.setReturnValue(true);
				}
			}
		}
	}

	@Inject(
			method = "reset",
			at = @At("RETURN")
	)
	private void resetInjection(CallbackInfo callbackInfo) {
		if (children != null && !children.isEmpty()) {
			for (KeyBinding child : children) {
				child.setPressed(false);
			}
		}
	}

	@Inject(
			method = "compareTo",
			at = @At("HEAD"),
			cancellable = true
	)
	public void compareToInjection(KeyBinding other, CallbackInfoReturnable<Integer> cir) {
		if (parent != null) {
			if (other == parent) {
				cir.setReturnValue(1);
			} else if (category.equals(other.getCategory())) {
				KeyBinding otherParent = ((IKeyBinding) other).nmuk_getParent();
				if (otherParent == parent) {
					cir.setReturnValue(Integer.compare(nmuk_getIndexInParent(), ((IKeyBinding) other).nmuk_getIndexInParent()));
				} else {
					cir.setReturnValue(
							I18n.translate(StringUtils.substringBeforeLast(translationKey, "%"))
									.compareTo(I18n.translate(StringUtils.substringBeforeLast(other.getTranslationKey(), "%")))
					);
				}
			}
		}
	}

	@Inject(
			method = "matchesKey",
			at = @At("HEAD"),
			cancellable = true
	)
	public void matchesKeyInjection(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> cir) {
		if (children != null && !children.isEmpty()) {
			for (KeyBinding child : children) {
				if (child.matchesKey(keyCode, scanCode)) {
					cir.setReturnValue(true);
				}
			}
		}
	}

	@Inject(
			method = "matchesMouse",
			at = @At("HEAD"),
			cancellable = true
	)
	public void matchesMouseInjection(int code, CallbackInfoReturnable<Boolean> cir) {
		if (children != null && !children.isEmpty()) {
			for (KeyBinding child : children) {
				if (child.matchesMouse(code)) {
					cir.setReturnValue(true);
				}
			}
		}
	}
}
