package ximtool.tools

import ximtool.dat.*
import ximtool.gltf.GltfImporter
import ximtool.misc.Log
import ximtool.misc.LogColor
import ximtool.resource.TempFile

private val raceGender = RaceGenderConfig.Mithra
private val itemModelSlot = ItemModelSlot.Body
private const val importModelId = 0

fun main() {
    RestoreFromBackup.run(raceGender, itemModelSlot, importModelId)

    Log.info("Working on ${raceGender.name}", LogColor.Green)
    ItemModelGltfImporter(raceGender, itemModelSlot).apply()
}

class ItemModelGltfImporter(val race: RaceGenderConfig, val itemModelSlot: ItemModelSlot) {

    private val path = "Gltf-$race-$itemModelSlot-$importModelId"

    fun apply() {
        val item = DatFile.itemModel(race, itemModelSlot, importModelId)
        val itemRoot = DatTree.parse(item)

        val gltfFile = TempFile.getFile("$path/model.gltf")
        val gltfData = GltfImporter.get(gltfFile)

        ModelGltfImporter(itemRoot, gltfData).import()
        item.writeBytes(itemRoot.serialize())
    }

}