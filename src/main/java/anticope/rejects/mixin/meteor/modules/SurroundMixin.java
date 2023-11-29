package anticope.rejects.mixin.meteor.modules;


import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.combat.Surround;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = Surround.class, remap = false)
public abstract class SurroundMixin extends Module{
    @Shadow
    @Final
    private SettingGroup sgGeneral;
    @Shadow
    @Final
    private Setting<Boolean> rotate;
    @Shadow
    @Final
    private Setting<Boolean> swing;

    @Mutable
    @Shadow @Final
    private final Setting<SettingColor> normalSideColor;
    @Mutable
    @Shadow @Final
    private final Setting<SettingColor> normalLineColor;

    @Shadow protected abstract FindItemResult getInvBlock();

    @Shadow @Final private Setting<ShapeMode> shapeMode;

    public SurroundMixin(Category category, String name, String description, Setting<SettingColor> normalSideColor, Setting<SettingColor> normalLineColor) {
        super(category, name, description);
        this.normalSideColor = normalSideColor;
        this.normalLineColor = normalLineColor;
    }

    @Unique
    private Setting<Boolean> protectTop;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        protectTop = sgGeneral.add(new BoolSetting.Builder()
                .name("protectTop")
                .description("to determine if should place at top of head (prevent auto anchoring)")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method = "onTick", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onTick(TickEvent.Pre event, CallbackInfo ci) {
        if (mc.world == null) return;
        if (mc.player == null) return;

        if (protectTop.get()) {
            BlockPos pos = mc.player.getBlockPos().add(0,2,0);
            BlockUtils.place(pos, getInvBlock(), rotate.get(), 100, swing.get(), true);
        }
    }

    @Inject(method = "onRender3D", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onRender3d(Render3DEvent event, CallbackInfo ci) {
        if (mc.world == null) return;
        if (mc.player == null) return;

        if (protectTop.get()) {
            event.renderer.box(mc.player.getBlockPos().add(0,2,0), normalSideColor.get(), normalLineColor.get(), shapeMode.get(), 0);
        }
    }

}
