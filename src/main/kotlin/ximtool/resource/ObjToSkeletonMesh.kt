package ximtool.resource

import ximtool.dat.DatId
import ximtool.dat.SkeletonMesh
import ximtool.datresource.*
import ximtool.datresource.SkeletonMeshInstructions.TriMeshEntry
import ximtool.datresource.SkeletonMeshInstructions.TriMeshInstruction

class ObjToSkeletonMeshConfig(
    val datId: DatId,
    val objData: ObjData,
    val joint: Int,
    val materialInstruction: SkeletonMeshInstructions.MaterialInstruction,
    val textureInstruction: SkeletonMeshInstructions.TextureInstruction,
)

object ObjToSkeletonMeshConverter {
    fun convert(config: ObjToSkeletonMeshConfig): SkeletonMesh {
        return ObjToSkeletonMesh(config).convert()
    }
}

private class ObjToSkeletonMesh(val config: ObjToSkeletonMeshConfig) {

    fun convert(): SkeletonMesh {
        val flags = SkeletonMeshData.FlagHeader(
            hasJointArray = true,
            occlusionType = 0,
            mirrored = false,
            cloth = false,
        )

        val instructionSection = makeInstructionSection()
        val jointListSection = makeJointListSection()
        val vertexCountSection = makeVertexCountSection()
        val vertexJointMapSection = makeVertexJointMapSection()
        val vertexDataSection = makeVertexDataSection()

        return SkeletonMesh(
            datId = config.datId,
            flagHeader = flags,
            instructions = instructionSection,
            jointList = jointListSection,
            vertexCountSection = vertexCountSection,
            jointReferenceBuffer = vertexJointMapSection,
            vertexBuffer = vertexDataSection,
        )
    }

    private fun makeInstructionSection(): List<SkeletonMeshInstructions.Instruction> {
        return listOf(
            config.materialInstruction,
            config.textureInstruction,
            config.objData.toTriMeshInstruction(),
            SkeletonMeshInstructions.EndInstruction,
        )
    }

    private fun makeJointListSection(): SkeletonMeshData.JointListSection {
        return SkeletonMeshData.JointListSection(jointIndices = listOf(config.joint))
    }

    private fun makeVertexCountSection(): SkeletonMeshData.VertexCountSection {
        return SkeletonMeshData.VertexCountSection(
            singleJointedVertices = config.objData.vertices.size,
            doubleJointedVertices = 0,
        )
    }

    private fun makeVertexJointMapSection(): List<SkeletonMeshData.JointReferenceEntry> {
        val ref = SkeletonMeshData.JointReferenceEntry(
            JointReference(index = 0, mirroredIndex = 0, mirrorAxis = 1),
            JointReference(index = 0, mirroredIndex = 0, mirrorAxis = 0),
        )

        val buffer = ArrayList<SkeletonMeshData.JointReferenceEntry>(config.objData.vertices.size)
        for (i in 0 until config.objData.vertices.size) { buffer += ref }

        return buffer
    }

    private fun makeVertexDataSection(): SkeletonMeshData.VertexBuffer {
        val singles = ArrayList<SkeletonMeshData.SingleJointVertex>()

        for (i in config.objData.vertices.indices) {
            val normalIndex = config.objData.positionNormalPairing[i] ?: throw IllegalStateException("Unpaired position $i")
            val position = config.objData.vertices[i]
            val normal = config.objData.normals[normalIndex]
            singles += SkeletonMeshData.SingleJointVertex(position = position, normal = normal)
        }

        return SkeletonMeshData.VertexBuffer(
            singleJointVertices = singles,
            doubleJointVertices = emptyList(),
        )
    }

}

private fun ObjData.toTriMeshInstruction(): TriMeshInstruction {
    val triMeshEntries = ArrayList<TriMeshEntry>(faces.size)
    for (face in faces) {
        triMeshEntries += TriMeshEntry(
            v0 = face.indices[0].p, // 0x02
            v1 = face.indices[1].p, // 0x04
            v2 = face.indices[2].p, // 0x06
            uv0 = uvs[face.indices[0].u], // 0x0E
            uv1 = uvs[face.indices[1].u], // 0x16
            uv2 = uvs[face.indices[2].u], // 0x1E
        )
    }

    return TriMeshInstruction(numTriangles = faces.size, entries = triMeshEntries)
}