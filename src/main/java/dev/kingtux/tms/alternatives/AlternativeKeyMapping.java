package dev.kingtux.tms.alternatives;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.api.BindingModifiers;
import dev.kingtux.tms.api.IKeyBinding;
import dev.kingtux.tms.config.ConfigBindings;
import net.minecraft.client.KeyMapping;

/**
 * An extra bind for an existing keybind ("alternative" in upstream's terminology). Every
 * one of these is its own fully independent {@link KeyMapping}, registered under a unique
 * synthetic name ({@code parentName%N}) so it never collides with the parent's entry in
 * {@code KeyMapping.ALL}. Ported from upstream's {@code AlternativeKeyBinding}.
 */
public final class AlternativeKeyMapping extends KeyMapping {

    public AlternativeKeyMapping(KeyMapping parent, String translationKey, InputConstants.Key code) {
        super(translationKey, code.getType(), code.getValue(), parent.getCategory());
        if (!(parent instanceof IKeyBinding)) {
            throw new IllegalArgumentException("Parent keybinding is not an IKeyBinding (mixin missing?)");
        }
        ((IKeyBinding) parent).tms$addAlternative(this);
        ((IKeyBinding) (Object) this).tms$setParent(parent);
    }

    public AlternativeKeyMapping(KeyMapping parent) {
        this(parent, nextTranslationKey(parent), InputConstants.UNKNOWN);
    }

    public AlternativeKeyMapping(KeyMapping parent, ConfigBindings config) {
        this(parent, nextTranslationKey(parent), InputConstants.UNKNOWN);
        ((IKeyBinding) (Object) this).tms$setBoundKey(config.resolve());
        BindingModifiers modifiers = config.modifiers != null ? config.modifiers : new BindingModifiers();
        ((IKeyBinding) (Object) this).tms$setKeyModifiers(modifiers);
    }

    private static String nextTranslationKey(KeyMapping parent) {
        return parent.getName() + "%" + ((IKeyBinding) parent).tms$getNextChildId();
    }

    @Override
    public boolean isDefault() {
        return getDefaultKey().equals(((IKeyBinding) (Object) this).tms$getBoundKey());
    }
}
