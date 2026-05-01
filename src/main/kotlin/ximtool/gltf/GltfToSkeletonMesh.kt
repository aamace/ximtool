package ximtool.gltf

import ximtool.dat.DatId
import ximtool.dat.SkeletonMesh
import ximtool.dat.TextureName
import ximtool.datresource.JointReference
import ximtool.datresource.SkeletonMeshData
import ximtool.datresource.SkeletonMeshInstructions
import ximtool.datresource.SkeletonMeshInstructions.TriMeshEntry
import ximtool.datresource.SkeletonMeshInstructions.TriMeshInstruction
import ximtool.math.Vector3f
import kotlin.math.abs

class GltfSkeletonMeshEntry(
    val extras: MeshExtras,
    val primitive: GltfMeshPrimitive,
)

class GltfToSkeletonMeshConfig(
    val datId: DatId,
    val gltfMeshPrimitives: List<GltfSkeletonMeshEntry>,
    val gltfData: GltfData,
    val occludeType: Int,
)

object GltfToSkeletonMeshConverter {
    fun convert(config: GltfToSkeletonMeshConfig): SkeletonMesh {
        return GltfToSkeletonMesh(config).convert()
    }
}

private class GltfToSkeletonMesh(val config: GltfToSkeletonMeshConfig) {

    private val jointMapper = JointMapper(config)
    private val vertexBuffer = VertexDataBuffer(config, jointMapper)

    fun convert(): SkeletonMesh {
        val flags = SkeletonMeshData.FlagHeader(
            hasJointArray = true,
            occlusionType = config.occludeType,
            mirrored = false,
            cloth = false,
        )

        val jointListSection = jointMapper.toList()
        val vertexCountSection = vertexBuffer.getCounts()
        val vertexJointMapSection = makeVertexJointMapSection()
        val vertexDataSection = makeVertexDataSection()
        val instructionSection = makeInstructionSection()

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
        val instructions = ArrayList<SkeletonMeshInstructions.Instruction>()

        var currentMaterialInstruction: SkeletonMeshInstructions.MaterialInstruction? = null
        var currentTextureInstruction: SkeletonMeshInstructions.TextureInstruction? = null

        for (primitiveEntry in config.gltfMeshPrimitives) {
            val materialInstruction = primitiveEntry.extras.toMaterialInstruction()
            if (currentMaterialInstruction != materialInstruction) {
                currentMaterialInstruction = materialInstruction
                instructions += materialInstruction
            }

            val textureInstruction = primitiveEntry.extras.toTextureInstruction()
            if (currentTextureInstruction != textureInstruction) {
                currentTextureInstruction = textureInstruction
                instructions += textureInstruction
            }

            instructions += primitiveEntry.primitive.toTriMeshInstruction()
        }

        instructions += SkeletonMeshInstructions.EndInstruction
        return instructions
    }

    private fun makeVertexJointMapSection(): List<SkeletonMeshData.JointReferenceEntry> {
        return vertexBuffer.getSingles().map { it.jointEntry } + vertexBuffer.getDoubles().map { it.jointEntry }
    }

    private fun makeVertexDataSection(): SkeletonMeshData.VertexBuffer {
        return SkeletonMeshData.VertexBuffer(
            singleJointVertices = vertexBuffer.getSingles().map { it.vertex },
            doubleJointVertices = vertexBuffer.getDoubles().map { it.vertex },
        )
    }

    private fun GltfMeshPrimitive.toTriMeshInstruction(): TriMeshInstruction {
        val triMeshEntries = ArrayList<TriMeshEntry>(indices.size / 3)

        for (i in indices.indices step 3) {
            val gltfV0 = vertices[indices[i+0]]
            val gltfV1 = vertices[indices[i+1]]
            val gltfV2 = vertices[indices[i+2]]

            triMeshEntries += TriMeshEntry(
                v0 = vertexBuffer[gltfV0],
                v1 = vertexBuffer[gltfV1],
                v2 = vertexBuffer[gltfV2],
                uv0 = gltfV0.uv,
                uv1 = gltfV1.uv,
                uv2 = gltfV2.uv,
            )
        }

        return TriMeshInstruction(numTriangles = indices.size/3, entries = triMeshEntries)
    }

}

