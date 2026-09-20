package dev.kingtux.tms.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import dev.kingtux.tms.TooManyShortcuts;
import dev.kingtux.tms.alternatives.AlternativeKeyMapping;
import dev.kingtux.tms.api.IKeyBinding;
import net.minecraft.client.KeyMapping;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads/saves {@code config/too_many_shortcuts.json}. Ported from upstream's Kotlin
 * {@code ConfigManager}, using Gson instead of kotlinx.serialization since this is a
 * Java-only port (no Kotlin runtime dependency beyond Kotlin for Forge, which this class
 * does not need at all).
 *
 * <p>Note: upstream stored this file in the instance root ({@code .minecraft/}). This port
 * uses the standard NeoForge {@code config/} directory instead, which is the convention
 * players and config-sync mods expect.</p>
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigManager instance;

    private final Path path;
    private Config config;

    public static ConfigManager instance() {
        if (instance == null) {
            instance = new ConfigManager(FMLPaths.CONFIGDIR.get().resolve("too_many_shortcuts.json"));
        }
        return instance;
    }

    private ConfigManager(Path path) {
        this.path = path;
        load();
    }

    private void load() {
        if (!Files.exists(path)) {
            config = new Config();
            save();
            return;
        }
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            Config loaded = GSON.fromJson(text, Config.class);
            config = loaded != null ? loaded : new Config();
        } catch (IOException | JsonSyntaxException e) {
            TooManyShortcuts.LOGGER.warn("Could not read {}, using defaults", path, e);
            config = new Config();
        }
    }

    private void save() {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException e) {
            TooManyShortcuts.LOGGER.warn("Could not write {}", path, e);
        }
    }

    public void saveBindings(KeyMapping[] allKeys) {
        for (KeyMapping keyBinding : allKeys) {
            if (!(keyBinding instanceof IKeyBinding tms)) continue;
            if (tms.tms$isAlternative()) continue;

            List<ConfigBindings> alternatives = new ArrayList<>();
            List<KeyMapping> children = tms.tms$getAlternatives();
            if (children != null) {
                for (KeyMapping child : children) {
                    alternatives.add(((IKeyBinding) child).tms$toConfig());
                }
            }
            config.keybindings.put(keyBinding.getName(), new ConfigKeyBinding(tms.tms$toConfig(), alternatives));
        }
        save();
    }

    public KeyMapping[] loadBindings(KeyMapping[] allKeys) {
        List<KeyMapping> newKeys = new ArrayList<>(List.of(allKeys));
        for (var entry : config.keybindings.entrySet()) {
            String name = entry.getKey();
            ConfigKeyBinding configBindings = entry.getValue();

            KeyMapping keyBinding = newKeys.stream()
                    .filter(k -> k.getName().equals(name))
                    .findFirst()
                    .orElse(null);
            if (keyBinding == null) {
                TooManyShortcuts.LOGGER.warn("Keybinding not found, skipping: {}", name);
                continue;
            }
            ((IKeyBinding) keyBinding).tms$fromConfig(configBindings.primaryBinding);

            if (configBindings.hasAlternatives()) {
                for (ConfigBindings alt : configBindings.alternatives) {
                    newKeys.add(new AlternativeKeyMapping(keyBinding, alt));
                }
            }
        }
        return newKeys.toArray(new KeyMapping[0]);
    }
}
