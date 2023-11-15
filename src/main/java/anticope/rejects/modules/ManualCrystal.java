package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

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

    private enum RotateType{
        NoRotate,
        DoRotate
    }

    private final Setting<RotateType> rotateSetting = sgGeneral.add(new EnumSetting.Builder<RotateType>()
            .name("breakRotateSetting")
            .description("whether or not should rotate to break a crystal")
            .defaultValue(RotateType.NoRotate)
            .build()
    );

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        /*
        if (mc.player == null) return;
        ItemStack mainHand = mc.player.getMainHandStack();
        if (mainHand == null) return;

        Item handItem = mainHand.getItem();

        if (mc.options.useKey.isPressed()) {
            if (handItem == Items.END_CRYSTAL) {

            }
        }*/
    }

}
