package ximtool.tools

import de.javagl.jgltf.model.impl.DefaultGltfModel
import ximtool.dat.*
import ximtool.datresource.getCombinedSkeletonAnimations
import ximtool.datresource.getOnlySkeleton
import ximtool.datresource.getSkeletonMeshes
import ximtool.gltf.*

class ModelGltfExporter(
    val skeletonDirectory: Directory,
    val meshDirectory: Directory,
) {

    private val gltfExporter = GltfExporter()

    fun convert(): DefaultGltfModel {
        val skeleton = skeletonDirectory.getOnlySkeleton()
        gltfExporter.addSkeleton(skeleton)

        val animations = skeletonDirectory.getCombinedSkeletonAnimations()
        animations.forEach { gltfExporter.addSkeletonAnimation(skeleton, it) }

        val textures = meshDirectory.getTextures()
        textures.forEach { gltfExporter.addTexture(it) }

        val skeletonMeshes = meshDirectory.getSkeletonMeshes()
        skeletonMeshes.forEach { gltfExporter.addMeshModel(skeleton, it) }

        return gltfExporter.flushBuffer()
    }

}

class ModelGltfImporter(
    val meshDirectory: Directory,
    val gltfData: GltfData,
) {

    fun import() {
        meshDirectory.deleteRecursive(SectionType.S2A_SkeletonMesh)

        val groupedMeshes = gltfData.meshes.groupBy { MeshCombineKey(it.extras.datId ?: DatId("hh_c"), it.extras.occludeType) }
        val addedMeshes = groupedMeshes.map { (key, meshes) -> importGroup(key, meshes) }

        val textureFinder = GltfTextureRefenceResolver(gltfData.file.parentFile.path)
        textureFinder.resolveExternalReferences(meshDirectory, addedMeshes)
    }

    private fun importGroup(groupKey: MeshCombineKey, meshes: List<GltfMesh>): SkeletonMesh {
        val meshEntries = meshes.flatMap { mesh ->
            mesh.primitives.map { GltfSkeletonMeshEntry(mesh.extras, it) }
        }

        val config = GltfToSkeletonMeshConfig(
            datId = groupKey.datId,
            gltfMeshPrimitives = meshEntries,
            gltfData = gltfData,
            occludeType = groupKey.occludeType,
        )

        val skeletonMesh = GltfToSkeletonMeshConverter.convert(config)
        return meshDirectory.addChild(skeletonMesh)
    }

}

private data class MeshCombineKey(val datId: DatId, val occludeType: Int)