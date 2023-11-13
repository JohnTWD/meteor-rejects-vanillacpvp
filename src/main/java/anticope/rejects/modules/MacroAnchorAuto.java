package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.entity.player.PlaceBlockEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Items.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

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

    private final Setting<Integer> switchDel = sgGeneral.add(new IntSetting.Builder()
            .name("switchdelay")
            .description("how long in ticks to wait for switch between blocks")
            .defaultValue(1)
            .range(0,40)
            .sliderRange(0,40)
            .build()
    );

    private final Setting<Integer> placeAnchorDel = sgGeneral.add(new IntSetting.Builder()
            .name("placeAnchorDelay")
            .description("how long in ticks to wait to place an anchor (If this)")
            .defaultValue(1)
            .range(0,40)
            .sliderRange(1,40)
            .build()
    );

    private final Setting<Integer> chargeAnchorDel = sgGeneral.add(new IntSetting.Builder()
            .name("placeAnchorDelay")
            .description("how long in ticks to charge anchor")
            .defaultValue(1)
            .range(0,40)
            .sliderRange(0,40)
            .build()
    );

    private final Setting<Boolean> awaitBlockPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("immediatePlace")
            .description("Whether to check for anchor to actually appear in world first (place/charge delays will still apply afterwards)")
            .defaultValue(false)
            .build()
    );
    private int Odel = 0;
    private boolean hasAnchored = false;
    private int phase = 0;
    /* PHASES
     * 0 = Begin
     * 1 = Anchor placed
     * 2 = Charge w/ GS
     * 3 = Switch back to anchor and activate
     */

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        ItemStack mainHand = mc.player.getMainHandStack();
        if (mainHand == null) return;

        Item handItem = mainHand.getItem();

        if (!hasAnchored && mc.options.useKey.isPressed() && handItem == Items.RESPAWN_ANCHOR) {
            //placeBlok
            BlockPos toPlaceOn = ((BlockHitResult) mc.crosshairTarget).getBlockPos();

            int sidex = ((BlockHitResult) mc.crosshairTarget).getSide().getOffsetX();
            int sidey = ((BlockHitResult) mc.crosshairTarget).getSide().getOffsetY();
            int sidez = ((BlockHitResult) mc.crosshairTarget).getSide().getOffsetZ();

            toPlaceOn = toPlaceOn.add(sidex, sidey, sidez);

            info("%d %d %d", toPlaceOn.getX(), toPlaceOn.getY(), toPlaceOn.getZ());
            info("side: %d %d %d", sidex, sidey, sidez);
            BlockUtils.place(toPlaceOn, Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, false, 0, true, true, false);

            hasAnchored = true;
            phase = 1;
        }
    }

    private BlockPos findBlockForPlace(){
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPlaceBlock(PlaceBlockEvent event) {
    }

    public Boolean disablePlacing() {
        if (!isActive()) return false;
        Item handItem = mc.player.getMainHandStack().getItem();
        return (handItem == Items.RESPAWN_ANCHOR) || (handItem == Items.GLOWSTONE);
    }
}
