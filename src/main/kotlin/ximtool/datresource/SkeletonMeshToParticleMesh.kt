package ximtool.datresource

import ximtool.dat.*
import ximtool.math.Vector2f
import ximtool.math.Vector3f

object SkeletonMeshToParticleMesh {

    fun convert(datId: DatId, skeletonMesh: SkeletonMesh): ParticleMesh {
        return SkeletonMeshToParticleMeshInstance(datId, skeletonMesh).convert()
    }

}

private class SkeletonMeshToParticleMeshInstance(val datId: DatId, val skeletonMesh: SkeletonMesh) {

    fun convert(): ParticleMesh {
        check(!skeletonMesh.flagHeader.cloth) { "Cloth meshes can't be converted into particles" }
        check(skeletonMesh.jointList.jointIndices.size == 1) { "Skeleton mesh should only attach to 1 joint" }

        val texturedEntries = ArrayList<ParticleMeshEntry>()
        val untexturedEntries = ArrayList<ParticleMeshEntry>()

        var currentTextureName: TextureName? = null

        for (instruction in skeletonMesh.instructions) {
            when (instruction) {
                is SkeletonMeshInstructions.MaterialInstruction -> {
                    // No-op
                }
                is SkeletonMeshInstructions.TextureInstruction -> {
                    currentTextureName = instruction.textureName
                }
                is SkeletonMeshInstructions.TriMeshInstruction -> {
                    val textureName = currentTextureName ?: throw IllegalStateException("Texture hasn't been set yet")
                    texturedEntries += ParticleMeshEntry(textureName, convertMeshInstruction(instruction).toMutableList())
                }
                is SkeletonMeshInstructions.TriStripInstruction -> {
                    val textureName = currentTextureName ?: throw IllegalStateException("Texture hasn't been set yet")
                    texturedEntries += ParticleMeshEntry(textureName, convertMeshInstruction(instruction).toMutableList())
                }
                is SkeletonMeshInstructions.UntexturedTriMeshInstruction -> {
                    untexturedEntries += ParticleMeshEntry(textureName = null, convertMeshInstruction(instruction).toMutableList())
                }
                SkeletonMeshInstructions.EndInstruction -> break
            }
        }

        return ParticleMesh(datId, texturedEntries + untexturedEntries)
    }

    private fun convertMeshInstruction(meshInstruction: SkeletonMeshInstructions.TriMeshInstruction): List<ParticleMeshVertex> {
        return meshInstruction.entries.flatMap { convertTriMeshEntry(it) }
    }

    private fun convertTriMeshEntry(entry: SkeletonMeshInstructions.TriMeshEntry): List<ParticleMeshVertex> {
        val vertex0 = skeletonMesh.vertexBuffer.singleJointVertices[entry.v0]
        val vertex1 = skeletonMesh.vertexBuffer.singleJointVertices[entry.v1]
        val vertex2 = skeletonMesh.vertexBuffer.singleJointVertices[entry.v2]

        return listOf(
            ParticleMeshVertex(
                position = Vector3f(vertex0.position),
                normal = Vector3f(vertex0.normal),
                color = ByteColor(ByteColor.half),
                uv = Vector2f(entry.uv0),
            ),
            ParticleMeshVertex(
                position = Vector3f(vertex1.position),
                normal = Vector3f(vertex1.normal),
                color = ByteColor(ByteColor.half),
                uv = Vector2f(entry.uv1),
            ),
            ParticleMeshVertex(
                position = Vector3f(vertex2.position),
                normal = Vector3f(vertex2.normal),
                color = ByteColor(ByteColor.half),
                uv = Vector2f(entry.uv2),
            ),
        )
    }

    private fun convertMeshInstruction(meshInstruction: SkeletonMeshInstructions.TriStripInstruction): List<ParticleMeshVertex> {
        val strip = ArrayDeque<ParticleMeshVertex>()
        val output = ArrayList<ParticleMeshVertex>()

        for (entry in meshInstruction.entries) {
            val entryData = skeletonMesh.vertexBuffer.singleJointVertices[entry.v]

            val vertex = ParticleMeshVertex(
                position = Vector3f(entryData.position),
                normal = Vector3f(entryData.normal),
                color = ByteColor(ByteColor.half),
                uv = Vector2f(entry.uv),
            )

            strip += vertex
            if (strip.size < 3) { continue }

            output += strip
            strip.removeFirst()
        }

        return output
    }

    private fun convertMeshInstruction(meshInstruction: SkeletonMeshInstructions.UntexturedTriMeshInstruction): List<ParticleMeshVertex> {
        return meshInstruction.entries.flatMap { convertTriMeshEntry(it) }
    }

    private fun convertTriMeshEntry(entry: SkeletonMeshInstructions.UntexturedTriMeshEntry): List<ParticleMeshVertex> {
        val vertex0 = skeletonMesh.vertexBuffer.singleJointVertices[entry.v0]
        val vertex1 = skeletonMesh.vertexBuffer.singleJointVertices[entry.v1]
        val vertex2 = skeletonMesh.vertexBuffer.singleJointVertices[entry.v2]

        return listOf(
            ParticleMeshVertex(
                position = Vector3f(vertex0.position),
                normal = Vector3f(vertex0.normal),
                color = ByteColor(entry.color),
                uv = Vector2f(),
            ),
            ParticleMeshVertex(
                position = Vector3f(vertex1.position),
                normal = Vector3f(vertex1.normal),
                color = ByteColor(entry.color),
                uv = Vector2f(),
            ),
            ParticleMeshVertex(
                position = Vector3f(vertex2.position),
                normal = Vector3f(vertex2.normal),
                color = ByteColor(entry.color),
                uv = Vector2f(),
            ),
        )
    }

}