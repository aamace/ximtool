package ximtool.datresource

import ximtool.dat.*

object SkeletonMeshSerializer {

    fun serialize(skeletonMesh: SkeletonMesh): ByteArray {
        return SkeletonMeshWriterInstance(skeletonMesh).convert()
    }

}

private class SkeletonMeshWriterInstance(val skeletonMesh: SkeletonMesh) {

    fun convert(): ByteArray {
        val offsets = SkeletonMeshData.OffsetHeader()
        var writePosition = 0x40

        val instructionSection = makeInstructionSection(skeletonMesh.instructions)
        offsets.instructionOffset.offset = writePosition
        offsets.instructionOffset.count = instructionSection.bytes.size / 2
        writePosition += instructionSection.bytes.size

        val jointListSection = makeJointListSection()
        offsets.jointArrayOffset.offset = writePosition
        offsets.jointArrayOffset.count = skeletonMesh.jointList.jointIndices.size
        writePosition += jointListSection.bytes.size

        val vertexCountSection = makeVertexCountSection()
        offsets.vertexCountsOffset.offset = writePosition
        offsets.vertexCountsOffset.count = 2
        writePosition += vertexCountSection.bytes.size

        val vertexJointMapSection = makeVertexJointMapSection()
        offsets.vertexJointMappingOffset.offset = writePosition
        offsets.vertexJointMappingOffset.count = 2 * skeletonMesh.jointReferenceBuffer.size
        writePosition += vertexJointMapSection.bytes.size

        val vertexDataSection = makeVertexDataSection()
        offsets.vertexDataOffset.offset = writePosition
        offsets.vertexDataOffset.count = vertexDataSection.bytes.size / 2
        writePosition += vertexDataSection.bytes.size

        offsets.endOffset.offset = writePosition

        // Serialize
        val finalSize = 0x10 + writePosition.padTo16()
        val outputBuffer = ByteReader(ByteArray(finalSize))

        writeSectionHeader(outputBuffer)
        writeFlagHeader(skeletonMesh.flagHeader, outputBuffer)
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
        val jointListSection = skeletonMesh.jointList

        val br = ByteReader(ByteArray(size = 2 * jointListSection.jointIndices.size))
        for (joint in jointListSection.jointIndices) { br.write16(joint) }
        return br
    }

    private fun makeVertexCountSection(): ByteReader {
        val vertexCountSection = skeletonMesh.vertexCountSection

        val br = ByteReader(ByteArray(size = 4))
        br.write16(vertexCountSection.singleJointedVertices)
        br.write16(vertexCountSection.doubleJointedVertices)
        return br
    }

    private fun makeVertexJointMapSection(): ByteReader {
        val size = 0x04 * skeletonMesh.jointReferenceBuffer.size
        val br = ByteReader(ByteArray(size))

        for (ref in skeletonMesh.jointReferenceBuffer) {
            br.write16(packJoint(ref.jointRef0))
            br.write16(packJoint(ref.jointRef1))
        }

        if (br.hasMore()) { throw IllegalStateException("Didn't fill vertex joint map!") }
        return br
    }

    private fun makeVertexDataSection(): ByteReader {
        val singlesSize = skeletonMesh.vertexBuffer.singleJointVertices.size * 24
        val doublesSize = skeletonMesh.vertexBuffer.doubleJointVertices.size * 56
        val br = ByteReader(ByteArray(singlesSize + doublesSize))

        for (single in skeletonMesh.vertexBuffer.singleJointVertices) {
            br.write(single.position)
            br.write(single.normal)
        }

        for (double in skeletonMesh.vertexBuffer.doubleJointVertices) {
            br.writeFloat(double.p0.x)
            br.writeFloat(double.p1.x)
            br.writeFloat(double.p0.y)
            br.writeFloat(double.p1.y)
            br.writeFloat(double.p0.z)
            br.writeFloat(double.p1.z)

            br.writeFloat(double.joint0Weight)
            br.writeFloat(double.joint1Weight)

            br.writeFloat(double.n0.x)
            br.writeFloat(double.n1.x)
            br.writeFloat(double.n0.y)
            br.writeFloat(double.n1.y)
            br.writeFloat(double.n0.z)
            br.writeFloat(double.n1.z)
        }

        if (br.hasMore()) { throw IllegalStateException("Didn't fill vertex data map!") }
        return br
    }

    private fun writeSectionHeader(byteReader: ByteReader) {
        val header = SectionHeader(skeletonMesh.datId, SectionType.S2A_SkeletonMesh, byteReader.bytes.size)
        header.write(byteReader)
    }

    private fun writeFlagHeader(flagHeader: SkeletonMeshData.FlagHeader, byteReader: ByteReader) {
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

    private fun writeOffsetHeader(offsetHeader: SkeletonMeshData.OffsetHeader, byteReader: ByteReader) {
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

    private fun packJoint(jointReference: JointReference): Int {
        return (jointReference.index and 0x7F) +
                (jointReference.mirroredIndex shl 0x7) +
                (jointReference.mirrorAxis shl 0xE)
    }

}