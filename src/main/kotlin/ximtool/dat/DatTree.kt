package ximtool.dat

class Node(
    val parent: Node?,
    val sectionHeader: SectionHeader,
    val data: ByteArray,
    val children: MutableList<Node> = ArrayList(),
)

object DatTree {

    fun parse(rawDat: ByteArray) : Node {
        var rootNode: Node? = null
        var currentNode: Node? = null

        val byteReader = ByteReader(rawDat)

        while (true) {
            if (!byteReader.hasMore()) {
                break
            }

            val sectionStart = byteReader.position
            val header = SectionHeader.read(byteReader)
            val data = byteReader.subArray(length = header.sectionSize, offset = sectionStart)

            val node = Node(currentNode, header, data)
            currentNode?.children?.add(node)

            if (rootNode == null) { rootNode = node }

            if (header.sectionType == SectionType.S01_Directory) {
                currentNode = node
            } else if (header.sectionType == SectionType.S00_End) {
                currentNode = currentNode?.parent
            }

            byteReader.position = sectionStart + header.sectionSize
        }

        return rootNode!!
    }

}

fun Node.deleteRecursive(filter: (Node) -> Boolean) {
    children.forEach { it.deleteRecursive(filter) }
    children.removeAll { filter(it) }
}

fun Node.addChild(childHeader: SectionHeader, childBody: ByteArray): Node {
    check(sectionHeader.sectionType == SectionType.S01_Directory)
    val child = Node(this, childHeader, childBody)
    children += child
    sortChildren()
    return child
}

fun Node.addChild(childBody: ByteArray): Node {
    val childHeader = SectionHeader.read(ByteReader(childBody))
    return addChild(childHeader, childBody)
}

fun Node.toArray(): ByteArray {
    val array = ByteArray(size())
    write(ByteReader(array))
    return array
}

fun Node.size(): Int {
    return sectionHeader.sectionSize + children.sumOf { it.size() }
}

private fun Node.write(byteReader: ByteReader) {
    byteReader.write(data)
    children.forEach { it.write(byteReader) }
}

private fun Node.sortChildren() {
    children.sortBy {
        when (it.sectionHeader.sectionType) {
            SectionType.S00_End -> Int.MAX_VALUE
            SectionType.S01_Directory -> Int.MAX_VALUE - 1
            else -> it.sectionHeader.sectionType.code
        }
    }
}