package ximtool.datresource.particle

import ximtool.dat.ByteReader

sealed interface ParticleGeneratorUpdater {
    fun sizeInBytes(): Int
    fun write(byteReader: ByteReader)
}

object ParticleGeneratorUpdaters {

    class AssociationUpdater(val followPosition: Boolean, val followFacing: Boolean, val rate: Int = 255): ParticleGeneratorUpdater {
        override fun sizeInBytes(): Int {
            return 8
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x11, size = sizeInBytes())

            var param = 0
            if (followPosition) { param += 0x01 }
            if (followFacing) { param += 0x02 }
            param += rate shl 2

            byteReader.write32(param)
        }
    }

    object EndMarker: ParticleGeneratorUpdater {
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