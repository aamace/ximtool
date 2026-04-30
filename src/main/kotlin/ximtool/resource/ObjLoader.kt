package ximtool.resource

import ximtool.math.Vector2f
import ximtool.math.Vector3f

class ObjLoaderConfig(
    val verticalFlipUvs: Boolean = false
)

object ObjLoader {

    fun load(path: String, config: ObjLoaderConfig = ObjLoaderConfig()): ObjData {
        val lines = ResourceReader.readLines(path)

        val obj = ObjData()

        for (line in lines) {
            val parts = line.split(" ")

            when (parts[0]) {
                "v" -> obj.vertices += toVector3f(parts)
                "vn" -> obj.normals += toVector3f(parts)
                "vt" -> obj.uvs += toVector2f(parts)
                "f" -> obj.faces += toVector3(parts)
            }
        }

        for (face in obj.faces) {
            for (index in face.indices) {
                obj.positionNormalPairing[index.p] = index.n
            }
        }

        if (config.verticalFlipUvs) {
            obj.uvs.forEach { it.y = 1f - it.y }
        }

        return obj
    }

    private fun toVector3f(arr: List<String>): Vector3f {
        return Vector3f(arr[1].toFloat(), arr[2].toFloat(), arr[3].toFloat())
    }

    private fun toVector2f(arr: List<String>): Vector2f {
        return Vector2f(arr[1].toFloat(), arr[2].toFloat())
    }

    private fun toVector3(arr: List<String>): Face {
        return Face(arrayOf(
            toIndices(arr[1]),
            toIndices(arr[2]),
            toIndices(arr[3]),
        ))
    }

    private fun toIndices(arr: String): VertexIndex {
        val parts = arr.split("/")
        return VertexIndex(parts[0].toInt() - 1, parts[2].toInt() - 1, parts[1].toInt() - 1,)
    }

}

class Face(val indices: Array<VertexIndex>)

class VertexIndex(
    val p: Int,
    val n: Int,
    val u: Int,
)

class ObjData(
    val vertices: MutableList<Vector3f> = ArrayList(),
    val normals: MutableList<Vector3f> = ArrayList(),
    val uvs: MutableList<Vector2f> = ArrayList(),
    val faces: MutableList<Face> = ArrayList(),
    val materialName: String? = null,
    val positionNormalPairing: MutableMap<Int, Int> = HashMap(),
)

data class ObjMtl(
    var name: String,
    var ambient: Vector3f = Vector3f(1f, 1f, 1f),
    var diffuse: Vector3f = Vector3f(1f, 1f, 1f),
    var specularColor: Vector3f = Vector3f(0f, 0f, 0f),
    var specularPower: Float = 0f,
    val ambientMap: String? = null,
    val diffuseMap: String? = ambientMap,
)