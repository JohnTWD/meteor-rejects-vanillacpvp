package anticope.rejects.mixin.meteor.modules;


import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.world.PacketMine;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;


@Mixin(value = PacketMine.class, remap = false)
public abstract class PacketMineMixin extends Module {
    @Shadow @Final private SettingGroup sgGeneral;

    @Shadow @Final private Setting<Boolean> autoSwitch;

    @Shadow @Final public List<PacketMine.MyBlock> blocks;

    @Shadow private boolean shouldUpdateSlot;

    @Shadow private boolean swapped;
    @Unique private Setting<Boolean> shouldForceSwitch;
    @Unique private Setting<Boolean> onlyGoodTools;
    @Mutable @Shadow @Final private Setting<Boolean> notOnUse;

    public PacketMineMixin(Category category, String name, String description, Setting<Boolean> notOnUse) {
        super(category, name, description);
        this.notOnUse = notOnUse;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        shouldForceSwitch = sgGeneral.add(new BoolSetting.Builder()
                .name("forceSwitch")
                .description("If should use force switching instead of the other switches (this overrides autoSwitch, which uses packets instead of inv switch)")
                .defaultValue(false)
                .visible(autoSwitch::get)
                .build()
        );

        onlyGoodTools = sgGeneral.add(new BoolSetting.Builder()
                .name("onlyTools")
                .description("Only activates if the tool you are holding is good for mining said block")
                .defaultValue(false)
                .visible(autoSwitch::get)
                .build()
        );
    }

    @Inject(method = "onStartBreakingBlock", at = @At(value = "HEAD"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void modifyOnStartBreakingBlock(StartBreakingBlockEvent event, CallbackInfo ci) {
        if (onlyGoodTools.get()) {
            ItemStack mainHandStack = mc.player.getMainHandStack();
            ItemStack o0fhandStack = mc.player.getOffHandStack();
            BlockState attackedBl0ck = mc.world.getBlockState(event.blockPos);
            if (!mainHandStack.isSuitableFor(attackedBl0ck) && !o0fhandStack.isSuitableFor(attackedBl0ck))
                ci.cancel();
        }
    }

    @Inject(method = "onTick", at = @At(value = "HEAD"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void onTick(TickEvent.Pre event, CallbackInfo ci) {
        if (mc.world == null) return;
        if (mc.player == null) return;

        if (shouldForceSwitch.get()) {
            this.blocks.removeIf(PacketMine.MyBlock::shouldRemove);

            if (this.shouldUpdateSlot) {
                InvUtils.swapBack();
                shouldUpdateSlot = false;
            }

            if (!blocks.isEmpty()) blocks.get(0).mine();

            if (!this.swapped && autoSwitch.get() && (!mc.player.isUsingItem() || !notOnUse.get())) {
                for (PacketMine.MyBlock block : blocks) {
                    if (block.isReady()) {
                        FindItemResult slot = InvUtils.findFastestTool(block.blockState);
                        if (!slot.found() || mc.player.getInventory().selectedSlot == slot.slot()) continue;
                        InvUtils.swap(slot.slot(), true);
                        swapped = true;
                        shouldUpdateSlot = true;
                        break;
                    }
                }
            }
            ci.cancel();
        }
    }
}
