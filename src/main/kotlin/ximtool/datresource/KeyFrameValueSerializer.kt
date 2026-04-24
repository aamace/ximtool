package ximtool.datresource

import ximtool.dat.*

object KeyFrameValueSerializer {

    fun serialize(keyFrameValue: KeyFrameValue): ByteArray {
        return serialize(keyFrameValue.datId, keyFrameValue.entries)
    }

    fun serialize(datId: DatId, values: List<KeyFrameEntry>): ByteArray {
        sanityCheck(values)

        val size = (0x10 + values.size * 8).padTo16()
        val out = ByteReader(ByteArray(size))

        SectionHeader(datId, SectionType.S19_ParticleKeyFrameData, size).write(out)

        values.forEach {
            out.writeFloat(it.progress)
            out.writeFloat(it.value)
        }

        return out.bytes
    }

    private fun sanityCheck(values: List<KeyFrameEntry>) {
        check(values.firstOrNull()?.progress == 0f) { "Expected first key-frame progress-value to be 0" }
        check(values.lastOrNull()?.progress == 1f) { "Expected last key-frame progress-value to be 1" }
    }

}
