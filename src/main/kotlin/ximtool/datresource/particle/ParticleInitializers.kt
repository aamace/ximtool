package ximtool.datresource.particle

import ximtool.dat.ByteColor
import ximtool.dat.ByteReader
import ximtool.dat.DatId
import ximtool.math.Vector3f

enum class BillBoardType {
    None,
    XYZ,
    XZ,
    Camera,
    Movement,
    MovementHorizontal,
}

enum class LinkedDataType(val value: Int) {
    Actor(0x01),
    StaticMesh(0x0B),
    SpriteSheet(0x0E),
    WeightedMesh(0x1D),
    Distortion(0x22),
    RingMesh(0x24),
    LensFlare(0x39),
    Audio(0x3D),
    PointLight(0x47),
    Null(0x57),
    Unknown(-1),
    ;
}

enum class RotationOrder {
    XYZ,
    ZYX,
}

class InitializerContext(
    val allocationPoolSize: Int,
)

class KeyFrameConfig(
    val interpolationFn: Int = 0,
    val cycleCount: Int = 1,
)

sealed interface ParticleInitializer {
    val opCode: Int
    fun sizeInBytes(): Int
    fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext)
    fun allocationSizeInBytes(): Int = 0
}

object ParticleInitializers {

    class StandardParticleSetup(
        val positionOrientationFlags: Int,
        val renderStateFlags: Int,
        val linkedDatId: DatId,
        val basePosition: Vector3f,
        val linkedDataType: LinkedDataType,
        val lifeSpan: Int,
        val lifeSpanVariance: Int,
    ): ParticleInitializer {

        override val opCode = 0x01

        override fun sizeInBytes() = 48

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write16(positionOrientationFlags)
            byteReader.write16(renderStateFlags)
            byteReader.write32(0)
            byteReader.write(linkedDatId)

            byteReader.write32(0)
            byteReader.write(basePosition)

            byteReader.write8(context.allocationPoolSize)
            byteReader.write8(linkedDataType.value)
            byteReader.write16(lifeSpan)
            byteReader.write16(lifeSpanVariance)
            byteReader.align0x10()
        }

    }

    class RotationInitializer(val rotation: Vector3f): ParticleInitializer {

        override val opCode = 0x09

        override fun sizeInBytes() = 16

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this)
            byteReader.write(rotation)
        }

    }

    class ScaleInitializer(val scale: Vector3f): ParticleInitializer {

        override val opCode = 0x0F

        override fun sizeInBytes() = 16

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this)
            byteReader.write(scale)
        }
    }

    class ColorInitializer(val color: ByteColor): ParticleInitializer {

        override val opCode = 0x16

        override fun sizeInBytes() = 8

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this)
            byteReader.writeRgba(color)
        }

    }

    class ColorRedKeyFrameSetup(refId: DatId, config: KeyFrameConfig = KeyFrameConfig()): KeyFrameValueSetup(refId, config) {
        override val opCode = 0x2A
    }

    class ColorGreenKeyFrameSetup(refId: DatId, config: KeyFrameConfig = KeyFrameConfig()): KeyFrameValueSetup(refId, config) {
        override val opCode = 0x2B
    }

    class ColorBlueKeyFrameSetup(refId: DatId, config: KeyFrameConfig = KeyFrameConfig()): KeyFrameValueSetup(refId, config) {
        override val opCode = 0x2C
    }

    class ColorAlphaKeyFrameSetup(refId: DatId, config: KeyFrameConfig = KeyFrameConfig()): KeyFrameValueSetup(refId, config) {
        override val opCode = 0x2D
    }

    class ProjectionBiasInitializer(
        val param0: Float,
        val param1: Float,
    ): ParticleInitializer {

        override val opCode: Int = 0x72

        override fun sizeInBytes() = 12

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this)
            byteReader.writeFloat(param0)
            byteReader.writeFloat(param1)
        }

    }

    object EndMarker: ParticleInitializer {
        override val opCode = 0x00

        override fun sizeInBytes(): Int {
            return 4
        }

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
        }
    }

    abstract class KeyFrameValueSetup(
        val refId: DatId,
        val config: KeyFrameConfig,
    ): ParticleInitializer {
        override fun sizeInBytes() = 16

        override fun allocationSizeInBytes() = 12

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write32(0)
            byteReader.write(refId)
            val adjustedCycleCount = (config.cycleCount - 1).coerceAtLeast(0)
            byteReader.write32(config.interpolationFn + (adjustedCycleCount shl 4))
        }
    }

}

private fun ByteReader.write(initializer: ParticleInitializer, allocationOffset: Int = 0) {
    val sizeInDWord = initializer.sizeInBytes()/4
    val value = initializer.opCode + (sizeInDWord shl 8) + (allocationOffset shl 13)
    write32(value)
}