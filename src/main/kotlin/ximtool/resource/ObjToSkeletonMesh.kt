package ximtool.resource

import ximtool.dat.*
import ximtool.math.Vector2f
import ximtool.resource.SkeletonMeshData.FlagHeader
import ximtool.resource.SkeletonMeshData.OffsetHeader
import ximtool.resource.SkeletonMeshData.VertexCountSection
import ximtool.resource.SkeletonMeshInstructions.TriMeshEntry
import ximtool.resource.SkeletonMeshInstructions.TriMeshInstruction
import kotlin.math.PI

const val PI_f = PI.toFloat()

class ObjToSkeletonMeshConfig(
    val datId: DatId,
    val objData: ObjData,
    val joint: Int,
    val instructions: List<SkeletonMeshInstructions.Instruction>,
)

object ObjToSkeletonMeshConverter {
    fun convert(config: ObjToSkeletonMeshConfig): ByteArray {
        return ObjToSkeletonMesh(config).convert()
    }
}

private class ObjToSkeletonMesh(val config: ObjToSkeletonMeshConfig) {

    fun convert(): ByteArray {
        val flags = FlagHeader(
            hasJointArray = true,
            hasDoubleJointedVertices = false,
            occlusionType = 0,
            mirrored = false,
            cloth = false,
        )

        val offsets = OffsetHeader()
        var writePosition = 0x40

        val instructionSection = makeInstructionSection(config.instructions)
        offsets.instructionOffset.offset = writePosition
        offsets.instructionOffset.count = instructionSection.bytes.size
        writePosition += instructionSection.bytes.size

        val jointListSection = makeJointListSection()
        offsets.jointArrayOffset.offset = writePosition
        offsets.jointArrayOffset.count = 1
        writePosition += jointListSection.bytes.size

        val vertexCountSection = makeVertexCountSection()
        offsets.vertexCountsOffset.offset = writePosition
        offsets.vertexCountsOffset.count = 2
        writePosition += vertexCountSection.bytes.size

        val vertexJointMapSection = makeVertexJointMapSection()
        offsets.vertexJointMappingOffset.offset = writePosition
        offsets.vertexJointMappingOffset.count = config.objData.vertices.size
        writePosition += vertexJointMapSection.bytes.size

        val vertexDataSection = makeVertexDataSection()
        offsets.vertexDataOffset.offset = writePosition
        offsets.vertexDataOffset.count = config.objData.faces.size
        writePosition += vertexDataSection.bytes.size

        offsets.endOffset.offset = writePosition

        // Serialize
        val finalSize = 0x10 + writePosition.padTo16()
        val outputBuffer = ByteReader(ByteArray(finalSize))

        writeSectionHeader(outputBuffer)
        writeFlagHeader(flags, outputBuffer)
        writeOffsetHeader(offsets, outputBuffer)

        val outputBytes = outputBuffer.bytes

        instructionSection.bytes.copyInto(outputBytes, destinationOffset = 0x10 + offsets.instructionOffset.offset)
        jointListSection.bytes.copyInto(outputBytes, destinationOffset = 0x10 + offsets.jointArrayOffset.offset)
        vertexCountSection.bytes.copyInto(outputBytes, destinationOffset = 0x10 + offsets.vertexCountsOffset.offset)
        vertexJointMapSection.bytes.copyInto(outputBytes, destinationOffset = 0x10 + offsets.vertexJointMappingOffset.offset)
        vertexDataSection.bytes.copyInto(outputBytes, destinationOffset = 0x10 + offsets.vertexDataOffset.offset)

        return outputBytes
    }

    private fun makeInstructionSection(instructions: List<SkeletonMeshInstructions.Instruction>): ByteReader {
        check(instructions.last() == SkeletonMeshInstructions.EndInstruction) { "Expected last instruction to be 'End'" }

        val totalSize = instructions.sumOf { it.getSize() }
        val buffer = ByteReader(ByteArray(totalSize))

        instructions.forEach {
            val instructionStart = buffer.position
            it.write(buffer)

            if (buffer.position != instructionStart + it.getSize()) {
                throw IllegalStateException("Did not write expected amount of data for $it")
            }
        }

        return buffer
    }

