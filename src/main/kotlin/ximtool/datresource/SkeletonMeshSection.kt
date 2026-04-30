package ximtool.datresource

import ximtool.dat.*
import ximtool.math.Vector3f

object SkeletonMeshSection {

    fun read(data: ByteArray): SkeletonMesh {
        return read(ByteReader(data))
    }

    fun read(byteReader: ByteReader): SkeletonMesh {
        val sectionHeader = SectionHeader.read(byteReader)
        val dataStart = byteReader.position

        val flagHeader = readFlagHeader(byteReader)
        check(!flagHeader.cloth) { "Cloth meshes are not yet supported" }

        val offsetHeader = readOffsetHeader(byteReader)

        byteReader.position = dataStart + offsetHeader.instructionOffset.offset
        val instructions = readInstructions(byteReader, offsetHeader.instructionOffset.count)

        byteReader.position = dataStart + offsetHeader.jointArrayOffset.offset
        val jointList = readJointListSection(byteReader, offsetHeader.jointArrayOffset.count)

        byteReader.position = dataStart + offsetHeader.vertexCountsOffset.offset
        val vertexCounts = readVertexCountSection(byteReader, offsetHeader.vertexCountsOffset.count)

        byteReader.position = dataStart + offsetHeader.vertexJointMappingOffset.offset
        val jointBuffer = readJointBuffer(byteReader, offsetHeader.vertexJointMappingOffset.count)

        byteReader.position = dataStart + offsetHeader.vertexDataOffset.offset
        val vertexBuffer = readVertexBuffer(byteReader, vertexCounts)

        return SkeletonMesh(
            datId = sectionHeader.sectionId,
            flagHeader = flagHeader,
            instructions = instructions,
            jointList = jointList,
            vertexCountSection = vertexCounts,
            jointReferenceBuffer = jointBuffer,
            vertexBuffer = vertexBuffer,
        )

    }

    private fun readFlagHeader(byteReader: ByteReader): SkeletonMeshData.FlagHeader {
        val maybeType = byteReader.next16()

        val flags1 = byteReader.next8()
        val clothEffect = (flags1 and 0x01) != 0
        val useJointArray = (flags1 and 0x80) != 0

        val occludeType = byteReader.next8()

        val flags2 = byteReader.next8()
        val mirrored = flags2 == 0x01

        val flags3 = byteReader.next8()

        return SkeletonMeshData.FlagHeader(
            maybeType = maybeType,
            hasJointArray = useJointArray,
            occlusionType = occludeType,
            mirrored = mirrored,
            cloth = clothEffect,
        )
    }

    private fun readOffsetHeader(byteReader: ByteReader): SkeletonMeshData.OffsetHeader {
        val instructionOffset = 2 * byteReader.next32()
        val instructionSectionSize = byteReader.next16()

        val jointArrayOffset = 2 * byteReader.next32()
        val numJoints = byteReader.next16()

        val vertexCountsOffset = 2 * byteReader.next32()
        val numVertexCounts = byteReader.next16()

        val vertexJointMappingOffset = 2 * byteReader.next32()
        val numMappings = byteReader.next16()

        val vertexDataOffset = 2 * byteReader.next32()
        val vertexDataSize = 2 * byteReader.next16()

        val endOffset = 2 * byteReader.next32()
        val endOffsetDataSize = byteReader.next16()

        return SkeletonMeshData.OffsetHeader(
            instructionOffset = SkeletonMeshData.OffsetCountPair(instructionOffset, instructionSectionSize),
            jointArrayOffset = SkeletonMeshData.OffsetCountPair(jointArrayOffset, numJoints),
            vertexCountsOffset = SkeletonMeshData.OffsetCountPair(vertexCountsOffset, numVertexCounts),
            vertexJointMappingOffset = SkeletonMeshData.OffsetCountPair(vertexJointMappingOffset, numMappings),
            vertexDataOffset = SkeletonMeshData.OffsetCountPair(vertexDataOffset, vertexDataSize),
            endOffset = SkeletonMeshData.OffsetCountPair(endOffset, endOffsetDataSize),
        )
    }

    private fun readInstructions(byteReader: ByteReader, sectionSize: Int): List<SkeletonMeshInstructions.Instruction> {
        val start = byteReader.position
        val instructions = ArrayList<SkeletonMeshInstructions.Instruction>()

        while (true) {
            val opCode = byteReader.next16()

            if (opCode == 0xFFFF) {
                instructions += SkeletonMeshInstructions.EndInstruction
                break
            } else if (opCode == 0x8010) {
                instructions += readMaterialInstruction(byteReader)
            } else if (opCode == 0x8000) {
                instructions += readTextureInstruction(byteReader)
            } else if (opCode == 0x0054) {
                instructions += readTriMeshInstruction(byteReader)
            } else if (opCode == 0x5453) {
                instructions += readTriStripInstruction(byteReader)
            } else if (opCode == 0x0043) {
                instructions += readUntexturedTriMeshInstruction(byteReader)
            } else {
                throw IllegalStateException("Unimplemented op-code [${opCode.toString(0x10)}] @ $byteReader")
            }
        }

        check(byteReader.position == start + 2 * sectionSize)
        return instructions
    }

