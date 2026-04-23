package ximtool.resource

import ximtool.dat.*

object DdsToTexture {

    fun convert(path: String, datId: DatId, name: TextureName): ByteArray {
        val bytes = ResourceReader.readBytes(path)
        return convert(bytes, datId, name)
    }

    private fun convert(bytes: ByteArray, datId: DatId, name: TextureName): ByteArray {
        val br = ByteReader(bytes)
        val header = DdsStructs.parseHeader(br)
        val dataSize = br.bytes.size - br.position

        val outSize = (0x10 + 0x45 + dataSize).padTo16()
        val out = ByteReader(ByteArray(outSize))

        val sectionHeader = SectionHeader(datId, SectionType.S20_Texture, outSize)
        sectionHeader.write(out)

        out.write8(0xA1) // Type
        out.write(name)

        out.write32(0x28) // Header-size

        out.write32(header.dwWidth)
        out.write32(header.dwHeight)

        out.write16(0x02) // Bit planes?
        out.write16(0x08) // Bit count - always 8 for DDS

        out.write32(0) // unused
        out.write32(0)
        out.write32(0)
        out.write32(0)
        out.write32(0)

        out.write32(0x20) // "Important colors"?

        out.write(header.ddspf.dwFourCC.reversed())
        out.write32(8192) // ???
        out.write32(header.dwWidth * 2)

        br.bytes.copyInto(
            destination = out.bytes,
            destinationOffset = out.position,
            startIndex = br.position,
        )

        return out.bytes
    }

}

private object DdsStructs {

    class DdsHeader {
        var dwSize: Int = 0
        var dwFlags: Int = 0
        var dwHeight: Int = 0
        var dwWidth: Int = 0
        var dwPitchOrLinearSize: Int = 0
        var dwDepth: Int = 0
        var dwMipMapCount: Int = 0
        var dwReserved1: Array<Int> = Array(11) { 0 }
        var ddspf: DdsPixelFormat = DdsPixelFormat()
        var dwCaps: Int = 0
        var dwCaps2: Int = 0
        var dwCaps3: Int = 0
        var dwCaps4: Int = 0
        var dwReserved2: Int = 0
    }

    class DdsPixelFormat {
        var dwSize: Int = 0
        var dwFlags: Int = 0
        var dwFourCC: String = ""
        var dwRGBBitCount: Int = 0
        var dwRBitMask: Int = 0
        var dwGBitMask: Int = 0
        var dwBBitMask: Int = 0
        var dwABitMask: Int = 0
    }
    
    fun parseHeader(byteReader: ByteReader): DdsHeader {
        byteReader.next32() // "DDS "

        val header = DdsHeader()
        header.dwSize = byteReader.next32(); check(header.dwSize == 0x7C)
        header.dwFlags = byteReader.next32()
        header.dwHeight = byteReader.next32()
        header.dwWidth = byteReader.next32()
        header.dwPitchOrLinearSize = byteReader.next32()
        header.dwDepth = byteReader.next32()
        header.dwMipMapCount = byteReader.next32()
        for (i in header.dwReserved1.indices) { header.dwReserved1[i] = byteReader.next32() }

        header.ddspf.dwSize = byteReader.next32()
        header.ddspf.dwFlags = byteReader.next32()
        header.ddspf.dwFourCC = byteReader.nextString(4)
        header.ddspf.dwRGBBitCount = byteReader.next32()
        header.ddspf.dwRBitMask = byteReader.next32()
        header.ddspf.dwGBitMask = byteReader.next32()
        header.ddspf.dwBBitMask = byteReader.next32()
        header.ddspf.dwABitMask = byteReader.next32()

        header.dwCaps = byteReader.next32()
        header.dwCaps2 = byteReader.next32()
        header.dwCaps3 = byteReader.next32()
        header.dwCaps4 = byteReader.next32()
        header.dwReserved2 = byteReader.next32()

        return header
    }

}