private class JointMapper(val config: GltfToSkeletonMeshConfig) {

    private val arrayIndexLookup = LinkedHashMap<Int, Int>()

    init {
        config.gltfMeshPrimitives.forEach { mapPrimitive(it.primitive) }
    }

    operator fun get(jointIndex: Int) = arrayIndexLookup[jointIndex] ?: throw IllegalStateException("Missing joint remapping")

    fun toList(): SkeletonMeshData.JointListSection {
        val skeletonIndexLookup = config.gltfData.skeletonIndexLookup
        return SkeletonMeshData.JointListSection(jointIndices = arrayIndexLookup.keys.map { skeletonIndexLookup[it] })
    }

    private fun mapPrimitive(primitive: GltfMeshPrimitive) {
        val allIndices = (primitive.vertices.map { it.j0.index } + primitive.vertices.map { it.j1.index }).distinct()
        for (index in allIndices) { arrayIndexLookup.getOrPut(index) { arrayIndexLookup.size } }
        check(allIndices.size <= 127) { "The mesh is attached to too many joints: ${allIndices.size} (Max: 127)" }
    }

}

private class VertexDataBuffer(val config: GltfToSkeletonMeshConfig, val jointMapping: JointMapper) {

    private val gltfData = config.gltfData

    private val singles = LinkedHashMap<SinglesVertexData, Int>()
    private val doubles = LinkedHashMap<DoublesVertexData, Int>()

    init {
        storeAll()
    }

    operator fun get(gltfVertex: GltfVertex): Int {
        return if (isSingleJointed(gltfVertex)) {
            val data = toSingleData(gltfVertex)
            singles[data] ?: throw IllegalStateException("Single wasn't mapped")
        } else {
            val data = toDoubleData(gltfVertex)
            singles.size + (doubles[data] ?: throw IllegalStateException("Double wasn't mapped"))
        }
    }

    fun getCounts(): SkeletonMeshData.VertexCountSection {
        return SkeletonMeshData.VertexCountSection(
            singleJointedVertices = singles.size,
            doubleJointedVertices = doubles.size,
        )
    }

    fun getSingles(): List<SinglesVertexData> {
        return singles.keys.toList()
    }

    fun getDoubles(): List<DoublesVertexData> {
        return doubles.keys.toList()
    }

    private fun storeAll() {
        config.gltfMeshPrimitives.forEach { primitiveEntry ->
            primitiveEntry.primitive.vertices.forEach { store(it) }
        }
    }

    private fun store(gltfVertex: GltfVertex) {
        if (isSingleJointed(gltfVertex)) {
            val data = toSingleData(gltfVertex)
            singles.getOrPut(data) { singles.size }
        } else {
            val data = toDoubleData(gltfVertex)
            doubles.getOrPut(data) { doubles.size }
        }
    }

    private fun toSingleData(vertex: GltfVertex): SinglesVertexData {
        val joint = getSingleJoint(vertex)
        val inv = gltfData.inverseBindingMatrices[joint.index]

        val p = inv.transform(vertex.position, w = 1f)
        val n = inv.transform(vertex.normal, w = 0f)

        val vertexEntry = SkeletonMeshData.SingleJointVertex(position = p, normal = n)

        val jointArrayIndex = jointMapping[getSingleJoint(vertex).index]
        val jointEntry = SkeletonMeshData.JointReferenceEntry(
            JointReference(index = jointArrayIndex, mirroredIndex = 0, mirrorAxis = 1),
            JointReference(index = 0, mirroredIndex = 0, mirrorAxis = 0),
        )

        return SinglesVertexData(vertexEntry, jointEntry)
    }

