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
        // Options.modelParts is a private Set<PlayerModelPart>; exposed via AccessTransformer
        // (there's no public isModelPartEnabled/setModelPartEnabled pair in 1.21.1's Options).
        boolean newValue = !client.options.modelParts.contains(playerModelPart);
        if (newValue) {
            client.options.modelParts.add(playerModelPart);
        } else {
            client.options.modelParts.remove(playerModelPart);
        }
        // NOTE: vanilla's own Options screen sends a ServerboundClientInformationPacket when
        // this changes, so other players/the server see the update. This toggle changes it
        // locally (your own client sees the layer appear/disappear immediately) but does not
        // re-send that packet, so other players may not see it until you next open/close the
        // regular Options menu. Wire that packet in here if that matters for your use case.
        TooManyShortcuts.sendToggleMessage(client.player, newValue,
                net.minecraft.network.chat.Component.translatable("options.modelPart." + playerModelPart.getId()));
    }
}
