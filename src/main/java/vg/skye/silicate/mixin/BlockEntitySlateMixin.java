package vg.skye.silicate.mixin;

import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import vg.skye.silicate.pattern_renderer.HexPatternPoints;
import vg.skye.silicate.pattern_renderer.IMixinExtendedPoints;

@Mixin(BlockEntitySlate.class)
public class BlockEntitySlateMixin implements IMixinExtendedPoints {
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
