package dev.kingtux.tms.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.KeyBindingManager;
import dev.kingtux.tms.TooManyShortcuts;
import dev.kingtux.tms.api.IKeyBinding;
import dev.kingtux.tms.api.KeyModifier;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks held modifiers, dispatches priority keys, and implements the "alternative escape"
 * key. Ported from upstream's {@code MixinKeyboard}; the original anchored on a private debug-
 * timer field to find the right injection point mid-method. This port injects at {@code HEAD}
 * instead, which is less precise about ordering relative to vanilla's own key handling but far
 * less likely to silently break on a future 1.21.x patch.
 */
@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboardHandler {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void tms$keyPress(long window, int keyCode, int scanCode, int action, int modifiers, CallbackInfo ci) {
        KeyModifier pressedModifier = KeyModifier.fromKeyCode(keyCode);
        if (pressedModifier != null) {
            TooManyShortcuts.currentModifiers().set(pressedModifier, action != GLFW.GLFW_RELEASE);
        }

        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);

        // Alternative escape: an extra bind that behaves like vanilla Escape.
        if (action == GLFW.GLFW_PRESS && tms$matchesEscape(key)) {
            Minecraft client = Minecraft.getInstance();
            Screen screen = client.screen;
            if (screen != null) {
                screen.onClose();
            } else {
                client.pauseGame(false);
            }
            ci.cancel();
            return;
        }

        if (action == GLFW.GLFW_PRESS) {
            if (KeyBindingManager.onPressedPriority(key)) ci.cancel();
        } else if (action == GLFW.GLFW_RELEASE) {
            if (KeyBindingManager.onReleasedPriority(key)) ci.cancel();
        }
    }

    private boolean tms$matchesEscape(InputConstants.Key key) {
        IKeyBinding escape = (IKeyBinding) (Object) TooManyShortcuts.ESCAPE_KEY_MAPPING;
        InputConstants.Key bound = escape.tms$getBoundKey();
        if (bound.equals(InputConstants.UNKNOWN) || !bound.equals(key)) return false;
        return escape.tms$getKeyModifiers().isUnset()
                || escape.tms$getKeyModifiers().equals(TooManyShortcuts.currentModifiers());
    }
}
