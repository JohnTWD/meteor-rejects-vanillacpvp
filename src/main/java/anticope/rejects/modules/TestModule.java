package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class TestModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public TestModule() {
        super(MeteorRejectsAddon.CATEGORY, "testmod", "for testing purposes only");
    }

    public enum Testwhat {
        testseecanplace,
        nothin
    }
    private final Setting<TestModule.Testwhat> what2test = sgGeneral.add(new EnumSetting.Builder<TestModule.Testwhat>()
            .name("what2test")
            .description("duh")
            .defaultValue(Testwhat.testseecanplace)
            .build()
    );

    private boolean canPlace(Vec3d toPlace, Box me) {
        Box placebox = new Box(toPlace, toPlace.add(1, 1, 1));
        return me.intersects(placebox);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        if (mc.world == null) return;
        if (mc.player == null) return;

        mc.crosshairTarget.getType();

        switch (what2test.get()) {
            case testseecanplace:
                HitResult ggerni = mc.crosshairTarget;
                if (ggerni.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blok = (BlockHitResult) ggerni;
                    if (!canPlace(Vec3d.of((blok.getBlockPos().up())), mc.player.getBoundingBox()))
                        info("cant place");
                    else info("you canplace lol");
                }

                break;
            case nothin:
                info("none");
                break;
        }

    }
}
