/*package anticope.rejects.mixin;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerInteractEntityC2SPacket.class)
public class PlayerInteractEntityC2SPacketMixin {
    @Invoker("<init>")
    static PlayerInteractEntityC2SPacket invokeNew (
            int entityId,
            boolean playerSneaking,
            PlayerInteractEntityC2SPacket.InteractTypeHandler type
    ) {
        throw new AssertionError();
    }
}
*/