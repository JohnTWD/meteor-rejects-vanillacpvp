package anticope.rejects.modules;

// stolen from https://github.com/Volcanware/Envy-Client/blob/1.19.3/src/main/java/mathax/client/systems/modules/combat/PistonAura.java#L26

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class PistonAura extends Module {
    private PlayerEntity target;

    private BlockPos headPos;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    public final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get targeted.")
            .defaultValue(5.0)
            .min(0.0)
            .sliderRange(0, 10.0)
            .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to select the player to target.")
            .defaultValue(SortPriority.LowestHealth)
            .build()
    );

    private final Setting<Boolean> trap = sgGeneral.add(new BoolSetting.Builder()
            .name("trap")
            .description("Traps the enemy player.")
            .defaultValue(true)
            .build()
    );

    public PistonAura() {
        super(MeteorRejectsAddon.CATEGORY, "piston-aura", "Moves crystals into people using pistons and attacks them.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (TargetUtils.isBadTarget(target, targetRange.get())) target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
        if (TargetUtils.isBadTarget(target, targetRange.get())) return;

    }

    private boolean hasEnoughSpace(BlockPos direction, BlockPos enemyOrigin, boolean checkHavSupport) { // ensure direction has enough space
        BlockPos checkMe = enemyOrigin;
        for (int i = 0 ; i >=3 ; i++) {
            checkMe = checkMe.add(direction);
            if (mc.world.getBlockState(checkMe).getBlock() != Blocks.AIR)
                return false;
        }
        return true;
    }

    private boolean attemptLayer(Direction direction, BlockPos enemyOrigin, int tick) {
        // steps: piston, then crystal, then redstone

    }

    private void hasItems() {
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.isHotbar() && !obsidian.isOffhand()) {
            info("No obsidian found in hotbar, disabling...");
            toggle();
            return;
        }

        FindItemResult piston = InvUtils.findInHotbar(Items.PISTON);
        if (!piston.isHotbar() && !piston.isOffhand()) {
            info("No piston found in hotbar, disabling...");
            toggle();
            return;
        }

        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (!crystal.isHotbar() && !crystal.isOffhand()) {
            info("No crystal found in hotbar, disabling...");
            toggle();
            return;
        }

        FindItemResult redstoneBlock = InvUtils.findInHotbar(Items.REDSTONE_BLOCK);
        if (!redstoneBlock.isHotbar() && !redstoneBlock.isOffhand()) {
            info("No redstone block found in hotbar, disabling...");
            toggle();
            return;
        }
    }
}