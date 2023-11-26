package anticope.rejects.mixin.meteor.modules;

import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.combat.Surround;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Surround.class, remap = false)
public class SurroundMixin extends Module{
    public SurroundMixin(Category category, String name, String description) {
        super(category, name, description);
    }

}
