package vg.skye.silicate.mixin;

import at.petrak.hexcasting.common.blocks.akashic.BlockEntityAkashicBookshelf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import vg.skye.silicate.pattern_renderer.HexPatternPoints;
import vg.skye.silicate.pattern_renderer.IMixinExtendedPoints;

@Mixin(BlockEntityAkashicBookshelf.class)
public class BlockEntityAkashicBookshelfMixin implements IMixinExtendedPoints {
	@Unique
	private HexPatternPoints points;

	@Unique
	@Override
	public HexPatternPoints getPoints() {
		return points;
	}

	@Unique
	@Override
	public void setPoints(HexPatternPoints hexPatternPoints) {
		points = hexPatternPoints;
	}
}
