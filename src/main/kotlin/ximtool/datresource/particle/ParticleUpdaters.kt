package ximtool.datresource.particle

import ximtool.dat.ByteReader
import ximtool.math.Vector3f

sealed interface ParticleUpdater {
    fun sizeInBytes(): Int
    fun write(byteReader: ByteReader, allocationOffset: Int)
    fun linkedDataType(): DynamicParticleData? = null
}

object ParticleUpdaters {

    interface NoDataUpdater: ParticleUpdater {
        val opCode: Int

        override fun sizeInBytes() = 4

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = opCode, opSize = sizeInBytes(), allocationOffset = allocationOffset)
        }
    }

    object PositionUpdater: NoDataUpdater {
        override val opCode = 0x02
        override fun linkedDataType() = DynamicParticleDataTypes.PositionData
    }

    data class PositionVelocityAccelerator(val acceleration: Vector3f): ParticleUpdater {
        override fun sizeInBytes() = 16

        override fun linkedDataType() = DynamicParticleDataTypes.PositionData

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0x03, opSize = sizeInBytes(), allocationOffset = allocationOffset)
            byteReader.write(acceleration)
        }
    }

    object RotationUpdater: NoDataUpdater {
        override val opCode = 0x05
        override fun linkedDataType() = DynamicParticleDataTypes.RotationData
    }

    data class RotationVelocityAccelerator(val acceleration: Vector3f): ParticleUpdater {
        override fun sizeInBytes() = 16

        override fun linkedDataType() = DynamicParticleDataTypes.RotationData

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0x06, opSize = sizeInBytes(), allocationOffset = allocationOffset)
            byteReader.write(acceleration)
        }
    }

    object ScaleUpdater: NoDataUpdater {
        override val opCode = 0x08
        override fun linkedDataType() = DynamicParticleDataTypes.ScaleData
    }

    data class ScaleVelocityAccelerator(val acceleration: Vector3f): ParticleUpdater {
        override fun sizeInBytes() = 16

        override fun linkedDataType() = DynamicParticleDataTypes.ScaleData

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0x09, opSize = sizeInBytes(), allocationOffset = allocationOffset)
            byteReader.write(acceleration)
        }
    }

    object ColorTransformApplier: NoDataUpdater {
        override val opCode = 0x0B
        override fun linkedDataType() = DynamicParticleDataTypes.ColorTransform
    }

    object SpriteSheetUpdater: NoDataUpdater {
        override val opCode = 0x0D
    }

    object LifeTimeUpdater: NoDataUpdater {
        override val opCode = 0x0E
    }

    object ColorRedKeyFrameUpdater: KeyFrameUpdater(opCode = 0x18, linkedOpCode = 0x2A)
    object ColorGreenKeyFrameUpdater: KeyFrameUpdater(opCode = 0x19, linkedOpCode = 0x2B)
    object ColorBlueKeyFrameUpdater: KeyFrameUpdater(opCode = 0x1A, linkedOpCode = 0x2C)
    object ColorAlphaKeyFrameUpdater: KeyFrameUpdater(opCode = 0x1B, linkedOpCode = 0x2D)

    data class TexCoordTranslateU(val amountPerFrame: Float): ParticleUpdater {
        override fun sizeInBytes() = 8

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0x27, opSize = sizeInBytes(), allocationOffset = allocationOffset)
            byteReader.writeFloat(amountPerFrame)
        }
    }

    data class TexCoordTranslateV(val amountPerFrame: Float): ParticleUpdater {
        override fun sizeInBytes() = 8

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0x28, opSize = sizeInBytes(), allocationOffset = allocationOffset)
            byteReader.writeFloat(amountPerFrame)
        }
    }

    data class VelocityDampener(val magnitude: Float, val unknown: Float = 0f) : ParticleUpdater {
        override fun sizeInBytes() = 12

        override fun linkedDataType(): DynamicParticleData = DynamicParticleDataTypes.PositionData

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0x2C, opSize = sizeInBytes(), allocationOffset = allocationOffset)
            byteReader.writeFloat(magnitude)
            byteReader.writeFloat(unknown)
        }
    }

    data class DistanceBasedFadeUpdater(val fadeStartDistance: Float, val fullFadeDistance: Float): ParticleUpdater {
        override fun sizeInBytes() = 16

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0x2E, opSize = sizeInBytes(), allocationOffset = allocationOffset)
            byteReader.writeFloat(fadeStartDistance)
            byteReader.writeFloat(fullFadeDistance)
            byteReader.writeFloat(0f)
        }
    }

    object EndMarker: NoDataUpdater {
        override val opCode: Int = 0x00
    }

    abstract class KeyFrameUpdater(val opCode: Int, val linkedOpCode: Int): ParticleUpdater {
        override fun sizeInBytes() = 4

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = opCode, opSize = sizeInBytes(), allocationOffset = allocationOffset)
        }

        override fun linkedDataType(): DynamicParticleData {
            return DynamicParticleDataTypes.KeyFrameData(linkedOpCode)
        }
    }

}

private fun ByteReader.write(opCode: Int, opSize: Int, allocationOffset: Int = 0) {
    val value = opCode + ((opSize/4) shl 8) + ((allocationOffset/4) shl 13)
    write32(value)
}