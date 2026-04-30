package ximtool.gltf

import de.javagl.jgltf.model.*
import de.javagl.jgltf.model.impl.DefaultAccessorModel
import de.javagl.jgltf.model.impl.DefaultBufferModel
import de.javagl.jgltf.model.impl.DefaultBufferViewModel
import de.javagl.jgltf.model.impl.DefaultGltfModel
import ximtool.dat.ByteReader
import ximtool.gltf.BufferTargets.ARRAY_BUFFER
import ximtool.gltf.BufferTargets.ELEMENT_ARRAY_BUFFER
import ximtool.math.Matrix4f
import ximtool.math.Quaternion
import ximtool.math.Vector2f
import ximtool.math.Vector3f
import java.nio.ByteBuffer
import java.nio.ByteOrder

private object ComponentTypes {
    const val BYTE = 5120
    const val UNSIGNED_BYTE = 5121
    const val SHORT = 5122
    const val UNSIGNED_SHORT = 5123
    const val UNSIGNED_INT = 5125
    const val FLOAT = 5126
}

private object BufferTargets {
    const val ARRAY_BUFFER = 34962
    const val ELEMENT_ARRAY_BUFFER = 34963
}

class GltfAllocator(private val root: DefaultGltfModel) {

    private val buffers = ArrayList<ByteArray>()
    private val bufferModel = DefaultBufferModel()

    private var totalAllocated = 0

    fun allocateIndices(v: List<Int>): DefaultAccessorModel {
        val data = ByteArray(v.size * 2)
        val br = ByteReader(data)
        v.forEach { br.write16(it) }

        val accessorModel = DefaultAccessorModel(ComponentTypes.UNSIGNED_SHORT, v.size, ElementType.SCALAR)
        val viewModel = allocate(data, ELEMENT_ARRAY_BUFFER)
        accessorModel.bufferViewModel = viewModel

        accessorModel.accessorData = AccessorShortData(
            ComponentTypes.UNSIGNED_SHORT,
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN),
            0,
            v.size,
            ElementType.SCALAR,
            viewModel.byteStride,
        )

