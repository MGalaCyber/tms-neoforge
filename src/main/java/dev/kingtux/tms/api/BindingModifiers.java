package dev.kingtux.tms.api;

import dev.kingtux.tms.TooManyShortcuts;

/** A mutable Shift/Ctrl/Alt combination. Ported from upstream's {@code BindingModifiers}. */
public final class BindingModifiers {
    public boolean shift;
    public boolean ctrl;
    public boolean alt;

    public BindingModifiers() {}

    public BindingModifiers(boolean shift, boolean ctrl, boolean alt) {
        this.shift = shift;
        this.ctrl = ctrl;
        this.alt = alt;
    }

    public boolean hasModifiers() { return shift || ctrl || alt; }

    public boolean isUnset() { return !shift && !ctrl && !alt; }

    public void unset() { shift = ctrl = alt = false; }

    public boolean isSet(KeyModifier modifier) {
        return switch (modifier) {
            case SHIFT -> shift;
            case CONTROL -> ctrl;
            case ALT -> alt;
        };
    }

    public void set(KeyModifier modifier, boolean value) {
        switch (modifier) {
            case SHIFT -> shift = value;
            case CONTROL -> ctrl = value;
            case ALT -> alt = value;
        }
    }

    public void set(BindingModifiers other) {
        this.shift = other.shift;
        this.ctrl = other.ctrl;
        this.alt = other.alt;
    }

    /** True when every modifier set in {@code other} is also set here. */
    public boolean contains(BindingModifiers other) {
        return (!other.shift || shift) && (!other.ctrl || ctrl) && (!other.alt || alt);
    }

    public boolean isPressed() {
        return this.equals(TooManyShortcuts.currentModifiers());
    }

    public BindingModifiers copy() { return new BindingModifiers(shift, ctrl, alt); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BindingModifiers other)) return false;
        return shift == other.shift && ctrl == other.ctrl && alt == other.alt;
    }

    @Override
    public int hashCode() {
        return (shift ? 1 : 0) | (ctrl ? 2 : 0) | (alt ? 4 : 0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (ctrl) sb.append("Ctrl+");
        if (shift) sb.append("Shift+");
        if (alt) sb.append("Alt+");
        return sb.isEmpty() ? "none" : sb.substring(0, sb.length() - 1);
    }
}
