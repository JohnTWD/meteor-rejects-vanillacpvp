package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import anticope.rejects.utils.WorldUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    public enum RotateMode {
        none,
        packet,
        forced
    }
    private final Setting<RotateMode> rotMode = sgGeneral.add(new EnumSetting.Builder<RotateMode>()
            .name("rotatemode")
            .description("what rotate to use")
            .defaultValue(RotateMode.none)
            .build()
    );

    private final Setting<Boolean> doNaturalPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("naturalPlace")
            .description("Since minecraft already auto places, use this to stop extra placing (timed rotations still apply)")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> doPacketAttack = sgGeneral.add(new BoolSetting.Builder()
            .name("doPacketAttack")
            .description("use packets to attack crystals instead of normal attack")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> doManualBreakLook = sgGeneral.add(new BoolSetting.Builder()
            .name("manualLookBreak")
            .description("manually look up to hit the crystal")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> forceSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("switch2Crystal")
            .description("Exactly what it says...")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> delayFuzzing = sgGeneral.add(new IntSetting.Builder()
            .name("delayFuzzing")
            .description("+/- a random offset to your delays")
            .defaultValue(1)
            .range(0,20)
            .sliderRange(0,20)
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

    private final Setting<Boolean> stopOnEat = sgGeneral.add(new BoolSetting.Builder()
            .name("stopOnEat")
            .description("Exactly what it saYs")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> stopBlockPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("stopBlockPlace")
            .description("prevents blocks from placing if you are offhanding")
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
    public boolean shouldStopItemUse() {
        if (!this.isActive()) return false;
        if (mc.player == null) return false;
        ItemStack h1 = mc.player.getMainHandStack();
        if (!handsHasCrystal()) return false;
        if (!mc.options.useKey.isPressed()) return false;
        if (mc.crosshairTarget == null) return false;
        HitResult allcrosshair = mc.crosshairTarget;
        if (allcrosshair.getType() == HitResult.Type.MISS) return false;
        if (doNaturalPlace.get()) return false;
        if (stopBlockPlace.get() && h1.getItem() instanceof BlockItem) return false;

        mc.player.stopUsingItem();
        return true;
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

    private boolean handsHasCrystal() {
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();

        if (mainHand == null || offHand == null) return false;

        Item handItem = mainHand.getItem();
        Item offItem = offHand.getItem();

        return (handItem == Items.END_CRYSTAL || offItem == Items.END_CRYSTAL);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        
        if (mc.player == null) return;

        if (mc.crosshairTarget == null) return;
        HitResult allcrosshair = mc.crosshairTarget;

        if (forceSwitch.get() && !handsHasCrystal()){
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
            if (stopOnEat.get() && mc.player.isUsingItem()) return;
            if (mc.interactionManager.isBreakingBlock()) return;

            if (handsHasCrystal()) {
                if (allcrosshair.getType() == HitResult.Type.MISS)
                    return;
                crystalListFilter();
                int randomFuzz = ThreadLocalRandom.current().nextInt(-delayFuzzing.get(), delayFuzzing.get());

                if (pDel + randomFuzz < 0) {
                    if (allcrosshair.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult asshair = (BlockHitResult) allcrosshair;
                        BlockPos ptrPos = asshair.getBlockPos();
                        if (WorldUtils.canCrystalPlace(ptrPos)) {
                            if (rotMode.get() == RotateMode.packet) {
                                float randomOffsetYaw = (float) Utils.random(-.14, .88);
                                float randomOffsetPitch = (float) Utils.random(-2.4, 2.69);
                                Rotations.rotate(mc.player.getHeadYaw() + randomOffsetYaw, Rotations.getPitch(ptrPos) + randomOffsetPitch);
                            } else if (rotMode.get() == RotateMode.forced) {
                                float randomOffsetYaw = (float) Utils.random(-.14, .88);
                                float randomOffsetPitch = (float) Utils.random(-2.4, 2.69);
                                mc.player.setPitch((float) Rotations.getPitch(ptrPos) + randomOffsetPitch);
                                mc.player.setHeadYaw(mc.player.getHeadYaw() + randomOffsetYaw);
                            }
                            if (!doNaturalPlace.get()) {
                                if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL)
                                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, asshair);
                                else if (mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL)
                                    mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, asshair);
                            }
                        }
                    }
                    pDel = placeDelay.get();
                    return;
                }
                if (bDel + randomFuzz <= 0) {
                    if (allcrosshair.getType() == HitResult.Type.ENTITY) { // looking at crystal, KILL IT!!!
                        EntityHitResult enthr = (EntityHitResult) allcrosshair;
                        if (isGoodCrystal(enthr.getEntity(), false)) attack(enthr.getEntity());
                    } else if (!doManualBreakLook.get()) noCrystalInteract();
                    bDel = breakDel.get();
                } else {
                    if (rotMode.get() == RotateMode.packet) {
                        Entity crysEnt = doesBlockHaveEntOnTop();
                        if (isGoodCrystal(crysEnt, true)) {
                            float entPitch = (float) Rotations.getPitch(crysEnt, Target.Feet);
                            float rotDiv = getRotDiv(bDel, entPitch);
                            float randomOffsetYaw = (float) Utils.random(-1.4, .88);
                            float randomOffsetPitch = (float) Utils.random(-2.4, 2.69);
                            Rotations.rotate(mc.player.getHeadYaw() + randomOffsetYaw, mc.player.getPitch() + randomOffsetPitch + rotDiv);
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
        if (!handsHasCrystal()) return;

        HitResult allcrosshair = mc.crosshairTarget;
        BlockPos ptrPos = null;
        if (allcrosshair.getType() == HitResult.Type.BLOCK) {
            BlockHitResult asshair = (BlockHitResult) allcrosshair;
            ptrPos = asshair.getBlockPos();
            if (WorldUtils.canCrystalPlace(ptrPos))
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
        if (ptrPos == null || !isGoodCrystal(targetCrystal, true)) return;

        double x = MathHelper.lerp(event.tickDelta, targetCrystal.lastRenderX, targetCrystal.getX()) - targetCrystal.getX();
        double y = MathHelper.lerp(event.tickDelta, targetCrystal.lastRenderY, targetCrystal.getY()) - targetCrystal.getY();
        double z = MathHelper.lerp(event.tickDelta, targetCrystal.lastRenderZ, targetCrystal.getZ()) - targetCrystal.getZ();

        Box box = targetCrystal.getBoundingBox();
        event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, new Color(0, 0, 0, 0), Color.CYAN, ShapeMode.Lines, 0);

    }

    private float getPitchDelta(float target) {
        assert mc.player != null;
        return mc.player.getPitch() - target;
    }

    private float getRotDiv(int div, float target) {
        if (div != 0) div = 1;
        return Math.abs(getPitchDelta(target)) / div;
    }

    private boolean isGoodCrystal(Entity target, boolean shouldCheckOnTop) { // the only good crystal is a living one
        if (target == null) return false;
        if ((target instanceof LivingEntity && ((LivingEntity) target).isDead()) || !target.isAlive()) return false;
        if (!target.isAttackable()) return false;
        if (!(target instanceof EndCrystalEntity)) return false;
        if (mc.player == null) return false;
        return  (!(shouldCheckOnTop && !isEntityOnTop(target)));
    }

    private void attack(Entity target) {
        if (!isGoodCrystal(target, false)) return;
        if (doPacketAttack.get()) mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        else mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isEntityOnTop(Entity ent) { // given block, check for entity
        if (!(ent instanceof EndCrystalEntity)) return false;
        HitResult allcrosshair = mc.crosshairTarget;
        if (!(allcrosshair instanceof BlockHitResult)) return false;
        BlockPos ptrPos = ((BlockHitResult) allcrosshair).getBlockPos();
        return ptrPos.equals(ent.getBlockPos().down());
    }

    private Entity doesBlockHaveEntOnTop() { // given block, find entity
        for (Entity i : crystalEntList) {
            if (isGoodCrystal(i, true))
                return i;
        }
        return null;
    }

    private void crystalListFilter() {
        crystalEntList.clear();
        if (mc.world == null) return;

        for (Entity i : mc.world.getEntities()) {
            if (!isGoodCrystal(i, true)) continue;
            crystalEntList.add(i);
        }
    }

    private void noCrystalInteract() {
        // look for crystals & // find crystal at basePos.up- Done in onEntAdd
        // rotate if necessary
        // attack
        Entity targetCrystal = doesBlockHaveEntOnTop();
        if (!isGoodCrystal(targetCrystal, true)) return;
        if (noWallCrystal.get() && !PlayerUtils.canSeeEntity(targetCrystal)) return;

        if (rotMode.get() == RotateMode.packet) {
            float randomOffsetYaw = (float) Utils.random(-.14, .88);
            float randomOffsetPitch = (float) Utils.random(-2.4, 2.69);
            Rotations.rotate(mc.player.getHeadYaw() + randomOffsetYaw, Rotations.getPitch(targetCrystal, Target.Feet) +randomOffsetPitch, EventPriority.HIGHEST);
        } else if (rotMode.get() == RotateMode.forced) {
            float randomOffsetYaw = (float) Utils.random(-.14, .88);
            float randomOffsetPitch = (float) Utils.random(-2.4, 2.69);
            mc.player.setPitch((float) Rotations.getPitch(targetCrystal, Target.Feet) + randomOffsetPitch);
            mc.player.setHeadYaw(mc.player.getHeadYaw() + randomOffsetYaw);
        }
        attack(targetCrystal);
    }

}
