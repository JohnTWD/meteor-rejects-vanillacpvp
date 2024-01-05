package anticope.rejects.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.entity.Entity;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

// stolen from https://github.com/cally72jhb/vector-addon/blob/80dc766f9177e0f4bef9f9d8bbc5b70e1853ab1a/src/main/java/cally72jhb/addon/commands/commands/DesyncCommand.java#L11
public class EntityDesyncCommand extends Command {
    private Entity entity = null;

    public EntityDesyncCommand() {
        super("edesync", "Desyncs yourself or the vehicle you're riding from the server.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (this.entity == null) {
                if (mc.player.hasVehicle()) {
                    this.entity = mc.player.getVehicle();

                    mc.player.dismountVehicle();
                    mc.world.removeEntity(this.entity.getId(), Entity.RemovalReason.UNLOADED_TO_CHUNK);

                    info("Successfully desynced your vehicle");
                } else {
                    error("You are not riding an entity.");
                }
            } else {
                if (!mc.player.hasVehicle()) {
                    mc.world.addEntity(this.entity.getId(), this.entity);
                    mc.player.startRiding(this.entity, true);

                    this.entity = null;

                    info("Successfully resynced your vehicle");
                } else {
                    error("You are not riding another entity.");
                }
            }

            return SINGLE_SUCCESS;
        });

        builder.then(literal("entity")).executes(context -> {
            if (this.entity == null) {
                if (mc.player.hasVehicle()) {
                    this.entity = mc.player.getVehicle();

                    mc.player.dismountVehicle();
                    mc.world.removeEntity(this.entity.getId(), Entity.RemovalReason.UNLOADED_TO_CHUNK);

                    info("Successfully desynced your vehicle");
                } else {
                    error("You are not riding an entity.");
                }
            } else {
                if (!mc.player.hasVehicle()) {
                    mc.world.addEntity(this.entity.getId(), this.entity);
                    mc.player.startRiding(this.entity, true);

                    this.entity = null;

                    info("Successfully resynced your vehicle");
                } else {
                    error("You are not riding another entity.");
                }
            }

            return SINGLE_SUCCESS;
        });

        builder.then(literal("player")).executes(context -> {
            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(0));
            info("Successfully desynced your player entity");

            return SINGLE_SUCCESS;
        });
    }
}
