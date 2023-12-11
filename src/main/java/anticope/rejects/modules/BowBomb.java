// thanks https://github.com/0x4D2D/InstantKillBow/blob/master/src/main/java/me/saturn/instantkill/InstantKill.java#L11

package anticope.rejects.modules;

import meteordevelopment.meteorclient.settings.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.systems.modules.Module;

public class BowBomb extends Module {
    public BowBomb() {
        super(MeteorRejectsAddon.CATEGORY, "BowBomb", "u know what this is ;)");
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> power = sgGeneral.add(new IntSetting.Builder()
            .name("power")
            .description("experimental")
            .defaultValue(100)
            .range(50,1000)
            .sliderRange(50,1000)
            .build()
    );

    public void addVelocityToPlayer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        for (int i = 0; i < power.get(); i++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.000000001, mc.player.getZ(), true));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(), false));
        }
    }
}