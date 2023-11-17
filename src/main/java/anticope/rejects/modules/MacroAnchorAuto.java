package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class MacroAnchorAuto extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public MacroAnchorAuto(){
        super(MeteorRejectsAddon.CATEGORY, "macroanchorauto", "Macro for manually placing and activating anchors.");
    }

    private final Setting<Boolean> shouldUseInv = sgGeneral.add(new BoolSetting.Builder()
            .name("useInventorySwitch")
            .description("whether to grab stuff from inv instead of hotbar")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> placeAnchorDel = sgGeneral.add(new IntSetting.Builder()
            .name("placeAnchorDelay")
            .description("how long in ticks to wait to place an anchor")
            .defaultValue(1)
            .range(0,20)
            .sliderRange(0,20)
            .build()
    );

    private final Setting<Integer> breakAnchorDel = sgGeneral.add(new IntSetting.Builder()
            .name("breakeAnchorDelay")
            .description("how long in ticks to wait to activate anchors")
            .defaultValue(1)
            .range(0,20)
            .sliderRange(0,20)
            .build()
    );
    private final Setting<Integer> changeSlotDel = sgGeneral.add(new IntSetting.Builder()
            .name("slotChangeDelay")
            .description("change how long swapping should take")
            .defaultValue(1)
            .range(0,20)
            .sliderRange(0,20)
            .build()
    );

    private final Setting<Integer> chargeAnchorDel = sgGeneral.add(new IntSetting.Builder()
            .name("chargeAnchorDelay")
            .description("how long in ticks to charge anchor")
            .defaultValue(1)
            .range(0,20)
            .sliderRange(0,20)
            .build()
    );
    private boolean emptyWarningThrown = false;
    private int pDel = placeAnchorDel.get();
    private int cDel = chargeAnchorDel.get();
    private int sDel = changeSlotDel.get();
    private int bDel = breakAnchorDel.get();
    private int phase = 0;
    /* PHASES
     * 0 = Begin & Anchor placed & Switch to GS
     * 1 = Charge w/ GS
     * 2 = Switch back to anchor
     * 3 =  and activate
     */
    private void resetPhase() {
        pDel = placeAnchorDel.get();
        cDel = chargeAnchorDel.get();
        sDel = changeSlotDel.get();
        bDel = breakAnchorDel.get();
        phase = 0;
        emptyWarningThrown = false;
    }

    @Override
    public void onActivate() {
        resetPhase();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!disablePlacing()) return; // this can also double as a check for whether we should render or not
        if (mc.crosshairTarget == null) return;
        HitResult allcrosshair = mc.crosshairTarget;
        if (allcrosshair.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult asshair = (BlockHitResult) allcrosshair;
            BlockPos renderMe = asshair.getBlockPos();
            Color renderColoreds = Color.MAGENTA;
            assert mc.world != null;
            if (mc.world.getBlockState(renderMe).getBlock() != Blocks.RESPAWN_ANCHOR) {
                int sidex = (((asshair))).getSide().getOffsetX();
                int sidey = (((asshair))).getSide().getOffsetY();
                int sidez = (((asshair))).getSide().getOffsetZ();
                renderColoreds = Color.YELLOW;
                renderMe = renderMe.add(sidex, sidey, sidez);
            }
            event.renderer.box(renderMe, new Color(0,0,0,0) , renderColoreds, ShapeMode.Lines, 0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (mc.world == null) return;
        ItemStack mainHand = mc.player.getMainHandStack();
        if (mainHand == null) return;
        Item handItem = mainHand.getItem();
        if (mc.crosshairTarget == null) return;
        HitResult allcrosshair = mc.crosshairTarget;

        if (mc.options.useKey.isPressed() && allcrosshair.getType() == HitResult.Type.BLOCK) {
            BlockHitResult asshair = (BlockHitResult)allcrosshair;
            assert mc.interactionManager != null;
            boolean anchorExistsAtPtr = mc.world.getBlockState(asshair.getBlockPos()).getBlock() == Blocks.RESPAWN_ANCHOR;

            if (anchorExistsAtPtr && phase == 0) {// early w/o placing
                phase = 1;
            }

            if (phase == 0 && handItem == Items.RESPAWN_ANCHOR) { // place anchor
                BlockPos toPlaceOn = asshair.getBlockPos();
                assert mc.world != null;
                if (mc.world.getBlockState(toPlaceOn).getBlock() != Blocks.RESPAWN_ANCHOR) {
                    int sidex = (((asshair))).getSide().getOffsetX();
                    int sidey = (((asshair))).getSide().getOffsetY();
                    int sidez = (((asshair))).getSide().getOffsetZ();

                    toPlaceOn = toPlaceOn.add(sidex, sidey, sidez);

                    BlockUtils.place(toPlaceOn, Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, false, 0, true, true, false);
                }
                return;
            }

            if (phase == 1 && sDel <= 0) { // switch to item glowstone
                FindItemResult result = InvUtils.find(Items.GLOWSTONE);
                if (!result.found()) {warning("completely out of GLOWSTONE! Disabling!"); toggle();}

                if (!shouldUseInv.get()) {
                    if (result.isHotbar())
                        InvUtils.swap(result.slot(), false);
                    else {
                        warning("GLOWSTONE not in HOTBAR! Disabling!");
                        toggle();
                    }
                } else {
                    InvUtils.move().from(result.slot()).to(mc.player.getInventory().selectedSlot);
                }
                phase = 2;
            }
            if (phase == 2 && cDel <= 0) { // charge anchor
                if (anchorExistsAtPtr) {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, asshair);
                    phase = 3;
                    sDel = changeSlotDel.get();
                } else {
                    phase = 6;
                }
            }
            if (phase == 3 && sDel <= 0) { // switch to item anchor
                FindItemResult result = InvUtils.find(Items.RESPAWN_ANCHOR);
                if (!shouldUseInv.get()) {
                    if (result.isHotbar())
                        InvUtils.swap(result.slot(), false);
                    else {
                        warning("No more anchors in hotbar!");
                        emptyWarningThrown = true;
                        InvUtils.swap(mc.player.getInventory().selectedSlot + 1, false);
                    }
                } else {
                    if (!result.found()) {
                        warning("No more anchors in hotbar!");
                        emptyWarningThrown = true;
                        InvUtils.swap(mc.player.getInventory().selectedSlot + 1, false);
                    } else InvUtils.move().from(result.slot()).to(mc.player.getInventory().selectedSlot);
                }
                phase = 4;
            }
            if (phase == 4 && bDel <= 0){ // explode
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, asshair);
                phase = 5;
                if (emptyWarningThrown)
                    toggle();
            }

            if (phase == 1 || phase == 3) sDel--;
            else if (phase == 2) cDel--;
            else if (phase == 4) bDel--; // decr ticks
            else if (phase == 5 && pDel > 0) pDel--;
            else resetPhase();
        } else resetPhase();
    }

    public Boolean disablePlacing() {
        if (!isActive()) return false;
        if (mc.player == null) return false;
        Item handItem = mc.player.getMainHandStack().getItem();
        return (handItem == Items.RESPAWN_ANCHOR) || (handItem == Items.GLOWSTONE);
    }
}
