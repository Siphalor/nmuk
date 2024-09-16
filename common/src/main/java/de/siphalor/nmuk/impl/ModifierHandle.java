package de.siphalor.nmuk.impl;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.option.KeyBinding;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ModifierHandle {
    @ExpectPlatform
    public static void resetKeyModifiers(KeyBinding keyBinding) {
        throw new AssertionError();
    }
}
