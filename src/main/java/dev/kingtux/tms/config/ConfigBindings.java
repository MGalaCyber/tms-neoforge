package dev.kingtux.tms.config;

import dev.kingtux.tms.api.BindingModifiers;

/** One bound key + its modifiers, as stored on disk. Ported from upstream. */
public final class ConfigBindings {
    public String key;
    public BindingModifiers modifiers = new BindingModifiers();

    public ConfigBindings() {}

    public ConfigBindings(String key, BindingModifiers modifiers) {
        this.key = key;
        this.modifiers = modifiers;
    }

    public boolean hasModifiers() {
        return modifiers != null && modifiers.hasModifiers();
    }
}
