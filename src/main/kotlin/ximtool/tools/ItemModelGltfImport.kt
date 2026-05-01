package ximtool.tools

import ximtool.dat.*
import ximtool.gltf.GltfImporter
import ximtool.misc.Log
import ximtool.resource.TempFile

fun main() {
    ItemModelGltfImporter(
        race = RaceGenderConfig.Mithra,
        itemModelSlot = ItemModelSlot.Body,
        itemModelId = 0,
        config = ModelGltfImporterConfig(),
    ).apply()
}

class ItemModelGltfImporter(
    val race: RaceGenderConfig,
    val itemModelSlot: ItemModelSlot,
    val itemModelId: Int,
    val config: ModelGltfImporterConfig,
) {

    private val path = "Gltf-$race-$itemModelSlot-$itemModelId"

    fun apply() {
        RestoreFromBackup.run(race, itemModelSlot, itemModelId)

        val item = DatFile.itemModel(race, itemModelSlot, itemModelId)
        val itemRoot = DatTree.parse(item)

        val gltfFile = TempFile.getFile("$path/${config.modelFileName}")
        val gltfData = GltfImporter.get(gltfFile)

        ModelGltfImporter(itemRoot, gltfData).import()
        item.writeBytes(itemRoot.serialize())
        Log.info("Complete!")
    }

}