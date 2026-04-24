package ximtool.datresource.effectroutine

import ximtool.dat.ByteReader
import ximtool.dat.DatId

sealed interface Effect {
    var delay: Int
    fun sizeInBytes(): Int
    fun write(byteReader: ByteReader)
}

object Effects {

    class EndRoutine(override var delay: Int = 0) : Effect {
        override fun sizeInBytes(): Int {
            return 8
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x00, size = sizeInBytes(), delay = delay, duration = 0)
        }
    }

    class StartRoutine(override var delay: Int) : Effect {
        override fun sizeInBytes(): Int {
            return 8
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x01, size = sizeInBytes(), delay = delay, duration = 0)
        }
    }

    class ParticleGenRoutine(override var delay: Int, var duration: Int, var refId: DatId) : Effect {
        override fun sizeInBytes(): Int {
            return 16
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x02, size = sizeInBytes(), delay = delay, duration = duration)
            byteReader.write(refId)
            byteReader.write32(0)
        }
    }

    class ParticleDampenRoutine(override var delay: Int, var duration: Int, var refId: DatId) : Effect {
        override fun sizeInBytes(): Int {
            return 16
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x1E, size = sizeInBytes(), delay = delay, duration = duration)
            byteReader.write(refId)
            byteReader.write32(0)
        }
    }

    class StopGeneratorRoutine(override var delay: Int, var duration: Int, var refId: DatId) : Effect {
        override fun sizeInBytes(): Int {
            return 16
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x2D, size = sizeInBytes(), delay = delay, duration = duration)
            byteReader.write(refId)
            byteReader.write32(0)
        }
    }

    class TransitionParticleEffect(override var delay: Int, var duration: Int, var stopEffect: DatId, var startEffect: DatId): Effect {
        override fun sizeInBytes() = 28

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x3F, size = sizeInBytes(), delay = delay, duration = duration)

            byteReader.write(stopEffect)
            byteReader.write32(0)

            byteReader.write(startEffect)
            byteReader.write32(0)

            byteReader.write32(0)
        }
    }

    class StartLoopRoutine(override var delay: Int, var refId: DatId) : Effect {
        override fun sizeInBytes(): Int {
            return 20
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x73, size = sizeInBytes(), delay = delay, duration = 0)
            byteReader.write(refId)
            byteReader.write32(0)
            byteReader.write32(0)
        }
    }

    class EndLoopRoutine(override var delay: Int, var refId: DatId) : Effect {
        override fun sizeInBytes(): Int {
            return 16
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x85, size = sizeInBytes(), delay = delay, duration = 0)
            byteReader.write(refId)
            byteReader.write32(0)
        }
    }

    class NotImplementedRoutine(val opCode: Int, override var delay: Int, var duration: Int, val body: ByteArray): Effect {
        override fun sizeInBytes(): Int {
            return 4 + 4 + body.size
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = opCode, size = sizeInBytes(), delay = delay, duration = duration)
            byteReader.write(body)
        }
    }

}

private fun ByteReader.write(opCode: Int, size: Int, delay: Int, duration: Int) {
    val sizeInDWords = size / 4
    write32(opCode + (sizeInDWords shl 8))
    write16(delay)
    write16(duration)
}