    private fun makeJointListSection(): ByteReader {
        val jointListSection = SkeletonMeshData.JointListSection(jointIndices = listOf(config.joint))

        val br = ByteReader(ByteArray(size = 2 * jointListSection.jointIndices.size))
        for (joint in jointListSection.jointIndices) { br.write16(joint) }
        return br
    }

    private fun makeVertexCountSection(): ByteReader {
        val vertexCountSection = VertexCountSection(
            singleJointedVertices = config.objData.vertices.size,
            doubleJointedVertices = 0,
        )

        val br = ByteReader(ByteArray(size = 4))
        br.write16(vertexCountSection.singleJointedVertices)
        br.write16(vertexCountSection.doubleJointedVertices)
        return br
    }

    private fun makeVertexJointMapSection(): ByteReader {
        val jointIndex = 0
        val mirroredIndex = 0
        val mirrorAxis = 1

        val packedJointRef = (jointIndex and 0x7F) +
                (mirroredIndex shl 0x7) +
                (mirrorAxis shl 0xE)

        val size = 0x04 * config.objData.vertices.size
        val br = ByteReader(ByteArray(size))

        for (i in 0 until config.objData.vertices.size) {
            br.write16(packedJointRef) // Joint1
            br.write16(0) // Joint2 - not used in single-jointed vertices
        }

        if (br.hasMore()) { throw IllegalStateException("Didn't fill vertex joint map!") }
        return br
    }

    private fun makeVertexDataSection(): ByteReader {
        val objData = config.objData
        val size = objData.vertices.size * 12 * 2

        val br = ByteReader(ByteArray(size))
        for (i in objData.vertices.indices) {
            br.write(objData.vertices[i])
            val normalIndex = objData.positionNormalPairing[i] ?: throw IllegalStateException("Unpaired position $i")
            br.write(objData.normals[normalIndex])
        }

        if (br.hasMore()) { throw IllegalStateException("Didn't fill vertex data map!") }
        return br
    }

    private fun writeSectionHeader(byteReader: ByteReader) {
        val header = SectionHeader(config.datId, SectionType.S2A_SkeletonMesh, byteReader.bytes.size)
        header.write(byteReader)
    }

    private fun writeFlagHeader(flagHeader: FlagHeader, byteReader: ByteReader) {
        var flag1 = 0
        if (flagHeader.cloth) { flag1 += 0x01 }
        if (flagHeader.hasJointArray) { flag1 += 0x80 }

        var flag2 = 0
        if (flagHeader.mirrored) { flag2 += 0x01 }

        byteReader.write16(flagHeader.maybeType)
        byteReader.write8(flag1)
        byteReader.write8(flagHeader.occlusionType)
        byteReader.write8(flag2)
        byteReader.write8(0)
    }

    private fun writeOffsetHeader(offsetHeader: OffsetHeader, byteReader: ByteReader) {
        byteReader.write32(offsetHeader.instructionOffset.offset / 2)
        byteReader.write16(offsetHeader.instructionOffset.count)

        byteReader.write32(offsetHeader.jointArrayOffset.offset / 2)
        byteReader.write16(offsetHeader.jointArrayOffset.count)

        byteReader.write32(offsetHeader.vertexCountsOffset.offset / 2)
        byteReader.write16(offsetHeader.vertexCountsOffset.count)

        byteReader.write32(offsetHeader.vertexJointMappingOffset.offset / 2)
        byteReader.write16(offsetHeader.vertexJointMappingOffset.count)

        byteReader.write32(offsetHeader.vertexDataOffset.offset / 2)
        byteReader.write16(offsetHeader.vertexDataOffset.count)

        byteReader.write32(offsetHeader.endOffset.offset / 2)
        byteReader.write16(offsetHeader.endOffset.count)

        // TODO - skipped a duplicated end-offset field - is it needed?
        // Cloth data omitted
    }

}

