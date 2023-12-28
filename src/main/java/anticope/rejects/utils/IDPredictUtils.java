package anticope.rejects.utils;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static meteordevelopment.meteorclient.MeteorClient.mc;


public class IDPredictUtils {

    private final ScheduledExecutorService execSvc = Executors.newSingleThreadScheduledExecutor();

    private int highestID = -1;

    public void checkID(int id) {
        if (id > highestID) {
            setHighestID(id);
        }
    }

    public void update() {
        if (mc.world == null) {
            setHighestID(-1);
            return;
        }

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

    public void packetPredAttack(int swingType, int idOffset, int pktCount, int sendSleepTime, boolean idDebug) {
        update();
        if(idDebug)
            ChatUtils.info("highest: 0x%X", highestID);

        execSvc.schedule(() -> {
            if (idDebug)
                ChatUtils.info("currently attacking 0x%X to 0x%X", highestID + idOffset, highestID + idOffset + pktCount - 1);
            for (int i = 0; i < pktCount; i++) {
                int id = highestID + idOffset + i;
                Entity ent = mc.world.getEntityById(id);

                if (ent == null || ent instanceof EndCrystalEntity) {
                    if (swingType == 2) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    retardedPacketAttackWorkaround(id);
                }
            }
            if (swingType == 1) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

            setHighestID(-1);
        }, sendSleepTime, TimeUnit.MILLISECONDS);
    }

    private void retardedPacketAttackWorkaround(int entityId) {
        mc.getNetworkHandler().sendPacket(
                 new PlayerInteractEntityC2SPacket(
                        entityId,
                        mc.player.isSneaking(),
                        PlayerInteractEntityC2SPacket.ATTACK
                )
        );
    }

    public int getHighestID() {return highestID;}

    public void setHighestID(int id) {this.highestID = id;}
}