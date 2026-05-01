package ximtool.gltf

import de.javagl.jgltf.model.AnimationModel.Interpolation.LINEAR
import de.javagl.jgltf.model.impl.DefaultAccessorModel
import de.javagl.jgltf.model.impl.DefaultAnimationModel
import ximtool.dat.Skeleton
import ximtool.dat.SkeletonAnimation
import ximtool.datresource.SkeletonAnimationData
import ximtool.math.Quaternion
import ximtool.misc.Log
import ximtool.misc.LogColor

object GltfAnimationConverter {

    fun convert(context: GltfExporter, skeleton: Skeleton, animation: SkeletonAnimation): DefaultAnimationModel {
        return GltfAnimationConverterInstance(context, skeleton, animation).convert()
    }

}

private class GltfAnimationConverterInstance(val context: GltfExporter, val skeleton: Skeleton, val animation: SkeletonAnimation) {

    private val animationModel = DefaultAnimationModel()
    private val keyFrameTimestamps: DefaultAccessorModel

    init {
        animationModel.name = animation.datId.id
        val keyFrameIndices = (0 until animation.numFrames).map { it.toFloat() * animation.keyFrameDuration }
        keyFrameTimestamps = context.allocator.allocateFloats(keyFrameIndices)
    }

    fun convert(): DefaultAnimationModel {
        for ((jointIndex, keyframes) in animation.jointKeyFrames) {
            addJointAnimat(jointIndex, keyframes)
        }

        return animationModel
    }

    private fun addJointAnimat(jointIndex: Int, keyFrames: SkeletonAnimationData.KeyFrames) {
        val jointNode = context.joints[jointIndex]
        if (jointNode == null) {
            Log.warn("Animation targets joint $jointIndex, but its node was not created!", LogColor.Red)
            return
        }

        val joint = skeleton.joints[jointIndex]

        val translations = keyFrames.keyFrames.map { joint.translation + it.translation }
        val translationAccessor = context.allocator.allocateVec3(translations)
        val translationSampler = DefaultAnimationModel.DefaultSampler(keyFrameTimestamps, LINEAR, translationAccessor)
        val translationChannel = DefaultAnimationModel.DefaultChannel(translationSampler, jointNode, "translation")

        val rotations = keyFrames.keyFrames.map { Quaternion.multiplyAndStore(it.rotation, joint.rotation, Quaternion()) }
        val rotationAccessor = context.allocator.allocateQuaternions(rotations)
        val rotationSampler = DefaultAnimationModel.DefaultSampler(keyFrameTimestamps, LINEAR, rotationAccessor)
        val rotationChannel = DefaultAnimationModel.DefaultChannel(rotationSampler, jointNode, "rotation")

        val scales = keyFrames.keyFrames.map { it.scale }
        val scaleAccessor = context.allocator.allocateVec3(scales)
        val scaleSampler = DefaultAnimationModel.DefaultSampler(keyFrameTimestamps, LINEAR, scaleAccessor)
        val scaleChannel = DefaultAnimationModel.DefaultChannel(scaleSampler, jointNode, "scale")

        animationModel.addChannel(translationChannel)
        animationModel.addChannel(rotationChannel)
        animationModel.addChannel(scaleChannel)
    }

}