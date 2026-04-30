package ximtool.resource

import ximtool.dat.*
import java.io.File

object DdsToTexture {

    fun convert(path: String, datId: DatId, name: TextureName): Texture {
        val bytes = ResourceReader.readBytes(path)
        return convert(bytes, datId, name)
    }

    fun convert(file: File, datId: DatId, name: TextureName): Texture {
        val bytes = file.readBytes()
        return convert(bytes, datId, name)
    }

    private fun convert(bytes: ByteArray, datId: DatId, name: TextureName): Texture {
        val br = ByteReader(bytes)
        val header = DdsStructs.parseHeader(br)

        val textureHeader = TextureData.TextureHeader(
            name = name,
            width = header.dwWidth,
            height = header.dwHeight,
            bitPlanes = 2,
            bitCount = 8,
            importantColors = 32,
        )

        val textureBody = TextureData.CompressedTexture(
            code = header.ddspf.dwFourCC,
            unk = 0x2000, // ???
            data = br.remaining(),
        )

        return Texture(datId, textureHeader, textureBody)
    }

}

object TextureToDds {

    fun convert(texture: Texture): ByteArray {
        check(texture.body is TextureData.CompressedTexture)

        val size = 0x04 + 0x7C + texture.body.data.size
        val out = ByteReader(ByteArray(size))

        val header = DdsStructs.DdsHeader()
        header.dwSize = 0x7C
        header.dwFlags = 0x01 + 0x02 + 0x04 + 0x1000
        header.dwHeight = texture.header.height
        header.dwWidth = texture.header.width
        header.dwPitchOrLinearSize = if (texture.body.code == "DXT1") {
            texture.header.height * texture.header.width / 2
        } else {
            texture.header.height * texture.header.width
        }

        header.ddspf.dwSize = 0x20
        header.ddspf.dwFlags = 0x04 // TODO alpha flags? 0x01 and 0x02
        header.ddspf.dwFourCC = texture.body.code

        header.dwCaps = 0x1000

        DdsStructs.writeHeader(header, out)
        out.write(texture.body.data)

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

    fun writeHeader(header: DdsHeader, byteReader: ByteReader) {
        byteReader.write("DDS ")

        byteReader.write32(header.dwSize)
        byteReader.write32(header.dwFlags)
        byteReader.write32(header.dwHeight)
        byteReader.write32(header.dwWidth)
        byteReader.write32(header.dwPitchOrLinearSize)
        byteReader.write32(header.dwDepth)
        byteReader.write32(header.dwMipMapCount)
        for (i in header.dwReserved1.indices) { byteReader.write32(header.dwReserved1[i]) }

         byteReader.write32(header.ddspf.dwSize)
         byteReader.write32(header.ddspf.dwFlags)
         byteReader.write(header.ddspf.dwFourCC)
         byteReader.write32(header.ddspf.dwRGBBitCount)
         byteReader.write32(header.ddspf.dwRBitMask)
         byteReader.write32(header.ddspf.dwGBitMask)
         byteReader.write32(header.ddspf.dwBBitMask)
         byteReader.write32(header.ddspf.dwABitMask)

         byteReader.write32(header.dwCaps)
         byteReader.write32(header.dwCaps2)
         byteReader.write32(header.dwCaps3)
         byteReader.write32(header.dwCaps4)
         byteReader.write32(header.dwReserved2)
    }

}