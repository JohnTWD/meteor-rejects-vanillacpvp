package anticope.rejects.modules;
// autoshield from https://github.com/MeteorDevelopment/meteor-client/blob/d5f6f82ec7baa5f1abf74e05dcc003fdb95d585f/src/main/java/meteordevelopment/meteorclient/systems/modules/combat/Shield.java

import anticope.rejects.MeteorRejectsAddon;
import anticope.rejects.utils.KeyBindingPresser;
import anticope.rejects.utils.RejectEntityUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Shield extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to block against.")
            .defaultValue(EntityType.PLAYER)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range the entity can be it.")
            .defaultValue(3)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build()
    );

    private final KeyBindingPresser keybindingPresser = new KeyBindingPresser(mc.options.useKey);
    private final List<Entity> targets = new ArrayList<>();

    public Shield() {
        super(MeteorRejectsAddon.CATEGORY, "shield", "Automatically blocks damage.");
    }

    @Override
    public void onDeactivate() {
        keybindingPresser.stopIfPressed();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!InvUtils.testInHands(Items.SHIELD)) keybindingPresser.stopIfPressed();
        TargetUtils.getList(targets, this::entityCheck, priority.get(), 1);

        if (!mc.options.attackKey.isPressed() && !targets.isEmpty()) {
            Entity target = targets.get(0);
            if (!RejectEntityUtils.blockedByShield(target, mc.player)) Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target));
            keybindingPresser.use();
        } else {
            boolean using = keybindingPresser.isPressed();
            keybindingPresser.stopIfPressed();
            if (mc.options.attackKey.isPressed()) {
                if (using) Utils.leftClick();
            }
        }
    }

    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.cameraEntity)) return false;
        if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
        if (PlayerUtils.distanceTo(entity) > range.get()) return false;
        if (!entities.get().contains(entity.getType())) return false;

        if (entity instanceof ArrowEntity && entity.isOnGround()) return false; // so we only detect arrows that are flying
        if (entity instanceof EndCrystalEntity && !PlayerUtils.canSeeEntity(entity)) return false;
        if (entity instanceof PlayerEntity pe) {
            Item m = pe.getMainHandStack().getItem();
            Item o = pe.getOffHandStack().getItem();
            if (!(m instanceof SwordItem && o instanceof SwordItem)) return false;
        }
        return true;
    }
}