package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import anticope.rejects.utils.PlaceData;
import anticope.rejects.utils.WorldUtils;
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
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.Comparator;

public class PistonAura extends Module {

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


    private final Setting<Boolean> airplaceCan = sgGeneral.add(new BoolSetting.Builder()
            .name("allowAirplace")
            .description("Will not skip over layers if there are no supports")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> trap = sgGeneral.add(new BoolSetting.Builder()
            .name("trap")
            .description("Traps the enemy player. (Will not work if server disallows air place!)")
            .defaultValue(false)
            .visible(airplaceCan::get)
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
        super(MeteorRejectsAddon.CATEGORY, "piston-aura", "module dedicated to akarson(RIP) ~ funniest module ever created made obsolete by anchors. also i may or may not fix non-redstone blk activation methods");
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
            if (TargetUtils.isBadTarget(enemy, targetRange.get())) {
                enemy = null;
                return;
            }
        }


        hasItems(grabInv.get());
        PlaceData tempFocus = null;
        for (int yOffset = 1; yOffset < 3 ; yOffset++) {
            tempFocus = getFocusBlock(enemy, yOffset);
            if (tempFocus != null) break;
        }
        focusBlock = tempFocus;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null) return;
        //if (focusBlock == null) return;

        PlaceData piston = focusBlock;
        if (piston == null) return;
        BlockPos crystalLoc = getCrystalLoc(piston);
        BlockPos getPowerPlacement = getPowerPlacement(piston);
        if (getPowerPlacement == null) return;

        event.renderer.box(piston.pos(), new Color(0, 0, 0, 0), Color.ORANGE, ShapeMode.Lines, 0);
        event.renderer.box(crystalLoc, new Color(0, 0, 0, 0), Color.MAGENTA, ShapeMode.Lines, 0);
        event.renderer.box(getPowerPlacement, new Color(0, 0, 0, 0), Color.RED, ShapeMode.Lines, 0);
    }

    private PlaceData getFocusBlock(PlayerEntity target, int yOffset) {
        BlockPos targCtr = target.getBlockPos().up(yOffset);

        PlaceData[] pistonBlocks = {
            new PlaceData(new BlockPos(targCtr.north(2)), Direction.NORTH),
            new PlaceData(new BlockPos(targCtr.south(2)), Direction.SOUTH),
            new PlaceData(new BlockPos(targCtr.east(2)), Direction.EAST),
            new PlaceData(new BlockPos(targCtr.west(2)), Direction.WEST)
        };

        Arrays.sort(pistonBlocks, Comparator.comparingDouble(pD -> pD.pos().getSquaredDistance(mc.player.getPos())));

        for (PlaceData rtn : pistonBlocks) {
            if (!isGoodDirection(rtn))
                continue;
            return rtn;
        }
        return null;
    }


    private boolean isGoodDirection(PlaceData pistonLoc) { // check direction has enough space and is within range
        BlockPos[] rtn = {
                pistonLoc.pos(),             // piston  rtn[0]
                getCrystalLoc(pistonLoc),    // crystal rtn[1]
                getPowerPlacement(pistonLoc) // power   rtn[2]
        };

        if (rtn[2] == null) return false; // power nil rtn 0
        if (!airplaceCan.get() && WorldUtils.needAirPlace(rtn[2])) return false; // !airplace & airplaceneed->for:power
        if (!WorldUtils.canCrystalPlaceIgnorePiston(rtn[1].down())) return false; // crystal placeable

        if (!BlockUtils.canPlace(rtn[0]) && !(mc.world.getBlockState(rtn[0]).getBlock() == Blocks.PISTON || mc.world.getBlockState(rtn[0]).getBlock() == Blocks.STICKY_PISTON))
            return false; // cannot place and not a piston at piston loc

        if (!BlockUtils.canPlace(rtn[1]) && !(canPlacePower(rtn[1]))) // low prio TODO: add the other power methods
            return false;

        for (BlockPos b : rtn) {
            if (!b.isWithinDistance(mc.player.getPos(), placeRange.get()))
                return false;
        }
        return true;
    }

    private BlockPos[] getSurroundingPosExceptFace(BlockPos center, Direction excludedFace) {
        BlockPos[] surroundingBlocks = new BlockPos[10]; // this is a really shitty fix but hey, it works
        int i = 0;
        surroundingBlocks[9] = center.offset(Direction.UP); // reserve UP direction as last element
        for (Direction facing : Direction.values()) {
            if (facing == excludedFace || facing == Direction.UP) // skip over exclusion and UP
                continue;

            BlockPos offsetPos = center.offset(facing);
            if(!WorldUtils.needAirPlace(offsetPos)) { // prioritize blocks with supports
                surroundingBlocks[i] = offsetPos;
            } else { // blocks that need airplacing go to the back
                surroundingBlocks[8 - i] = offsetPos;
            }
            i++;
        }
        return surroundingBlocks;
    }

    private boolean canPlacePower(BlockPos blockPos) {
        return (!BlockUtils.canPlace(blockPos, true) && !(mc.world.getBlockState(blockPos).getBlock() == Blocks.REDSTONE_BLOCK)) // low prio TODO: add the other power methods
    }

    private BlockPos getPowerPlacement(PlaceData pistonData) {
        BlockPos[] surroundings = getSurroundingPosExceptFace(pistonData.pos(), pistonData.dir().getOpposite());

        BlockPos theOne = null;
        for (BlockPos block : surroundings) {
            if (block == null) continue;

            if (canPlacePower(block) && block.isWithinDistance(mc.player.getPos(), placeRange.get())) {
                theOne = block;
                break;
            }
        }
        return theOne;
    }

    private BlockPos getCrystalLoc(PlaceData pistonData) {
        Direction facing =  (pistonData.dir());
        return pistonData.pos().offset(facing.getOpposite());
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

        FindItemResult activator = switch (activationType.get()) {
            case redstoneBlock -> InvUtils.find(Items.REDSTONE_BLOCK);
            case torch -> InvUtils.find(Items.REDSTONE_TORCH);
            case button -> findButton();
        };
        if (!activator.found() || checkHotbarOnly && !activator.isHotbar()) {
            info("No activator, disabling...");
            toggle();
        }
    }
}
