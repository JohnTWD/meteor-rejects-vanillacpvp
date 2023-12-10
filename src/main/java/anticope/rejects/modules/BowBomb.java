// thanks https://github.com/0x4D2D/InstantKillBow/blob/master/src/main/java/me/saturn/instantkill/InstantKill.java#L11

package anticope.rejects.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.systems.modules.Module;

public class BowBomb extends Module {
    public static boolean shouldAddVelocity = true;
    public BowBomb() {
        super(MeteorRejectsAddon.CATEGORY, "BowBomb", "u know what this is ;)");
    }

    public static void addVelocityToPlayer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if(shouldAddVelocity){
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            for (int i = 0; i < 100; i++) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.000000001, mc.player.getZ(), true));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(), false));
            }
        }
    }
}