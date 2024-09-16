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

package de.siphalor.nmuk.impl.forge.mixin;

import de.siphalor.nmuk.impl.NMUKKeyBinding;
import de.siphalor.nmuk.impl.mixin.KeyBindingAccessor;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(value = KeyBinding.class, priority = 800)
public abstract class MixinKeyBindingForge implements NMUKKeyBinding {
    @Inject(
            method = "onKeyPressed",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/KeyBinding;timesPressed:I"),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void onKeyPressed(InputUtil.Key key, CallbackInfo callbackInfo, Iterator var1, KeyBinding binding) {
        KeyBinding parent = ((NMUKKeyBinding) binding).nmuk_getParent();
        if (parent != null) {
            ((KeyBindingAccessor) parent).setTimesPressed(((KeyBindingAccessor) parent).getTimesPressed() + 1);
            callbackInfo.cancel();
        }
    }
}
