package anticope.rejects.mixin.meteor.modules;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoCity;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(value = AutoCity.class, remap = false)
public abstract class AutoCityMixin extends Module {
    @Shadow @Final private SettingGroup sgGeneral;
    public AutoCityMixin(Category category, String name, String description) {
        super(category, name, description);
    }
    @Unique
    private Setting<Boolean> activateCA;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        activateCA = sgGeneral.add(new BoolSetting.Builder()
                .name("Turn on CA")
                .description("Turn on CA when shit is done")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method="mine", at = @At("TAIL"))
    private void onMine(CallbackInfo info) {
        if (activateCA.get())
            Modules.get().get(CrystalAura.class).toggle();
    }

}
