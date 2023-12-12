package anticope.rejects.utils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
public class RejectEntityUtils {
    public static Direction rayTraceCheck(BlockPos pos, boolean forceReturn) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        Direction[] var3 = Direction.values();

        for (Direction direction : var3) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d((double) pos.getX() + 0.5D + (double) direction.getVector().getX() * 0.5D, (double) pos.getY() + 0.5D + (double) direction.getVector().getY() * 0.5D, (double) pos.getZ() + 0.5D + (double) direction.getVector().getZ() * 0.5D), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                return direction;
            }
        }

        if (forceReturn) {
            if ((double)pos.getY() > eyesPos.y) {
                return Direction.DOWN;
            } else {
                return Direction.UP;
            }
        } else {
            return null;
        }
    }

    public static Vec3d crystalEdgePos(EndCrystalEntity crystal) {
        Vec3d crystalPos = crystal.getPos();
        return new Vec3d(
                crystalPos.x < mc.player.getX() ? crystalPos.add(Math.min(1, mc.player.getX() - crystalPos.x), 0, 0).x : crystalPos.x > mc.player.getX() ? crystalPos.add(Math.max(-1, mc.player.getX() - crystalPos.x), 0, 0).x : crystalPos.x,
                crystalPos.y < mc.player.getY() ? crystalPos.add(0, Math.min(1, mc.player.getY() - crystalPos.y), 0).y : crystalPos.y,
                crystalPos.z < mc.player.getZ() ? crystalPos.add(0, 0, Math.min(1, mc.player.getZ() - crystalPos.z)).z : crystalPos.z > mc.player.getZ() ? crystalPos.add(0, 0, Math.max(-1, mc.player.getZ() - crystalPos.z)).z : crystalPos.z);
    }

    public static int getBlockBreakingSpeed(BlockState block, BlockPos pos, int slot) {
        PlayerEntity player = mc.player;

        float f = (player.getInventory().getStack(slot)).getMiningSpeedMultiplier(block);
        if (f > 1.0F) {
            int i = EnchantmentHelper.get(player.getInventory().getStack(slot)).getOrDefault(Enchantments.EFFICIENCY, 0);
            if (i > 0) {
                f += (float)(i * i + 1);
            }
        }

        if (StatusEffectUtil.hasHaste(player)) {
            f *= 1.0F + (float)(StatusEffectUtil.getHasteAmplifier(player) + 1) * 0.2F;
        }

        if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k = switch (player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            f *= k;
        }

        if (player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player)) {
            f /= 5.0F;
        }

        if (!player.isOnGround()) {
            f /= 5.0F;
        }

        float t = block.getHardness(mc.world, pos);
        if (t == -1.0F) {
            return 0;
        } else {
            return (int) Math.ceil(1 / (f / t / 30));
        }
    }

}
