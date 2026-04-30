package ximtool.tools

import de.javagl.jgltf.model.io.GltfModelWriter
import ximtool.dat.*
import ximtool.misc.Log
import ximtool.misc.LogColor
import ximtool.resource.TempFile

private val raceGender = RaceGenderConfig.Mithra
private val itemModelSlot = ItemModelSlot.Body
private const val exportModelId = 0

fun main() {
    RestoreFromBackup.run(raceGender, itemModelSlot, exportModelId)

    Log.info("Working on ${raceGender.name}", LogColor.Green)
    ItemModelGltfExporter(raceGender, itemModelSlot).apply()
}

class ItemModelGltfExporter(val race: RaceGenderConfig, val itemModelSlot: ItemModelSlot) {

    fun apply() {
        val raceGenderRoot = PcModel.getRaceGenderResource(race)

        val item = DatFile.itemModel(race, ximtool.tools.itemModelSlot, exportModelId)
        val itemRoot = DatTree.parse(item)

        val gltfModel = ModelGltfExporter(raceGenderRoot, itemRoot).convert()
        val file = TempFile.makeFile("Gltf-$race-$itemModelSlot-$exportModelId/model.gltf")

        val writer = GltfModelWriter()
        writer.write(gltfModel, file)
    }

}