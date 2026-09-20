package dev.kingtux.tms.api;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.config.ConfigBindings;
import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Implemented on {@code net.minecraft.client.KeyMapping} by {@code MixinKeyMapping}.
 * Method names keep the {@code tms$} prefix from upstream for anyone porting other mods
 * against this fork's API.
 */
public interface IKeyBinding {

    void tms$setKeyModifiers(BindingModifiers modifiers);

    BindingModifiers tms$getKeyModifiers();

    ConfigBindings tms$toConfig();

    void tms$fromConfig(ConfigBindings configBindings);

    /** Resets to the default key. If this is an alternative, alternatives param is ignored. */
    void tms$resetBinding(boolean resetAlternatives);

    /** Unbinds and clears modifiers. If this is an alternative, alternatives param is ignored. */
    void tms$clearBinding(boolean clearAlternatives);

    int tms$getTimesPressed();

    void tms$incrementTimesPressed();

    void tms$setTimesPressed(int timesPressed);

    short tms$getNextChildId();

    void tms$setNextChildId(short nextChildId);

    boolean tms$hasAlternatives();

    default boolean tms$isAlternative() {
        return tms$getParent() != null;
    }

    @Nullable
    KeyMapping tms$getParent();

    void tms$setParent(KeyMapping binding);

    @Nullable
    List<KeyMapping> tms$getAlternatives();

    int tms$getAlternativesCount();

    void tms$removeAlternative(KeyMapping binding);

    void tms$addAlternative(KeyMapping binding);

    int tms$getIndexInParent();

    InputConstants.Key tms$getBoundKey();

    void tms$setBoundKey(InputConstants.Key key);

    InputConstants.Key tms$getDefaultKey();

    /** Cascades a release down to alternatives too. */
    void tms$releaseNow();
}
