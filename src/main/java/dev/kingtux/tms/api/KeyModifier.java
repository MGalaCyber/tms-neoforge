package dev.kingtux.tms.api;

import org.lwjgl.glfw.GLFW;

/**
 * The modifier keys TMS supports. Ported from Too Many Shortcuts (Siphalor / Wyatt Herkamp,
 * Apache-2.0), branch {@code ver/1.21}.
 */
public enum KeyModifier {
    // Order kept the same as upstream so the on-screen "Ctrl + Shift + Alt + K" ordering matches.
    ALT(GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT),
    SHIFT(GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT),
    CONTROL(GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL);

    public static final KeyModifier[] VALUES = values();

    private final int leftKey;
    private final int rightKey;

    KeyModifier(int leftKey, int rightKey) {
        this.leftKey = leftKey;
        this.rightKey = rightKey;
    }

    public boolean matches(int keyCode) {
        return keyCode == leftKey || keyCode == rightKey;
    }

    public String translationKey() {
        return "too_many_shortcuts.modifier." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static KeyModifier fromKeyCode(int keyCode) {
        for (KeyModifier m : VALUES) {
            if (m.matches(keyCode)) return m;
        }
        return null;
    }
}
