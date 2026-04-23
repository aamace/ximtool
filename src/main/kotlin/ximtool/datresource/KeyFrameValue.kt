package ximtool.datresource

import ximtool.dat.*

object KeyFrameValue {

    fun convert(datId: DatId, values: List<Pair<Float, Float>>): ByteArray {
        sanityCheck(values)

        val size = (0x10 + values.size * 8).padTo16()
        val out = ByteReader(ByteArray(size))

        SectionHeader(datId, SectionType.S19_ParticleKeyFrameData, size).write(out)

        values.forEach {
            out.writeFloat(it.first)
            out.writeFloat(it.second)
        }

        return out.bytes
    }

    private fun sanityCheck(values: List<Pair<Float, Float>>) {
        check(values.firstOrNull()?.first == 0f) { "Expected first key-frame progress-value to be 0" }
        check(values.lastOrNull()?.first == 1f) { "Expected last key-frame progress-value to be 1" }
    }

}
