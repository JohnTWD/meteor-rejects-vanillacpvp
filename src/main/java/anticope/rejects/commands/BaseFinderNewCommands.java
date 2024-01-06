package anticope.rejects.commands;
import anticope.rejects.modules.BaseFinderNew;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BaseFinderNewCommands extends Command {
    public BaseFinderNewCommands() {
        super("bfn", "Extra functionality for the BaseFinder module.");
    }
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        BaseFinderNew b=new BaseFinderNew();
        builder.executes(ctx -> {
            if(b.isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and run the command again.");
                return SINGLE_SUCCESS;
            } else {
                b.findnearestbaseticks=1;
                return SINGLE_SUCCESS;
            }
        });
        builder.then(literal("add").executes(ctx -> {
            if(b.isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and run the command again.");
                return SINGLE_SUCCESS;
            } else {
                b.AddCoordX= mc.player.getChunkPos().x;
                b.AddCoordZ= mc.player.getChunkPos().z;
                ChatUtils.sendMsg(Text.of("Base near X"+mc.player.getChunkPos().getCenterX()+", Z"+mc.player.getChunkPos().getCenterZ()+" added to the BaseFinder."));
                return SINGLE_SUCCESS;}
        }));
        builder.then(literal("add").then(argument("x",FloatArgumentType.floatArg()).then(argument("z",FloatArgumentType.floatArg()).executes(ctx -> {
            if(b.isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and run the command again.");
                return SINGLE_SUCCESS;
            } else {
                float X = FloatArgumentType.getFloat(ctx, "x");
                float Z = FloatArgumentType.getFloat(ctx, "z");
                b.AddCoordX= Math.floorDiv((int) X,16);
                b.AddCoordZ= Math.floorDiv((int) Z,16);
                ChatUtils.sendMsg(Text.of("Base near X"+X+", Z"+Z+" added to the BaseFinder."));
                return SINGLE_SUCCESS;}
        }))));
        builder.then(literal("rmv").executes(ctx -> {
            if(b.isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and run the command again.");
                return SINGLE_SUCCESS;
            } else {
                b.RemoveCoordX= mc.player.getChunkPos().x;
                b.RemoveCoordZ= mc.player.getChunkPos().z;
                ChatUtils.sendMsg(Text.of("Base near X"+mc.player.getChunkPos().getCenterX()+", Z"+mc.player.getChunkPos().getCenterZ()+" removed from the BaseFinder."));
                return SINGLE_SUCCESS;}
        }));
        builder.then(literal("rmv").then(literal("last").executes(ctx -> {
            if(b.isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and run the command again.");
                return SINGLE_SUCCESS;
            } else if(b.isBaseFinderModuleOn!=0 && (b.LastBaseFound.x==2000000000 || b.LastBaseFound.z==2000000000)){
                error("Please find a base and run the command again.");
                return SINGLE_SUCCESS;
            } else {
                b.RemoveCoordX= b.LastBaseFound.x;
                b.RemoveCoordZ= b.LastBaseFound.z;
                ChatUtils.sendMsg(Text.of("Base near X"+b.LastBaseFound.getCenterX()+", Z"+b.LastBaseFound.getCenterZ()+" removed from the BaseFinder."));
                b.LastBaseFound= new ChunkPos(2000000000, 2000000000);
                return SINGLE_SUCCESS;
            }
        })));
        builder.then(literal("rmv").then(argument("x",FloatArgumentType.floatArg()).then(argument("z",FloatArgumentType.floatArg()).executes(ctx -> {
            if(b.isBaseFinderModuleOn==0){
                error("Please turn on BaseFinder module and run the command again.");
                return SINGLE_SUCCESS;
            } else {
                float X = FloatArgumentType.getFloat(ctx, "x");
                float Z = FloatArgumentType.getFloat(ctx, "z");
                b.RemoveCoordX= Math.floorDiv((int) X,16);
                b.RemoveCoordZ= Math.floorDiv((int) Z,16);
                ChatUtils.sendMsg(Text.of("Base near X"+X+", Z"+Z+" removed from the BaseFinder."));
                return SINGLE_SUCCESS;}
        }))));
    }
}