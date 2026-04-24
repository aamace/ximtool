package ximtool.resource

import ximtool.dat.ByteColor
import ximtool.dat.DatId
import ximtool.dat.ParticleMesh
import ximtool.dat.TextureName
import ximtool.datresource.ParticleMeshEntry
import ximtool.datresource.ParticleMeshVertex

class ObjToParticleMeshConfig(
    val datId: DatId,
    val objData: ObjData,
    val textureName: TextureName,
    val vertexColor: ByteColor = ByteColor(0x80, 0x80, 0x80, 0x80),
)

object ObjToParticleMeshConverter {
    fun convert(config: ObjToParticleMeshConfig): ParticleMesh {
        return ObjToParticleMesh(config).convert()
    }
}

private class ObjToParticleMesh(val config: ObjToParticleMeshConfig) {

    fun convert(): ParticleMesh {
        val obj = config.objData

        val vertices = ArrayList<ParticleMeshVertex>()

        for (face in obj.faces) {
            for (index in face.indices) {
                vertices += ParticleMeshVertex(
                    position = obj.vertices[index.p],
                    normal = obj.normals[index.n],
                    color = config.vertexColor,
                    uv = obj.uvs[index.u],
                )
            }
        }

        val entry = ParticleMeshEntry(textureName = config.textureName, vertices = vertices)
        return ParticleMesh(config.datId, listOf(entry))
    }

}