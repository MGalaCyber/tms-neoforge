package dev.kingtux.tms.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.KeyBindingManager;
import dev.kingtux.tms.TooManyShortcuts;
import dev.kingtux.tms.api.BindingModifiers;
import dev.kingtux.tms.api.IKeyBinding;
import dev.kingtux.tms.api.KeyModifier;
import dev.kingtux.tms.config.ConfigBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedList;
import java.util.List;

/**
 * The heart of the port: lets several {@link KeyMapping}s share one physical key, gated by
 * modifiers, and lets a binding have "alternatives" (extra binds). Ported from upstream's
 * {@code MixinKeyBinding} (Amecs API design by Siphalor, Too Many Shortcuts by Wyatt Herkamp,
 * both Apache-2.0), translated from Yarn mappings to Mojang mappings and from Fabric's static
 * hook methods to NeoForge/Mojmap's equivalents.
 */
@Mixin(KeyMapping.class)
public abstract class MixinKeyMapping implements IKeyBinding {

    @Unique
    private final BindingModifiers tms$modifiers = new BindingModifiers();
    @Unique
    private List<KeyMapping> tms$children = null;
    @Unique
    private KeyMapping tms$parent = null;
    @Unique
    private short tms$nextChildId = 0;

    @Shadow public InputConstants.Key key;
    @Shadow @Final public InputConstants.Key defaultKey;
    @Shadow @Final private String category;
    @Shadow private int clickCount;
    @Shadow private boolean isDown;

    @Shadow public abstract void setDown(boolean down);
    @Shadow public abstract boolean isDown();
    @Shadow protected abstract void release();
    @Shadow public abstract boolean isUnbound();

