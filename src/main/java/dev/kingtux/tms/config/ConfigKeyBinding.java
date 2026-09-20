package dev.kingtux.tms.config;

import java.util.ArrayList;
import java.util.List;

/** A primary binding plus its alternatives, as stored on disk. Ported from upstream. */
public final class ConfigKeyBinding {
    public ConfigBindings primaryBinding;
    public List<ConfigBindings> alternatives = new ArrayList<>();

    public ConfigKeyBinding() {}

    public ConfigKeyBinding(ConfigBindings primaryBinding) {
        this.primaryBinding = primaryBinding;
    }

    public ConfigKeyBinding(ConfigBindings primaryBinding, List<ConfigBindings> alternatives) {
        this.primaryBinding = primaryBinding;
        this.alternatives = alternatives;
    }

    public boolean hasAlternatives() {
        return alternatives != null && !alternatives.isEmpty();
    }
}
