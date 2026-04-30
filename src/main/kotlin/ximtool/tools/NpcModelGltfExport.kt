package ximtool.tools

import de.javagl.jgltf.model.io.GltfModelWriter
import ximtool.dat.DatFile
import ximtool.dat.DatTree
import ximtool.dat.restoreFromBackup
import ximtool.misc.Log
import ximtool.misc.LogColor
import ximtool.resource.TempFile

private val file = DatFile.romFile(version = 1, folder = 4, file = 127)

fun main() {
    file.restoreFromBackup()

    Log.info("Working on ${file.path}", LogColor.Green)
    NpcModelGltfExporter(file).apply()
}

class NpcModelGltfExporter(val resource: DatFile) {

    fun apply() {
        val resourceRoot = DatTree.parse(resource)
        val gltfModel = ModelGltfExporter(resourceRoot, resourceRoot).convert()

        val file = TempFile.makeFile("Gltf-Npc/model.gltf")
        val writer = GltfModelWriter()
        writer.write(gltfModel, file)
    }

}