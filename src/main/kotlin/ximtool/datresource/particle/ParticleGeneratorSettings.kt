package ximtool.datresource.particle

import ximtool.dat.DatId

enum class ActorScaleTarget {
    None, Source, Target
}

data class ActorScaleParams(
    var scaleSize: ActorScaleTarget,
    var scalePosition: ActorScaleTarget,
    var scaleSizeAmount: Float,
    var scalePositionAmount: Float,
)

enum class AttachType(val flag: Int) {
    None(0x0),
    SourceActor(0x1),
    TargetActor(0x2),
    SourceToTargetBasis(0x3),
    TargetActorSourceFacing(0x04),
    SourceActorTargetFacing(0x05),
    TargetToSourceBasis(0x6),
    SourceActorWeapon(0x9),
    ZoneActor0xA(0xA),
    ZoneActor0xB(0xB),
    ZoneActor0xC(0xC),
    Sun(0xE),
    Moon(0xF),
    ;

    companion object {
        fun from(flag: Int): AttachType? {
            return AttachType.values().firstOrNull { it.flag == flag }
        }
    }

}

class ParticleGeneratorHeader(
    var attachType: AttachType = AttachType.None,
    var attachedJoint0: Int = 0,
    var attachedJoint1: Int = 0,
    var attachSourceOriented: Boolean = false,
    var actorScaleParams: ActorScaleParams,
    var environmentId: DatId? = null,
    var emissionVariance: Int = 0,
    var framesPerEmission: Int = 0,
    var particlesPerEmission: Int = 0,
    var continuous: Boolean = false,
    var autoRun: Boolean = false,
    var batched: Boolean = false,
)
