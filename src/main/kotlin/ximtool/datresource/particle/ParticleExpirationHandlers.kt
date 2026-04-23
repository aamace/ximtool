package ximtool.datresource.particle

import ximtool.dat.ByteReader

sealed interface ParticleExpirationHandler {
    fun sizeInBytes(): Int
    fun write(byteReader: ByteReader)
}

object ParticleExpirationHandlers {

    object Repeat: ParticleExpirationHandler {
        override fun sizeInBytes() = 4

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 5, size = EndMarker.sizeInBytes())
        }
    }

    object EndMarker: ParticleExpirationHandler {
        override fun sizeInBytes(): Int {
            return 4
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0, size = sizeInBytes())
        }
    }

}

private fun ByteReader.write(opCode: Int, size: Int) {
    val sizeInDWords = size/4
    write32(opCode + (sizeInDWords shl 8))
}