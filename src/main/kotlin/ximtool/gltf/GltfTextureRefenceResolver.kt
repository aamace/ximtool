package ximtool.gltf

import ximtool.dat.DatId
import ximtool.dat.Directory
import ximtool.dat.SkeletonMesh
import ximtool.dat.TextureName
import ximtool.dat.addChild
import ximtool.dat.getTexturesRecursive
import ximtool.datresource.SkeletonMeshInstructions
import ximtool.misc.Log
import ximtool.misc.LogColor
import ximtool.resource.DdsToTexture
import ximtool.resource.TempFile

class GltfTextureRefenceResolver(val path: String) {

    fun resolveExternalReferences(itemRoot: Directory, addedMeshes: List<SkeletonMesh>) {
        addedMeshes.forEach { resolveExternalReferences(itemRoot, it) }
    }

    fun resolveExternalReferences(itemRoot: Directory, addedMesh: SkeletonMesh) {
        val existingTextures = itemRoot.getTexturesRecursive().map { it.header.name }.toSet()

        addedMesh.instructions.filterIsInstance<SkeletonMeshInstructions.TextureInstruction>()
            .map { it.textureName }
            .filter { !existingTextures.contains(it) }
            .forEach { resolveExternalReference(itemRoot, it) }
    }

    private fun resolveExternalReference(itemRoot: Directory, textureName: TextureName) {
        Log.info("Didn't find $textureName in [${itemRoot.datId}]. Checking $path for potential matches (Known types: .dds)", LogColor.Teal)

        val ddsFile = TempFile.getFile("$path/${textureName}.dds")
        if (ddsFile.exists()) {
            Log.info("\tFound ${ddsFile.name} - importing it.")
            itemRoot.addChild(DdsToTexture.convert(ddsFile, DatId("txt_"), textureName))
            return
        }

        Log.warn("\tDidn't find any matches. Model will not have any texture.")
    }

}