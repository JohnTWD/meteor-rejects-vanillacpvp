package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import anticope.rejects.utils.PlaceData;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.Comparator;

public class PistonAura extends Module {

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

    public final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The radius blocks should be allowed to place")
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

        if (TargetUtils.isBadTarget(enemy, targetRange.get())) {
            enemy = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
            if (TargetUtils.isBadTarget(enemy, targetRange.get())) return;
        }

        hasItems(grabInv.get());
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null) return;
        //if (focusBlock == null) return;
        HitResult ct = mc.crosshairTarget;
        if (ct == null) return;
        if (ct.getType() != HitResult.Type.BLOCK) return;

        BlockPos center = ((BlockHitResult) ct).getBlockPos().up();
        PlaceData piston = getFocusBlock(center, 0);//new PlaceData(center, directions.get().toDirection());
        if (piston == null) {
            info("No valid placements");
            return;
        }
        BlockPos crystalLoc = getCrystalLoc(piston);
        BlockPos getPowerPlacement = getPowerPlacement(piston);
        event.renderer.box(center, new Color(0, 0, 0, 0), Color.BLUE, ShapeMode.Lines, 0);
        event.renderer.box(piston.pos(), new Color(0, 0, 0, 0), Color.ORANGE, ShapeMode.Lines, 0);
        event.renderer.box(crystalLoc, new Color(0, 0, 0, 0), Color.MAGENTA, ShapeMode.Lines, 0);
        event.renderer.box(getPowerPlacement, new Color(0, 0, 0, 0), Color.RED, ShapeMode.Lines, 0);
    }
    private BlockPos getPowerPlacement(PlaceData pistonData) {
        Direction facing =  (pistonData.dir());
        return pistonData.pos().offset(facing);
    }

    private PlaceData getFocusBlock(/*PlayerEntity*/ BlockPos targCtr, int yOffset) {
        //BlockPos targCtr = target.getBlockPos().up().up(yOffset);

        PlaceData[] directionBlocks = {
            new PlaceData(new BlockPos(targCtr.north(2)).up(yOffset), Direction.NORTH),
            new PlaceData(new BlockPos(targCtr.south(2)).up(yOffset), Direction.SOUTH),
            new PlaceData(new BlockPos(targCtr.east(2)).up(yOffset), Direction.EAST),
            new PlaceData(new BlockPos(targCtr.west(2)).up(yOffset), Direction.WEST)
        };

        Arrays.sort(directionBlocks, Comparator.comparingDouble(pD -> pD.pos().getSquaredDistance(mc.player.getPos())));

        for (PlaceData rtn : directionBlocks) {
            if (!hasEnoughSpace(rtn.pos(), rtn.dir()))
                continue;
            return rtn;
        }
        return null;
    }


    private boolean hasEnoughSpace(BlockPos enemyOrigin, Direction direction) { // ensure direction has enough space
        BlockPos checkMe = enemyOrigin;
        BlockPos directionVec = new BlockPos(direction.getVector());
        for (int i = -1; i <= 1; i++) {
            checkMe = checkMe.add(directionVec);
            if (!BlockUtils.canPlace(checkMe, true))
                return false;
            if (!PlayerUtils.isWithin(checkMe, placeRange.get()))
                return false;
        }
        return true;
    }

    private BlockPos getCrystalLoc(PlaceData pistonData) {
        Direction facing =  (pistonData.dir());
        return pistonData.pos().offset(facing.getOpposite());
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
