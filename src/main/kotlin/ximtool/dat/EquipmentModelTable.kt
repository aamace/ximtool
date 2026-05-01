package ximtool.dat

private data class TableEntry(val tableOffset: Int, val entryCount: Int)

private data class RaceGenderTable(val entries: Map<ItemModelSlot, List<TableEntry>>)

object EquipmentModelTable {

    private val tables: Map<RaceGenderConfig, RaceGenderTable> by lazy { parse(MainDll.getEquipmentLookupTable()) }

    fun getItemModelPath(raceGenderConfig: RaceGenderConfig, itemModelSlot: ItemModelSlot, itemModelId: Int): String {
        val table = tables[raceGenderConfig] ?: throw IllegalStateException("Race table wasn't loaded")
        val subTable = table.entries[itemModelSlot] ?: throw IllegalStateException("Failed to find sub-table for $itemModelSlot")

        var cumulativeEntryCount = 0

        for (subTableEntry in subTable) {
            if (itemModelId >= cumulativeEntryCount + subTableEntry.entryCount ) {
                cumulativeEntryCount += subTableEntry.entryCount
                continue
            }

            val offset = subTableEntry.tableOffset + (itemModelId - cumulativeEntryCount)
            return FileTables[offset] ?: throw IllegalStateException("No file-table entry for item-model $itemModelSlot/$raceGenderConfig/$itemModelId")
        }

        throw IllegalStateException("Failed to find item-model mapping $itemModelSlot/$raceGenderConfig")
    }

    private fun parse(byteReader: ByteReader): Map<RaceGenderConfig, RaceGenderTable> {
        val tables = HashMap<RaceGenderConfig, RaceGenderTable>()

        for (config in RaceGenderConfig.values()) {
            byteReader.position = 0x1B0 * (config.equipmentTableIndex - 1)
            tables[config] = parseRaceGenderTable(byteReader)
        }

        return tables
    }

    private fun parseRaceGenderTable(byteReader: ByteReader): RaceGenderTable {
        val basePosition = byteReader.position
        val table = HashMap<ItemModelSlot, List<TableEntry>>()

        for (slot in ItemModelSlot.values()) {
            byteReader.position = basePosition + 0x30 * (slot.prefix shr 0xC)
            val entries = ArrayList<TableEntry>(6)

            for (i in 0 until 6) {
                val entry = TableEntry(tableOffset = byteReader.next32(), entryCount = byteReader.next32())
                if (entry.tableOffset == 0x0) { continue }
                entries += entry
            }

            table[slot] = entries
        }

        return RaceGenderTable(entries = table)
    }

}