package ximtool.dat

import ximtool.Environment
import java.io.File

object FileTables {

    private const val numTables = 9
    private val vTables = HashMap<Int, VTable>()
    private val fTables = HashMap<Int, FTable>()

    private var loaded = false

    operator fun get(fileId: Int): String? {
        return getFilePath(fileId)
    }

    fun getFilePath(fileId: Int): String? {
        load()

        for (i in 1..numTables) {
            val vTable = vTables[i] ?: continue
            if (!vTable.hasFile(fileId)) { continue }

            val fTable = fTables[i] ?: continue
            return fTable.getFile(fileId)
        }

        return null
    }

    private fun load() {
        if (loaded) { return }
        loaded = true
        for (i in 1 .. numTables) { loadTable(i) }
    }

    private fun loadTable(tableIndex: Int) {
        val prefix = Environment.ffxiDir + if (tableIndex == 1) { "" } else { "ROM${tableIndex}" }
        val postfix = if (tableIndex == 1) { "" } else { tableIndex.toString() }

        val vTable = File("${prefix}/VTABLE${postfix}.DAT").readBytes()
        vTables[tableIndex] = VTable(tableIndex, ByteReader(vTable))

        val fTable = File("${prefix}/FTABLE${postfix}.DAT").readBytes()
        fTables[tableIndex] = FTable(tableIndex, ByteReader(fTable))
    }

}

private class VTable(private val vtableIndex: Int, private val byteReader: ByteReader) {
    fun hasFile(fileId: Int): Boolean {
        return byteReader.bytes[fileId] == vtableIndex.toByte()
    }
}

private class FTable(private val directoryNumber: Int, private val byteReader: ByteReader) {

    fun getFile(fileId: Int): String {
        byteReader.position = 2 * fileId
        val fileValue = byteReader.next16()

        val fileName = fileValue and 0x7F
        val folderName = fileValue ushr 7

        val directoryName = if (directoryNumber == 1) { "" } else { directoryNumber.toString() }

        return "ROM$directoryName/$folderName/$fileName.DAT"
    }

}