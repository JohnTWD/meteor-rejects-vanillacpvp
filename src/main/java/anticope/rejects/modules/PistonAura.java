package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import anticope.rejects.utils.PlaceData;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
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
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import static meteordevelopment.meteorclient.utils.world.CardinalDirection.fromDirection;

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

    private final Setting<Boolean> pauseOnEat = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses while eating.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnDrink = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Pauses while drinking potions.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnMine = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Pauses while mining blocks.")
            .defaultValue(false)
            .build()
    );

    private final Setting<CardinalDirection> directions = sgGeneral.add(new EnumSetting.Builder<CardinalDirection>()
            .name("directionTester")
            .description("duh")
            .defaultValue(CardinalDirection.North)
            .build()
    ); // temporary, should be removed later

    public PistonAura() {
        super(MeteorRejectsAddon.CATEGORY, "piston-aura", "Moves crystals into people using pistons and attacks them.");
    }
    private PlayerEntity enemy;
    private PlaceData focusBlock; // this is where the piston should be placed


    @Override
    public void onActivate() {
        enemy = null;
    }


    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc == null || mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;

        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
            if (TargetUtils.isBadTarget(target, targetRange.get())) return;
        }

        hasItems(grabInv.get());
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null) return;

        HitResult allcrosshair = mc.crosshairTarget; // remove these later, this is jsut for testing
        if (allcrosshair.getType() == HitResult.Type.BLOCK) {
            BlockPos center = ((BlockHitResult) allcrosshair).getBlockPos();
            PlaceData piston = new PlaceData(center, directions.get().toDirection());
            BlockPos crystalLoc = getCrystalLoc(piston);
            event.renderer.box(center, new Color(0, 0, 0, 0), Color.ORANGE, ShapeMode.Lines, 0);
            event.renderer.box(crystalLoc, new Color(0, 0, 0, 0), Color.MAGENTA, ShapeMode.Lines, 0);
        }
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

    private BlockPos getCrystalLoc(PlaceData pistonData) {
        Direction facing =  (pistonData.dir());
        return pistonData.pos().offset(facing);
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
