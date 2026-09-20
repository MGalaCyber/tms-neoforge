package dev.kingtux.tms.api;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Base class for TMS's own keybinds (auto-jump toggle, skin-layer toggles, the alternative-
 * escape key). Ported from upstream's {@code TMSKeyBinding}.
 */
public class TMSKeyMapping extends KeyMapping {
    private final BindingModifiers defaultModifiers;

    public TMSKeyMapping(String name, InputConstants.Type type, int code, String category, BindingModifiers defaultModifiers) {
        super(name, type, code, category);
        this.defaultModifiers = defaultModifiers == null ? new BindingModifiers() : defaultModifiers;
        ((IKeyBinding) (Object) this).tms$getKeyModifiers().set(this.defaultModifiers);
    }

    @Override
    public void setDown(boolean down) {
        boolean was = isDown();
        super.setDown(down);
        if (down && !was) {
            onPressed();
        } else if (!down && was) {
            onReleased();
        }
    }

    /** Called once when the binding transitions from up to down. */
    public void onPressed() {}

    /** Called once when the binding transitions from down to up. */
    public void onReleased() {}

    /** Fired by the "Reset" button in the TMS screen. */
    public void resetKeyBinding() {
        ((IKeyBinding) (Object) this).tms$getKeyModifiers().set(defaultModifiers);
    }

    public BindingModifiers getDefaultModifiers() {
        return defaultModifiers;
    }

    @Override
    public boolean isDefault() {
        if (getDefaultKey().equals(InputConstants.UNKNOWN)) {
            return true;
        }
        return super.isDefault();
    }
}
