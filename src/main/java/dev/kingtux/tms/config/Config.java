package dev.kingtux.tms.config;

import java.util.LinkedHashMap;
import java.util.Map;

/** Root of {@code too_many_shortcuts.json}. Ported from upstream. */
public final class Config {
    public Map<String, ConfigKeyBinding> keybindings = new LinkedHashMap<>();
}
