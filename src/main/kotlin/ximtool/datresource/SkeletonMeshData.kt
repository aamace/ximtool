package ximtool.datresource

import ximtool.dat.ByteColor
import ximtool.dat.ByteReader
import ximtool.dat.TextureName
import ximtool.math.Vector2f
import ximtool.math.Vector3f

data class JointReference(val index: Int, val mirroredIndex: Int, val mirrorAxis: Int)

object SkeletonMeshInstructions {

    sealed interface Instruction {
        fun getSize(): Int
        fun write(buffer: ByteReader)
    }

    data class MaterialInstruction(
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

    data class TextureInstruction(val textureName: TextureName): Instruction {
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

    class TriStripInstruction(val numTriangles: Int, val entries: ArrayList<TriStripEntry>): Instruction {
        override fun getSize(): Int {
            return 0x04 + entries.size * 0x0A
        }

        override fun write(buffer: ByteReader) {
            buffer.write16(0x5453) // instruction op-code
            buffer.write16(numTriangles)

            val headEntries = entries.take(3)
            headEntries.forEach { buffer.write16(it.v) }
            headEntries.forEach { buffer.write(it.uv) }

            for (entry in entries.drop(3)) {
                buffer.write16(entry.v)
                buffer.write(entry.uv)
            }
        }
    }

    class TriStripEntry(
        val v: Int,
        val uv: Vector2f,
    )

    class UntexturedTriMeshInstruction(val numTriangles: Int, val entries: ArrayList<UntexturedTriMeshEntry>): Instruction {
        override fun getSize(): Int {
            return 0x04 + entries.size * 0x0A
        }

        override fun write(buffer: ByteReader) {
            buffer.write16(0x0043) // instruction op-code
            buffer.write16(numTriangles)

            for (entry in entries) {
                buffer.write16(entry.v0)
                buffer.write16(entry.v1)
                buffer.write16(entry.v2)
                buffer.writeBgra(entry.color)
            }
        }
    }

    class UntexturedTriMeshEntry(
        val v0: Int,
        val v1: Int,
        val v2: Int,
        val color: ByteColor,
    )

}

object SkeletonMeshData {

    data class OffsetCountPair(var offset: Int = 0, var count: Int = 0)

    data class FlagHeader(
        val maybeType: Int = 1,
        val hasJointArray: Boolean,
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

    class VertexBuffer(
        val singleJointVertices: List<SingleJointVertex>,
        val doubleJointVertices: List<DoubleJointVertex>,
    ) {
        operator fun get(index: Int): Vertex {
            val single = singleJointVertices.getOrNull(index)
            if (single != null) { return single }

            val adjustedIndex = index - singleJointVertices.size
            return doubleJointVertices[adjustedIndex]
        }
    }

    class JointReferenceEntry(
        val jointRef0: JointReference,
        val jointRef1: JointReference,
    )

    sealed interface Vertex

    class SingleJointVertex(
        val position: Vector3f,
        val normal: Vector3f,
    ): Vertex

    class DoubleJointVertex(
        val p0: Vector3f,
        val p1: Vector3f = Vector3f(),
        val n0: Vector3f,
        val n1: Vector3f = Vector3f(),
        val joint0Weight: Float = 1.0f,
        val joint1Weight: Float = 0.0f,
    ): Vertex

}
