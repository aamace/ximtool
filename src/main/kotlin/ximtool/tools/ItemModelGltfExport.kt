package ximtool.tools

import de.javagl.jgltf.model.io.GltfModelWriter
import ximtool.dat.*
import ximtool.misc.Log
import ximtool.resource.TempFile

fun main() {
    ItemModelGltfExporter(
        race = RaceGenderConfig.Mithra,
        itemModelSlot = ItemModelSlot.Body,
        itemModelId = 0,
    ).apply()
}

class ItemModelGltfExporter(
    val race: RaceGenderConfig,
    val itemModelSlot: ItemModelSlot,
    val itemModelId: Int,
    val config: ModelGltfExporterConfig = ModelGltfExporterConfig(),
) {

    fun apply() {
        RestoreFromBackup.run(race, itemModelSlot, itemModelId)
        val raceGenderRoot = PcModel.getRaceGenderResource(race)

        val item = DatFile.itemModel(race, itemModelSlot, itemModelId)
        val itemRoot = DatTree.parse(item)

        val gltfModel = ModelGltfExporter(raceGenderRoot, itemRoot, config).convert()
        val file = TempFile.makeFile("Gltf-$race-$itemModelSlot-$itemModelId/${config.modelFileName}")

        val writer = GltfModelWriter()
        writer.write(gltfModel, file)

        Log.info("Exported to ${file.absolutePath}")
        Log.info("Complete!")
    }

}