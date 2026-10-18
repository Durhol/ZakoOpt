package zako.opt.block;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

// Create draws items its renderers hold (value box filters, items riding belts) through the whole item pipeline every frame;
// each is drawn once in its own space and replayed through its pose while the stack, its model, the light and the caller stay the same
@UtilityClass
public class ItemMeshes {
	private final Map<ItemStack, Entry> CACHE = new WeakHashMap<>();
	// Create's classes no longer match ours: the original draws from then on
	private boolean off;
	public boolean recording;

	private static final class Entry {
		Object kind;
		Item item;
		BakedModel model;
		int light, overlay;
		List<SpawnerMeshCache.Segment> mesh;
	}

	// kind tells the callers apart, as each records a different part of the drawing; false: draw it the original way
	public boolean render(Object kind, ItemStack stack, BakedModel model, PoseStack ms, MultiBufferSource buffer, int light, int overlay, Consumer<MultiBufferSource> draw) {
		// a glint goes to two buffers at once, which the recorder keeps apart; custom renderers may animate
		if (off || stack.isEmpty() || model.isCustomRenderer() || stack.hasFoil()) {
			return false;
		}
		try {
			Entry e = CACHE.computeIfAbsent(stack, s -> new Entry());
			if (e.mesh == null || e.kind != kind || e.item != stack.getItem() || e.model != model || e.light != light || e.overlay != overlay) {
				e.kind = kind;
				e.item = stack.getItem();
				e.model = model;
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
