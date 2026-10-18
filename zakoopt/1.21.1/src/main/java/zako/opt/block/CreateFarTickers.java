package zako.opt.block;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.phys.Vec3;
import zako.opt.feature.Features;
import zako.opt.mixin.block.CreateBoundTickerAccessor;
import zako.opt.mixin.block.CreateRebindableTickerAccessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

// the client level walks every ticking block entity each tick, even those whose tick far-machine rest skips: in a factory district
// that walk was the longest part of a tick frame. Far Create tickers leave the level's list for a parked one and come back on approach
@UtilityClass
public class CreateFarTickers {
	private final Map<Level, List<TickingBlockEntity>> PARKED = new WeakHashMap<>();
	private int clock;

	public void sweep(Level level, List<TickingBlockEntity> tickers) {
		List<TickingBlockEntity> parked = PARKED.computeIfAbsent(level, l -> new ArrayList<>());
		if (!Features.CREATE_FAR_TICK.on) {
			if (!parked.isEmpty()) {
				tickers.addAll(parked);
				parked.clear();
			}
			return;
		}
		// ponytail: a sweep each quarter second; flying in, a machine wakes at most 5 ticks late, inside the margin past 64 blocks
		if (++clock % 5 != 0) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
		double far = Features.CREATE_FAR_TICK.distance;
		double view = mc.options.getEffectiveRenderDistance() * 16 + 32;
		for (Iterator<TickingBlockEntity> it = parked.iterator(); it.hasNext(); ) {
			TickingBlockEntity t = it.next();
			if (t.isRemoved()) {
				it.remove();
			} else if (!far(t.getPos(), cam, far, view)) {
				tickers.add(t);
				it.remove();
			}
		}
		tickers.removeIf(t -> !t.isRemoved() && create(t) && far(t.getPos(), cam, far, view) && parked.add(t));
	}

	// Sable keeps sub-level blocks in plots far past the view distance: those stay in the list
	private boolean far(BlockPos pos, Vec3 cam, double far, double view) {
		double dx = pos.getX() + 0.5 - cam.x, dy = pos.getY() + 0.5 - cam.y, dz = pos.getZ() + 0.5 - cam.z;
		return dx * dx + dy * dy + dz * dz > far * far && dx * dx + dz * dz < view * view;
	}

	private boolean create(TickingBlockEntity t) {
		if (t instanceof CreateRebindableTickerAccessor wrapper) {
			t = wrapper.zakoopt$ticker();
		}
		return t instanceof CreateBoundTickerAccessor bound && bound.zakoopt$ticker() instanceof SmartBlockEntityTicker;
	}
}
