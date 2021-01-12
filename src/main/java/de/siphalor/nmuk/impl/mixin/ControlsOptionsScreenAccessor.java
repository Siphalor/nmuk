package de.siphalor.nmuk.impl.mixin;

import net.minecraft.client.gui.screen.options.ControlsListWidget;
import net.minecraft.client.gui.screen.options.ControlsOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ControlsOptionsScreen.class)
public interface ControlsOptionsScreenAccessor {
	@Accessor
	ControlsListWidget getKeyBindingListWidget();
}
