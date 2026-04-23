package ximtool.datresource

import ximtool.dat.ByteReader
import ximtool.dat.DatId
import ximtool.dat.SectionHeader
import ximtool.dat.SectionType

object SoundEffectPointer {

    fun make(datId: String, soundId: Int): ByteArray {
        val arr = ByteArray(0x20)
        val br = ByteReader(arr)

        val header = SectionHeader(DatId(datId), SectionType.S3D_SoundEffectPointer, arr.size)
        header.write(br)

        br.write("SeSep  ")
        br.write8(0)
        br.write32(soundId)
        br.write32(0)

        return arr
    }

}