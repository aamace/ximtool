package ximtool.datresource.effectroutine

import ximtool.dat.ByteReader

sealed interface OnInitializeEffect {
    fun sizeInBytes(): Int
    fun write(byteReader: ByteReader)
}

object OnInitializeEffects {

    object EndEffect: OnInitializeEffect {
        override fun sizeInBytes(): Int {
            return 4
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = 0x00, size = sizeInBytes())
        }
    }

    class NotImplementedRoutine(val opCode: Int, val body: ByteArray): OnInitializeEffect {
        override fun sizeInBytes(): Int {
            return 4 + body.size
        }

        override fun write(byteReader: ByteReader) {
            byteReader.write(opCode = opCode, size = sizeInBytes())
            byteReader.write(body)
        }
    }

}

private fun ByteReader.write(opCode: Int, size: Int) {
    val sizeInDWords = size/4
    write32(opCode + (sizeInDWords shl 8))
}