package ximtool.gltf

import de.javagl.jgltf.model.impl.DefaultMeshPrimitiveModel
import ximtool.math.Vector2f
import ximtool.math.Vector3f

enum class PrimitiveMode(val value: Int) {
    POINTS(0),
    LINES(1),
    LINE_LOOP(2),
    LINE_STRIP(3),
    TRIANGLES(4),
    TRIANGLE_STRIP(5),
    TRIANGLE_FAN(6),
}

class GltfJoint(
    val index: Int,
    val weight: Float,
)

class GltfVertex(
    val position: Vector3f,
    val normal: Vector3f,
    val uv: Vector2f,
    val j0: GltfJoint,
    val j1: GltfJoint,
)

class GltfMeshPrimitive(
    val vertices: List<GltfVertex>,
    val indices: List<Int>,
)


fun GltfMeshPrimitive.toModel(mode: PrimitiveMode, context: GltfExporter): DefaultMeshPrimitiveModel {
    val model = DefaultMeshPrimitiveModel(mode.value)

    val positions = context.allocator.allocateVec3(vertices.map { it.position })
    model.putAttribute("POSITION", positions)

    val normals = context.allocator.allocateVec3(vertices.map { it.normal })
    model.putAttribute("NORMAL", normals)

    val texcoord = context.allocator.allocateUvs(vertices.map { it.uv })
    model.putAttribute("TEXCOORD_0", texcoord)

    val joints = context.allocator.allocateJointIndices(vertices.map { it })
    model.putAttribute("JOINTS_0", joints)

    val weights = context.allocator.allocateWeights(vertices.map { it })
    model.putAttribute("WEIGHTS_0", weights)

    model.indices = context.allocator.allocateIndices(indices)

    return model
}