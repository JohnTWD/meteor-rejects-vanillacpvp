package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPChunk;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

/*
    Ported from: https://github.com/BleachDrinker420/BleachHack/blob/master/BleachHack-Fabric-1.16/src/main/java/bleach/hack/module/mods/NewChunks.java
*/
public class NewChunks extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgRender = settings.createGroup("Render");

	// general

	private final Setting<Boolean> remove = sgGeneral.add(new BoolSetting.Builder()
        .name("remove")
        .description("Removes the cached chunks when disabling the module.")
        .defaultValue(true)
        .build()
    );

	private final Setting<Boolean> find1_19 = sgGeneral.add(new BoolSetting.Builder()
			.name("find1.19")
			.description("Tries to look for 1.19 chunk using copper ore and nether gold Ore")
			.defaultValue(false)
			.build()
	);

	// render
	public final Setting<Integer> renderHeight = sgRender.add(new IntSetting.Builder()
			.name("render-height")
			.description("The height at which new chunks will be rendered")
			.defaultValue(0)
			.min(-64)
			.sliderRange(-64,319)
			.build()
	);

	private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
			.name("shape-mode")
			.description("How the shapes are rendered.")
			.defaultValue(ShapeMode.Both)
			.build()
	);

	private final Setting<SettingColor> newChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("new-chunks-side-color")
			.description("Color of the chunks that are (most likely) completely new.")
			.defaultValue(new SettingColor(255, 0, 0, 75))
			.visible(() -> shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both)
			.build()
	);

	private final Setting<SettingColor> oldChunksSideColor = sgRender.add(new ColorSetting.Builder()
			.name("old-chunks-side-color")
			.description("Color of the chunks that have (most likely) been loaded before.")
			.defaultValue(new SettingColor(0, 255, 0, 75))
			.visible(() -> shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both)
			.build()
	);

	private final Setting<SettingColor> newChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("new-chunks-line-color")
			.description("Color of the chunks that are (most likely) completely new.")
			.defaultValue(new SettingColor(255, 0, 0, 255))
			.visible(() -> shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both)
			.build()
	);

	private final Setting<SettingColor> oldChunksLineColor = sgRender.add(new ColorSetting.Builder()
			.name("old-chunks-line-color")
			.description("Color of the chunks that have (most likely) been loaded before.")
			.defaultValue(new SettingColor(0, 255, 0, 255))
			.visible(() -> shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both)
			.build()
	);

	private final Setting<SettingColor> oneDot19Color = sgRender.add(new ColorSetting.Builder()
			.name("1.19Color")
			.description("Color of likely 1.19 chunks")
			.defaultValue(new SettingColor(255, 0, 0, 75))
			.visible(find1_19::get)
			.build()
	);

    private final Set<ChunkPos> newChunks = Collections.synchronizedSet(new HashSet<>());
    private final Set<ChunkPos> oldChunks = Collections.synchronizedSet(new HashSet<>());
	private final Set<ChunkPos> od19chunks = Collections.synchronizedSet(new HashSet<>());
    private static final Direction[] searchDirs = new Direction[] { Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP };

    public NewChunks() {
        super(MeteorRejectsAddon.CATEGORY,"new-chunks", "Detects completely new chunks using certain traits of them (now with 1.19 finder)!");
    }

	@Override
	public void onDeactivate() {
		if (remove.get()) {
			newChunks.clear();
			oldChunks.clear();
			od19chunks.clear();
		}
		super.onDeactivate();
	}

	@EventHandler
	private void onRender(Render3DEvent event) {
		if (newChunksLineColor.get().a > 5 || newChunksSideColor.get().a > 5) {
			synchronized (newChunks) {
				for (ChunkPos c : newChunks) {
					if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), 1024)) {
						render(new Box(c.getStartPos(), c.getStartPos().add(16, renderHeight.get(), 16)), newChunksSideColor.get(), newChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}

		if (oldChunksLineColor.get().a > 5 || oldChunksSideColor.get().a > 5){
			synchronized (oldChunks) {
				for (ChunkPos c : oldChunks) {
					if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), 1024)) {
						render(new Box(c.getStartPos(), c.getStartPos().add(16, renderHeight.get(), 16)), oldChunksSideColor.get(), oldChunksLineColor.get(), shapeMode.get(), event);
					}
				}
			}
		}
		if (oneDot19Color.get().a > 5) {
			synchronized (od19chunks) {
				for (ChunkPos c : od19chunks) {
					if (mc.getCameraEntity().getBlockPos().isWithinDistance(c.getStartPos(), 1024)) {
						render(new Box(c.getStartPos().up(3), c.getStartPos().add(16, renderHeight.get(), 16)), oneDot19Color.get(), oneDot19Color.get(), shapeMode.get(), event);
					}
				}
			}
		}
	}

	private void render(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
		event.renderer.box(
			box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, lines, shapeMode, 0);
	}

	private final List<Block> specific1_19Blocks = Arrays.asList(
			Blocks.NETHER_GOLD_ORE,
			Blocks.COPPER_ORE
	);

	@Override
	public void onActivate() {
		if (find1_19.get())
			for (Chunk chunk : Utils.chunks()) {
				searchChunk(chunk, null);
			}
	}

	@EventHandler
	private void onChunkData(ChunkDataEvent event) {
		if (find1_19.get())
			searchChunk(event.chunk, event);
	}

	private void searchChunk(Chunk ck, ChunkDataEvent cde) {
		MeteorExecutor.execute(() -> {
			if (!isActive()) return;
			ESPChunk schunk = ESPChunk.searchChunk(ck, specific1_19Blocks);
			if (schunk.size() > 0) {
				od19chunks.add(ck.getPos());
			}
			if (cde != null) ChunkDataEvent.returnChunkDataEvent(cde);
		});
	}

	@EventHandler
	private void onReadPacket(PacketEvent.Receive event) {
		if (event.packet instanceof ChunkDeltaUpdateS2CPacket) {
			ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket) event.packet;

			packet.visitUpdates((pos, state) -> {
				if (!state.getFluidState().isEmpty() && !state.getFluidState().isStill()) {
					ChunkPos chunkPos = new ChunkPos(pos);

					for (Direction dir: searchDirs) {
						if (mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill() && !oldChunks.contains(chunkPos)) {
							newChunks.add(chunkPos);
							return;
						}
					}
				}
			});
		}

		else if (event.packet instanceof BlockUpdateS2CPacket) {
			BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet;

			if (!packet.getState().getFluidState().isEmpty() && !packet.getState().getFluidState().isStill()) {
				ChunkPos chunkPos = new ChunkPos(packet.getPos());

				for (Direction dir: searchDirs) {
					if (mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill() && !oldChunks.contains(chunkPos)) {
						newChunks.add(chunkPos);
						return;
					}
				}
			}
		}

		else if (event.packet instanceof ChunkDataS2CPacket && mc.world != null) {
			ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet;

			ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());

			if (!newChunks.contains(pos) && mc.world.getChunkManager().getChunk(packet.getX(), packet.getZ()) == null) {
				WorldChunk chunk = new WorldChunk(mc.world, pos);
				try {
					chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), new NbtCompound(), packet.getChunkData().getBlockEntities(packet.getX(), packet.getZ()));
				} catch (ArrayIndexOutOfBoundsException e) {
					return;
				}

				for (int x = 0; x < 16; x++) {
					for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
						for (int z = 0; z < 16; z++) {
							FluidState fluid = chunk.getFluidState(x, y, z);

							if (!fluid.isEmpty() && !fluid.isStill()) {
								oldChunks.add(pos);
								return;
							}
						}
					}
				}
			}
		}
	}
}
