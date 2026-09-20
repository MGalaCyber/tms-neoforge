package dev.kingtux.tms.api;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Builds the "Ctrl + Shift + " style prefix shown in front of a keybind's label. Ported from
 * upstream; upstream's four size variations (normal/short/tiny/compressed) are collapsed to
 * two here (normal/tiny) since NeoForge doesn't need the extra granularity for width-fitting
 * in the custom TMS screen — add lang keys ending in {@code .tiny} if you want a third size.
 */
public final class ModifierPrefixTextProvider {
    private final String translationKey;

    public ModifierPrefixTextProvider(KeyModifier modifier) {
        this.translationKey = modifier.translationKey();
    }

    public MutableComponent getText(boolean tiny) {
        String key = tiny ? translationKey + ".tiny" : translationKey;
        return Component.translatable(key).copy().append(tiny ? "+" : " + ");
    }
}
