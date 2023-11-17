package anticope.rejects.mixin;

import anticope.rejects.modules.MacroAnchorAuto;
import anticope.rejects.modules.ManualCrystal;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "doItemUse", at = @At(value = "HEAD"), cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        // Check if building blocks should be disabled
        if (Modules.get().get(MacroAnchorAuto.class).disablePlacing()) {
            // Cancel the event to prevent block placement
            ci.cancel();
        }
        if (Modules.get().get(ManualCrystal.class).shouldStopItemUse())
            ci.cancel();
    }
}
