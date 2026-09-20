package dev.kingtux.tms.keybinding;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.TooManyShortcuts;
import dev.kingtux.tms.api.BindingModifiers;
import dev.kingtux.tms.api.TMSKeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerModelPart;

/** Toggles one skin layer (cape, hat, sleeves...). Ported from upstream's {@code SkinLayerKeyBinding}. */
public final class SkinLayerKeyMapping extends TMSKeyMapping {
    private final PlayerModelPart playerModelPart;

    public SkinLayerKeyMapping(String name, InputConstants.Type type, int code, String category, PlayerModelPart playerModelPart) {
        super(name, type, code, category, new BindingModifiers());
        this.playerModelPart = playerModelPart;
    }

    @Override
    public void onPressed() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        boolean newValue = !client.options.isModelPartEnabled(playerModelPart);
        client.options.setModelPartEnabled(playerModelPart, newValue);
        TooManyShortcuts.sendToggleMessage(client.player, newValue,
                net.minecraft.network.chat.Component.translatable("options.modelPart." + playerModelPart.getId()));
    }
}
