package dev.kingtux.tms.api;

import dev.kingtux.tms.KeyBindingManager;
import net.minecraft.client.KeyMapping;

import java.util.Map;

/** Utility methods for TMS/Amecs-style key bindings. Ported from upstream. */
public final class KeyBindingUtils {
    private KeyBindingUtils() {}

    /** The vanilla name-to-KeyMapping map ({@code KeyMapping.ALL}). Use with care. */
    public static Map<String, KeyMapping> getIdToKeyBindingMap() {
        return KeyMapping.ALL;
    }

    /**
     * Unregisters a keybinding from input querying but leaves it in the controls GUI's
     * backing array. Safe to call after game init.
     */
    public static boolean unregisterKeyBinding(KeyMapping keyBinding) {
        if (keyBinding == null) return false;
        return KeyBindingManager.unregister(keyBinding);
    }

    /** Registers a keybinding for input querying without adding it to the controls GUI. */
    public static boolean registerHiddenKeyBinding(KeyMapping keyBinding) {
        return KeyBindingManager.register(keyBinding);
    }

    public static BindingModifiers getBoundModifiers(KeyMapping keyBinding) {
        return ((IKeyBinding) keyBinding).tms$getKeyModifiers();
    }

    public static BindingModifiers getDefaultModifiers(KeyMapping keyBinding) {
        if (keyBinding instanceof TMSKeyMapping tms) {
            return tms.getDefaultModifiers();
        }
        return new BindingModifiers();
    }

    public static void resetBoundModifiers(KeyMapping keyBinding) {
        ((IKeyBinding) keyBinding).tms$getKeyModifiers().unset();
        if (keyBinding instanceof TMSKeyMapping tms) {
            tms.resetKeyBinding();
        }
    }
}
