package ximtool.resource

import ximtool.dat.*

class ObjToParticleMeshConfig(
    val datId: DatId,
    val objData: ObjData,
    val textureName: TextureName,
    val vertexColor: ByteColor = ByteColor(0x80, 0x80, 0x80, 0x80),
)

object ObjToParticleMeshConverter {
    fun convert(config: ObjToParticleMeshConfig): ByteArray {
        return ObjToParticleMesh(config).convert()
    }
}

private class ObjToParticleMesh(val config: ObjToParticleMeshConfig) {

    fun convert(): ByteArray {
        val obj = config.objData

        val size = (0x10 + 0x1E + config.objData.faces.size * 3 * 0x24).padTo16()
        val out = ByteReader(ByteArray(size))

        val header = SectionHeader(config.datId, SectionType.S1F_ParticleMesh, size)
        header.write(out)

        out.write32(0x06) // Version

        out.write8(1) // Number of meshes with textures
        out.write8(0) // Number of meshes without textures
        out.write16(obj.faces.size) // Total number of triangles

        // Triangles-per-mesh (array)
        out.write16(obj.faces.size)
        out.write16(0)
        out.write16(0)

        out.write(config.textureName)

        for (face in obj.faces) {
            for (index in face.indices) {
                val position = obj.vertices[index.p]
                out.write(position)

                val normal = obj.normals[index.n]
                out.write(normal)

                out.writeRgba(config.vertexColor)

                val uv = obj.uvs[index.u]
                out.write(uv)
            }
        }

        return out.bytes
    }

}