package vg.skye.silicate.mixin;

import at.petrak.hexcasting.api.spell.math.HexPattern;
import at.petrak.hexcasting.client.be.BlockEntityAkashicBookshelfRenderer;
import at.petrak.hexcasting.common.blocks.akashic.BlockEntityAkashicBookshelf;
import vg.skye.silicate.pattern_renderer.PatternTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockEntityAkashicBookshelfRenderer.class)
public class BlockEntityAkashicBookshelfRendererMixin {
	/**
	 * @author Skye
	 * @reason why not
	 */
	@Overwrite
	public void render(BlockEntityAkashicBookshelf tile, float pPartialTick, MatrixStack ps,
					   VertexConsumerProvider buffer, int light, int overlay) {
		HexPattern pattern = tile.getPattern();
		if (pattern == null) {
			return;
		}

		var bs = tile.getCachedState();
		PatternTextureManager.INSTANCE.renderPatternForAkashicBookshelf(tile, pattern, ps, buffer, light, bs);
	}
}
