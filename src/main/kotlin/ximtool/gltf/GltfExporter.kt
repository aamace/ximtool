package ximtool.gltf

import de.javagl.jgltf.model.MaterialModel
import de.javagl.jgltf.model.impl.*
import de.javagl.jgltf.model.v2.MaterialModelV2
import ximtool.dat.*
import ximtool.datresource.*
import ximtool.gltf.GltfConverter.toGltfPrimitives
import ximtool.math.Matrix3f
import ximtool.math.Matrix4f
import ximtool.math.Vector2f
import ximtool.math.Vector3f
import ximtool.misc.Log
import ximtool.resource.TextureToDds

class GltfExporter {

    val root = DefaultGltfModel()
    val allocator = GltfAllocator(root)

    val sceneModel = DefaultSceneModel()
    val textures = HashMap<TextureName, MaterialModel>()
    val skeletons = HashMap<DatId, DefaultSkinModel>()
    val joints = LinkedHashMap<Int, DefaultNodeModel>()

    init {
        root.addSceneModel(sceneModel)
    }

    fun addSkeleton(skeleton: Skeleton) {
        val instance = SkeletonInstance(skeleton)
        instance.tPose()

        val childrenNodes = HashMap<Int, ArrayList<DefaultNodeModel>>()
        val inverseBindMatrics = ArrayList<Matrix4f>()

        for (joint in instance.getAll()) {
            val definition = joint.definition
            val jointNode = DefaultNodeModel()

            jointNode.name = "joint-${joint.index}"
            jointNode.translation = floatArrayOf(definition.translation.x, definition.translation.y, definition.translation.z)
            jointNode.rotation = floatArrayOf(definition.rotation.x, definition.rotation.y, definition.rotation.z, definition.rotation.w)
            jointNode.extras = JointExtras(joint.index).serialize()

            root.addNodeModel(jointNode)
            joints[joint.index] = jointNode

            childrenNodes.getOrPut(joint.definition.parentIndex) { ArrayList() }.add(jointNode)
            inverseBindMatrics += joint.currentTransform.affineInverse()
        }

        for ((index, node) in joints) {
            val children = childrenNodes[index] ?: continue
            children.forEach { node.addChild(it) }
        }

        val skin = DefaultSkinModel()
        skin.name = skeleton.datId.id
        skin.inverseBindMatrices = allocator.allocateTransforms(inverseBindMatrics)
        joints.values.forEach { skin.addJoint(it) }
        root.addSkinModel(skin)

        val skeletonRoot = joints[0] ?: throw IllegalStateException("Root wasn't added?")
        sceneModel.addNode(skeletonRoot)

        skeletons[skeleton.datId] = skin
    }

    fun addMeshModel(skeleton: Skeleton, skeletonMesh: SkeletonMesh) {
        var currentTextureName: TextureName? = null
        var currentMaterial: SkeletonMeshInstructions.MaterialInstruction? = null
        val currentPrimitives = ArrayList<DefaultMeshPrimitiveModel>()
        val meshModels = ArrayList<DefaultMeshModel>()

        fun flushPrimitives() {
            if (currentPrimitives.isEmpty()) { return }
            meshModels += toMeshNode(currentPrimitives, skeletonMesh, currentMaterial, currentTextureName)
            currentPrimitives.clear()
        }

        for (instruction in skeletonMesh.instructions) {
            when (instruction) {
                is SkeletonMeshInstructions.MaterialInstruction -> {
                    if (shouldFlush(currentMaterial, instruction)) {
                        flushPrimitives()
                    }
                    currentMaterial = instruction
                }
                is SkeletonMeshInstructions.TextureInstruction -> {
                    if (shouldFlush(currentTextureName, instruction.textureName)) {
                        flushPrimitives()
                    }
                    currentTextureName = instruction.textureName
                }
                is SkeletonMeshInstructions.TriMeshInstruction -> {
                    currentPrimitives += toPrimitiveMeshNode(skeleton, skeletonMesh, instruction, currentTextureName)
                }
                is SkeletonMeshInstructions.TriStripInstruction -> {
                    currentPrimitives += toPrimitiveMeshNode(skeleton, skeletonMesh, instruction.convertToMesh(), currentTextureName)
                }
                SkeletonMeshInstructions.EndInstruction -> {
                    flushPrimitives()
                    break
                }
                else -> {
                    Log.warn("GLTF Unimplemented instruction: ${instruction::class.simpleName}")
                }
            }
        }

        root.addMeshModels(meshModels)
        meshModels.forEach { addToScene(skeleton, it) }
    }

    private fun shouldFlush(currentMaterial: SkeletonMeshInstructions.MaterialInstruction?, newMaterialInstruction: SkeletonMeshInstructions.MaterialInstruction): Boolean {
        return currentMaterial != newMaterialInstruction
    }

    private fun shouldFlush(currentTextureName: TextureName?, newTextureName: TextureName): Boolean {
        return currentTextureName != newTextureName
    }

