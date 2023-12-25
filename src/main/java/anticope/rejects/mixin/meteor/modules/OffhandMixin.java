package anticope.rejects.mixin.meteor.modules;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.Offhand;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;


@Mixin(value = Offhand.class, remap = false)
public abstract class OffhandMixin extends Module {
    @Final private SettingGroup sgPauses;
    public OffhandMixin(Category category, String name, String description) {
        super(category, name, description);
    }
    @Unique private Setting<Boolean> pauseOnUse;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        pauseOnUse = sgPauses.add(new BoolSetting.Builder()
                .name("stopOnUse")
                .description("temp stops things when using something")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method = "onTick", at = @At(value = "HEAD"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void onTick(TickEvent.Pre event, CallbackInfo ci) {
        if (mc.world == null) return;
        if (mc.player == null) return;

        if (pauseOnUse.get() && mc.player.isUsingItem()) {
            ci.cancel();
        }
    }
}
