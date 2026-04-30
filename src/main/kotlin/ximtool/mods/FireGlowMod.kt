package ximtool.mods

import ximtool.dat.*
import ximtool.datresource.*
import ximtool.datresource.effectroutine.*
import ximtool.datresource.particle.*
import ximtool.math.Vector2f
import ximtool.math.Vector3f
import ximtool.misc.Log
import ximtool.misc.LogColor
import ximtool.misc.WeaponJointMapping
import ximtool.misc.WeaponType
import ximtool.tools.RestoreFromBackup

private const val routerModelId = 603 // Router, the pulse Great-Axe
private const val baseModelId = 783 // The weapon to add the glow effect to (Fire Tongue)
private const val destinationModelId = 268 // The weapon to be overwritten (Bronze Sword)

fun main() {
    RestoreFromBackup.runMainSub(destinationModelId)
    FireGlowMod.invoke()
}

private object FireGlowMod {
    fun invoke() {
        for (race in RaceGenderConfig.values()) {
            val jointConfig = WeaponJointMapping[race, WeaponType.Sword]

            Log.info("Working on ${race.name} main-hand", LogColor.Green)
            FireGlowApplier(WeaponModContext(race, jointConfig.mainHand, mainHand = true, ItemModelSlot.Main)).apply()

            Log.info("Working on ${race.name} off-hand", LogColor.Green)
            FireGlowApplier(WeaponModContext(race, jointConfig.offHand, mainHand = false, ItemModelSlot.Sub)).apply()
        }
    }
}

private class FireGlowApplier(val context: WeaponModContext) {

    fun apply() {
        val routerRoot = DatTree.fromItemModel(context.race, ItemModelSlot.Main, routerModelId)
        val routerEffectDirectory = routerRoot.getSubDirectory(DatId("06ro"))

        val baseRoot = DatTree.fromItemModel(context.race, context.itemModelSlot, baseModelId)
        val baseEffectDirectory = baseRoot.addSubDirectory(routerEffectDirectory)

        replaceParticleMesh(baseRoot, baseEffectDirectory)
        adjustAlphaKeyFrameValues(baseEffectDirectory)
        patchParticleEffects(baseEffectDirectory)
        patchRoutineEffects(baseEffectDirectory)
        makeOnHitEffect(baseRoot, baseEffectDirectory)

        val destinationFile = DatFile.itemModel(context.race, context.itemModelSlot, destinationModelId)
        destinationFile.writeBytes(baseRoot.serialize())
    }

    private fun replaceParticleMesh(baseTree: Directory, effectDirectory: Directory) {
        val skeletonMesh = baseTree.getSkeletonMesh(StandardDatIds.weaponMesh(context.mainHand))
        val newParticleMesh = SkeletonMeshToParticleMesh.convert(
            datId = DatId("ono0"),
            skeletonMesh = skeletonMesh,
        )

        // Assigns the simple "glow" texture to the new meshes
        newParticleMesh.entries.forEach {
            it.textureName = TextureName("eff     grw2    ")
        }

        // Modify the UVs to make the glow effect visible on only the blade & top half of the hilt
        newParticleMesh.entries.flatMap { it.vertices }.forEach {
            it.uv = if (it.position.z > -0.1f) { Vector2f(0.25f, 0.25f) } else { Vector2f(1f, 1f) }
        }

        effectDirectory.delete(SectionType.S1F_ParticleMesh, DatId("ono0"))
        effectDirectory.addChild(newParticleMesh)
    }

    private fun adjustAlphaKeyFrameValues(effectDirectory: Directory) {
        // Changes the alpha values to make the glow effect stronger.
        val alphaKeyFrames = effectDirectory.getKeyFrameValue(DatId("axa1"))
        alphaKeyFrames.entries[0].value = 0.10f
        alphaKeyFrames.entries[1].value = 0.32f
        alphaKeyFrames.entries[2].value = 0.80f
        alphaKeyFrames.entries[3].value = 1.00f
        alphaKeyFrames.entries[4].value = 0.72f
        alphaKeyFrames.entries[5].value = 0.20f
        alphaKeyFrames.entries[6].value = 0.10f
    }

    private fun patchParticleEffects(effectDirectory: Directory) {
        effectDirectory.getParticleGenerators().forEach { patchEffect(it) }
    }

    private fun patchEffect(effect: ParticleGenerator) {
        renameParticleGeneratorIfOffhand(effect)
        effect.header.attachedJoint0 = StandardJointPositions[WeaponType.Sword, context.itemModelSlot]

        if (effect.datId.id.startsWith("axe")) {
            // This fixes the position & scaling of the particle-mesh glow effect
            effect.header.actorScaleParams.scaleSize = ActorScaleTarget.None
            effect.header.actorScaleParams.scalePosition = ActorScaleTarget.None

            val particleSetup = effect[ParticleInitializers.StandardParticleSetup::class]
            particleSetup.basePosition.copyFrom(getRaceParticlePositionOffset())

            val scale = effect[ParticleInitializers.ScaleInitializer::class]
            scale.scale.copyFrom(getRaceBasedParticleScale())
            return
        }

        // This fixes the base spawn-position of the flame & distortion effects
        val particleSetup = effect[ParticleInitializers.StandardParticleSetup::class]
        particleSetup.basePosition.copyFrom(Vector3f(0f, 0f, -0.8f))

        // This fixes the spawn-position variance of the flame & distortion effects, so that they stay along the blade
        val positionVariance = effect.getOptional(ParticleInitializers.PositionVarianceMedium::class)
        if (positionVariance != null) {
            positionVariance.radiusVariance = 0.05f
            positionVariance.radius = 0.03f
            positionVariance.radiusScale = Vector3f(0f, 0f, 2f)
        }
    }

