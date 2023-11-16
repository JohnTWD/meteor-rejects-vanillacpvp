package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

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
    private int origSlot = 0;
    private int pDel = 0;
    private int bDel = 0;
    private boolean toggleBreak = false;
    Entity targetCrystal = null;

    void resetPhase() {
        pDel = 0;
        bDel = 0;
        targetCrystal = null;
    }

    @Override
    public void onActivate() {
        resetPhase();
    }
    @Override
    public void onDeactivate() {
        if (forceSwitch.get()) InvUtils.swap(origSlot, false);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        
        if (mc.player == null) return;
        ItemStack mainHand = mc.player.getMainHandStack();
        if (mainHand == null) return;
        if (mc.crosshairTarget == null) return;
        HitResult allcrosshair = mc.crosshairTarget;

        Item handItem = mainHand.getItem();

        if (mc.options.useKey.isPressed()) {
            if (handItem == Items.END_CRYSTAL) {
                if (pDel >= placeDelay.get()) {
                    if (allcrosshair.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult asshair = (BlockHitResult) allcrosshair;
                        BlockPos ptrPos = asshair.getBlockPos();
                        if (canPlace(ptrPos)) {;
                            BlockUtils.place(asshair.getBlockPos(), Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, false, 0, true, true, false);
                        }
                    } else if (allcrosshair.getType() == HitResult.Type.ENTITY) {
                        EntityHitResult enthr = (EntityHitResult) allcrosshair;
                        if (enthr.getEntity() instanceof EndCrystalEntity) attack(enthr.getEntity());
                    }
                    pDel = 0;
                } else pDel++;

                if (bDel >= breakDel.get()) {
                    if (allcrosshair.getType() == HitResult.Type.BLOCK) {
                        noCrystalInteract();
                    } else if (allcrosshair.getType() == HitResult.Type.ENTITY) {
                        EntityHitResult enthr = (EntityHitResult) allcrosshair;
                        if (enthr.getEntity() instanceof EndCrystalEntity) attack(enthr.getEntity());
                    }
                    targetCrystal = null;
                    bDel = 0;
                } else bDel++;
            } else if (forceSwitch.get()){
                FindItemResult result = InvUtils.find(Items.GLOWSTONE);

                if (result.found() && result.isHotbar()) {
                    origSlot = mc.player.getInventory().selectedSlot;
                    InvUtils.swap(result.slot(), false);
                }
                else {
                    warning("Crystals not in hotbar, disabling!");
                    toggle();
                }
            }
        }
    }

    private void attack(Entity target) {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return; // stops if not is endcrystal
        if (mc.crosshairTarget == null) return;

        HitResult allcrosshair = mc.crosshairTarget;
        if ((allcrosshair.getType() == HitResult.Type.BLOCK)
                && (((BlockHitResult)allcrosshair).getBlockPos().equals(event.entity.getBlockPos().down()))) {
            targetCrystal = event.entity;
        }
    }

    private void noCrystalInteract() {
        // look for crystals & // find crystal at basePos.up- Done in onEntAdd
        // rotate if necessary
        // attack
        if (targetCrystal == null) return;
        attack(targetCrystal);
    }

    
    private boolean canPlace(BlockPos loc) {
        if (mc.player == null) return false;
        BlockState upstate = mc.world.getBlockState(loc.up());
        BlockState state = mc.world.getBlockState(loc);

        return ((state.getBlock() == Blocks.OBSIDIAN || state.getBlock() == Blocks.BEDROCK) && upstate.getBlock() == Blocks.AIR);
    }
 
}
