package ximtool.datresource.particle

import ximtool.dat.DatId

enum class ActorScaleTarget {
    None, Source, Target
}

data class ActorScaleParams(
    val scaleSize: ActorScaleTarget,
    val scalePosition: ActorScaleTarget,
    val scaleSizeAmount: Float,
    val scalePositionAmount: Float,
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
}

class ParticleGeneratorHeader(
    val attachType: AttachType = AttachType.None,
    val attachedJoint0: Int = 0,
    val attachedJoint1: Int = 0,
    val actorScaleParams: ActorScaleParams,
    val emissionVariance: Int = 0,
    val framesPerEmission: Int = 0,
    val particlesPerEmission: Int = 1,
    val continuous: Boolean = false,
)
