package ximtool.datresource

import ximtool.dat.ByteReader
import ximtool.dat.DatId
import ximtool.dat.SectionHeader
import ximtool.dat.SectionType

object DirectorySection {

    fun make(datId: DatId): ByteArray {
        val out = ByteReader(ByteArray(0x20))
        SectionHeader(datId, SectionType.S01_Directory, 0x20).write(out)
        return out.bytes
    }

}