    private fun readMaterialInstruction(byteReader: ByteReader): SkeletonMeshInstructions.MaterialInstruction {
        val tFactor = byteReader.nextBGRA()

        val f0 = byteReader.nextFloat()
        val f1 = byteReader.nextFloat()

        val flag0 = byteReader.next8()
        val displayType = byteReader.next8()
        val flag2 = byteReader.next8()
        val flag3 = byteReader.next8()

        val ambientMultiplier = byteReader.nextFloat()

        val unk0 = byteReader.next32()
        val unk1 = byteReader.next32()
        val unk2 = byteReader.next16()

        val f4 = byteReader.nextFloat()
        val unk3 = byteReader.next16()

        val specularHighlightPower = byteReader.nextFloat()
        val specularHighlightEnabled = byteReader.nextFloat() == 1.0f

        return SkeletonMeshInstructions.MaterialInstruction(
            color = tFactor,
            specularHighlightPower = specularHighlightPower,
            specularHighlightEnabled = specularHighlightEnabled,
            displayType = displayType,
            ambientMultiplier = ambientMultiplier,
        )
    }

    private fun readTextureInstruction(byteReader: ByteReader): SkeletonMeshInstructions.TextureInstruction {
        return SkeletonMeshInstructions.TextureInstruction(byteReader.nextTextureName())
    }

    private fun readTriMeshInstruction(byteReader: ByteReader): SkeletonMeshInstructions.TriMeshInstruction {
        val numTriangles = byteReader.next16()
        val meshVertices = ArrayList<SkeletonMeshInstructions.TriMeshEntry>(numTriangles)

        for (i in 0 until numTriangles) {
            val vert0 = byteReader.next16()
            val vert1 = byteReader.next16()
            val vert2 = byteReader.next16()

            val uv0 = byteReader.nextVector2f()
            val uv1 = byteReader.nextVector2f()
            val uv2 = byteReader.nextVector2f()

            meshVertices += SkeletonMeshInstructions.TriMeshEntry(vert0, vert1, vert2, uv0, uv1, uv2)
        }

        return SkeletonMeshInstructions.TriMeshInstruction(
            numTriangles = numTriangles,
            entries = meshVertices,
        )
    }

    private fun readTriStripInstruction(byteReader: ByteReader): SkeletonMeshInstructions.TriStripInstruction {
        val numTriangles = byteReader.next16()
        val numVertices = numTriangles + 2

        val entries = ArrayList<SkeletonMeshInstructions.TriStripEntry>(numVertices)

        val vert0 = byteReader.next16()
        val vert1 = byteReader.next16()
        val vert2 = byteReader.next16()

        val uv0 = byteReader.nextVector2f()
        val uv1 = byteReader.nextVector2f()
        val uv2 = byteReader.nextVector2f()

        entries += SkeletonMeshInstructions.TriStripEntry(vert0, uv0)
        entries += SkeletonMeshInstructions.TriStripEntry(vert1, uv1)
        entries += SkeletonMeshInstructions.TriStripEntry(vert2, uv2)

        for (i in 1 until numTriangles) {
            val vert = byteReader.next16()
            val uv = byteReader.nextVector2f()
            entries += SkeletonMeshInstructions.TriStripEntry(vert, uv)
        }

        return SkeletonMeshInstructions.TriStripInstruction(
            numTriangles = numTriangles,
            entries = entries,
        )
    }

    private fun readUntexturedTriMeshInstruction(byteReader: ByteReader): SkeletonMeshInstructions.UntexturedTriMeshInstruction {
        val numTriangles = byteReader.next16()
        val meshVertices = ArrayList<SkeletonMeshInstructions.UntexturedTriMeshEntry>(numTriangles)

        for (i in 0 until numTriangles) {
            val vert0 = byteReader.next16()
            val vert1 = byteReader.next16()
            val vert2 = byteReader.next16()
            val color = byteReader.nextBGRA()

            meshVertices += SkeletonMeshInstructions.UntexturedTriMeshEntry(vert0, vert1, vert2, color)
        }

        return SkeletonMeshInstructions.UntexturedTriMeshInstruction(
            numTriangles = numTriangles,
            entries = meshVertices,
        )
    }

