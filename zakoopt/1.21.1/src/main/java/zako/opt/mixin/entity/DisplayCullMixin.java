package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// 1.21.1: every display sets Entity.noCulling and the renderer's frustum test reads it, so the test is where displays get culled
@Mixin(EntityRenderer.class)
public abstract class DisplayCullMixin {
	@ModifyExpressionValue(method = "shouldRender", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;noCulling:Z"))
	private boolean zakoopt$cull(boolean noCulling, Entity entity) {
		return noCulling && !(entity instanceof Display && Features.DISPLAY_CULL.on);
	}

	// ponytail: a fixed 4-block box, text scaled past that can vanish at the screen edge; size it from the text if that shows up
	@ModifyExpressionValue(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBoundingBoxForCulling()Lnet/minecraft/world/phys/AABB;"))
	private AABB zakoopt$box(AABB box, Entity entity) {
		if (entity instanceof Display && entity.noCulling && Features.DISPLAY_CULL.on) {
			return new AABB(entity.getX() - 4, entity.getY() - 4, entity.getZ() - 4, entity.getX() + 4, entity.getY() + 4, entity.getZ() + 4);
		}
		return box;
	}
}
