package ximtool.gltf

import de.javagl.jgltf.model.*
import de.javagl.jgltf.model.io.GltfAssetReader
import ximtool.math.Matrix4f
import ximtool.math.Vector2f
import ximtool.math.Vector3f
import java.io.File

object GltfImporter {

    fun get(resource: File): GltfData {
        val asset = GltfAssetReader().read(resource.toURI())
        val model = GltfModels.create(asset)

        val skin = model.skinModels.first()
        val jointLookup = skin.joints.map { JointExtras.deserialize(it.extras).index  }
        val inverseBindingMatrices = readInverseBindMatrics(skin.inverseBindMatrices)

        val meshes = model.meshModels.map { handle(it) }
        return GltfData(resource, meshes, jointLookup, inverseBindingMatrices)
    }

    private fun handle(meshModel: MeshModel): GltfMesh {
        val primitives = meshModel.meshPrimitiveModels.map { handlePrimitive(it) }
        val extras = MeshExtras.deserialize(meshModel.extras)

        return GltfMesh(name = meshModel.name, primitives = primitives, extras = extras)
    }

    private fun handlePrimitive(primitive: MeshPrimitiveModel): GltfMeshPrimitive {
        check(primitive.mode == 4) { "Unimplemented mode: ${primitive.mode}" }

        val data = PrimitiveData()
        for ((attributeName, accessor) in primitive.attributes) {
            when (attributeName) {
                "POSITION" -> data.positions += readVec3(accessor)
                "NORMAL" -> data.normals += readVec3(accessor)
                "TEXCOORD_0" -> data.texCoords += readVec2(accessor)
                "JOINTS_0" -> data.joints += readJoints(accessor)
                "WEIGHTS_0" -> data.weights += readWeights(accessor)
            }
        }

        val indices = readIndices(primitive.indices)
        val vertices = ArrayList<GltfVertex>()

        for (i in data.positions.indices) {
            vertices += GltfVertex(
                position = data.positions[i],
                normal = data.normals[i],
                uv = data.texCoords[i],
                j0 = GltfJoint(data.joints[i].j0, data.weights[i].x),
                j1 = GltfJoint(data.joints[i].j1, data.weights[i].y),
            )
        }

        return GltfMeshPrimitive(vertices, indices)
    }

    private fun readVec2(accessor: AccessorModel): List<Vector2f> {
        check(accessor.elementType == ElementType.VEC2)
        check(accessor.componentDataType == Float::class.java)

        val data = accessor.accessorData as AccessorFloatData
        val output = ArrayList<Vector2f>()

        for (i in 0 until accessor.count) {
            output += Vector2f(data[i, 0], data[i, 1])
        }

        return output
    }

    private fun readVec3(accessor: AccessorModel): List<Vector3f> {
        check(accessor.elementType == ElementType.VEC3)
        check(accessor.componentDataType == Float::class.java)

        val data = accessor.accessorData as AccessorFloatData
        val output = ArrayList<Vector3f>()

        for (i in 0 until accessor.count) {
            output += Vector3f(data[i, 0], data[i, 1], data[i, 2])
        }

        return output
    }

    private fun readWeights(accessor: AccessorModel): List<Vector2f> {
        check(accessor.elementType == ElementType.VEC4)
        check(accessor.componentDataType == Float::class.java)

        val data = accessor.accessorData as AccessorFloatData
        val output = ArrayList<Vector2f>()

        for (i in 0 until accessor.count) {
            output += Vector2f(data[i, 0], data[i, 1])

            val w2 = data[i, 2]
            val w3 = data[i, 3]

            if (w2 != 0f || w3 != 0f) { throw IllegalStateException("Model specifies too many weights per vertex (max 2).") }
        }

        return output
    }

    private fun readJoints(accessor: AccessorModel): List<Joints> {
        check(accessor.elementType == ElementType.VEC4)

        val data = accessor.accessorData as AccessorByteData
        val output = ArrayList<Joints>()

        for (i in 0 until accessor.count) {
            output += Joints(
                data[i, 0].toInt(),
                data[i, 1].toInt(),
            )

            val j2 = data[i, 2].toInt()
            val j3 = data[i, 3].toInt()

            if (j2 != 0 || j3 != 0) { throw IllegalStateException("Model specifies too many joints per vertex (max 2).") }
        }

        return output
    }

    private fun readIndices(accessor: AccessorModel): List<Int> {
        check(accessor.elementType == ElementType.SCALAR)
        check(accessor.componentDataType == Short::class.java)

        val data = accessor.accessorData as AccessorShortData
        val output = ArrayList<Int>()

        for (i in 0 until accessor.count) {
            output += data[i, 0].toInt()
        }

        return output
    }

    private fun readInverseBindMatrics(accessor: AccessorModel): List<Matrix4f> {
        check(accessor.elementType == ElementType.MAT4)
        check(accessor.componentDataType == Float::class.java)

        val data = accessor.accessorData as AccessorFloatData
        val output = ArrayList<Matrix4f>()

        for (i in 0 until accessor.count) {
            val matrix4f = Matrix4f()
            for (j in 0 until 16 ) { matrix4f.m[j] = data[i, j] }
            output += matrix4f
        }

        return output
    }

    private class PrimitiveData(
        val positions: ArrayList<Vector3f> = ArrayList(),
        val normals: ArrayList<Vector3f> = ArrayList(),
        val texCoords: ArrayList<Vector2f> = ArrayList(),
        val joints: ArrayList<Joints> = ArrayList(),
        val weights: ArrayList<Vector2f> = ArrayList(),
    )

    private class Joints(
        val j0: Int,
        val j1: Int,
    )

}

class GltfMesh(
    val name: String,
    val primitives: List<GltfMeshPrimitive>,
    val extras: MeshExtras = MeshExtras(),
)

class GltfData(
    val file: File,
    val meshes: List<GltfMesh>,
    val jointLookup: List<Int>,
    val inverseBindingMatrices: List<Matrix4f>,
)