    private fun readJointListSection(byteReader: ByteReader, numJoints: Int): SkeletonMeshData.JointListSection {
        val joints = ArrayList<Int>(numJoints)

        for(i in 0 until numJoints) {
            joints += byteReader.next16()
        }

        return SkeletonMeshData.JointListSection(joints)
    }

    private fun readVertexCountSection(byteReader: ByteReader, counts: Int): SkeletonMeshData.VertexCountSection {
        check(counts == 2)
        return SkeletonMeshData.VertexCountSection(
            singleJointedVertices = byteReader.next16(),
            doubleJointedVertices = byteReader.next16(),
        )
    }

    private fun readJointBuffer(byteReader: ByteReader, count: Int): List<SkeletonMeshData.JointReferenceEntry> {
        val entries = ArrayList<SkeletonMeshData.JointReferenceEntry>(count/2)

        for (i in 0 until count/2) {
            val joint0 = unpackJointRef(byteReader.next16())
            val joint1 = unpackJointRef(byteReader.next16())
            entries += SkeletonMeshData.JointReferenceEntry(joint0, joint1)
        }

        return entries
    }

    private fun readVertexBuffer(byteReader: ByteReader, vertexCountSection: SkeletonMeshData.VertexCountSection): SkeletonMeshData.VertexBuffer {
        val singles = ArrayList<SkeletonMeshData.SingleJointVertex>(vertexCountSection.singleJointedVertices)
        val doubles = ArrayList<SkeletonMeshData.DoubleJointVertex>(vertexCountSection.doubleJointedVertices)

        for (i in 0 until vertexCountSection.singleJointedVertices) {
            val p = byteReader.nextVector3f()
            val n = byteReader.nextVector3f()
            singles += SkeletonMeshData.SingleJointVertex(position = p, normal = n)
        }

        for (i in 0 until vertexCountSection.doubleJointedVertices) {
            val p0x = byteReader.nextFloat()
            val p1x = byteReader.nextFloat()
            val p0y = byteReader.nextFloat()
            val p1y = byteReader.nextFloat()
            val p0z = byteReader.nextFloat()
            val p1z = byteReader.nextFloat()

            val p0 = Vector3f(p0x, p0y, p0z)
            val p1 = Vector3f(p1x, p1y, p1z)

            val joint0Weight = byteReader.nextFloat()
            val joint1Weight = byteReader.nextFloat()

            val n0x = byteReader.nextFloat()
            val n1x = byteReader.nextFloat()
            val n0y = byteReader.nextFloat()
            val n1y = byteReader.nextFloat()
            val n0z = byteReader.nextFloat()
            val n1z = byteReader.nextFloat()

            val n0 = Vector3f(n0x, n0y, n0z)
            val n1 = Vector3f(n1x, n1y, n1z)

            doubles += SkeletonMeshData.DoubleJointVertex(
                p0 = p0,
                p1 = p1,
                n0 = n0,
                n1 = n1,
                joint0Weight = joint0Weight,
                joint1Weight = joint1Weight,
            )
        }

        return SkeletonMeshData.VertexBuffer(singles, doubles)
    }

    private fun unpackJointRef(data: Int): JointReference {
        return JointReference(
            index = (data and 0x7F),
            mirroredIndex = ((data shr 0x7) and 0x7F),
            mirrorAxis = ((data shr 0xE) and 0x3)
        )
    }

}

fun SkeletonMesh.getJointIndex(index: Int): Int {
    return if (flagHeader.hasJointArray) {
        jointList.jointIndices[index]
    } else {
        index
    }
}

fun SkeletonMeshInstructions.TriStripInstruction.convertToMesh(): SkeletonMeshInstructions.TriMeshInstruction {
    val convertedEntries = ArrayList<SkeletonMeshInstructions.TriMeshEntry>()
    val strip = ArrayDeque<SkeletonMeshInstructions.TriStripEntry>()

    for (entry in entries) {
        strip += entry
        if (strip.size < 3) { continue }

        convertedEntries += SkeletonMeshInstructions.TriMeshEntry(
            v0 = strip[0].v,
            v1 = strip[1].v,
            v2 = strip[2].v,
            uv0 = strip[0].uv,
            uv1 = strip[1].uv,
            uv2 = strip[2].uv,
        )

        strip.removeFirst()
    }

    return SkeletonMeshInstructions.TriMeshInstruction(numTriangles, convertedEntries)
}

fun Directory.getSkeletonMesh(datId: DatId): SkeletonMesh {
    return getChild(SkeletonMesh::class, datId)
}

fun Directory.getSkeletonMeshes(): List<SkeletonMesh> {
    return getChildren(SkeletonMesh::class)
}