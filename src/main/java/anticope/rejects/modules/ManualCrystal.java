package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class ManualCrystal extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public ManualCrystal() {
        super(MeteorRejectsAddon.CATEGORY, "manualcrystal", "automate placing and breaking of crystals without doing a full CA");
    }

    private final Setting<Boolean> noWallCrystal = sgGeneral.add(new BoolSetting.Builder()
            .name("noAttackThroughWalls")
            .description("Whether to allow attacking of crystals through a wall")
            .defaultValue(false)
            .build()
    );


    private final Setting<Boolean> forceSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("switch2Crystal")
            .description("Exactly what it says...")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
            .name("placeDelay")
            .description("how long in ticks to wait to place a crystal")
            .defaultValue(1)
            .range(0,20)
            .sliderRange(0,20)
            .build()
    );
    private final Setting<Integer> breakDel = sgGeneral.add(new IntSetting.Builder()
            .name("breakDelay")
            .description("how long in ticks to wait to break crystals")
            .defaultValue(1)
            .range(0,20)
            .sliderRange(0,20)
            .build()
    );
    private final Setting<Boolean> rotateSetting = sgGeneral.add(new BoolSetting.Builder()
            .name("breakRotateSetting")
            .description("whether or not should rotate to break a crystal")
            .defaultValue(false)
            .build()
    );
    private int origSlot;
    private int pDel = placeDelay.get();
    private int bDel = breakDel.get();
    private final List<Entity> crystalEntList = new ArrayList<>();

    void resetPhase() {
        pDel = placeDelay.get();
        bDel = breakDel.get();
        crystalEntList.clear();
    }

    @Override
    public void onActivate() {
        resetPhase();
    }
    @Override
    public void onDeactivate() {
        if (forceSwitch.get()) InvUtils.swap(origSlot, false);
        resetPhase();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        
        if (mc.player == null) return;
        ItemStack mainHand = mc.player.getMainHandStack();
        if (mainHand == null) return;
        if (mc.crosshairTarget == null) return;
        HitResult allcrosshair = mc.crosshairTarget;

        Item handItem = mainHand.getItem();

        if (forceSwitch.get() && handItem != Items.END_CRYSTAL){
            FindItemResult result = InvUtils.find(Items.END_CRYSTAL);
            origSlot = mc.player.getInventory().selectedSlot;

            if (result.found() && result.isHotbar())
                InvUtils.swap(result.slot(), false);
            else {
                warning("Crystals not in hotbar, disabling!");
                toggle();
            }
        }

        if (mc.options.useKey.isPressed()) {
            if (handItem == Items.END_CRYSTAL) {
                crystalListFilter();

                if (pDel <= 0) {
                    if (allcrosshair.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult asshair = (BlockHitResult) allcrosshair;
                        BlockPos ptrPos = asshair.getBlockPos();
                        if (canPlace(ptrPos)) {
                            if (rotateSetting.get())
                                Rotations.rotate(mc.player.getHeadYaw(), Rotations.getPitch(ptrPos));
                            BlockUtils.place(ptrPos, Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, false, 0, true, true, false);
                        }
                    }
                    pDel = placeDelay.get();
                    return;
                }
                if (bDel <= 0) {
                    if (allcrosshair.getType() == HitResult.Type.ENTITY) { // looking at crystal, KILL IT!!!
                        EntityHitResult enthr = (EntityHitResult) allcrosshair;
                        if (enthr.getEntity() instanceof EndCrystalEntity) attack(enthr.getEntity());
                    } else noCrystalInteract();
                    bDel = breakDel.get();
                } else {
                    if (rotateSetting.get()) {
                        Entity crysEnt = doesBlockHaveEntOnTop();
                        if (crysEnt != null) {
                            float entPitch = (float) Rotations.getPitch(crysEnt, Target.Feet);
                            float rotDiv = getRotDiv(breakDel.get(), entPitch);
                            Rotations.rotate(mc.player.getHeadYaw(), mc.player.getPitch() + rotDiv);
                        }
                    }
                }

                pDel--; bDel--;
                return;
            }
        }
        resetPhase();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.crosshairTarget == null) return;
        HitResult allcrosshair = mc.crosshairTarget;
        BlockPos ptrPos = null;
        if (allcrosshair.getType() == HitResult.Type.BLOCK) {
            BlockHitResult asshair = (BlockHitResult) allcrosshair;
            ptrPos = asshair.getBlockPos();
            if (canPlace(ptrPos))
                event.renderer.box(ptrPos, new Color(0, 0, 0, 0), Color.MAGENTA, ShapeMode.Lines, 0);

        } else if (allcrosshair.getType() == HitResult.Type.ENTITY) {
            EntityHitResult enthr = (EntityHitResult) allcrosshair;
            if (enthr.getEntity() instanceof EndCrystalEntity) {
                double x = MathHelper.lerp(event.tickDelta, enthr.getEntity().lastRenderX, enthr.getEntity().getX()) - enthr.getEntity().getX();
                double y = MathHelper.lerp(event.tickDelta, enthr.getEntity().lastRenderY, enthr.getEntity().getY()) - enthr.getEntity().getY();
                double z = MathHelper.lerp(event.tickDelta, enthr.getEntity().lastRenderZ, enthr.getEntity().getZ()) - enthr.getEntity().getZ();

                Box box = enthr.getEntity().getBoundingBox();
                event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, new Color(0, 0, 0, 0), Color.CYAN, ShapeMode.Lines, 0);
            }
        }
        Entity targetCrystal = doesBlockHaveEntOnTop();
        if (targetCrystal == null || ptrPos == null) return;

        if (targetCrystal.isAlive()  && ptrPos.equals(targetCrystal.getBlockPos().down())) {
            double x = MathHelper.lerp(event.tickDelta, targetCrystal.lastRenderX, targetCrystal.getX()) - targetCrystal.getX();
            double y = MathHelper.lerp(event.tickDelta, targetCrystal.lastRenderY, targetCrystal.getY()) - targetCrystal.getY();
            double z = MathHelper.lerp(event.tickDelta, targetCrystal.lastRenderZ, targetCrystal.getZ()) - targetCrystal.getZ();

            Box box = targetCrystal.getBoundingBox();
            event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, new Color(0, 0, 0, 0), Color.CYAN, ShapeMode.Lines, 0);
        }
    }

    private float getPitchDelta(float target) {
        assert mc.player != null;
        return mc.player.getPitch() - target;
    }

    private float getRotDiv(int div, float target) {
        if (div != 0) div = 1;
        return getPitchDelta(target) / div;
    }

    private void attack(Entity target) {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isEntityOnTop(Entity ent) { // given block, check for entity
        HitResult allcrosshair = mc.crosshairTarget;
        if (!(allcrosshair instanceof BlockHitResult)) return false;
        BlockPos ptrPos = ((BlockHitResult) allcrosshair).getBlockPos();
        return ptrPos.equals(ent.getBlockPos().down());
    }

    private Entity doesBlockHaveEntOnTop() { // given block, find entity
        for (Entity i : crystalEntList) {
            if (isEntityOnTop(i))
                return i;
        }
        return null;
    }

    private void crystalListFilter() {
        crystalEntList.clear();
        if (mc.world == null) return;

        for (Entity i : mc.world.getEntities()) {
            if (i == null) continue;
            if (!i.isAlive()) continue;
            if (!(i instanceof EndCrystalEntity)) continue;

            HitResult allcrosshair = mc.crosshairTarget;
            if (!(allcrosshair instanceof BlockHitResult)) continue;
            if (!isEntityOnTop(i)) continue;
            crystalEntList.add(i);
        }
    }

    private void noCrystalInteract() {
        // look for crystals & // find crystal at basePos.up- Done in onEntAdd
        // rotate if necessary
        // attack
        Entity targetCrystal = doesBlockHaveEntOnTop();
        if (targetCrystal == null) return;
        if (mc.player == null) return;
        if (noWallCrystal.get() && !PlayerUtils.canSeeEntity(targetCrystal)) return;
        if (rotateSetting.get()) {
            Rotations.rotate(mc.player.getHeadYaw(), Rotations.getPitch(targetCrystal, Target.Feet));
        }
        attack(targetCrystal);
    }

    
    private boolean canPlace(BlockPos loc) {
        if (mc.player == null) return false;
        if (mc.world == null) return false;
        BlockState upstate = mc.world.getBlockState(loc.up());
        BlockState state = mc.world.getBlockState(loc);

        return ((state.getBlock() == Blocks.OBSIDIAN || state.getBlock() == Blocks.BEDROCK) && upstate.getBlock() == Blocks.AIR);
    }
 
}