    // ---- static takeovers, delegate to KeyBindingManager ----------------------

    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void tms$click(InputConstants.Key key, CallbackInfo ci) {
        KeyBindingManager.onClick(key);
        ci.cancel();
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void tms$set(InputConstants.Key key, boolean pressed, CallbackInfo ci) {
        KeyBindingManager.onSet(key, pressed);
        ci.cancel();
    }

    @Inject(method = "setAll", at = @At("HEAD"), cancellable = true)
    private static void tms$setAll(CallbackInfo ci) {
        KeyBindingManager.onSetAll();
        ci.cancel();
    }

    @Inject(method = "releaseAll", at = @At("HEAD"), cancellable = true)
    private static void tms$releaseAll(CallbackInfo ci) {
        KeyBindingManager.onReleaseAll();
        ci.cancel();
    }

    // Vanilla's own body (rebuild its private MAP from ALL) still runs after our injection
    // at RETURN, since we don't cancel this one: other mods that read KeyMapping's own
    // lookup map keep working. We just also rebuild ours.
    @Inject(method = "resetMapping", at = @At("RETURN"))
    private static void tms$resetMapping(CallbackInfo ci) {
        KeyBindingManager.rebuild();
    }

    // ---- per-instance -----------------------------------------------------

    @Inject(
            method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILjava/lang/String;)V",
            at = @At("RETURN")
    )
    private void tms$onConstructed(String name, InputConstants.Type type, int code, String category, CallbackInfo ci) {
        KeyBindingManager.register((KeyMapping) (Object) this);
    }

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true)
    private void tms$matches(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && tms$hasAlternatives()) {
            for (KeyMapping child : tms$children) {
                if (child.matches(keyCode, scanCode)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
        if (cir.getReturnValueZ() && !tms$modifiersSatisfied()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "matchesMouse", at = @At("RETURN"), cancellable = true)
    private void tms$matchesMouse(int button, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && tms$hasAlternatives()) {
            for (KeyMapping child : tms$children) {
                if (child.matchesMouse(button)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
        if (cir.getReturnValueZ() && !tms$modifiersSatisfied()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isDown", at = @At("RETURN"), cancellable = true)
    private void tms$isDown(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && tms$hasAlternatives()) {
            for (KeyMapping child : tms$children) {
                if (child.isDown()) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "release", at = @At("RETURN"))
    private void tms$releaseCascade(CallbackInfo ci) {
        if (tms$hasAlternatives()) {
            for (KeyMapping child : tms$children) {
                child.setDown(false);
            }
        }
    }

    @Inject(method = "getTranslatedKeyMessage", at = @At("RETURN"), cancellable = true)
    private void tms$prefixModifiers(CallbackInfoReturnable<Component> cir) {
        if (tms$modifiers.isUnset()) return;
        MutableComponent out = Component.empty();
        for (KeyModifier modifier : KeyModifier.VALUES) {
            if (tms$modifiers.isSet(modifier)) {
                out.append(Component.translatable(modifier.translationKey())).append(" + ");
            }
        }
        cir.setReturnValue(out.append(cir.getReturnValue()));
    }

    @Inject(method = "isDefault", at = @At("HEAD"), cancellable = true)
    private void tms$isDefault(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(dev.kingtux.tms.api.Utils.isDefaultBinding((KeyMapping) (Object) this));
    }

    @Unique
    private boolean tms$modifiersSatisfied() {
        return tms$modifiers.isUnset() || tms$modifiers.equals(TooManyShortcuts.currentModifiers());
    }

    // ---- IKeyBinding --------------------------------------------------------

    @Override public void tms$setKeyModifiers(BindingModifiers modifiers) { tms$modifiers.set(modifiers); }

    @Override public BindingModifiers tms$getKeyModifiers() { return tms$modifiers; }

    @Override
    public ConfigBindings tms$toConfig() {
        return new ConfigBindings(this.key, tms$modifiers.copy());
    }

    @Override
    public void tms$fromConfig(ConfigBindings config) {
        this.key = config.resolve();
        tms$modifiers.set(config.modifiers != null ? config.modifiers : new BindingModifiers());
    }

    @Override
    public void tms$resetBinding(boolean resetAlternatives) {
        tms$modifiers.unset();
        this.key = this.defaultKey;
        Minecraft.getInstance().options.save();
        if (resetAlternatives && tms$hasAlternatives()) {
            for (KeyMapping child : tms$children) {
                ((IKeyBinding) child).tms$resetBinding(true);
            }
        }
    }

    @Override
    public void tms$clearBinding(boolean clearAlternatives) {
        tms$modifiers.unset();
        this.key = InputConstants.UNKNOWN;
        Minecraft.getInstance().options.save();
        if (clearAlternatives && tms$hasAlternatives()) {
            for (KeyMapping child : tms$children) {
                ((IKeyBinding) child).tms$clearBinding(true);
            }
        }
    }

    @Override public int tms$getTimesPressed() { return clickCount; }

    @Override
    public void tms$incrementTimesPressed() {
        if (tms$parent != null) {
            ((IKeyBinding) tms$parent).tms$incrementTimesPressed();
        }
        clickCount++;
    }

    @Override public void tms$setTimesPressed(int timesPressed) { this.clickCount = timesPressed; }

    @Override public short tms$getNextChildId() { return tms$nextChildId++; }

    @Override public void tms$setNextChildId(short nextChildId) { this.tms$nextChildId = nextChildId; }

    @Override public KeyMapping tms$getParent() { return tms$parent; }

    @Override public void tms$setParent(KeyMapping binding) { tms$parent = binding; }

    @Override public List<KeyMapping> tms$getAlternatives() { return tms$children; }

    @Override public int tms$getAlternativesCount() { return tms$children == null ? 0 : tms$children.size(); }

    @Override
    public void tms$removeAlternative(KeyMapping binding) {
        if (tms$children != null) tms$children.remove(binding);
    }

    @Override
    public void tms$addAlternative(KeyMapping binding) {
        if (tms$children == null) tms$children = new LinkedList<>();
        tms$children.add(binding);
    }

    @Override
    public int tms$getIndexInParent() {
        if (tms$parent == null) return 0;
        List<KeyMapping> siblings = ((IKeyBinding) tms$parent).tms$getAlternatives();
        return siblings == null ? 0 : siblings.indexOf((KeyMapping) (Object) this);
    }

    @Override public boolean tms$hasAlternatives() { return tms$children != null && !tms$children.isEmpty(); }

    @Override public InputConstants.Key tms$getBoundKey() { return key; }

    @Override public void tms$setBoundKey(InputConstants.Key newKey) { this.key = newKey; }

    @Override public InputConstants.Key tms$getDefaultKey() { return defaultKey; }

    @Override
    public void tms$releaseNow() {
        release();
    }
}
