package vg.skye.silicate.mixin;

import at.petrak.hexcasting.common.entities.EntityWallScroll;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vg.skye.silicate.pattern_renderer.HexPatternPoints;
import vg.skye.silicate.pattern_renderer.IMixinExtendedPoints;

import java.util.List;

@Mixin(EntityWallScroll.class)
public class EntityWallScrollMixin implements IMixinExtendedPoints {
	@Shadow(remap = false)
	public List<Vec2f> zappyPoints;

	@Inject(at = @At("TAIL"), method = "recalculateDisplay", remap = false)
	private void recalculateDisplayMixin(CallbackInfo ci) {
		if (zappyPoints == null) {
			points = null;
		} else {
			points = new HexPatternPoints(zappyPoints);
		}
	}

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
