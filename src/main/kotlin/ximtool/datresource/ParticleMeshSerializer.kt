package ximtool.datresource

import ximtool.dat.*

object ParticleMeshSerializer {

    fun serialize(particleMesh: ParticleMesh): ByteArray {
        return ParticleMeshWriterInstance(particleMesh).convert()
    }

}

private class ParticleMeshWriterInstance(val mesh: ParticleMesh) {

    fun convert(): ByteArray {
        val numMeshesWithTextures = mesh.entries.count { it.textureName != null }
        val numMeshesWithoutTextures = mesh.entries.count { it.textureName == null }
        val totalMeshes = numMeshesWithTextures + numMeshesWithoutTextures

        val textureListSize = numMeshesWithTextures * 0x10

        val triArrayEntryCount = when {
            totalMeshes <= 3 -> 3
            totalMeshes <= 4 -> 4
            totalMeshes <= 7 -> 7
            totalMeshes <= 8 -> 8
            totalMeshes <= 11 -> 11
            totalMeshes <= 12 -> 12
            totalMeshes <= 15 -> 15
            else -> throw IllegalStateException("Too many meshes...? $numMeshesWithTextures.")
        }

        val triArraySize = triArrayEntryCount * 0x02
        val meshHeaderSize = 0x08 + triArraySize + textureListSize

        val totalVertexCount = mesh.entries.sumOf { it.vertices.size }
        val bodySize = totalVertexCount * 0x24

        val totalSize = (0x10 + meshHeaderSize + bodySize).padTo16()
        val out = ByteReader(ByteArray(totalSize))

        val header = SectionHeader(mesh.datId, SectionType.S1F_ParticleMesh, totalSize)
        header.write(out)

        out.write32(0x06) // Version

        out.write8(numMeshesWithTextures)
        out.write8(numMeshesWithoutTextures)

        val totalTriangles = mesh.entries.sumOf { it.vertices.size / 3 }
        out.write16(totalTriangles)

        // Triangles-per-mesh (array)
        for (i in 0 until triArrayEntryCount) {
            val count = mesh.entries.getOrNull(i)?.vertices?.size ?: 0
            out.write16(count / 3)
        }

        val textures = mesh.entries.mapNotNull { it.textureName }
        textures.forEach { out.write(it) }

        mesh.entries.forEach { writeEntry(out, it) }

        return out.bytes
    }

    private fun writeEntry(out: ByteReader, particleMeshEntry: ParticleMeshEntry) {
        for (vertex in particleMeshEntry.vertices) {
            out.write(vertex.position)
            out.write(vertex.normal)
            out.writeRgba(vertex.color)
            out.write(vertex.uv)
        }
    }

}