    private fun patchRoutineEffects(effectDirectory: Directory) {
        // Since the effects were copied from [Router], they all have "main-hand" naming conventions.
        // The effects for the off-hand model need to be changed to follow proper conventions & avoid clashes.
        // This function just renames the resources & fixes the references to those resources.
        if (context.mainHand) { return }

        val loopRoutine = effectDirectory.getEffectRoutine(DatId("lop0"))
        loopRoutine.datId = renameOffhandId(loopRoutine.datId)
        loopRoutine[Effects.ParticleGenRoutine::class].forEach { it.refId = renameOffhandId(it.refId) }

        val initRoutine = effectDirectory.getEffectRoutine(StandardDatIds.weaponInitRoutine(mainHand = true))
        initRoutine.datId = StandardDatIds.weaponInitRoutine(mainHand = false)
        initRoutine[Effects.EndLoopRoutine::class].forEach { it.refId = renameOffhandId(it.refId) }
        initRoutine[Effects.ParticleDampenRoutine::class].forEach { it.refId = renameOffhandId(it.refId) }

        val engageRoutine = effectDirectory.getEffectRoutine(StandardDatIds.weaponEngageRoutine(mainHand = true))
        engageRoutine.datId = StandardDatIds.weaponEngageRoutine(mainHand = false)
        engageRoutine[Effects.EndLoopRoutine::class].forEach { it.refId = renameOffhandId(it.refId) }
        engageRoutine[Effects.StartLoopRoutine::class].forEach { it.refId = renameOffhandId(it.refId) }

        val disengageRoutine = effectDirectory.getEffectRoutine(StandardDatIds.weaponDisengageRoutine(mainHand = true))
        disengageRoutine.datId = StandardDatIds.weaponDisengageRoutine(mainHand = false)
        disengageRoutine[Effects.EndLoopRoutine::class].forEach { it.refId = renameOffhandId(it.refId) }
        disengageRoutine[Effects.ParticleDampenRoutine::class].forEach { it.refId = renameOffhandId(it.refId) }
        disengageRoutine[Effects.TransitionParticleEffect::class].forEach {
            it.stopEffect = renameOffhandId(it.stopEffect)
            it.startEffect = renameOffhandId(it.startEffect)
        }
    }

    private fun makeOnHitEffect(baseRoot: Directory, baseEffectDirectory: Directory) {
        val baseEffectId = if (context.mainHand) { DatId("frm1") } else { renameOffhandId(DatId("frm1")) }
        val baseEffect = baseEffectDirectory.getParticleGenerator(baseEffectId)

        val copiedEffect = baseEffectDirectory.addChild(baseEffect.deepCopy())
        copiedEffect.datId = if (context.mainHand) { DatId("frm2") } else { renameOffhandId(DatId("frm2")) }

        // This causes the effect to emit particles more frequently & abundantly.
        copiedEffect.header.framesPerEmission = 0
        copiedEffect.header.particlesPerEmission = 4

        // This will locate the on-attack routine. Adding the ParticleGenRoutine will link the "fire" particle-generator.
        val onHitRoutineId = StandardDatIds.weaponOnAttack(context.mainHand)
        val onHitRoutine = baseRoot.getEffectRoutineRecursive(onHitRoutineId)

        // This will cause the effect to run for 28 frames (duration).
        // Delay applies to the next effect in the routine. Since this is the last effect in the routine, the delay is needed to prevent the generator from being culled early.
        onHitRoutine.appendEffectRoutine(Effects.ParticleGenRoutine(delay = 28, duration = 28, refId = copiedEffect.datId))
    }

    private fun renameParticleGeneratorIfOffhand(particleGenerator: ParticleGenerator) {
        if (context.mainHand) { return }
        particleGenerator.datId = renameOffhandId(particleGenerator.datId)
    }

    private fun renameOffhandId(datId: DatId): DatId {
        // arbitrarily chosen amount; just needs to be enough to prevent duplicate IDs
        return datId.incrementLastDigit(amount = 4)
    }

    private fun getRaceParticlePositionOffset(): Vector3f {
        return if (context.race == RaceGenderConfig.Galka) {
            Vector3f(0f, 0f, -0.0375f)
        } else {
            Vector3f.ZERO
        }
    }

    private fun getRaceBasedParticleScale(): Vector3f {
        return if (context.mainHand && context.race == RaceGenderConfig.ElvaanM) {
            Vector3f(1f, 0.825f, 0.875f)
        } else {
            Vector3f.ONE
        }
    }

}