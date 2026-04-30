package ximtool.datresource

import ximtool.dat.*
import ximtool.datresource.SkeletonAnimationData.FrameSequence
import ximtool.datresource.SkeletonAnimationData.KeyFrame
import ximtool.datresource.SkeletonAnimationData.KeyFrames
import ximtool.math.Quaternion
import ximtool.math.Vector3f

object SkeletonAnimationSection {

    fun read(sectionData: ByteArray): SkeletonAnimation {
        val byteReader = ByteReader(sectionData)
        val sectionHeader = SectionHeader.read(byteReader)

        val unk0 = byteReader.next16()
        val numJoints = byteReader.next16()
        val numFrames = byteReader.next16()
        val keyFrameDuration = byteReader.nextFloat() // time-per-animation-frame

        val keyFrameDataOffset = byteReader.position
        val jointKeyFrames = HashMap<Int, KeyFrames>()

        for (i in 0 until numJoints) {
            val jointIndex = byteReader.next32()

            val rotationSequences = readKeyFrameSequences(4, numFrames, byteReader, keyFrameDataOffset)
            val translationSequences = readKeyFrameSequences(3, numFrames, byteReader, keyFrameDataOffset)
            val scaleSequences = readKeyFrameSequences(3, numFrames, byteReader, keyFrameDataOffset)

            if (rotationSequences.isEmpty() || translationSequences.isEmpty() || scaleSequences.isEmpty()) { continue }

            jointKeyFrames[jointIndex] = resolveSequences(numFrames, rotationSequences, translationSequences, scaleSequences)
        }

        return SkeletonAnimation(
            datId = sectionHeader.sectionId,
            numFrames = numFrames,
            keyFrameDuration = keyFrameDuration,
            jointKeyFrames = jointKeyFrames,
            raw = sectionData,
        )
    }

    private fun readKeyFrameSequences(amount: Int, numFrames: Int, byteReader: ByteReader, sequenceDataOffset: Int) : List<FrameSequence> {
        val offsets = byteReader.next32(amount)
        val constValues = byteReader.nextFloat(amount).map { it.rem(10_000f) }
        val sequences = ArrayList<FrameSequence>(amount)

        if (offsets.any { it < 0 }) { return emptyList() }

        for (i in 0 until amount) {
            if (offsets[i] == 0) {
                sequences.add(i, FrameSequence(floatArrayOf(constValues[i])))
            } else {
                sequences.add(i, fetchSequence(offsets[i], numFrames, byteReader, sequenceDataOffset))
            }
        }

        return sequences
    }

    private fun fetchSequence(index: Int, numFrames: Int, byteReader: ByteReader, sequenceDataOffset: Int) : FrameSequence {
        val originalPos = byteReader.position
        byteReader.position = sequenceDataOffset + index * 4

        val sequence = byteReader.nextFloat(numFrames)
        byteReader.position = originalPos

        return FrameSequence(sequence)
    }

    private fun resolveSequences(numFrames: Int, rotationSequences: List<FrameSequence>, translationSequences: List<FrameSequence>, scaleSequences: List<FrameSequence>): KeyFrames {
        val keyFrames = ArrayList<KeyFrame>(numFrames)

        for (frame in 0 until numFrames) {
            val rotation = Quaternion(
                rotationSequences[0].getValue(frame),
                rotationSequences[1].getValue(frame),
                rotationSequences[2].getValue(frame),
                rotationSequences[3].getValue(frame),
            )

            val translation = Vector3f(
                translationSequences[0].getValue(frame),
                translationSequences[1].getValue(frame),
                translationSequences[2].getValue(frame),
            )

            val scale = Vector3f(
                scaleSequences[0].getValue(frame),
                scaleSequences[1].getValue(frame),
                scaleSequences[2].getValue(frame),
            )

            keyFrames += KeyFrame(rotation, translation, scale)
        }

        return KeyFrames(keyFrames)
    }

}

object SkeletonAnimationData {

    class FrameSequence(private val frameValues: FloatArray) {
        fun getValue(frame: Int) : Float {
            return if (frameValues.size == 1) { frameValues[0] } else { frameValues[frame] }
        }
    }

    class KeyFrame(
        val rotation: Quaternion = Quaternion(0f, 0f, 0f, 1f),
        val translation: Vector3f = Vector3f(0f, 0f, 0f),
        val scale: Vector3f = Vector3f(1f,1f,1f),
    )

    class KeyFrames(
        val keyFrames: List<KeyFrame>
    )

}

fun Directory.getSkeletonAnimations(): List<SkeletonAnimation> {
    return getChildren(SkeletonAnimation::class)
}

fun Directory.getCombinedSkeletonAnimations(): List<SkeletonAnimation> {
    val animations = getSkeletonAnimations()
    val grouped = animations.groupBy { it.datId.id.take(3) }.values

    val out = ArrayList<SkeletonAnimation>()
    for (grouping in grouped) { out += combineAnimations(grouping) }

    return out
}

private fun combineAnimations(animations: List<SkeletonAnimation>): List<SkeletonAnimation> {
    if (animations.size == 1) { return animations }

    if (animations.distinctBy { it.numFrames }.size != 1) {
        return animations
    }

    if (animations.distinctBy { it.keyFrameDuration }.size != 1) {
        return animations
    }

    val aggregateKeyFrames = HashMap<Int, KeyFrames>()
    for (animation in animations) { aggregateKeyFrames.putAll(animation.jointKeyFrames) }

    val sample = animations.first()
    return listOf(SkeletonAnimation(
        datId = DatId(sample.datId.id.take(3) + "?"),
        keyFrameDuration = sample.keyFrameDuration,
        numFrames = sample.numFrames,
        jointKeyFrames = aggregateKeyFrames,
        raw = ByteArray(0),
    ))
}