package ximtool.datresource.particle

import ximtool.dat.ByteReader

sealed interface ParticleUpdater {
    fun sizeInBytes(): Int
    fun write(byteReader: ByteReader, allocationOffset: Int)
    fun linkedInitializerOpCode(): Int? = null
}

object ParticleUpdaters {

    object LifeTimeUpdater: ParticleUpdater {
        override fun sizeInBytes() = 4

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0x0E, opSize = sizeInBytes())
        }
    }

    object ColorRedKeyFrameUpdater: KeyFrameUpdater(opCode = 0x18)
    object ColorGreenKeyFrameUpdater: KeyFrameUpdater(opCode = 0x19)
    object ColorBlueKeyFrameUpdater: KeyFrameUpdater(opCode = 0x1A)
    object ColorAlphaKeyFrameUpdater: KeyFrameUpdater(opCode = 0x1B)

    class TexCoordTranslateU(val amountPerFrame: Float): ParticleUpdater {
        override fun sizeInBytes() = 8

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0x27, opSize = sizeInBytes())
            byteReader.writeFloat(amountPerFrame)
        }
    }

    class TexCoordTranslateV(val amountPerFrame: Float): ParticleUpdater {
        override fun sizeInBytes() = 8

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0x27, opSize = sizeInBytes())
            byteReader.writeFloat(amountPerFrame)
        }
    }

    class DistanceBasedFadeUpdater(val fadeStartDistance: Float, val fullFadeDistance: Float): ParticleUpdater {
        override fun sizeInBytes() = 12

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0x2E, opSize = sizeInBytes())
            byteReader.writeFloat(fadeStartDistance)
            byteReader.writeFloat(fullFadeDistance)
            byteReader.writeFloat(0f)
        }
    }

    object EndMarker: ParticleUpdater {
        override fun sizeInBytes(): Int {
            return 4
        }

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = 0, opSize = sizeInBytes())
        }
    }

    abstract class KeyFrameUpdater(val opCode: Int): ParticleUpdater {
        override fun sizeInBytes() = 4

        override fun write(byteReader: ByteReader, allocationOffset: Int) {
            byteReader.write(opCode = opCode, opSize = sizeInBytes(), allocationOffset = allocationOffset)
        }
    }

}

private fun ByteReader.write(opCode: Int, opSize: Int, allocationOffset: Int = 0) {
    val sizeInDWord = opSize/4
    val value = opCode + (sizeInDWord shl 8) + (allocationOffset shl 13)
    write32(value)
}