package ximtool.datresource

import ximtool.dat.*

class KeyFrameEntry(
    var progress: Float,
    var value: Float,
)

object KeyFrameValueSection {

    fun read(data: ByteArray): KeyFrameValue {
        val byteReader = ByteReader(data)
        val sectionHeader = SectionHeader.read(byteReader)

        val entries = ArrayList<KeyFrameEntry>()
        while (true) {
            val entry = KeyFrameEntry(byteReader.nextFloat(), byteReader.nextFloat())
            entries += entry
            if (entry.progress == 1f) { break }
        }

        return KeyFrameValue(sectionHeader.sectionId, entries)
    }

}

fun Directory.getKeyFrameValue(datId: DatId): KeyFrameValue {
    return getChild(KeyFrameValue::class, datId)
}