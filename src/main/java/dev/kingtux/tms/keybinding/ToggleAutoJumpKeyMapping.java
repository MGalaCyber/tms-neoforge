package dev.kingtux.tms.keybinding;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.TooManyShortcuts;
import dev.kingtux.tms.api.BindingModifiers;
import dev.kingtux.tms.api.TMSKeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Ported from upstream's {@code ToggleAutoJumpKeyBinding}. */
public final class ToggleAutoJumpKeyMapping extends TMSKeyMapping {

    public ToggleAutoJumpKeyMapping(String name, InputConstants.Type type, int code, String category, BindingModifiers defaultModifiers) {
        super(name, type, code, category, defaultModifiers);
    }

    @Override
    public void onPressed() {
        Minecraft client = Minecraft.getInstance();
        boolean autoJump = !client.options.autoJump.get();
        client.options.autoJump.set(autoJump);
        if (client.player != null) {
            TooManyShortcuts.sendToggleMessage(client.player, autoJump, Component.translatable("too_many_shortcuts.toggled.auto_jump"));
        }
    }
}
