package ximtool.dat

object TextureSection {

    fun read(data: ByteArray): Texture {
        return read(ByteReader(data))
    }

    fun read(br: ByteReader): Texture {
        val sectionHeader = SectionHeader.read(br)

        val type = br.next8()
        val name = br.nextTextureName()

        val headerSize = br.next32() // Header-size
        check(headerSize == 0x28)

        val width = br.next32()
        val height = br.next32()

        val bitPlanes = br.next16()
        val bitCount = br.next16()

        // unused
        br.next32()
        br.next32()
        br.next32()
        br.next32()
        br.next32()

        val importantColors = br.next32()

        val header = TextureData.TextureHeader(
            name = name,
            width = width,
            height = height,
            bitCount = bitCount,
            bitPlanes = bitPlanes,
            importantColors = importantColors,
        )

        val body = when (type) {
            0xA1 -> {
                val fourcc = br.nextString(4).reversed()
                val unk = br.next32()
                br.next32() // Scan-width?
                val data = br.remaining()
                TextureData.CompressedTexture(fourcc, unk, data)
            }

            else -> TextureData.UnimplementedTexture(type, br.remaining())
        }

        return Texture(
            datId = sectionHeader.sectionId,
            header = header,
            body = body,
        )
    }
}

object TextureSerializer {

    fun serialize(texture: Texture): ByteArray {
        val bodySize = when (texture.body) {
            is TextureData.CompressedTexture -> texture.body.data.size + 12
            is TextureData.UnimplementedTexture -> texture.body.data.size
        }

        val size = (0x10 + 0x39 + bodySize).padTo16()
        val out = ByteReader(ByteArray(size))

        val sectionHeader = SectionHeader(texture.datId, SectionType.S20_Texture, size)
        sectionHeader.write(out)

        out.write8(texture.body.type)
        out.write(texture.header.name)

        out.write32(0x28) // Header-size

        out.write32(texture.header.width)
        out.write32(texture.header.height)

        out.write16(texture.header.bitPlanes)
        out.write16(texture.header.bitCount)

        out.write32(0) // unused
        out.write32(0)
        out.write32(0)
        out.write32(0)
        out.write32(0)

        out.write32(texture.header.importantColors)

        when (texture.body) {
            is TextureData.CompressedTexture -> {
                out.write(texture.body.code.reversed())
                out.write32(texture.body.unk)

                val scanWidth = texture.header.width * if (texture.body.code == "DXT1") { 2 } else { 4 }
                out.write32( scanWidth)
            }
            is TextureData.UnimplementedTexture -> {
            }
        }

        out.write(texture.body.data)
        return out.bytes
    }

}

object TextureData {

    data class TextureHeader(
        val name: TextureName,
        val width: Int,
        val height: Int,
        val bitPlanes: Int,
        val bitCount: Int,
        val importantColors: Int,
    )

    sealed interface TextureBody {
        val type: Int
        val data: ByteArray
    }

    class CompressedTexture(
        val code: String,
        val unk: Int,
        override val data: ByteArray,
    ) : TextureBody {
        override val type = 0xA1
    }

    class UnimplementedTexture(
        override val type: Int,
        override val data: ByteArray
    ) : TextureBody

}

fun Directory.getTextures(): List<Texture> {
    return getChildren(Texture::class)
}

fun Directory.getTexturesRecursive(): List<Texture> {
    return getChildrenRecursive(Texture::class)
}