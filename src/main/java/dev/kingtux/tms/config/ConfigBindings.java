package dev.kingtux.tms.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.api.BindingModifiers;

/** One bound key + its modifiers, as stored on disk. Ported from upstream. */
public final class ConfigBindings {
    /**
     * Human-readable translation key (e.g. {@code "key.keyboard.end"}), kept only so the JSON
     * file stays readable and for backwards compatibility with configs saved before {@link #type}
     * and {@link #code} existed.
     */
    public String key;

    /**
     * {@link InputConstants.Type} name ("KEYSYM"/"SCANCODE"/"MOUSE") and raw numeric code.
     * This is what {@link #resolve()} actually uses to rebuild the key.
     *
     * <p>{@code key} alone isn't enough: {@link InputConstants#getKey(String)} only resolves
     * names vanilla has pre-registered (every normal KEYSYM/MOUSE key has one). A key GLFW
     * reports as {@link InputConstants.Type#SCANCODE} — which happens for some laptops' Fn-combo
     * keys (e.g. Fn+Right Arrow for End) when the driver reports a scancode GLFW doesn't map to
     * a known keysym — has no such registered name, so looking it up by name silently falls back
     * to UNKNOWN and the binding goes unbound on the very next load. Storing type+code instead
     * round-trips any key exactly, regardless of whether vanilla has a name for it.</p>
     */
    public String type;
    public Integer code;

    public BindingModifiers modifiers = new BindingModifiers();

    public ConfigBindings() {}

    public ConfigBindings(InputConstants.Key key, BindingModifiers modifiers) {
        this.key = key.getName();
        this.type = key.getType().name();
        this.code = key.getValue();
        this.modifiers = modifiers;
    }

    public boolean hasModifiers() {
        return modifiers != null && modifiers.hasModifiers();
    }

    /** Rebuilds the actual {@link InputConstants.Key}, preferring type+code over the name. */
    public InputConstants.Key resolve() {
        if (type != null && code != null) {
            try {
                return InputConstants.Type.valueOf(type).getOrCreate(code);
            } catch (IllegalArgumentException ignored) {
                // Unknown/corrupted type string (e.g. hand-edited config) - fall through.
            }
        }
        // Legacy path, for configs saved before type+code existed.
        if (key != null) {
            InputConstants.Key parsed = InputConstants.getKey(key);
            if (parsed != null) return parsed;
        }
        return InputConstants.UNKNOWN;
    }
}
