package ximtool.tools

import ximtool.dat.*
import ximtool.datresource.SkeletonMeshInstructions
import ximtool.gltf.GltfImporter
import ximtool.gltf.GltfToSkeletonMeshConfig
import ximtool.gltf.GltfToSkeletonMeshConverter
import ximtool.misc.Log
import ximtool.misc.LogColor
import ximtool.resource.TempFile

private val file = DatFile.romFile(version = 1, folder = 4, file = 127)

fun main() {
    file.restoreFromBackup()

    Log.info("Working on ${file.path}", LogColor.Green)
    NpcModelGltfImporter(file).apply()
}

class NpcModelGltfImporter(val resource: DatFile) {

    companion object {
        private const val path = "Gltf-Npc"
    }

    fun apply() {
        val resourceRoot = DatTree.parse(resource)
        resourceRoot.deleteRecursive(SectionType.S2A_SkeletonMesh)

        val gltfFile = TempFile.getFile("$path/model.gltf")
        val gltfData = GltfImporter.get(gltfFile)

        ModelGltfImporter(resourceRoot, gltfData).import()
        resource.writeBytes(resourceRoot.serialize())
    }

}