    private fun toDoubleData(vertex: GltfVertex): DoublesVertexData {
        val inv = gltfData.inverseBindingMatrices

        val p0 = inv[vertex.j0.index].transform(vertex.position, 1f) * vertex.j0.weight
        val p1 = inv[vertex.j1.index].transform(vertex.position, 1f) * vertex.j1.weight

        val n0 = inv[vertex.j0.index].transform(vertex.normal, w = 0f) * vertex.j0.weight
        val n1 = inv[vertex.j1.index].transform(vertex.normal, w = 0f) * vertex.j1.weight

        val j0ArrayIndex = jointMapping[vertex.j0.index]
        val j1ArrayIndex = jointMapping[vertex.j1.index]

        val vertexEntry = SkeletonMeshData.DoubleJointVertex(
            p0 = p0,
            p1 = p1,
            n0 = n0,
            n1 = n1,
            joint0Weight = vertex.j0.weight,
            joint1Weight = vertex.j1.weight,
        )

        val jointEntry = SkeletonMeshData.JointReferenceEntry(
            jointRef0 = JointReference(j0ArrayIndex, mirroredIndex = 0, mirrorAxis = 1),
            jointRef1 = JointReference(j1ArrayIndex, mirroredIndex = 0, mirrorAxis = 1)
        )

        return DoublesVertexData(vertexEntry, jointEntry)
    }

}

private class SinglesVertexData(
    val vertex: SkeletonMeshData.SingleJointVertex,
    val jointEntry: SkeletonMeshData.JointReferenceEntry,
) {

    override fun equals(other: Any?): Boolean {
        val o = other as? SinglesVertexData ?: return false
        return fEquals(vertex.position, o.vertex.position) &&
                fEquals(vertex.normal, o.vertex.normal) &&
                jointEntry.jointRef0.index == o.jointEntry.jointRef0.index
    }

    override fun hashCode(): Int {
        return vertex.position.hashCode()
    }

}

private class DoublesVertexData(
    val vertex: SkeletonMeshData.DoubleJointVertex,
    val jointEntry: SkeletonMeshData.JointReferenceEntry,
) {

    override fun equals(other: Any?): Boolean {
        val o = other as? DoublesVertexData ?: return false

        return fEquals(vertex.p0, o.vertex.p0) &&
                fEquals(vertex.p1, o.vertex.p1) &&
                fEquals(vertex.n0, o.vertex.n0) &&
                fEquals(vertex.n1, o.vertex.n1) &&
                fEquals(vertex.joint0Weight, o.vertex.joint0Weight) &&
                fEquals(vertex.joint1Weight, o.vertex.joint1Weight) &&
                jointEntry.jointRef0.index == o.jointEntry.jointRef0.index &&
                jointEntry.jointRef1.index == o.jointEntry.jointRef1.index
    }

    override fun hashCode(): Int {
        return vertex.p0.hashCode() + 31 * vertex.p1.hashCode()
    }

}

private fun isSingleJointed(vertex: GltfVertex): Boolean {
    return vertex.j0.weight == 0f || vertex.j1.weight == 0f
}

private fun getSingleJoint(vertex: GltfVertex): GltfJoint {
    return if (vertex.j0.weight == 0f) { vertex.j1 } else { vertex.j0 }
}

private fun fEquals(a: Vector3f, b: Vector3f): Boolean {
    return Vector3f.distance(a, b) < 1e-3f
}

private fun fEquals(a: Float, b: Float): Boolean {
    return abs(a - b) < 1e-3f
}

private fun MeshExtras.toMaterialInstruction(): SkeletonMeshInstructions.MaterialInstruction {
    return SkeletonMeshInstructions.MaterialInstruction(
        displayType = displayType,
        ambientMultiplier = ambientMultiplier,
        specularHighlightPower = specularPower,
        specularHighlightEnabled = specularPower > 0f
    )
}

private fun MeshExtras.toTextureInstruction(): SkeletonMeshInstructions.TextureInstruction {
    return SkeletonMeshInstructions.TextureInstruction(textureName ?: TextureName.blank)
}