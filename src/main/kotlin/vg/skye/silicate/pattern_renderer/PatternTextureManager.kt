package vg.skye.silicate.pattern_renderer

import at.petrak.hexcasting.api.block.HexBlockEntity
import at.petrak.hexcasting.api.spell.math.HexPattern
import at.petrak.hexcasting.client.findDupIndices
import at.petrak.hexcasting.common.blocks.akashic.BlockAkashicBookshelf
import at.petrak.hexcasting.common.blocks.akashic.BlockEntityAkashicBookshelf
import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate
import at.petrak.hexcasting.common.blocks.circles.BlockSlate
import at.petrak.hexcasting.client.makeZappy
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.render.VertexConsumer
import net.minecraft.util.math.Vec3f
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import net.minecraft.util.Pair
import net.minecraft.util.math.Matrix3f
import net.minecraft.util.math.Matrix4f
import net.minecraft.block.enums.WallMountLocation
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.math.Vec2f
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.image.BufferedImage
import java.util.stream.Collectors
import kotlin.math.cos
import kotlin.math.sin

object PatternTextureManager {
    private var repaintIndex = 0
    private var resolutionByBlockSize = 512
    private var paddingByBlockSize = 50
    private var circleRadiusByBlockSize = 8
    private var scaleLimit = 16
    private val patternTextures: HashMap<String, Identifier> = HashMap()
    fun getPointsKey(zappyPoints: List<Vec2f>): String {
        return zappyPoints.stream()
            .map { p: Vec2f ->
                java.lang.String.format(
                    "(%f,%f)",
                    p.x,
                    p.y
                )
            }
            .collect(Collectors.joining(";"))
    }

    private fun generateHexPatternPoints(tile: HexBlockEntity, pattern: HexPattern, flowIrregular: Float): HexPatternPoints {
        val stupidHash = tile.pos.hashCode()
        val lines1 = pattern.toLines(1f, Vec2f.ZERO)
        val zappyPoints: List<Vec2f> = makeZappy(
            lines1, findDupIndices(pattern.positions()),
            10, 0.5f, 0f, flowIrregular, 0f, 1f, stupidHash.toDouble()
        )
        return HexPatternPoints(zappyPoints)
    }

    fun renderPatternForScroll(
        pointsKey: String,
        ps: MatrixStack,
        bufSource: VertexConsumerProvider,
        light: Int,
        zappyPoints: List<Vec2f>,
        blockSize: Int,
        showStrokeOrder: Boolean
    ) {
        renderPattern(
            pointsKey,
            ps,
            bufSource,
            light,
            zappyPoints,
            blockSize,
            showStrokeOrder,
            useFullSize = false,
            isOnWall = true,
            isOnCeiling = false,
            isSlate = false,
            isScroll = true,
            facing = -1
        )
    }

    fun renderPatternForSlate(
        tile: BlockEntitySlate,
        pattern: HexPattern,
        ps: MatrixStack,
        buffer: VertexConsumerProvider,
        light: Int,
        bs: BlockState
    ) {
        if ((tile as IMixinExtendedPoints).points == null) (tile as IMixinExtendedPoints).points = generateHexPatternPoints(tile, pattern, 0.2f)
        val isOnWall = bs.get(BlockSlate.ATTACH_FACE) === WallMountLocation.WALL
        val isOnCeiling = bs.get(BlockSlate.ATTACH_FACE) === WallMountLocation.CEILING
        val facing: Int = bs.get(BlockSlate.FACING).horizontal
        renderPatternForBlockEntity((tile as IMixinExtendedPoints).points!!, ps, buffer, light, isOnWall, isOnCeiling, true, facing)
    }

    fun renderPatternForAkashicBookshelf(
        tile: BlockEntityAkashicBookshelf,
        pattern: HexPattern,
        ps: MatrixStack,
        buffer: VertexConsumerProvider,
        light: Int,
        bs: BlockState
    ) {
        if ((tile as IMixinExtendedPoints).points == null) (tile as IMixinExtendedPoints).points = generateHexPatternPoints(tile, pattern, 0f)
        val facing: Int = bs.get(BlockAkashicBookshelf.FACING).horizontal
        renderPatternForBlockEntity((tile as IMixinExtendedPoints).points!!, ps, buffer, light,
            isOnWall = true,
            isOnCeiling = false,
            isSlate = false,
            facing = facing
        )
    }

