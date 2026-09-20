package dev.kingtux.tms;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.api.BindingModifiers;
import dev.kingtux.tms.config.ConfigManager;
import dev.kingtux.tms.gui.TMSKeyBindsScreen;
import dev.kingtux.tms.keybinding.SkinLayerKeyMapping;
import dev.kingtux.tms.keybinding.ToggleAutoJumpKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-side wiring: keybind registration and the Controls-screen button. */
@EventBusSubscriber(modid = TooManyShortcuts.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    static void register(IEventBus modBus) {
        modBus.addListener(ClientSetup::onClientSetup);
        modBus.addListener(ClientSetup::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onScreenInit);
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

    /** Adds a "Too Many Shortcuts..." button to the vanilla Controls screen. */
    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ControlsScreen screen)) return;

        int x = screen.width / 2 + 5;
        int y = screen.height - 27;
        Button button = Button.builder(
                        Component.translatable("too_many_shortcuts.open_screen"),
                        b -> screen.getMinecraft().setScreen(new TMSKeyBindsScreen(screen))
                )
                .bounds(x, y, 150, 20)
                .build();
        event.addListener(button);
    }
}
