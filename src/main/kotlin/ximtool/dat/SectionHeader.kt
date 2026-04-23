package ximtool.dat

enum class SectionType(val code: Int) {
    S00_End(0x00),
    S01_Directory(0x01),
    S04_Table(0x04),
    S05_ParticleGenerator(0x05),
    S06_Route(0x06),
    S07_EffectRoutine(0x07),
    S19_ParticleKeyFrameData(0x19),
    S1C_ZoneDef(0x1C),
    S1F_ParticleMesh(0x1F),
    S20_Texture(0x20),
    S21_SpriteSheetMesh(0x21),
    S25_WeightedMesh(0x25),
    S29_Skeleton(0x29),
    S2A_SkeletonMesh(0x2A),
    S2B_SkeletonAnimation(0x2B),
    S2E_ZoneMesh(0x2E),
    S2F_Environment(0x2F),
    S30_UiMenu(0x30),
    S31_UiElementGroup(0x31),
    S36_ZoneInteractions(0x36),
    S3E_PointList(0x3E),
    S3D_SoundEffectPointer(0x3D),
    S45_Info(0x45),
    S46_Unknown(0x46),
    S49_SpellList(0x49),
    S4A_Path(0x4A),
    S53_AbilityList(0x53),
    S54_WeaponTrace(0x54),
    S5E_Blur(0x5E),
    S5D_BumpMap(0x5D),
    S5F_Unknown(0x5F),
    Unknown(0xFF),
    ;

    companion object {
        fun fromCode(code: Int): SectionType {
            return SectionType.values().firstOrNull { it.code == code } ?: Unknown
        }
    }
}

class SectionHeader(
    val sectionId: DatId,
    val sectionType: SectionType,
    val sectionSize: Int,
) {

    companion object {
        fun read(byteReader: ByteReader): SectionHeader {
            val sectionId = DatId(byteReader.nextString(0x4))
            val sectionMeta = byteReader.next32()

            val sectionType = SectionType.fromCode(sectionMeta and 0x7F)
            val sectionSize = (sectionMeta shr 7 and 0xFFFFF) * 0x10

            byteReader.align0x10()

            return SectionHeader(sectionId, sectionType, sectionSize)
        }
    }

    fun write(byteReader: ByteReader) {
        byteReader.write(sectionId)

        val sectionCode = sectionType.code
        val sectionSize = (byteReader.bytes.size / 0x10) shl 7
        byteReader.write32(sectionCode + sectionSize)

        byteReader.align0x10()
    }

    override fun toString(): String {
        return "$sectionId"
    }

}
