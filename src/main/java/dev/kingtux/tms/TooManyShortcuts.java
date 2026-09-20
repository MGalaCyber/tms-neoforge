package dev.kingtux.tms;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.api.BindingModifiers;
import dev.kingtux.tms.api.TMSKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entry point. Ported from Too Many Shortcuts (github.com/wyatt-herkamp/too-many-shortcuts,
 * Apache-2.0, branch {@code ver/1.21}) for NeoForge 1.21.1.
 */
@Mod(value = TooManyShortcuts.MOD_ID, dist = Dist.CLIENT)
public final class TooManyShortcuts {
    public static final String MOD_ID = "too_many_shortcuts";
    public static final String MOD_NAME = "Too Many Shortcuts";
    public static final String SKIN_LAYER_CATEGORY = MOD_ID + ".key.categories.skin_layers";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    /** Held-modifier state, updated from the keyboard mixin every key event. */
    private static final BindingModifiers CURRENT_MODIFIERS = new BindingModifiers();

    /**
     * "Alternative escape" key: an extra key/mouse-button that behaves like Escape (closes
     * the current screen, or opens the pause menu). Registered eagerly here so other classes
     * (the keyboard mixin) can reference it before {@code RegisterKeyMappingsEvent} fires.
     */
    public static final TMSKeyMapping ESCAPE_KEY_MAPPING = new TMSKeyMapping(
            "key." + MOD_ID + ".alternative_escape",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories." + MOD_ID,
            new BindingModifiers()
    );

    public TooManyShortcuts(IEventBus modBus) {
        ClientSetup.register(modBus);
    }

    public static BindingModifiers currentModifiers() {
        return CURRENT_MODIFIERS;
    }

    public static void sendToggleMessage(Player player, boolean value, Component option) {
        player.displayClientMessage(
                Component.translatable("too_many_shortcuts.toggled." + (value ? "on" : "off"), option),
                true
        );
    }
}
