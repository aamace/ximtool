package ximtool.datresource

import ximtool.dat.Skeleton
import ximtool.math.Matrix4f

class SkeletonInstance(private val skeleton: Skeleton) {

    private val joints = ArrayList<JointInstance>()

    init {
        for (i in skeleton.joints.indices) {
            val jointDef = skeleton.joints[i]
            val parent = if (jointDef.parentIndex == -1) { null } else { joints[jointDef.parentIndex] }
            joints.add(JointInstance(i, parent, skeleton.joints[i], Matrix4f()))
        }
    }

    fun tPose() {
        identity()

        for (i in joints.indices) {
            val joint = joints[i]
            joint.currentTransform.translateInPlace(joint.definition.translation)
            joint.currentTransform.multiplyInPlace(joint.definition.rotation.toMat4())
            joint.parent?.currentTransform?.multiply(joint.currentTransform, joint.currentTransform)
        }
    }

    fun getAll(): List<JointInstance> {
        return joints
    }

    operator fun get(index: Int): JointInstance {
        return joints[index]
    }

    private fun identity() {
        joints.forEach { it.currentTransform.identity() }
    }

    class JointInstance(
        val index: Int,
        val parent: JointInstance?,
        val definition: SkeletonData.Joint,
        val currentTransform: Matrix4f,
    )
}
