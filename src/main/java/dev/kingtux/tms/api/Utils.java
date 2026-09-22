package dev.kingtux.tms.api;

import dev.kingtux.tms.TooManyShortcuts;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

/** Free functions from upstream's Kotlin {@code utils.kt}, as static methods. */
public final class Utils {
    private Utils() {}

    public static boolean isDefaultBinding(KeyMapping keyBinding) {
        IKeyBinding tms = (IKeyBinding) keyBinding;
        if (!tms.tms$hasAlternatives()) {
            return keyBinding.getDefaultKey().equals(tms.tms$getBoundKey());
        }
        for (KeyMapping alt : tms.tms$getAlternatives()) {
            if (!alt.isDefault()) return false;
        }
        return keyBinding.getDefaultKey().equals(tms.tms$getBoundKey());
    }

    public static boolean isAlternative(KeyMapping keyBinding) {
        return ((IKeyBinding) keyBinding).tms$isAlternative();
    }

    public static boolean hasConflicts(KeyMapping keyBinding, Options options) {
        for (KeyMapping other : options.keyMappings) {
            if (other == keyBinding) continue;
            if (((IKeyBinding) other).tms$getBoundKey().equals(((IKeyBinding) keyBinding).tms$getBoundKey())
                    && !((IKeyBinding) other).tms$getBoundKey().equals(com.mojang.blaze3d.platform.InputConstants.UNKNOWN)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Matches the currently-bound key's label (e.g. "Ctrl + Down Arrow") against a filter typed
     * into the search box. {@code keyFilter == null} means "no search box text" and always
     * matches, so every binding shows.
     */
    public static boolean entryKeyMatches(KeyMapping keyBinding, String keyFilter) {
        if (keyFilter == null) return true;
        return StringUtils.containsIgnoreCase(keyBinding.getTranslatedKeyMessage().getString(), keyFilter);
    }

    /** Matches the binding's action name (e.g. "Place item") against the search box text. */
    public static boolean translatedTextEqualsIgnoreCase(KeyMapping keyBinding, String search) {
        if (search == null || search.isEmpty()) return true;
        return StringUtils.containsIgnoreCase(Component.translatable(keyBinding.getName()).getString(), search);
    }

    public static void resetBinding(KeyMapping keyBinding, boolean resetAlternatives) {
        if (!(keyBinding instanceof IKeyBinding tms)) {
            logInvalid(keyBinding);
            return;
        }
        tms.tms$resetBinding(resetAlternatives);
    }

    public static void clearBinding(KeyMapping keyBinding, boolean clearAlternatives) {
        if (!(keyBinding instanceof IKeyBinding tms)) {
            logInvalid(keyBinding);
            return;
        }
        tms.tms$clearBinding(clearAlternatives);
    }

    public static void logInvalid(KeyMapping keyBinding) {
        TooManyShortcuts.LOGGER.error(
                "KeyMapping {} is not an IKeyBinding. This should never happen; the mixin may have failed to apply. Class: {}",
                keyBinding, keyBinding.getClass().getName());
    }

    public static boolean hasModifiedKeyBindings(Options options) {
        for (KeyMapping keyBinding : options.keyMappings) {
            if (!keyBinding.isDefault()) return true;
        }
        return false;
    }
}
