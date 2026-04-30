package ximtool.datresource

import ximtool.dat.*
import ximtool.datresource.SkeletonData.BoundingBox
import ximtool.datresource.SkeletonData.Joint
import ximtool.datresource.SkeletonData.WrappedJoint
import ximtool.math.Quaternion
import ximtool.math.Vector2f
import ximtool.math.Vector3f

object SkeletonSection {

    fun read(data: ByteArray): Skeleton {
        return read(ByteReader(data))
    }

    private fun read(byteReader: ByteReader): Skeleton {
        val sectionHeader = SectionHeader.read(byteReader)
        val dataStart = byteReader.position

        byteReader.position = dataStart + 0x02
        val numJoints = byteReader.next8()

        val joints = ArrayList<Joint>()
        val jointReferences = ArrayList<WrappedJoint>()
        val boundingBoxes = ArrayList<BoundingBox>()

        byteReader.position = dataStart + 0x04
        for (i in 0 until numJoints) {
            val maybeParentIndex = byteReader.next8()
            val parentIndex = if (maybeParentIndex == i) { -1 } else { maybeParentIndex }

            byteReader.position += 1

            val rotation = Quaternion(byteReader.nextFloat(), byteReader.nextFloat(), byteReader.nextFloat(), byteReader.nextFloat())
            val translation = Vector3f(byteReader.nextFloat(), byteReader.nextFloat(), byteReader.nextFloat())

            joints.add(Joint(rotation, translation, parentIndex))
        }

        val numReferences = byteReader.next16()
        val unk1 = byteReader.next16() // Usually -1?

        for (i in 0 until numReferences) {
            val start = byteReader.position

            val jointIndex = byteReader.next16()
            val unkV0 = byteReader.nextVector3f()
            val offset = byteReader.nextVector3f()

            jointReferences.add(WrappedJoint(jointIndex, unkV0, offset, fileOffset = start))
        }

        while (byteReader.position < sectionHeader.sectionSize) {
            val yMax = getNextFloat(byteReader) ?: break
            val yMin = getNextFloat(byteReader) ?: break

            val xMax = getNextFloat(byteReader) ?: break
            val xMin = getNextFloat(byteReader) ?: break

            val zMax = getNextFloat(byteReader) ?: break
            val zMin = getNextFloat(byteReader) ?: break

            boundingBoxes += BoundingBox(
                width = Vector2f(xMin, xMax),
                height = Vector2f(yMin, yMax),
                depth = Vector2f(zMin, zMax),
            )
        }

        return Skeleton(
            datId = sectionHeader.sectionId,
            joints = joints,
            jointReferences = jointReferences,
            boundingBoxes = boundingBoxes,
            raw = byteReader.bytes,
        )
    }

    private fun getNextFloat(byteReader: ByteReader): Float? {
        // 0xCDCDCDCD is "invalid"
        val a = byteReader.nextFloat()
        return if (a == Float.fromBits(-842150451)) { null } else { a }
    }

}

object SkeletonData {

    data class Joint(val rotation: Quaternion, val translation: Vector3f, val parentIndex: Int)

    data class WrappedJoint(val index: Int, val unkV0: Vector3f, val positionOffset: Vector3f, val fileOffset: Int)

    data class BoundingBox(val width: Vector2f, val height: Vector2f, val depth: Vector2f)

}

fun Directory.getSkeleton(datId: DatId): Skeleton {
    return getChild(Skeleton::class, datId)
}

fun Directory.getOnlySkeleton(): Skeleton {
    return getChildren(Skeleton::class).single()
}