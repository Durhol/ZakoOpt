package zako.opt.block;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

// Create's value boxes (factory gauges, funnels, deployers) draw their filter item through the whole item pipeline every frame;
// it is drawn once in the box's own space and replayed through the box's pose while the stack, its model and the light stay the same
@UtilityClass
public class ValueBoxItems {
	private final Map<ItemStack, Entry> CACHE = new WeakHashMap<>();
	// Create's classes no longer match ours: the original draws from then on
	private boolean off;
	public boolean recording;

	private static final class Entry {
		Item item;
		BakedModel model;
		int light, overlay;
		List<SpawnerMeshCache.Segment> mesh;
	}

	// false: draw it the original way
	public boolean render(ItemStack stack, PoseStack ms, MultiBufferSource buffer, int light, int overlay, Consumer<MultiBufferSource> draw) {
		if (off || stack.isEmpty()) {
			return false;
		}
		try {
			BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
			// a glint goes to two buffers at once, which the recorder keeps apart; custom renderers may animate
			if (model.isCustomRenderer() || stack.hasFoil()) {
				return false;
			}
			Entry e = CACHE.computeIfAbsent(stack, s -> new Entry());
			if (e.mesh == null || e.item != stack.getItem() || e.model != model || e.light != light || e.overlay != overlay) {
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