    private fun toPrimitiveMeshNode(
        skeleton: Skeleton,
        skeletonMesh: SkeletonMesh,
        instruction: SkeletonMeshInstructions.TriMeshInstruction,
        textureName: TextureName?,
    ): List<DefaultMeshPrimitiveModel> {
        val primitives = toGltfPrimitives(skeleton, skeletonMesh, instruction)
        val primitiveModels = primitives.map { it.toModel(PrimitiveMode.TRIANGLES, this) }

        val materialModel = textures[textureName]
        if (materialModel != null) {
            primitiveModels.forEach { it.materialModel = materialModel }
        }

        return primitiveModels
    }

    private fun toMeshNode(
        primitiveModels: List<DefaultMeshPrimitiveModel>,
        skeletonMesh: SkeletonMesh,
        currentMaterial: SkeletonMeshInstructions.MaterialInstruction?,
        currentTextureName: TextureName?,
    ): DefaultMeshModel {
        check(currentMaterial != null) { "Material must be set before flushing primitives" }

        val meshModel = DefaultMeshModel()
        meshModel.name = skeletonMesh.datId.toString()
        primitiveModels.forEach { meshModel.addMeshPrimitiveModel(it) }

        meshModel.extras = MeshExtras(
            datId = skeletonMesh.datId,
            occludeType = skeletonMesh.flagHeader.occlusionType,
            displayType = currentMaterial.displayType,
            textureName = currentTextureName,
            specularPower = if (currentMaterial.specularHighlightEnabled) { currentMaterial.specularHighlightPower } else { 0f }
        ).serialize()

        return meshModel
    }

    fun addTexture(texture: Texture): MaterialModelV2 {
        val rawData = TextureToDds.convert(texture)
        val bufferViewModel = allocator.allocate(rawData, 0)

        val imageModel = DefaultImageModel()
        imageModel.uri = texture.header.name.value + ".dds"
        imageModel.mimeType = "image/vnd-ms.dds"
        imageModel.bufferViewModel = bufferViewModel
        root.addImageModel(imageModel)

        val textureModel = DefaultTextureModel()
        textureModel.imageModel = imageModel
        textureModel.magFilter = 9729
        textureModel.minFilter = 9729
        textureModel.wrapS = 10497
        textureModel.wrapT = 10497
        root.addTextureModel(textureModel)

        val materialModel = MaterialModelV2()
        materialModel.name = texture.header.name.value
        materialModel.baseColorTexture = textureModel
        root.addMaterialModel(materialModel)

        textures[texture.header.name] = materialModel
        return materialModel
    }

    fun addSkeletonAnimation(skeleton: Skeleton, animation: SkeletonAnimation) {
        root.addAnimationModel(GltfAnimationConverter.convert(this, skeleton, animation))
    }

    fun addToScene(skeleton: Skeleton, meshModel: DefaultMeshModel) {
        val node = DefaultNodeModel()
        node.skinModel = skeletons[skeleton.datId]
        node.addMeshModel(meshModel)
        sceneModel.addNode(node)
        root.addNodeModel(node)
    }

    fun flushBuffer(): DefaultGltfModel {
        root.addBufferModel(allocator.output())
        return root
    }

}

object GltfConverter {

    fun toGltfPrimitives(skeleton: Skeleton, skeletonMesh: SkeletonMesh, instruction: SkeletonMeshInstructions.TriMeshInstruction): List<GltfMeshPrimitive> {
        val output = ArrayList<GltfMeshPrimitive>()
        output += GltfConverterInstance(skeleton, skeletonMesh, mirrored = false).convert(instruction)

        if (skeletonMesh.flagHeader.mirrored) {
            output += GltfConverterInstance(skeleton, skeletonMesh, mirrored = true).convert(instruction)
        }

        return output
    }

}

private class GltfConverterInstance(val skeleton: Skeleton, val skeletonMesh: SkeletonMesh, val mirrored: Boolean) {

    private val skeletonInstance = SkeletonInstance(skeleton)
    private val normalTransforms: List<Matrix3f>

    init {
        skeletonInstance.tPose()
        normalTransforms = skeletonInstance.getAll().map {
            Matrix3f.truncate(it.currentTransform).invert().transpose()
        }
    }

