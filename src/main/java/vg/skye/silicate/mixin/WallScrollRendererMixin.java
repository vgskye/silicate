package vg.skye.silicate.mixin;

import at.petrak.hexcasting.client.entity.WallScrollRenderer;
import at.petrak.hexcasting.common.entities.EntityWallScroll;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vg.skye.silicate.pattern_renderer.IMixinExtendedPoints;
import vg.skye.silicate.pattern_renderer.PatternTextureManager;

@Mixin(WallScrollRenderer.class)
public abstract class WallScrollRendererMixin extends EntityRenderer<EntityWallScroll> {
	protected WallScrollRendererMixin(EntityRendererFactory.Context ctx) {
		super(ctx);
		throw new RuntimeException("this should never be called ever");
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 0, shift = At.Shift.AFTER), method = "render(Lat/petrak/hexcasting/common/entities/EntityWallScroll;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", cancellable = true)
	private void renderMixin(EntityWallScroll wallScroll, float yaw, float partialTicks, MatrixStack ps, VertexConsumerProvider bufSource, int packedLight, CallbackInfo ci) {
		int light = WorldRenderer.getLightmapCoordinates(wallScroll.world, wallScroll.getDecorationBlockPos());
		if (((IMixinExtendedPoints)wallScroll).getPoints() != null) {
			PatternTextureManager.INSTANCE.renderPatternForScroll(((IMixinExtendedPoints) wallScroll).getPoints().getPointsKey(), ps, bufSource, light, ((IMixinExtendedPoints) wallScroll).getPoints().getZappyPoints(), wallScroll.blockSize, wallScroll.getShowsStrokeOrder());
		}
		ps.pop();
		super.render(wallScroll, yaw, partialTicks, ps, bufSource, packedLight);
		ci.cancel();
	}
}
