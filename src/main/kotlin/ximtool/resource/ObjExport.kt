package ximtool.resource

import ximtool.dat.Skeleton
import ximtool.dat.SkeletonMesh
import ximtool.dat.TextureName
import ximtool.datresource.SkeletonMeshInstructions
import ximtool.gltf.GltfConverter
import ximtool.gltf.GltfMeshPrimitive
import ximtool.math.Vector2f
import ximtool.math.Vector3f
import ximtool.misc.Log

class ObjExportConfig(
    val verticallyFlipUvs: Boolean = false
)

object ObjExport {

    fun toObj(objData: ObjData, config: ObjExportConfig = ObjExportConfig()): String {
        val lines = ArrayList<String>()

        objData.materialName?.let {
            lines += "mtllib ${it}.mtl\n"
        }

        for (v in objData.vertices) {
            lines += String.format("v %.6f %.6f %.6f", v.x, v.y, v.z)
        }

        for (v in objData.normals) {
            lines += String.format("vn %.6f %.6f %.6f", v.x, v.y, v.z)
        }

        for (v in objData.uvs) {
            val uv = if (config.verticallyFlipUvs) { Vector2f(v.x, 1f - v.y) } else { v }
            lines += String.format("vt %.6f %.6f", uv.x, uv.y)
        }

        objData.materialName?.let {
            lines += "\nusemtl ${it}\n"
        }

        for (f in objData.faces) {
            lines += String.format("f %d/%d/%d %d/%d/%d %d/%d/%d",
                f.indices[0].p, f.indices[0].n, f.indices[0].u,
                f.indices[1].p, f.indices[1].n, f.indices[1].u,
                f.indices[2].p, f.indices[2].n, f.indices[2].u,
                )
        }

        return lines.joinToString("\n")
    }

    fun toObjData(skeleton: Skeleton, skeletonMesh: SkeletonMesh): List<ObjData> {
        return SkeletonMeshToObj(skeleton, skeletonMesh).convert()
    }

    fun toString(mtl: ObjMtl): String {
        val body = StringBuilder()
        body.appendLine(mtl.ambient.format(prefix = "Ka", decimals = 3))
        body.appendLine(mtl.diffuse.format(prefix = "Kd", decimals = 3))
        body.appendLine(mtl.specularColor.format(prefix = "Ks", decimals = 3))
        body.appendLine(mtl.specularPower.format(prefix = "Ns", decimals = 3))

        mtl.ambientMap?.let { body.appendLine("map_Ka $it") }
        mtl.diffuseMap?.let { body.appendLine("map_Kd $it") }

        return "newmtl ${mtl.name}\n" + body.toString().prependIndent("  ")
    }

}

private class SkeletonMeshToObj(val skeleton: Skeleton, val skeletonMesh: SkeletonMesh) {

    fun convert(): List<ObjData> {
        val output = ArrayList<ObjData>()

        var currentMaterial: SkeletonMeshInstructions.MaterialInstruction? = null
        var currentTexture: TextureName? = null

        for (instruction in skeletonMesh.instructions) {
            when(instruction) {
                is SkeletonMeshInstructions.MaterialInstruction -> {
                    currentMaterial = instruction
                }
                is SkeletonMeshInstructions.TextureInstruction -> {
                    currentTexture = instruction.textureName
                }
                is SkeletonMeshInstructions.TriMeshInstruction -> {
                    val meshes = GltfConverter.toGltfPrimitives(skeleton, skeletonMesh, instruction)
                    output += meshes.map { meshToObj(it, currentTexture) }
                }
                is SkeletonMeshInstructions.TriStripInstruction -> {
                    Log.warn("TriStrip -> Obj not yet implemented")
                }
                is SkeletonMeshInstructions.UntexturedTriMeshInstruction -> {
                    Log.warn("Untextured TriMesh -> Obj not yet implemented")
                }
                SkeletonMeshInstructions.EndInstruction -> {
                    break
                }
            }
        }

        return output
    }

    private fun meshToObj(mesh: GltfMeshPrimitive, textureName: TextureName?): ObjData {
        val faces = ArrayList<Face>()
        for (i in mesh.indices.indices step 3) {
            faces += Face(arrayOf(
                VertexIndex(1 + mesh.indices[i + 0], 1 + mesh.indices[i + 0], 1 + mesh.indices[i + 0]),
                VertexIndex(1 + mesh.indices[i + 1], 1 + mesh.indices[i + 1], 1 + mesh.indices[i + 1]),
                VertexIndex(1 + mesh.indices[i + 2], 1 + mesh.indices[i + 2], 1 + mesh.indices[i + 2]),
            ))
        }

        return ObjData(
            vertices = mesh.vertices.map { it.position }.toMutableList(),
            normals = mesh.vertices.map { it.normal }.toMutableList(),
            uvs = mesh.vertices.map { it.uv }.toMutableList(),
            faces = faces,
            materialName = textureName?.value,
        )
    }

}

private fun Float.format(prefix: String, decimals: Int): String {
    return String.format("$prefix %.${decimals}f", this)
}

private fun Vector3f.format(prefix: String, decimals: Int): String {
    return String.format("$prefix %.${decimals}f %.${decimals}f %.${decimals}f", x, y, z)
}