        root.addAccessorModel(accessorModel)
        return accessorModel
    }

    fun allocateUvs(v: List<Vector2f>): DefaultAccessorModel {
        val data = ByteArray(v.size * 8)
        val br = ByteReader(data)
        v.forEach { br.write(it) }

        val accessorModel = DefaultAccessorModel(ComponentTypes.FLOAT, v.size, ElementType.VEC2)
        val viewModel = allocate(data, ARRAY_BUFFER)
        accessorModel.bufferViewModel = viewModel

        accessorModel.accessorData = AccessorFloatData(
            ComponentTypes.FLOAT,
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN),
            0,
            v.size,
            ElementType.VEC2,
            viewModel.byteStride,
        )

        root.addAccessorModel(accessorModel)
        return accessorModel
    }

    fun allocateVec3(v: List<Vector3f>): DefaultAccessorModel {
        val data = ByteArray(v.size * 12)
        val br = ByteReader(data)
        v.forEach { br.write(it) }

        val accessorModel = DefaultAccessorModel(ComponentTypes.FLOAT, v.size, ElementType.VEC3)
        val viewModel = allocate(data, ARRAY_BUFFER)
        accessorModel.bufferViewModel = viewModel

        accessorModel.accessorData = AccessorFloatData(
            ComponentTypes.FLOAT,
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN),
            0,
            v.size,
            ElementType.VEC3,
            viewModel.byteStride,
        )

        root.addAccessorModel(accessorModel)
        return accessorModel
    }

    fun allocateJointIndices(v: List<GltfVertex>): DefaultAccessorModel {
        val data = ByteArray(v.size * 4)
        val br = ByteReader(data)
        v.forEach {
            br.write8(it.j0.index)
            br.write8(it.j1.index)
            br.write8(0)
            br.write8(0)
        }

        val accessorModel = DefaultAccessorModel(ComponentTypes.UNSIGNED_BYTE, v.size, ElementType.VEC4)
        val viewModel = allocate(data, ARRAY_BUFFER)
        accessorModel.bufferViewModel = viewModel

        accessorModel.accessorData = AccessorByteData(
            ComponentTypes.UNSIGNED_BYTE,
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN),
            0,
            v.size,
            ElementType.VEC4,
            viewModel.byteStride,
        )

        root.addAccessorModel(accessorModel)
        return accessorModel
    }

    fun allocateWeights(v: List<GltfVertex>): DefaultAccessorModel {
        val data = ByteArray(v.size * 4 * 4)
        val br = ByteReader(data)
        v.forEach {
            br.writeFloat(it.j0.weight)
            br.writeFloat(it.j1.weight)
            br.writeFloat(0f)
            br.writeFloat(0f)
        }

        val accessorModel = DefaultAccessorModel(ComponentTypes.FLOAT, v.size, ElementType.VEC4)
        val viewModel = allocate(data, ARRAY_BUFFER)
        accessorModel.bufferViewModel = viewModel

        accessorModel.accessorData = AccessorFloatData(
            ComponentTypes.FLOAT,
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN),
            0,
            v.size,
            ElementType.VEC4,
            viewModel.byteStride,
        )

        root.addAccessorModel(accessorModel)
        return accessorModel
    }

    fun allocateQuaternions(v: List<Quaternion>): DefaultAccessorModel {
        val data = ByteArray(v.size * 4 * 4)
        val br = ByteReader(data)
        v.forEach {
            br.writeFloat(it.x)
            br.writeFloat(it.y)
            br.writeFloat(it.z)
            br.writeFloat(it.w)
        }

        val accessorModel = DefaultAccessorModel(ComponentTypes.FLOAT, v.size, ElementType.VEC4)
        val viewModel = allocate(data, ARRAY_BUFFER)
        accessorModel.bufferViewModel = viewModel

        accessorModel.accessorData = AccessorFloatData(
            ComponentTypes.FLOAT,
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN),
            0,
            v.size,
            ElementType.VEC4,
            viewModel.byteStride,
        )

        root.addAccessorModel(accessorModel)
        return accessorModel
    }

    fun allocateFloats(v: List<Float>): DefaultAccessorModel {
        val data = ByteArray(v.size * 4)
        val br = ByteReader(data)
        v.forEach { br.writeFloat(it) }

        val accessorModel = DefaultAccessorModel(ComponentTypes.FLOAT, v.size, ElementType.SCALAR)
        val viewModel = allocate(data, ARRAY_BUFFER)
        accessorModel.bufferViewModel = viewModel

        accessorModel.accessorData = AccessorFloatData(
            ComponentTypes.FLOAT,
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN),
            0,
            v.size,
            ElementType.SCALAR,
            viewModel.byteStride,
        )

        root.addAccessorModel(accessorModel)
        return accessorModel
    }

    fun allocateTransforms(t: ArrayList<Matrix4f>): AccessorModel {
        val data = ByteArray(t.size * 16 * 4)
        val br = ByteReader(data)
        t.forEach { br.write(it.m) }

        val accessorModel = DefaultAccessorModel(ComponentTypes.FLOAT, t.size, ElementType.MAT4)
        val viewModel = allocate(data, ARRAY_BUFFER)
        accessorModel.bufferViewModel = viewModel

        accessorModel.accessorData = AccessorFloatData(
            ComponentTypes.FLOAT,
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN),
            0,
            t.size,
            ElementType.MAT4,
            viewModel.byteStride,
        )

        root.addAccessorModel(accessorModel)
        return accessorModel
    }

    fun output(): DefaultBufferModel {
        val aggregateSize = buffers.sumOf { it.size }
        val aggregate = ByteArray(aggregateSize)

        val aggregateBuffer = ByteBuffer.wrap(aggregate)
        buffers.forEach { aggregateBuffer.put(it) }
        aggregateBuffer.flip()

        bufferModel.bufferData = aggregateBuffer
        return bufferModel
    }

    fun allocate(data: ByteArray, target: Int): DefaultBufferViewModel {
        val start = totalAllocated
        totalAllocated += data.size
        buffers += data

        val viewModel = DefaultBufferViewModel(target)
        viewModel.byteOffset = start
        viewModel.bufferModel = bufferModel
        viewModel.byteLength = data.size
        root.addBufferViewModel(viewModel)

        return viewModel
    }

}