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

    companion object {
        fun from(value: Int): LinkedDataType {
            return LinkedDataType.values().firstOrNull { it.value == value } ?: throw IllegalStateException("Unknown linked data type 0x${value.toString(0x10)}")
        }
    }
}

enum class RotationOrder {
    XYZ,
    ZYX,
}

class InitializerContext(
    val allocationPoolSize: Int,
)

data class KeyFrameConfig(
    val refId: DatId,
    val interpolationFn: Int = 0,
    val cycleCount: Int = 1,
)

sealed interface ParticleInitializer {
    val opCode: Int
    fun sizeInBytes(): Int
    fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext)
    fun allocationType(): DynamicParticleData? = null
}

object ParticleInitializers {

    interface NoData: ParticleInitializer {
        override fun sizeInBytes(): Int = 4

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
        }
    }

    data class StandardParticleSetup(
        val positionOrientationFlags: Int,
        val renderStateFlags: Int,
        val linkedDatId: DatId,
        val basePosition: Vector3f,
        val linkedDataType: LinkedDataType,
        val lifeSpan: Int,
        val lifeSpanVariance: Int,
    ): ParticleInitializer {

        companion object {
            fun read(byteReader: ByteReader): StandardParticleSetup {
                val positionOrientationFlags = byteReader.next16()
                val renderStateFlags = byteReader.next16()
                byteReader.position += 0x04
                val linkedDatId = byteReader.nextDatId()

                byteReader.position += 0x04
                val basePosition = byteReader.nextVector3f()

                byteReader.next8()
                val linkedDataType = LinkedDataType.from(byteReader.next8())
                val lifeSpan = byteReader.next16()
                val lifeSpanVariance = byteReader.next16()

                return StandardParticleSetup(
                    positionOrientationFlags = positionOrientationFlags,
                    renderStateFlags = renderStateFlags,
                    linkedDatId = linkedDatId,
                    basePosition = basePosition,
                    linkedDataType = linkedDataType,
                    lifeSpan = lifeSpan,
                    lifeSpanVariance = lifeSpanVariance,
                )
            }
        }

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

    data class VelocityInitializer(val velocity: Vector3f): ParticleInitializer {

        override val opCode = 0x02

        override fun sizeInBytes() = 16

        override fun allocationType() = DynamicParticleDataTypes.PositionData

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write(velocity)
        }

    }

    data class PositionVarianceMedium(
        var radiusVariance: Float,
        var radius: Float,
        var radiusScale: Vector3f,
        var yAxisRotation: Float = 0f,
        var unknown: Float = 0f,
    ): ParticleInitializer {
        companion object {
            fun read(byteReader: ByteReader): PositionVarianceMedium {
                return PositionVarianceMedium(
                    radiusVariance = byteReader.nextFloat(),
                    radius = byteReader.nextFloat(),
                    radiusScale = byteReader.nextVector3f(),
                    unknown = byteReader.nextFloat(),
                    yAxisRotation = byteReader.nextFloat()
                )
            }
        }

        override val opCode = 0x07

        override fun sizeInBytes() = 32

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.writeFloat(radiusVariance)
            byteReader.writeFloat(radius)
            byteReader.write(radiusScale)
            byteReader.writeFloat(unknown)
            byteReader.writeFloat(yAxisRotation)
        }
    }

    data class RelativeVelocitySetup(val magnitude: Float): ParticleInitializer {

        override val opCode = 0x08

        override fun sizeInBytes() = 8

        override fun allocationType() = DynamicParticleDataTypes.PositionData

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.writeFloat(magnitude)
        }

    }

    data class RotationInitializer(val rotation: Vector3f): ParticleInitializer {

        override val opCode = 0x09

        override fun sizeInBytes() = 16

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write(rotation)
        }

    }

    data class RotationVarianceInitializer(val rotation: Vector3f): ParticleInitializer {

        override val opCode = 0x0A

        override fun sizeInBytes() = 16

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write(rotation)
        }

    }

    data class RotationVelocitySetup(val velocity: Vector3f): ParticleInitializer {

        override val opCode = 0x0B

        override fun sizeInBytes() = 16

        override fun allocationType() = DynamicParticleDataTypes.RotationData

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write(velocity)
        }
    }

    data class RotationVelocityVarianceSetup(val velocity: Vector3f): ParticleInitializer {

        override val opCode = 0x0C

        override fun sizeInBytes() = 16

        override fun allocationType() = DynamicParticleDataTypes.RotationData

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write(velocity)
        }
    }


    data class ScaleInitializer(val scale: Vector3f): ParticleInitializer {

        override val opCode = 0x0F

        override fun sizeInBytes() = 16

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write(scale)
        }
    }

    data class UniformScaleVariance(val variance: Float): ParticleInitializer {

        override val opCode = 0x11

        override fun sizeInBytes() = 8

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.writeFloat(variance)
        }
    }

    data class ScaleVelocitySetup(val velocity: Vector3f): ParticleInitializer {

        override val opCode = 0x12

        override fun sizeInBytes() = 16

        override fun allocationType() = DynamicParticleDataTypes.ScaleData

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write(velocity)
        }
    }

    data class ColorInitializer(val color: ByteColor): ParticleInitializer {

        override val opCode = 0x16

        override fun sizeInBytes() = 8

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.writeRgba(color)
        }

    }

    data class ColorTransformSetup(val r: Int, val g: Int, val b: Int, val a: Int) : ParticleInitializer {
        override val opCode = 0x19

        override fun sizeInBytes(): Int = 12

        override fun allocationType() = DynamicParticleDataTypes.ColorTransform

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write16(r)
            byteReader.write16(g)
            byteReader.write16(b)
            byteReader.write16(a)
        }
    }

    data class SpriteSheetSetup(val unknown: Int): ParticleInitializer {
        override val opCode = 0x1D

        override fun sizeInBytes() = 8

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write32(unknown)
        }
    }

    data class ColorRedKeyFrameSetup(override val config: KeyFrameConfig): KeyFrameValueSetup {
        override val opCode = 0x2A
    }

    data class ColorGreenKeyFrameSetup(override val config: KeyFrameConfig): KeyFrameValueSetup {
        override val opCode = 0x2B
    }

    data class ColorBlueKeyFrameSetup(override val config: KeyFrameConfig): KeyFrameValueSetup {
        override val opCode = 0x2C
    }

    data class ColorAlphaKeyFrameSetup(override val config: KeyFrameConfig): KeyFrameValueSetup {
        override val opCode = 0x2D
    }

    data class HazeSetup(val unknown: Float, val horizontalStep: Float): ParticleInitializer {

        override val opCode = 0x32

        override fun sizeInBytes() = 12

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.writeFloat(unknown)
            byteReader.writeFloat(horizontalStep)
        }

    }

    data class IncrementalRotationInitializer(val rotationStep: Vector3f): ParticleInitializer {

        override val opCode = 0x3B

        override fun sizeInBytes() = 16

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write(rotationStep)
        }

    }

    data class RelativeVelocityVarianceSetup(val magnitude: Float): ParticleInitializer {

        override val opCode = 0x41

        override fun sizeInBytes() = 8

        override fun allocationType() = DynamicParticleDataTypes.PositionData

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.writeFloat(magnitude)
        }

    }

    object ParentColorConfig: NoData {
        override val opCode = 0x48
    }

    data class AudioRangeSetup(
        var nearThreshold: Float,
        var farThreshold: Float,
        var unknown: Float,
    ): ParticleInitializer {

        override val opCode = 0x4C

        override fun sizeInBytes() = 16

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.writeFloat(nearThreshold)
            byteReader.writeFloat(farThreshold)
            byteReader.writeFloat(unknown)
        }
    }

    data class ProjectionBiasInitializer(
        val param0: Float,
        val param1: Float,
    ): ParticleInitializer {

        override val opCode: Int = 0x72

        override fun sizeInBytes() = 12

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
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

    interface KeyFrameValueSetup: ParticleInitializer {
        val config: KeyFrameConfig

        companion object {
            fun read(byteReader: ByteReader): KeyFrameConfig {
                byteReader.position += 0x04
                val refId = byteReader.nextDatId()
                val cycleConfig = byteReader.next32()
                return KeyFrameConfig(
                    refId = refId,
                    interpolationFn = cycleConfig and 0x0F,
                    cycleCount = (cycleConfig shr 4) + 1,
                )
            }
        }

        override fun sizeInBytes() = 16

        override fun allocationType() = DynamicParticleDataTypes.KeyFrameData(opCode)

        override fun write(byteReader: ByteReader, allocationOffset: Int, context: InitializerContext) {
            byteReader.write(initializer = this, allocationOffset = allocationOffset)
            byteReader.write32(0)
            byteReader.write(config.refId)
            val adjustedCycleCount = (config.cycleCount - 1).coerceAtLeast(0)
            byteReader.write32(config.interpolationFn + (adjustedCycleCount shl 4))
        }
    }

}

private fun ByteReader.write(initializer: ParticleInitializer, allocationOffset: Int = 0) {
    val value = initializer.opCode + ((initializer.sizeInBytes()/4) shl 8) + ((allocationOffset/4) shl 13)
    write32(value)
}