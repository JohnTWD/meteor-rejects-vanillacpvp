package anticope.rejects.modules;

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
            .description("Traps the enemy player. (Will not work if server disallows air place!)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> grabInv = sgGeneral.add(new BoolSetting.Builder()
            .name("grabfrominv")
            .description("Use inventory items instead of just hotbar")
            .defaultValue(true)
            .build()
    );

    public enum redstoneMethod {
        redstoneBlock,
        torch,
        button
    }

    private final Setting<PistonAura.redstoneMethod> activationType = sgGeneral.add(new EnumSetting.Builder<redstoneMethod>()
            .name("Redstone Activation Method")
            .defaultValue(redstoneMethod.redstoneBlock)
            .build()
    );

    public PistonAura() {
        super(MeteorRejectsAddon.CATEGORY, "piston-aura", "Moves crystals into people using pistons and attacks them.");
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc == null || mc.player == null || mc.world == null || mc.interactionManager == null) return;
        hasItems(grabInv.get());
    }

    private boolean hasEnoughSpace(BlockPos direction, BlockPos enemyOrigin, boolean checkHavSupport) { // ensure direction has enough space
        BlockPos checkMe = enemyOrigin;
        for (int i = 0; i >= 3; i++) {
            checkMe = checkMe.add(direction);
            if (mc.world.getBlockState(checkMe).getBlock() != Blocks.AIR)
                return false;
        }
        return true;
    }

    private boolean attemptLayer(Direction direction, BlockPos enemyOrigin, int tick) {
        // steps: piston, then crystal, then redstone
        return false;
    }

    private FindItemResult findButton() {
        return InvUtils.find(itemStack -> itemStack.getItem() == Items.STONE_BUTTON); // low prio todo: add more buttons, but like, non repetitively
    }

    private void hasItems(boolean checkHotbarOnly) {
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.found() || (checkHotbarOnly && !obsidian.isHotbar())) {
            info("No obsidian found, disabling...");
            toggle();
            return;
        }

        FindItemResult piston = InvUtils.findInHotbar(Items.PISTON);
        if (!obsidian.found() || (checkHotbarOnly && !piston.isHotbar())) {
            info("No piston found, disabling...");
            toggle();
            return;
        }

        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (!obsidian.found() || (checkHotbarOnly && !crystal.isHotbar())) {
            info("No crystal found, disabling...");
            toggle();
            return;
        }

        FindItemResult activator = null;
        switch (activationType.get()) {
            case redstoneBlock:
                activator = InvUtils.find(Items.REDSTONE_BLOCK);
                break;
            case torch:
                activator = InvUtils.find(Items.REDSTONE_TORCH);
                break;
            case button:
                activator = findButton();
        }
        if (activator != null && !activator.found() || (checkHotbarOnly && !activator.isHotbar())) {
            info("No activator, disabling...");
            toggle();
        }
    }
}