object SkeletonMeshData {

    data class OffsetCountPair(var offset: Int = 0, var count: Int = 0)

    data class FlagHeader(
        val maybeType: Int = 1,
        val hasJointArray: Boolean,
        val hasDoubleJointedVertices: Boolean,
        val occlusionType: Int,
        val mirrored: Boolean,
        val cloth: Boolean,
    )

    data class OffsetHeader(
        val instructionOffset: OffsetCountPair = OffsetCountPair(),
        val jointArrayOffset: OffsetCountPair = OffsetCountPair(),
        val vertexCountsOffset: OffsetCountPair = OffsetCountPair(),
        val vertexJointMappingOffset: OffsetCountPair = OffsetCountPair(),
        val vertexDataOffset: OffsetCountPair = OffsetCountPair(),
        val endOffset: OffsetCountPair = OffsetCountPair(),
    )

    class VertexCountSection(
        val singleJointedVertices: Int,
        val doubleJointedVertices: Int,
    )

    class JointListSection(
        val jointIndices: List<Int>,
    )

}

object SkeletonMeshInstructions {

    sealed interface Instruction {
        fun getSize(): Int
        fun write(buffer: ByteReader)
    }

    class MaterialInstruction(
        val color: ByteColor = ByteColor(0x80, 0x80, 0x80, 0x00),
        val displayType: Int = 0, // Used for occlusion
        val ambientMultiplier: Float = 1f,
        val specularHighlightPower: Float = 128f,
        val specularHighlightEnabled: Boolean = false,
    ): Instruction {
        override fun getSize(): Int {
            return 0x2E
        }

        override fun write(buffer: ByteReader) {
            buffer.write16(0x8010) // instruction op-code

            buffer.writeRgba(color)

            buffer.writeFloat(4096f) // unknown, common value
            buffer.writeFloat(1f) // unknown, common value

            buffer.write8(0x00)
            buffer.write8(displayType)
            buffer.write8(0x00)
            buffer.write8(0x00)

            buffer.writeFloat(ambientMultiplier)
            buffer.write32(0) // unknown
            buffer.write32(0) // unknown

            buffer.write16(0) // unknown
            buffer.writeFloat(4096f) // unknown, common value
            buffer.write16(0) // unknown

            buffer.writeFloat(specularHighlightPower)
            buffer.writeFloat(if (specularHighlightEnabled) { 1f } else { 0f })
        }
    }

    class TextureInstruction(val textureName: TextureName): Instruction {
        override fun getSize(): Int {
            return 0x12
        }

        override fun write(buffer: ByteReader) {
            buffer.write16(0x8000) // instruction op-code
            buffer.write(textureName)
        }
    }

    object EndInstruction: Instruction {
        override fun getSize(): Int {
            return 0x02
        }

        override fun write(buffer: ByteReader) {
            buffer.write16(0xFFFF)
        }
    }

    class TriMeshInstruction(val numTriangles: Int, val entries: ArrayList<TriMeshEntry>): Instruction {
        override fun getSize(): Int {
            return 0x04 + entries.size * 0x1E
        }

        override fun write(buffer: ByteReader) {
            buffer.write16(0x0054) // instruction op-code
            buffer.write16(numTriangles)

            for (entry in entries) {
                buffer.write16(entry.v0)
                buffer.write16(entry.v1)
                buffer.write16(entry.v2)
                buffer.write(entry.uv0)
                buffer.write(entry.uv1)
                buffer.write(entry.uv2)
            }
        }
    }

    class TriMeshEntry(
        val v0: Int,
        val v1: Int,
        val v2: Int,
        val uv0: Vector2f,
        val uv1: Vector2f,
        val uv2: Vector2f,
    )

}

fun ObjData.toTriMeshInstruction(): TriMeshInstruction {
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