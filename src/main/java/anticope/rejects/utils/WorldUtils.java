package anticope.rejects.utils;

import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.world.BlockUtils.canPlace;
import static meteordevelopment.meteorclient.utils.world.BlockUtils.getPlaceSide;

public class WorldUtils {
    public enum SwitchMode {
        Packet,
        Client,
        Both
    }

    public enum PlaceMode {
        Packet,
        Client,
        Both
    }

    public enum AirPlaceDirection {
        Up,
        Down
    }
    private static final Vec3d hitPos = new Vec3d(0, 0, 0);

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, SwitchMode switchMode, PlaceMode placeMode, boolean onlyAirplace, AirPlaceDirection airPlaceDirection, boolean swingHand, boolean checkEntities, boolean swapBack) {
        if (findItemResult.isOffhand()) {
            return place(blockPos, Hand.OFF_HAND, mc.player.getInventory().selectedSlot, mc.player.getInventory().selectedSlot, rotate, rotationPriority, switchMode, placeMode, onlyAirplace, airPlaceDirection, swingHand, checkEntities, swapBack);
        } else if (findItemResult.isHotbar()) {
            return place(blockPos, Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, findItemResult.slot(), rotate, rotationPriority, switchMode, placeMode, onlyAirplace, airPlaceDirection, swingHand, checkEntities, swapBack);
        }
        return false;
    }

    public static boolean place(BlockPos blockPos, Hand hand, int oldSlot, int targetSlot, boolean rotate, int rotationPriority, SwitchMode switchMode, PlaceMode placeMode, boolean onlyAirplace, AirPlaceDirection airPlaceDirection, boolean swingHand, boolean checkEntities, boolean swapBack) {
        if (targetSlot < 0 || targetSlot > 8) return false;
        if (!canPlace(blockPos, checkEntities)) return false;

        ((IVec3d) hitPos).set(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);

        BlockPos neighbour;
        Direction side = getPlaceSide(blockPos);

        if (side == null || onlyAirplace) {
            if (airPlaceDirection == AirPlaceDirection.Up) side = Direction.UP;
            else side = Direction.DOWN;
            neighbour = blockPos;
        } else {
            neighbour = blockPos.offset(side.getOpposite());
            hitPos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        }

        Direction s = side;

        if (rotate) Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), rotationPriority, () -> place(new BlockHitResult(hitPos, s, neighbour, false), hand, oldSlot, targetSlot, switchMode, placeMode, swingHand, swapBack));
        else place(new BlockHitResult(hitPos, s, neighbour, false), hand, oldSlot, targetSlot, switchMode, placeMode, swingHand, swapBack);

        return true;
    }

    private static void place(BlockHitResult blockHitResult, Hand hand, int oldSlot, int targetSlot, SwitchMode switchMode, PlaceMode placeMode, boolean swing, boolean swapBack) {
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));

        if (switchMode != SwitchMode.Client) mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(targetSlot));
        if (switchMode != SwitchMode.Packet) InvUtils.swap(targetSlot, swapBack);

        if (placeMode != PlaceMode.Client) mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, blockHitResult, 0));
        if (placeMode != PlaceMode.Packet) mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);

        if (swing) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        if (swapBack) {
            if (switchMode != SwitchMode.Client) mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
            if (switchMode != SwitchMode.Packet) InvUtils.swapBack();
        }

        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
    }

    public static List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();

        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    public static double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double e = pos1.getY() - pos2.getY();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static boolean interact(BlockPos pos, FindItemResult findItemResult, boolean rotate) {
        if (!findItemResult.found()) return false;
        Runnable action = () -> {
            boolean wasSneaking = mc.player.input.sneaking;
            mc.player.input.sneaking = false;
            InvUtils.swap(findItemResult.slot(), true);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
            mc.player.swingHand(Hand.MAIN_HAND);
            InvUtils.swapBack();
            mc.player.input.sneaking = wasSneaking;
        };
        if (rotate) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), -100, action);
        else action.run();
        return true;
    }

    public static boolean canCrystalPlace(BlockPos loc) {
        BlockState upstate = mc.world.getBlockState(loc.up());
        BlockState state = mc.world.getBlockState(loc);

        return ((state.getBlock() == Blocks.OBSIDIAN || state.getBlock() == Blocks.BEDROCK) && upstate.getBlock() == Blocks.AIR);
    }

    public static boolean needAirPlace(BlockPos center) {
        for (Direction facing : Direction.values()) {
            BlockPos offsetPos = center.offset(facing);
            if (mc.world.getBlockState(offsetPos).getBlock() != Blocks.AIR) {
                return false; // no need airplace because there is a support block
            }
        }
        return true; // need airplace
    }

}
