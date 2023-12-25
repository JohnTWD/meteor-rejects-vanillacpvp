package anticope.rejects.modules;
// from https://github.com/MeteorDevelopment/meteor-client/blob/e0f42e10ae7f7e9b9741b02fad0b8332738644a2/src/main/java/meteordevelopment/meteorclient/systems/modules/combat/KillAura.java#L339

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.math.Vec3d;

import java.util.Set;


public class BetterAimbot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range the entity can be to aim at it.")
            .defaultValue(20)
            .range(0, 100)
            .sliderMax(100)
            .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to attack.")
            .onlyAttackable()
            .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("priority")
            .description("What type of entities to target.")
            .defaultValue(SortPriority.LowestHealth)
            .build()
    );

    private final Setting<Boolean> babies = sgGeneral.add(new BoolSetting.Builder()
            .name("babies")
            .description("Whether or not to attack baby variants of the entity.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> nametagged = sgGeneral.add(new BoolSetting.Builder()
            .name("nametagged")
            .description("Whether or not to attack mobs with a name tag.")
            .defaultValue(false)
            .build()
    );


    private final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-combat")
            .description("Freezes Baritone temporarily until you released the bow.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotate player's view angle.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> predict = sgGeneral.add(new BoolSetting.Builder()
            .name("predict")
            .description("Predicts the projectile path using tick delta.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> predictStrength = sgGeneral.add(new DoubleSetting.Builder()
            .name("predict-strength")
            .description("Controls the prediction strength.")
            .defaultValue(0.2)
            .min(0)
            .sliderMax(2)
            .visible(predict::get)
            .build()
    );

    private boolean wasPathing;
    private Entity target;

    public BetterAimbot() {
        super(MeteorRejectsAddon.CATEGORY, "better-bow-aimbot", "Automatically aims your bow for you.");
    }

    @Override
    public void onDeactivate() {
        target = null;
        wasPathing = false;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!PlayerUtils.isAlive() || !itemInHand()) return;
        if (!mc.player.getAbilities().creativeMode && !InvUtils.find(itemStack -> itemStack.getItem() instanceof ArrowItem).found()) return;

        target = TargetUtils.get(entity -> {
            if (entity == mc.player || entity == mc.cameraEntity) return false;
            if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
            if (!PlayerUtils.isWithin(entity, range.get())) return false;
            if (!entities.get().contains(entity.getType())) return false;
            if (!nametagged.get() && entity.hasCustomName()) return false;
            if (!PlayerUtils.canSeeEntity(entity)) return false;
            if (entity instanceof PlayerEntity) {
                if (((PlayerEntity) entity).isCreative()) return false;
                if (!Friends.get().shouldAttack((PlayerEntity) entity)) return false;
            }
            return !(entity instanceof AnimalEntity) || babies.get() || !((AnimalEntity) entity).isBaby();
        }, priority.get());


        if (mc.options.useKey.isPressed() && itemInHand()) {
            aim(event.tickDelta);
        }
    }

    private boolean itemInHand() {
        return mc.player.getMainHandStack().getItem() instanceof BowItem || mc.player.getMainHandStack().getItem() instanceof CrossbowItem;
    }

    private void aim(double tickDelta) {
        // Velocity based on bow charge.
        float velocity = (mc.player.getItemUseTime() - mc.player.getItemUseTimeLeft()) / 20f;
        velocity = (velocity * velocity + velocity * 2) / 3;
        if (velocity > 1) velocity = 1;

        // Positions
        double delta = predict.get() ? mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter()) * predictStrength.get() : tickDelta;
        double posX = target.getPos().getX() + (target.getPos().getX() - target.prevX) * delta;
        double posY = target.getPos().getY() + (target.getPos().getY() - target.prevY) * delta;
        double posZ = target.getPos().getZ() + (target.getPos().getZ() - target.prevZ) * delta;

        // Adjusting for hitbox heights
        posY -= 1.9f - target.getHeight();

        double relativeX = posX - mc.player.getX();
        double relativeY = posY - mc.player.getY();
        double relativeZ = posZ - mc.player.getZ();

        // Calculate the pitch
        double hDistance = Math.sqrt(relativeX * relativeX + relativeZ * relativeZ);
        double hDistanceSq = hDistance * hDistance;
        float g = 0.006f;
        float velocitySq = velocity * velocity;
        float pitch = (float) -Math.toDegrees(Math.atan((velocitySq - Math.sqrt(velocitySq * velocitySq - g * (g * hDistanceSq + 2 * relativeY * velocitySq))) / (g * hDistance)));

        // Set player rotation
        if (Float.isNaN(pitch)) {
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target));
            } else {
                mc.player.setYaw((float) Rotations.getYaw(target));
                mc.player.setPitch((float) Rotations.getPitch(target));
            }
        } else {
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(new Vec3d(posX, posY, posZ)), pitch);
            } else {
                mc.player.setYaw((float) Rotations.getYaw(new Vec3d(posX, posY, posZ)));
                mc.player.setPitch(pitch);
            }
        }
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}