package zako.opt.mixin.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.ValueBoxItems;
import zako.opt.feature.Features;

@Pseudo
@Mixin(value = ValueBoxRenderer.class, remap = false)
public class CreateValueBoxMixin {
	@Inject(require = 0, method = "renderItemIntoValueBox", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$cachedItem(ItemStack filter, PoseStack ms, MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
		if (Features.MICRO_OPTS.createItems && !ValueBoxItems.recording && ValueBoxItems.render(filter, ms, buffer, light, overlay,
				recorder -> ValueBoxRenderer.renderItemIntoValueBox(filter, new PoseStack(), recorder, light, overlay))) {
			ci.cancel();
		}
	}
}
