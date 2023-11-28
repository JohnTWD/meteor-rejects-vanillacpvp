package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import static java.lang.Math.abs;

public class HitboxDesync extends Module { // original code by mioclient https://github.com/mioclient/hitbox-desync/blob/main/HitboxDesyncModule.java
    // ported to meteor by me!! actually this was way easier to import than i thought itd be, vvtf
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public HitboxDesync() {
        super(MeteorRejectsAddon.CATEGORY, "hitboxdesync", "real csgo moment | how2use: enter a 2x1 hole, look in +X or +Z direction, on!");
    }

    private final Setting<Boolean> automatic = sgGeneral.add(new BoolSetting.Builder()
            .name("automatic")
            .description("duh")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyPositive = sgGeneral.add(new BoolSetting.Builder()
            .name("onlyPositive")
            .description("if should only turn on when +x +z")
            .defaultValue(false)
            .visible(automatic::get)
            .build()
    );
    private final Setting<Boolean> shouldShut = sgGeneral.add(new BoolSetting.Builder()
            .name("offOnAutoActive")
            .description("if should turn off when this activates when using automatic")
            .defaultValue(false)
            .visible(automatic::get)
            .build()
    );

    private static final double MAGIC_OFFSET = .200009968835369999878673424677777777777761; // wtf is this shit

    private boolean checkSingle(BlockPos center, BlockPos ignore, World world) {
        BlockPos[] surroundingBlocks = { center.north(), center.south(), center.east(), center.west() };
        for (BlockPos surroundingPos : surroundingBlocks) {
            if (surroundingPos.equals(ignore)) continue;
            Block sideBlok = world.getBlockState(surroundingPos).getBlock();
            if (sideBlok != Blocks.BEDROCK && sideBlok != Blocks.OBSIDIAN) return false;
        }
        return true;
    }
    private boolean isDirectionGood(BlockPos home, BlockPos off, World world) {
        BlockState offsBlock = world.getBlockState(off);
        // Check if the offset are free
        if (!offsBlock.isReplaceable()) return false;
        if (!world.getBlockState(off.up()).isReplaceable()) return false;
        if (world.getBlockState(off.down()).getBlock() != Blocks.OBSIDIAN && world.getBlockState(off.down()).getBlock() != Blocks.BEDROCK) return false; // must check if the offset can be placed on by crystals
        if (!checkSingle(home, off, world)) return false;// check home hole
        return checkSingle(off, home, world);//check offset
    }

    private Vec3d findGodSide(BlockPos home, World world) {
        BlockState homeBlock = world.getBlockState(home);
        if (!homeBlock.isReplaceable()) return null; // see if home is free

        if (isDirectionGood(home, home.south(), world)) return new Vec3d(0,0,1); // hardcoded sides MY SIDES XD!!!!!!!!!!!!!!!!!
        if (isDirectionGood(home, home.east(), world)) return new Vec3d(1, 0, 0);
        if (!onlyPositive.get()) {
            if (isDirectionGood(home, home.north(), world)) return new Vec3d(0,0,-1); // -X
            if (isDirectionGood(home, home.west(), world)) return new Vec3d(-1,0,0);  // -Z
        }
        return null;
    }

    private boolean canPlace(Vec3d toPlace, Box me) {
        Box placebox = new Box(toPlace, toPlace.add(1, 1, 1));
        return me.intersects(placebox);
    }
    private boolean hasDesynced = false;
    private Vec3d oldPos;
    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        if (mc.world == null) return;

        if (!automatic.get()) {doCSGO(new Vec3d(mc.player.getHorizontalFacing().getUnitVector()));toggle();}
        if (!mc.player.isOnGround()) return;

        Vec3d SETI = findGodSide(mc.player.getBlockPos(), mc.world); // search for extraterrestrial INTELLIJ IDEA
        if (SETI == null) return;

        if (!hasDesynced) {
            doCSGO(SETI);
            if (shouldShut.get()) toggle();
        }
        if (hasDesynced && !canPlace(SETI, mc.player.getBoundingBox())) { // shit stopped working; reset old pos
            mc.player.setPosition(oldPos);
            hasDesynced = false;
        }
    }
    @Override
    public void onActivate() {
        hasDesynced = false;
    }

    private void doCSGO(Vec3d offset) {
        oldPos = mc.player.getPos();
        Box bb = mc.player.getBoundingBox();
        Vec3d center = bb.getCenter();

        Vec3d fin = merge(Vec3d.of(BlockPos.ofFloored(center)).add(.5, 0, .5).add(offset.multiply(MAGIC_OFFSET)), offset);
        mc.player.setPosition(
                fin.x == 0 ? mc.player.getX() : fin.x,
                mc.player.getY(),
                fin.z == 0 ? mc.player.getZ() : fin.z
        );
        info("just did the csgo!");
        hasDesynced = true;
    }

    private Vec3d merge(Vec3d a, Vec3d Offset) {
        return new Vec3d(a.x * abs(Offset.x), a.y * abs(Offset.y), a.z * abs(Offset.z));
    }

}
