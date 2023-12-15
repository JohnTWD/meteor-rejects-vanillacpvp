package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class TestModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public TestModule() {
        super(MeteorRejectsAddon.CATEGORY, "testmod", "for testing purposes only");
    }

    public enum Testwhat {
        testseecanplace,
        placeDirections,
        whatamilookingat,
        nothin
    }
    private final Setting<TestModule.Testwhat> what2test = sgGeneral.add(new EnumSetting.Builder<TestModule.Testwhat>()
            .name("what2test")
            .description("duh")
            .defaultValue(Testwhat.testseecanplace)
            .build()
    );

    private final Setting<CardinalDirection> directions = sgGeneral.add(new EnumSetting.Builder<CardinalDirection>()
            .name("directionTester")
            .description("duh")
            .defaultValue(CardinalDirection.North)
            .visible(() -> what2test.get() == Testwhat.placeDirections)
            .build()
    );

    private boolean canPlace(Vec3d toPlace, Box me) {
        Box placebox = new Box(toPlace, toPlace.add(1, 1, 1));
        return !me.intersects(placebox);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null) return;
        if (mc.player == null) return;

        HitResult ct = mc.crosshairTarget;
        if (ct == null) return;

        switch (what2test.get()) {
            case testseecanplace:
                if (ct.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blok = (BlockHitResult) ct;
                    if (!canPlace(Vec3d.of((blok.getBlockPos().up())), mc.player.getBoundingBox()))
                        info("cant place");
                    else info("you canplace lol");
                }

                break;
            case placeDirections:
                if (ct.getType() == HitResult.Type.BLOCK) {
                    BlockPos bct = ((BlockHitResult) ct).getBlockPos();

                    if (mc.options.sneakKey.isPressed()) {
                        double yaw = switch (directions.get()) {
                            case East -> 90;
                            case South -> 180;
                            case West -> -90;
                            default -> 0;
                        };

                        Rotations.rotate(yaw, Rotations.getPitch(bct), () -> {
                            BlockUtils.place(bct, Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, false, 0, true, true, false);
                        });
                    }
                }
                break;
            case whatamilookingat:
                if (ct.getType() == HitResult.Type.BLOCK) {
                    BlockPos bct = ((BlockHitResult) ct).getBlockPos();
                    info(mc.world.getBlockState(bct).getBlock().getName());
                }
                break;
            case nothin:
                info("none");
                break;
        }

    }
}
