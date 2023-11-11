package vg.skye.silicate.pattern_renderer

import net.minecraft.util.math.Vec2f
import vg.skye.silicate.pattern_renderer.PatternTextureManager.getPointsKey

class HexPatternPoints(var zappyPoints: List<Vec2f>) {
    var pointsKey = getPointsKey(zappyPoints)
}
