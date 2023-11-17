package anticope.rejects.mixin;

import anticope.rejects.events.OffGroundSpeedEvent;
import anticope.rejects.modules.MacroAnchorAuto;
import anticope.rejects.modules.ManualCrystal;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "getOffGroundSpeed", at = @At("RETURN"), cancellable = true)
    private void onGetOffGroundSpeed(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(MeteorClient.EVENT_BUS.post(OffGroundSpeedEvent.get(cir.getReturnValueF())).speed);
    }

    @Inject(
            method = "eatFood",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onEatFood(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        // Check if the right-click button is held down
        if (Modules.get().get(ManualCrystal.class).shouldStopItemUse()) {
            // Cancel the event to prevent eating
            cir.setReturnValue(stack);
        }
    }
}
