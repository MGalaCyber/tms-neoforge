package dev.kingtux.tms;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.api.BindingModifiers;
import dev.kingtux.tms.config.ConfigManager;
import dev.kingtux.tms.gui.TMSKeyBindsScreen;
import dev.kingtux.tms.keybinding.SkinLayerKeyMapping;
import dev.kingtux.tms.keybinding.ToggleAutoJumpKeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.entity.player.PlayerModelPart;

/**
 * Client-side wiring: keybind registration and swapping vanilla's own "Key Binds..."
 * screen for {@link TMSKeyBindsScreen}, so there's exactly one keybinds menu instead
 * of a second button/page bolted on next to it.
 *
 * <p>Registered manually (via addListener) in {@link #register} — deliberately no
 * {@code @EventBusSubscriber} here, since this class has no {@code @SubscribeEvent}
 * static methods; that mismatch previously made NeoForge throw during mod
 * construction and take the whole load down with it.</p>
 */
public final class ClientSetup {
    private ClientSetup() {}

    /**
     * The screen open right before vanilla's Key Binds screen was about to open
     * (almost always the Controls screen). Used so TMSKeyBindsScreen's Done/Escape
     * goes back to the right place, the same way vanilla's own screen would.
     */
    private static Screen lastScreen;

    static void register(IEventBus modBus) {
        modBus.addListener(ClientSetup::onClientSetup);
        modBus.addListener(ClientSetup::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientSetup::trackLastScreen);
        NeoForge.EVENT_BUS.addListener(ClientSetup::replaceKeyBindsScreen);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Touches the singleton so the config file exists before the first save.
            ConfigManager.instance();
            TooManyShortcuts.LOGGER.info("Too Many Shortcuts (NeoForge port) ready");
        });
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TooManyShortcuts.ESCAPE_KEY_MAPPING);

        event.register(new ToggleAutoJumpKeyMapping(
                "key." + TooManyShortcuts.MOD_ID + ".toggle_auto_jump",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_B,
                "key.categories.movement",
                new BindingModifiers()
        ));

        for (PlayerModelPart part : PlayerModelPart.values()) {
            event.register(new SkinLayerKeyMapping(
                    "key." + TooManyShortcuts.MOD_ID + ".toggle_" + part.getId(),
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    TooManyShortcuts.SKIN_LAYER_CATEGORY,
                    part
            ));
        }
    }

    /**
     * Remembers the most recent non-TMS, non-vanilla-KeyBinds screen, as a "go back to" target.
     *
     * <p>Vanilla's {@link KeyBindsScreen} must never be captured here. Its {@code Init.Pre} is
     * always canceled by {@link #replaceKeyBindsScreen} below, which means its own
     * {@code init()} (the one that builds its {@code keyBindsList} widget) never runs — yet
     * {@code Init.Post} still fires for it right afterwards. If we stored that half-built
     * screen as {@code lastScreen}, a later {@code TMSKeyBindsScreen} could end up with it as
     * its {@code parent}, and pressing Done would call {@code Minecraft#setScreen} on it: since
     * the screen is already marked as initialized, Minecraft skips {@code init()} and calls
     * {@code repositionElements()} directly, which crashes with a NullPointerException because
     * {@code keyBindsList} was never set. Excluding it here means that broken instance can
     * never become a "go back to" target in the first place.</p>
     */
    private static void trackLastScreen(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof TMSKeyBindsScreen) return;
        if (screen instanceof KeyBindsScreen) return;
        lastScreen = screen;
    }

    /** Whenever vanilla is about to open its own Key Binds screen, open ours instead. */
    private static void replaceKeyBindsScreen(ScreenEvent.Init.Pre event) {
        if (!(event.getScreen() instanceof KeyBindsScreen)) return;
        event.setCanceled(true);
        Minecraft.getInstance().setScreen(new TMSKeyBindsScreen(lastScreen));
    }
}
