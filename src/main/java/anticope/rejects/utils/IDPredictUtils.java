package anticope.rejects.utils;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

import java.lang.reflect.Constructor;

import static meteordevelopment.meteorclient.MeteorClient.mc;


public class IDPredictUtils {
    private int highestID;

    public void checkID(int id) {
        if (id > highestID) {
            highestID = id;
        }
    }

    public void update() {
        for (Entity entity : mc.world.getEntities()) {
            checkID(entity.getId());
        }
    }

    private boolean hasIllegalItems(PlayerEntity ent) {
        return RejectEntityUtils.handsHaveItem(ent, Items.BOW) ||
                RejectEntityUtils.handsHaveItem(ent, Items.CROSSBOW) ||
                RejectEntityUtils.handsHaveItem(ent, Items.EXPERIENCE_BOTTLE);//(ent.getMainHandStack().getItem() instanceof MiningToolItem || ent.getOffHandStack().getItem() instanceof MiningToolItem)
    }

    public boolean isItGoodIdea() {
        for (PlayerEntity pe : mc.world.getPlayers()) {
            if (hasIllegalItems(pe))
                return false;
        }
        return true;
    }

    public void packetPredAttack(int swingType, int idOffset, int pktCount) {
        for (int i = 0; i < pktCount; i++) {
            int id = highestID + idOffset + i;
            Entity ent = mc.world.getEntityById(id);

            if (ent == null || ent instanceof EndCrystalEntity) {
                if (swingType == 2) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                retardedPacketAttackWorkaround(id);
                ChatUtils.info("attking %d", id);
            }
        }
        if (swingType == 1) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    private void retardedPacketAttackWorkaround(int entityId) {
        try {
            // Use reflection to access the private constructor of PlayerInteractEntityC2SPacket
            Constructor<PlayerInteractEntityC2SPacket> constructor = PlayerInteractEntityC2SPacket.class.getDeclaredConstructor(int.class, boolean.class, PlayerInteractEntityC2SPacket.InteractType.class);
            constructor.setAccessible(true);
            // Create a new instance of the packet
            PlayerInteractEntityC2SPacket packet = constructor.newInstance(entityId, mc.player.isSneaking(), PlayerInteractEntityC2SPacket.InteractType.ATTACK);
            mc.getNetworkHandler().sendPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public int getHighestID() {return highestID;}

    public void setHighestID(int id) {this.highestID = id;}
}