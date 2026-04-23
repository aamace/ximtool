package ximtool.datresource.effectroutine

import ximtool.dat.ByteReader
import ximtool.dat.DatId

sealed interface Effect {
    val delay: Int
    fun sizeInBytes(): Int
    fun write(byteReader: ByteReader)
}

object Effects {

    class EndRoutine(override val delay: Int = 0) : Effect {
        override fun sizeInBytes(): Int {
            return 8
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x00, size = sizeInBytes(), delay = delay, duration = 0)
        }
    }

    class StartRoutine(override val delay: Int) : Effect {
        override fun sizeInBytes(): Int {
            return 8
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x01, size = sizeInBytes(), delay = delay, duration = 0)
        }
    }

    class ParticleGenRoutine(override val delay: Int, val duration: Int, val refId: DatId) : Effect {
        override fun sizeInBytes(): Int {
            return 16
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x02, size = sizeInBytes(), delay = delay, duration = duration)
            byteReader.write(refId)
            byteReader.write32(0)
        }
    }

    class ParticleDampenRoutine(override val delay: Int, val duration: Int, val refId: DatId) : Effect {
        override fun sizeInBytes(): Int {
            return 16
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x1E, size = sizeInBytes(), delay = delay, duration = duration)
            byteReader.write(refId)
            byteReader.write32(0)
        }
    }

    class StopGeneratorRoutine(override val delay: Int, val duration: Int, val refId: DatId) : Effect {
        override fun sizeInBytes(): Int {
            return 16
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x2D, size = sizeInBytes(), delay = delay, duration = duration)
            byteReader.write(refId)
            byteReader.write32(0)
        }
    }

    class StartLoopRoutine(override val delay: Int, val refId: DatId) : Effect {
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

    class EndLoopRoutine(override val delay: Int, val refId: DatId) : Effect {
        override fun sizeInBytes(): Int {
            return 16
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x85, size = sizeInBytes(), delay = delay, duration = 0)
            byteReader.write(refId)
            byteReader.write32(0)
        }
    }

}

private fun ByteReader.write(opCode: Int, size: Int, delay: Int, duration: Int) {
    val sizeInDWords = size / 4
    write32(opCode + (sizeInDWords shl 8))
    write16(delay)
    write16(duration)
}