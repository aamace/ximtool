package ximtool.datresource

import ximtool.dat.*

object DirectorySection {

    fun serialize(directory: Directory): ByteArray {
        val localBytes = ByteReader(ByteArray(0x20))
        SectionHeader(directory.datId, SectionType.S01_Directory, 0x20).write(localBytes)

        val childrenData = directory.children.map { it.serialize() }

        val totalSize = localBytes.bytes.size + childrenData.sumOf { it.size }
        val aggregateBuffer = ByteArray(totalSize)

        val aggregateWriter = ByteReader(aggregateBuffer)
        aggregateWriter.write(localBytes.bytes)
        childrenData.forEach { aggregateWriter.write(it) }

        return aggregateBuffer
    }

}

fun Directory.addSubDirectory(directoryName: DatId): Directory {
    return addSubDirectory(Directory(directoryName))
}

fun Directory.addSubDirectory(directory: Directory): Directory {
    addChild(directory)
    return directory
}

fun Directory.getSubDirectory(datId: DatId): Directory {
    return getChild(Directory::class, datId)
}