    fun convert(instruction: SkeletonMeshInstructions.TriMeshInstruction): GltfMeshPrimitive {
        val vertices = ArrayList<GltfVertex>()
        val indices = ArrayList<Int>()

        val bufferedIndex = HashMap<VertexKey, Int>()

        for (entry in instruction.entries) {
            val j0 = skeletonMesh.jointReferenceBuffer[entry.v0]
            val j1 = skeletonMesh.jointReferenceBuffer[entry.v1]
            val j2 = skeletonMesh.jointReferenceBuffer[entry.v2]

            val i0 = bufferedIndex.getOrPut(VertexKey(entry.v0, entry.uv0)) {
                vertices += when (val vertex = skeletonMesh.vertexBuffer[entry.v0]) {
                    is SkeletonMeshData.SingleJointVertex -> toGltfVertex(vertex, entry.uv0, j0)
                    is SkeletonMeshData.DoubleJointVertex -> toGltfVertex(vertex, entry.uv0, j0)
                }
                bufferedIndex.size
            }

            val i1 = bufferedIndex.getOrPut(VertexKey(entry.v1, entry.uv1)) {
                vertices += when (val vertex = skeletonMesh.vertexBuffer[entry.v1]) {
                    is SkeletonMeshData.SingleJointVertex -> toGltfVertex(vertex, entry.uv1, j1)
                    is SkeletonMeshData.DoubleJointVertex -> toGltfVertex(vertex, entry.uv1, j1)
                }
                bufferedIndex.size
            }

            val i2 = bufferedIndex.getOrPut(VertexKey(entry.v2, entry.uv2)) {
                vertices += when (val vertex = skeletonMesh.vertexBuffer[entry.v2]) {
                    is SkeletonMeshData.SingleJointVertex -> toGltfVertex(vertex, entry.uv2, j2)
                    is SkeletonMeshData.DoubleJointVertex -> toGltfVertex(vertex, entry.uv2, j2)
                }
                bufferedIndex.size
            }

            indices += if (checkWinding(vertices, i0, i1, i2)) { listOf(i0, i1, i2) } else { listOf(i2, i1, i0) }
        }

        return GltfMeshPrimitive(vertices, indices)
    }

    private fun toGltfVertex(single: SkeletonMeshData.SingleJointVertex, uv: Vector2f, jointRef: SkeletonMeshData.JointReferenceEntry): GltfVertex {
        val jointIndex = skeletonMesh.getJointIndex(if (mirrored) { jointRef.jointRef0.mirroredIndex } else { jointRef.jointRef0.index })
        val jointTransform = skeletonInstance[jointIndex].currentTransform

        val position = if (mirrored) { mirror(single.position, jointRef.jointRef0) } else { single.position }
        val normal = if (mirrored) { mirror(single.normal, jointRef.jointRef0) } else { single.normal }

        val tposePosition = jointTransform.transform(position, w = 1f)
        val tposeNormal = jointTransform.transform(normal, w = 0f).normalizeInPlace()

        return GltfVertex(
            position = tposePosition,
            normal = tposeNormal,
            uv = uv,
            j0 = GltfJoint(jointIndex, weight = 1f),
            j1 = GltfJoint(0, weight = 0f),
        )
    }

    private fun toGltfVertex(double: SkeletonMeshData.DoubleJointVertex, uv: Vector2f, jointRef: SkeletonMeshData.JointReferenceEntry): GltfVertex {
        val j0Index = skeletonMesh.getJointIndex(if (mirrored) { jointRef.jointRef0.mirroredIndex } else { jointRef.jointRef0.index })
        val j0Transform = skeletonInstance[j0Index].currentTransform

        val j1Index = skeletonMesh.getJointIndex(if (mirrored) { jointRef.jointRef1.mirroredIndex } else { jointRef.jointRef1.index })
        val j1Transform = skeletonInstance[j1Index].currentTransform

        val p0 = if (mirrored) { mirror(double.p0, jointRef.jointRef0) } else { double.p0 }
        val n0 = if (mirrored) { mirror(double.n0, jointRef.jointRef0) } else { double.n0 }

        val p1 = if (mirrored) { mirror(double.p1, jointRef.jointRef1) } else { double.p1 }
        val n1 = if (mirrored) { mirror(double.n1, jointRef.jointRef1) } else { double.n1 }

        val tposePosition = j0Transform.transform(p0, w = double.joint0Weight) +
                j1Transform.transform(p1, w = double.joint1Weight)

        val tposeNormal = (j0Transform.transform(n0, w = 0f) * double.joint0Weight +
                j1Transform.transform(n1, w = 0f) * double.joint1Weight).normalizeInPlace()

        return GltfVertex(
            position = tposePosition,
            normal = tposeNormal,
            uv = uv,
            j0 = GltfJoint(j0Index, weight = double.joint0Weight),
            j1 = GltfJoint(j1Index, weight = double.joint1Weight),
        )
    }

    private fun mirror(original: Vector3f, flipType: JointReference): Vector3f {
        val mirrored = Vector3f(original)

        when (flipType.mirrorAxis) {
            1 -> { mirrored.x *= -1 }
            2 -> { mirrored.y *= -1 }
            3 -> { mirrored.z *= -1 }
        }

        return mirrored
    }

    private fun checkWinding(vertices: List<GltfVertex>, i0: Int, i1: Int, i2: Int): Boolean {
        val a = vertices[i0]
        val b = vertices[i1]
        val c = vertices[i2]

        val n = (a.normal + b.normal + c.normal).normalizeInPlace()
        val winding = (a.position - b.position).cross(b.position - c.position).normalizeInPlace().dot(n)
        return winding > 0
    }

    data class VertexKey(val index: Int, val uv: Vector2f)

}