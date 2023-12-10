package anticope.rejects.mixin;

import anticope.rejects.events.StopUsingItemEvent;
import anticope.rejects.modules.BowBomb;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    public void onStopUsingItem(PlayerEntity player, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(StopUsingItemEvent.get(player.getInventory().getMainHandStack()));

        if (Modules.get().get(BowBomb.class).isActive() && player.getInventory().getMainHandStack().getItem().equals(Items.BOW)) {
            BowBomb.addVelocityToPlayer();
        }
    }
}