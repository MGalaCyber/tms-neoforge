package dev.kingtux.tms.mixin;

import dev.kingtux.tms.config.ConfigManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks options.txt save/load to also persist modifiers and alternatives. Ported from upstream. */
@Mixin(Options.class)
public abstract class MixinOptions {

    @Shadow public KeyMapping[] keyMappings;

    @Inject(method = "save", at = @At("RETURN"))
    private void tms$save(CallbackInfo ci) {
        ConfigManager.instance().saveBindings(keyMappings);
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void tms$load(CallbackInfo ci) {
        keyMappings = ConfigManager.instance().loadBindings(keyMappings);
        KeyMapping.resetMapping();
    }
}
