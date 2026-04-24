package ximtool.datresource

import ximtool.dat.ByteReader
import ximtool.dat.DatId
import ximtool.dat.SectionHeader
import ximtool.dat.SectionType

object EndSection {

    fun serialize(datId: DatId): ByteArray {
        val out = ByteReader(ByteArray(0x10))
        SectionHeader(datId, SectionType.S00_End, 0x10).write(out)
        return out.bytes
    }

}