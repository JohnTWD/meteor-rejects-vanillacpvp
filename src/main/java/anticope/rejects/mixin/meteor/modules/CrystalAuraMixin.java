package anticope.rejects.mixin.meteor.modules;

import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import org.spongepowered.asm.mixin.*;



@Mixin(value = CrystalAura.class, remap = false)
public abstract class CrystalAuraMixin extends Module {
    @Shadow @Final private SettingGroup sgSwitch;
    public CrystalAuraMixin(Category category, String name, String description) {
        super(category, name, description);
    }
}
