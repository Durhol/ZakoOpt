package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.particle.ParallelQuadParticles;

import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;

// 1.21.1 renders every particle type inside render(): the queue goes out in parallel right after begin, and the vanilla loop
// over it that follows then gets an empty iterator
@Mixin(ParticleEngine.class)
public class ParticleEngineParallelMixin {
	@Unique
	private boolean zakoopt$written;

	@WrapOperation(method = {"render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"}, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/particle/ParticleRenderType;begin(Lcom/mojang/blaze3d/vertex/Tesselator;Lnet/minecraft/client/renderer/texture/TextureManager;)Lcom/mojang/blaze3d/vertex/BufferBuilder;"))
	private BufferBuilder zakoopt$parallel(ParticleRenderType type, Tesselator tesselator, TextureManager textures, Operation<BufferBuilder> original,
										   LightTexture light, Camera camera, float partialTick, @Local Queue<Particle> particles) {
		BufferBuilder builder = original.call(type, tesselator, textures);
		// only the vanilla sheets: a mod's own type may write another vertex format
		zakoopt$written = builder != null && (type == ParticleRenderType.PARTICLE_SHEET_OPAQUE || type == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
				|| type == ParticleRenderType.PARTICLE_SHEET_LIT) && ParallelQuadParticles.render(particles, builder, DefaultVertexFormat.PARTICLE, camera, partialTick);
		return builder;
	}

	@WrapOperation(method = {"render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"}, at = @At(value = "INVOKE", target = "Ljava/util/Queue;iterator()Ljava/util/Iterator;"))
	private Iterator<Particle> zakoopt$skipWritten(Queue<Particle> particles, Operation<Iterator<Particle>> original) {
		if (zakoopt$written) {
			zakoopt$written = false;
			return Collections.emptyIterator();
		}
		return original.call(particles);
	}
}