    private fun renderPatternForBlockEntity(
        points: HexPatternPoints,
        ps: MatrixStack,
        buffer: VertexConsumerProvider,
        light: Int,
        isOnWall: Boolean,
        isOnCeiling: Boolean,
        isSlate: Boolean,
        facing: Int
    ) {
        val oldShader = RenderSystem.getShader()
        ps.push()
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        renderPattern(
            points.pointsKey,
            ps,
            buffer,
            light,
            points.zappyPoints,
            blockSize = 1,
            showStrokeOrder = false,
            useFullSize = true,
            isOnWall,
            isOnCeiling,
            isSlate,
            isScroll = false,
            facing
        )
        ps.pop()
        RenderSystem.setShader { oldShader }
    }

    private fun renderPattern(
        pointsKey: String,
        ps: MatrixStack,
        bufSource: VertexConsumerProvider,
        light: Int,
        zappyPoints: List<Vec2f>,
        blockSize: Int,
        showStrokeOrder: Boolean,
        useFullSize: Boolean,
        isOnWall: Boolean,
        isOnCeiling: Boolean,
        isSlate: Boolean,
        isScroll: Boolean,
        facing: Int
    ) {
        ps.push()
        val last = ps.peek()
        val mat: Matrix4f = last.positionMatrix
        val normal: Matrix3f = last.normalMatrix
        val x = blockSize.toFloat()
        val y = blockSize.toFloat()
        var z = -1f / 16f - 0.01f
        var nx = 0f
        val ny = 0f
        var nz = 0f

        //TODO: refactor this mess of a method
        if (isOnWall) {
            if (isScroll) {
                ps.translate(-blockSize / 2.0, -blockSize / 2.0, 1 / 32.0)
                nz = -1f
            } else {
                ps.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180f))
                if (isSlate) {
                    if (facing == 0) ps.translate(0.0, -1.0, 0.0)
                    if (facing == 1) ps.translate(-1.0, -1.0, 0.0)
                    if (facing == 2) ps.translate(-1.0, -1.0, 1.0)
                    if (facing == 3) ps.translate(0.0, -1.0, 1.0)
                } else {
                    z = -0.01f
                    if (facing == 0) ps.translate(0.0, -1.0, 1.0)
                    if (facing == 1) ps.translate(0.0, -1.0, 0.0)
                    if (facing == 2) ps.translate(-1.0, -1.0, 0.0)
                    if (facing == 3) ps.translate(-1.0, -1.0, 1.0)
                }
                if (facing == 0) ps.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180f))
                if (facing == 1) ps.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(270f))
                if (facing == 3) ps.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90f))
                if (facing == 0 || facing == 2) nz = -1f
                if (facing == 1 || facing == 3) nx = -1f
                ps.translate(0.0, 0.0, 0.0)
            }
        } else  //slates on the floor or ceiling
        {
            if (facing == 0) ps.translate(0.0, 0.0, 0.0)
            if (facing == 2) ps.translate(1.0, 0.0, 1.0)
            if (facing == 3) ps.translate(0.0, 0.0, 1.0)
            if (facing == 1) ps.translate(1.0, 0.0, 0.0)
            ps.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(facing * -90f))
            if (isOnCeiling) {
                ps.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-90f))
                ps.translate(0.0, -1.0, 1.0)
            } else ps.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90f))
            nz = -1f
        }
        var lineWidth = 16
        var outerColor = 0xB4B4BE //0xff_c8c8d2;
        var innerColor = 0x2A2A2A //0xc8_322b33;
        if (isScroll) {
            lineWidth = 20
            outerColor = 0xDEDEDE //0xff_d2c8c8;
            innerColor = 0x343434 //0xc8_322b33;
        }
        val texture: Identifier? = getTexture(
            zappyPoints,
            pointsKey,
            blockSize,
            showStrokeOrder,
            lineWidth,
            useFullSize,
            Color(innerColor),
            Color(outerColor)
        )
        val verts: VertexConsumer = bufSource.getBuffer(RenderLayer.getEntityCutout(texture))
        vertex(mat, normal, light, verts, 0f, 0f, z, 0f, 0f, nx, ny, nz)
        vertex(mat, normal, light, verts, 0f, y, z, 0f, 1f, nx, ny, nz)
        vertex(mat, normal, light, verts, x, y, z, 1f, 1f, nx, ny, nz)
        vertex(mat, normal, light, verts, x, 0f, z, 1f, 0f, nx, ny, nz)
        ps.pop()
    }

    private fun vertex(
        mat: Matrix4f, normal: Matrix3f, light: Int, verts: VertexConsumer, x: Float, y: Float, z: Float,
        u: Float, v: Float, nx: Float, ny: Float, nz: Float
    ) {
        verts.vertex(mat, x, y, z)
            .color(-0x1)
            .texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light)
            .normal(normal, nx, ny, nz)
            .next()
    }

    private fun getTexture(
        points: List<Vec2f>,
        pointsKey: String,
        blockSize: Int,
        showsStrokeOrder: Boolean,
        lineWidth: Int,
        useFullSize: Boolean,
        innerColor: Color?,
        outerColor: Color?
    ): Identifier? {
        return if (patternTextures.containsKey(pointsKey)) patternTextures[pointsKey] else createTexture(
            points,
            pointsKey,
            blockSize,
            showsStrokeOrder,
            lineWidth,
            useFullSize,
            innerColor,
            outerColor
        )
    }

    private fun createTexture(
        points: List<Vec2f>,
        pointsKey: String,
        blockSize: Int,
        showsStrokeOrder: Boolean,
        lineWidth: Int,
        useFullSize: Boolean,
        innerColor: Color?,
        outerColor: Color?
    ): Identifier {
        val resolution = resolutionByBlockSize * blockSize
        val padding = paddingByBlockSize * blockSize
        var minX = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var minY = Double.MAX_VALUE
        var maxY = Double.MIN_VALUE
        for (point in points) {
            minX = minX.coerceAtMost(point.x.toDouble())
            maxX = maxX.coerceAtLeast(point.x.toDouble())
            minY = minY.coerceAtMost(point.y.toDouble())
            maxY = maxY.coerceAtLeast(point.y.toDouble())
        }
        val rangeX = maxX - minX
        val rangeY = maxY - minY
        var scale = ((resolution - 2 * padding) / rangeX).coerceAtMost((resolution - 2 * padding) / rangeY)
        val limit = (blockSize * scaleLimit).toDouble()
        if (!useFullSize && scale > limit) scale = limit
        val offsetX = (resolution - 2 * padding - rangeX * scale) / 2
        val offsetY = (resolution - 2 * padding - rangeY * scale) / 2
        val img = BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_ARGB)
        val g2d = img.createGraphics()
        g2d.color = outerColor
        g2d.stroke = BasicStroke(blockSize * 5f / 3f * lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        drawLines(g2d, points, minX, minY, scale, offsetX, offsetY, padding)
        g2d.color = innerColor
        g2d.stroke = BasicStroke(blockSize * 2f / 3f * lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        drawLines(g2d, points, minX, minY, scale, offsetX, offsetY, padding)
        if (showsStrokeOrder) {
            g2d.color = Color(-0x2884a5)
            val point: Pair<Int, Int> = getTextureCoordinates(points[0], minX, minY, scale, offsetX, offsetY, padding)
            val spotRadius = circleRadiusByBlockSize * blockSize
            drawHexagon(g2d, point.left, point.right, spotRadius)
        }
        g2d.dispose()
        val nativeImage = NativeImage(img.width, img.height, true)
        for (y in 0 until img.height) for (x in 0 until img.width) nativeImage.setColor(x, y, img.getRGB(x, y))
        val dynamicTexture = NativeImageBackedTexture(nativeImage)
        val resourceLocation: Identifier = MinecraftClient.getInstance().textureManager
            .registerDynamicTexture("hex_pattern_texture_" + points.hashCode() + "_" + repaintIndex + ".png", dynamicTexture)
        patternTextures[pointsKey] = resourceLocation
        return resourceLocation
    }

    private fun drawLines(
        g2d: Graphics2D,
        points: List<Vec2f>,
        minX: Double,
        minY: Double,
        scale: Double,
        offsetX: Double,
        offsetY: Double,
        padding: Int
    ) {
        for (i in 0 until points.size - 1) {
            val pointFrom: Pair<Int, Int> =
                getTextureCoordinates(points[i], minX, minY, scale, offsetX, offsetY, padding)
            val pointTo: Pair<Int, Int> =
                getTextureCoordinates(points[i + 1], minX, minY, scale, offsetX, offsetY, padding)
            g2d.drawLine(pointFrom.left, pointFrom.right, pointTo.left, pointTo.right)
        }
    }

    private fun getTextureCoordinates(
        point: Vec2f,
        minX: Double,
        minY: Double,
        scale: Double,
        offsetX: Double,
        offsetY: Double,
        padding: Int
    ): Pair<Int, Int> {
        val x = ((point.x - minX) * scale + offsetX).toInt() + padding
        val y = ((point.y - minY) * scale + offsetY).toInt() + padding
        return Pair(x, y)
    }

    private fun drawHexagon(g2d: Graphics2D, x: Int, y: Int, radius: Int) {
        val fracOfCircle = 6
        val hexagon = Polygon()
        for (i in 0 until fracOfCircle) {
            val theta = i / fracOfCircle.toDouble() * Math.PI * 2
            val hx = (x + cos(theta) * radius).toInt()
            val hy = (y + sin(theta) * radius).toInt()
            hexagon.addPoint(hx, hy)
        }
        g2d.fill(hexagon)
    }

    fun repaint() {
        repaintIndex++
        patternTextures.clear()
    }
}
