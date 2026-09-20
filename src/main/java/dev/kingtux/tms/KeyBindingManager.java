package dev.kingtux.tms;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.api.BindingModifiers;
import dev.kingtux.tms.api.IKeyBinding;
import dev.kingtux.tms.api.KeyModifier;
import dev.kingtux.tms.api.PriorityKeyBinding;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Replaces vanilla's one-KeyMapping-per-key bookkeeping. Several bindings may share a key as
 * long as their modifier sets differ. Ported from upstream's {@code de.siphalor.amecs.KeyBindingManager}
 * (Amecs API, Siphalor, Apache-2.0) as adapted by Too Many Shortcuts (Wyatt Herkamp, Apache-2.0).
 */
public final class KeyBindingManager {
    public static final Map<InputConstants.Key, List<KeyMapping>> keysById = new HashMap<>();
    public static final Map<InputConstants.Key, List<KeyMapping>> priorityKeysById = new HashMap<>();

    private static final List<KeyMapping> pressedKeyBindings = new ArrayList<>(10);

    private KeyBindingManager() {}

    private static Map<InputConstants.Key, List<KeyMapping>> mapFor(KeyMapping binding) {
        return (binding instanceof PriorityKeyBinding) ? priorityKeysById : keysById;
    }

    public static boolean register(KeyMapping binding) {
        InputConstants.Key key = ((IKeyBinding) binding).tms$getBoundKey();
        return register(binding, key);
    }

    public static boolean register(KeyMapping binding, InputConstants.Key key) {
        if (key == null || key.equals(InputConstants.UNKNOWN)) return false;
        List<KeyMapping> list = mapFor(binding).computeIfAbsent(key, k -> new ArrayList<>());
        if (list.contains(binding)) return false;
        return list.add(binding);
    }

    public static boolean unregister(KeyMapping binding) {
        if (binding == null) return false;
        boolean removed = false;
        removed |= removeFrom(keysById, binding);
        removed |= removeFrom(priorityKeysById, binding);
        return removed;
    }

    private static boolean removeFrom(Map<InputConstants.Key, List<KeyMapping>> target, KeyMapping binding) {
        boolean removed = false;
        for (List<KeyMapping> list : target.values()) {
            while (list.remove(binding)) removed = true;
        }
        return removed;
    }

    /** Rebuilds both maps from {@code KeyMapping.ALL}. Called from resetMapping. */
    public static void rebuild() {
        keysById.clear();
        priorityKeysById.clear();
        for (KeyMapping binding : KeyMapping.ALL.values()) {
            register(binding);
        }
    }

    /**
     * Bindings on {@code key} whose modifier set matches what is currently held. If nothing
     * matches exactly, falls back to bindings with no modifiers bound.
     */
    public static Stream<KeyMapping> getMatching(InputConstants.Key key, boolean priority) {
        List<KeyMapping> candidates = (priority ? priorityKeysById : keysById).get(key);
        if (candidates == null || candidates.isEmpty()) return Stream.empty();

        BindingModifiers current = TooManyShortcuts.currentModifiers();
        // When the pressed key is itself a modifier, also test with that modifier cleared,
        // so a binding on plain "Left Ctrl" (no modifier requirement) still fires.
        BindingModifiers relaxed = current.copy();
        KeyModifier self = KeyModifier.fromKeyCode(key.getValue());
        if (self != null) relaxed.set(self, false);

        List<KeyMapping> matches = candidates.stream()
                .filter(b -> {
                    BindingModifiers bound = ((IKeyBinding) b).tms$getKeyModifiers();
                    return bound.equals(relaxed) || bound.equals(current);
                })
                .toList();

        if (matches.isEmpty()) {
            return candidates.stream().filter(b -> ((IKeyBinding) b).tms$getKeyModifiers().isUnset());
        }
        return matches.stream();
    }

    // ---- vanilla static hooks, invoked from MixinKeyMapping -------------------

    /** Replaces {@code KeyMapping.click}. */
    public static void onClick(InputConstants.Key key) {
        getMatching(key, false).forEach(b -> ((IKeyBinding) b).tms$incrementTimesPressed());
    }

    /** Replaces {@code KeyMapping.set}. */
    public static void onSet(InputConstants.Key key, boolean pressed) {
        forEachWithKey(key, b -> setPressed(b, pressed));
        if (!pressed) releaseStaleBindings();
    }

    /** Replaces {@code KeyMapping.setAll}. */
    public static void onSetAll() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        forEachBinding(binding -> {
            InputConstants.Key key = ((IKeyBinding) binding).tms$getBoundKey();
            if (binding.isUnbound()) return;
            boolean down = key.getType() == InputConstants.Type.KEYSYM
                    && InputConstants.isKeyDown(window, key.getValue());
            setPressed(binding, down);
        });
    }

    /** Replaces {@code KeyMapping.releaseAll}. */
    public static void onReleaseAll() {
        pressedKeyBindings.clear();
        for (KeyMapping binding : KeyMapping.ALL.values()) {
            ((IKeyBinding) binding).tms$releaseNow();
        }
    }

    public static boolean onPressedPriority(InputConstants.Key key) {
        return getMatching(key, true)
                .anyMatch(b -> b instanceof PriorityKeyBinding p && p.onPressedPriority());
    }

    public static boolean onReleasedPriority(InputConstants.Key key) {
        return getMatching(key, true)
                .anyMatch(b -> b instanceof PriorityKeyBinding p && p.onReleasedPriority());
    }

    // ---- helpers ----------------------------------------------------------

    public static void setPressed(KeyMapping binding, boolean pressed) {
        if (pressed != binding.isDown()) {
            if (pressed) pressedKeyBindings.add(binding);
            else pressedKeyBindings.remove(binding);
        }
        binding.setDown(pressed);
    }

    private static void forEachBinding(Consumer<KeyMapping> consumer) {
        priorityKeysById.values().forEach(l -> l.forEach(consumer));
        keysById.values().forEach(l -> l.forEach(consumer));
    }

    private static void forEachWithKey(InputConstants.Key key, Consumer<KeyMapping> consumer) {
        getMatching(key, true).forEach(consumer);
        getMatching(key, false).forEach(consumer);
    }

    /** When a modifier goes up, bindings that required it must go up too. */
    private static void releaseStaleBindings() {
        BindingModifiers current = TooManyShortcuts.currentModifiers();
        pressedKeyBindings.removeIf(binding -> {
            BindingModifiers bound = ((IKeyBinding) binding).tms$getKeyModifiers();
            if (bound.isUnset()) return false;
            if (!current.contains(bound)) {
                binding.setDown(false);
                return true;
            }
            return false;
        });
    }
}
