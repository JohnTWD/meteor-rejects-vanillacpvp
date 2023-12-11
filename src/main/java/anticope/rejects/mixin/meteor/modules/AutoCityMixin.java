package anticope.rejects.mixin.meteor.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoCity;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;


@Mixin(value = AutoCity.class, remap = false)
public abstract class AutoCityMixin extends Module {
    @Shadow @Final private SettingGroup sgGeneral;
    @Shadow private FindItemResult pick;

    @Shadow @Final private Setting<AutoCity.SwitchMode> switchMode;

    @Shadow @Final private Setting<Boolean> rotate;
    @Shadow @Final private Setting<Boolean> chatInfo;
    @Shadow @Final private Setting<Integer> breakRange;

    @Shadow private BlockPos targetPos;

    public AutoCityMixin(Category category, String name, String description) {
        super(category, name, description);
    }
    @Unique
    private Setting<Boolean> activateCA;
    @Unique
    private Setting<Boolean> switchToCrystals;
    @Unique
    private Setting<Boolean> instamine;
    @Unique
    private Setting<Integer> tickDelay;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        activateCA = sgGeneral.add(new BoolSetting.Builder()
                .name("Turn on CA")
                .description("Turn on CA when shit is done")
                .defaultValue(false)
                .build()
        );
        switchToCrystals = sgGeneral.add(new BoolSetting.Builder()
                .name("Switch to Crystals")
                .description("when mining done, do this www")
                .defaultValue(false)
                .build()
        );
        instamine = sgGeneral.add(new BoolSetting.Builder()
                .name("instamine")
                .description("run the enemy out of obby")
                .defaultValue(false)
                .build()
        );
        tickDelay = sgGeneral.add(new IntSetting.Builder()
                .name("delay")
                .description("The delay between breaks.")
                .defaultValue(0)
                .min(0)
                .sliderMax(20)
                .build()
        );
    }

    @Inject(method="mine", at = @At("HEAD"), cancellable = true)
    private void onMine2(boolean done, CallbackInfo info) {
        if (!done && instamine.get()) {
            info.cancel();
        }
    }
    @Unique
    private int oDel = 0;
    @Inject(method = "onTick", at = @At("HEAD"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void onTick(TickEvent.Pre event, CallbackInfo ci) {
        if (instamine.get()) {

            if (PlayerUtils.squaredDistanceTo(targetPos) > Math.pow(this.breakRange.get(), 2)) {
                if (this.chatInfo.get())
                    error("Block too far");
                toggle();
                return;
            }
            if (!pick.isHotbar()) {
                error("No pickaxe found... disabling.");
                toggle();
                return;
            }


            if (oDel >= tickDelay.get()) {
                oDel = 0;
                InvUtils.swap(this.pick.slot(), this.switchMode.get() == AutoCity.SwitchMode.Silent);
                Direction direction = BlockUtils.getDirection(targetPos);
                if (this.rotate.get())
                    Rotations.rotate(Rotations.getYaw(this.targetPos), Rotations.getPitch(this.targetPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, direction)));
                else
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, direction));
            } else oDel++;
            ci.cancel();
        }
    }

    @Inject(method="mine", at = @At("TAIL"))
    private void onMine(boolean done, CallbackInfo info) {
        if (done) {
            if (switchToCrystals.get()) {
                FindItemResult result = InvUtils.find(Items.END_CRYSTAL);
                if (result.isHotbar())
                    InvUtils.swap(result.slot(), false);
            }


            if (activateCA.get() && !Modules.get().get(CrystalAura.class).isActive())
                Modules.get().get(CrystalAura.class).toggle();
        }
    }
}
