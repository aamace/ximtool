package ximtool.datresource

import ximtool.dat.ByteReader
import ximtool.dat.DatId
import ximtool.dat.SectionHeader
import ximtool.dat.SectionType

object EndSection {

    fun make(): ByteArray {
        val out = ByteReader(ByteArray(0x10))
        SectionHeader(DatId("end "), SectionType.S00_End, 0x10).write(out)
        return out.bytes
    }

}