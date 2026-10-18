package zako.opt.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import zako.opt.feature.Features;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

// a factory gauge's arrow path is dozens of 4-vertex sprites, each with its own model lookup, three rotations and vertex loop;
// it is drawn once in the gauge's own space and replayed through the gauge's pose while everything renderPath reads stays the same
@UtilityClass
public class FactoryPaths {
	private final Map<FactoryPanelConnection, Entry> CACHE = new WeakHashMap<>();
	// Create's classes no longer match ours: the original draws from then on
	private boolean off;
	public boolean recording;

	private static final class Entry {
		Level level;
		BlockState state;
		Object slot;
		Direction[] path = new Direction[0];
		int flags, color, light, overlay;
		float glow;
		List<SpawnerMeshCache.Segment> mesh;

		boolean same(Level level, BlockState state, Object slot, List<Direction> path, int flags, int color, float glow, int light, int overlay) {
			if (mesh == null || level != this.level || state != this.state || slot != this.slot || flags != this.flags || color != this.color
					|| Float.floatToIntBits(glow) != Float.floatToIntBits(this.glow) || light != this.light || overlay != this.overlay || path.size() != this.path.length) {
				return false;
			}
			for (int i = 0; i < this.path.length; i++) {
				if (path.get(i) != this.path[i]) {
					return false;
				}
			}
			return true;
		}
	}

	// false: draw it the original way
	public boolean render(FactoryPanelBehaviour behaviour, FactoryPanelConnection connection, float partialTicks, PoseStack ms, MultiBufferSource buffer,
						  int light, int overlay, Consumer<MultiBufferSource> draw) {
		// the sprites' diffuse shade is baked from their own normals: only a pose that does not rotate them may replay it
		Matrix3f n = ms.last().normal();
		if (off || n.m00() != 1.0F || n.m11() != 1.0F || n.m22() != 1.0F) {
			return false;
		}
		try {
			Level level = behaviour.getWorld();
			BlockState state = behaviour.blockEntity.getBlockState();
			List<Direction> path = connection.getPath(level, state, behaviour.getPanelPosition());
			FactoryPanelSupportBehaviour link = FactoryPanelBehaviour.linkAt(level, connection);
			int flags = link == null ? 0 : link.blockEntity instanceof DisplayLinkBlockEntity ? 1 : link.blockEntity instanceof RedstoneLinkBlockEntity ? 2 : 3;
			flags |= (link != null && link.isOutput() ? 4 : 0) | (behaviour.count == 0 ? 8 : 0) | (behaviour.satisfied ? 16 : 0) | (behaviour.promisedSatisfied ? 32 : 0)
					| (behaviour.redstonePowered ? 64 : 0) | (behaviour.waitingForNetwork ? 128 : 0) | (behaviour.isMissingAddress() ? 256 : 0)
					| (connection.success ? 512 : 0) | (Features.shaders() ? 1024 : 0);
			int color = behaviour.getIngredientStatusColor();
			float glow = behaviour.bulb.getValue(partialTicks);
			Entry e = CACHE.computeIfAbsent(connection, c -> new Entry());
			if (!e.same(level, state, behaviour.slot, path, flags, color, glow, light, overlay)) {
				e.level = level;
				e.state = state;
				e.slot = behaviour.slot;
				e.path = path.toArray(new Direction[0]);
				e.flags = flags;
				e.color = color;
				e.glow = glow;
				e.light = light;
				e.overlay = overlay;
				recording = true;
				try {
					e.mesh = SpawnerMeshCache.record(draw);
				} finally {
					recording = false;
				}
			}
			return SpawnerMeshCache.emit(e.mesh, ms.last(), buffer, light);
		} catch (RuntimeException | LinkageError t) {
			off = true;
			throw t;
		}
	}
}
