package ximtool.mods

import ximtool.dat.*
import ximtool.datresource.DirectorySection
import ximtool.datresource.EndSection
import ximtool.datresource.KeyFrameValue
import ximtool.datresource.effectroutine.EffectRoutine
import ximtool.datresource.effectroutine.EffectRoutineConfig
import ximtool.datresource.effectroutine.Effects
import ximtool.datresource.particle.*
import ximtool.math.Matrix4f
import ximtool.math.Vector3f
import ximtool.misc.Log
import ximtool.misc.LogColor
import ximtool.misc.WeaponJointMapping
import ximtool.misc.WeaponType
import ximtool.resource.*
import java.io.File

private const val texturePath  = "MogBlade/diffuse.dds"
private const val shinyObjPath = "MogBlade/shiny.obj"
private const val matteObjPath = "MogBlade/matte.obj"

private const val particleTexturePath = "MogBlade/particle.dds"
private const val particleObjPath = "MogBlade/particle.obj"

private const val destinationModelId = 268 // [Bronze Sword]

private class ModContext(
    val race: RaceGenderConfig,
    val joint: Int,
    val mainHand: Boolean,
    val itemModelSlot: ItemModelSlot,
)

fun main() {
    RestoreFromBackup.run(destinationModelId)
    MogBlade.invoke()
}

private object MogBlade {

    fun invoke() {
        for (race in RaceGenderConfig.values()) {
            if (race == RaceGenderConfig.TaruF) { continue }
            val jointConfig = WeaponJointMapping[race, WeaponType.Sword]

            Log.info("Working on ${race.name} main-hand", LogColor.Green)
            MogBladeApplier(ModContext(race, jointConfig.mainHand, mainHand = true, ItemModelSlot.Main)).apply()

            Log.info("Working on ${race.name} off-hand", LogColor.Green)
            MogBladeApplier(ModContext(race, jointConfig.offHand, mainHand = false, ItemModelSlot.Sub)).apply()
        }
    }

}

private class MogBladeApplier(val context: ModContext) {

    fun apply() {
        val outputFile = DatFile.itemModel(context.race, context.itemModelSlot, destinationModelId)
        val rootNode = DatTree.parse(outputFile.readBytes())

        rootNode.deleteRecursive { it.sectionHeader.sectionType == SectionType.S20_Texture }
        rootNode.deleteRecursive { it.sectionHeader.sectionType == SectionType.S2A_SkeletonMesh }

        val textureName = TextureName("tim     mogblade")
        rootNode.addChild(DdsToTexture.convert(path = texturePath, datId = DatId("mogt"), name = textureName, mystery = 0x100))

        rootNode.addChild(makeShinySkeletonMesh(textureName))
        rootNode.addChild(makeMatteSkeletonMesh(textureName))

        addParticleEffect(rootNode)

        val output = rootNode.toArray()
        outputFile.writeBytes(output)

        val localFile = File("./output/MogBlade/${context.race}-${context.itemModelSlot}.DAT")
        localFile.parentFile.mkdirs()
        localFile.createNewFile()
        localFile.writeBytes(output)
    }

    private fun makeShinySkeletonMesh(textureName: TextureName): ByteArray {
        val obj = ObjLoader.load(shinyObjPath)
        applyObjTransform(obj)

        val instructions = listOf(
            SkeletonMeshInstructions.MaterialInstruction(
                ambientMultiplier = 0.6f,
                specularHighlightEnabled = true,
                specularHighlightPower = 50f,
            ),
            SkeletonMeshInstructions.TextureInstruction(textureName = textureName),
            obj.toTriMeshInstruction(),
            SkeletonMeshInstructions.EndInstruction,
        )

        return ObjToSkeletonMeshConverter.convert(ObjToSkeletonMeshConfig(
            datId = getMeshDatId(),
            objData = obj,
            joint = context.joint,
            instructions = instructions,
        ))
    }

    private fun makeMatteSkeletonMesh(textureName: TextureName): ByteArray {
        val obj = ObjLoader.load(matteObjPath)
        applyObjTransform(obj)

        val instructions = listOf(
            SkeletonMeshInstructions.MaterialInstruction(
                ambientMultiplier = 0.80f,
            ),
            SkeletonMeshInstructions.TextureInstruction(textureName = textureName),
            obj.toTriMeshInstruction(),
            SkeletonMeshInstructions.EndInstruction,
        )

        return ObjToSkeletonMeshConverter.convert(ObjToSkeletonMeshConfig(
            datId = getMeshDatId(),
            objData = obj,
            joint = context.joint,
            instructions = instructions,
        ))
    }

    private fun getMeshDatId(): DatId {
        return if (context.mainHand) { DatId("wep0") } else { DatId("wep1") }
    }

    private fun applyObjTransform(obj: ObjData) {
        // Fixes the forward/up axis, flips the texture-coordinates, and ensures the moogle is facing outward.
        // (Alternatively, this could've been fixed in Blender)
        val zRotateSign = if (context.mainHand) { -1f } else { 1f }
        val rotation = Matrix4f().rotateZInPlace(zRotateSign * PI_f /2f).rotateXInPlace(-PI_f /2f)

        obj.vertices.forEach {
            rotation.transformInPlace(it)
        }

        obj.normals.forEach {
            rotation.transformInPlace(it)
        }

        obj.uvs.forEach {
            it.y = 1f - it.y
        }
    }

    private fun addParticleEffect(rootNode: Node) {
        val directoryName = if (context.mainHand) { DatId("effr") } else { DatId("effl") }
        val effectDirectory = rootNode.addChild(DirectorySection.make(directoryName))

        val particleTextureName = TextureName("tim     mogglow ")
        val particleTexture = DdsToTexture.convert(particleTexturePath, DatId("ptxt"), particleTextureName, mystery = 512)
        effectDirectory.addChild(particleTexture)

        val particleMeshId = DatId("pmsh")
        effectDirectory.addChild(ObjToParticleMeshConverter.convert(ObjToParticleMeshConfig(
            datId = particleMeshId,
            objData = ObjLoader.load(particleObjPath).also { applyObjTransform(it) },
            textureName = particleTextureName,
        )))

        val keyFrameValueId = DatId("alph")
        effectDirectory.addChild(KeyFrameValue.convert(keyFrameValueId, listOf(
            0f to 0.0f,
            0.5f to 0.15f,
            1f to 0.0f,
        )))

        val particleName = if (context.mainHand) { DatId("gr00") } else { DatId("gl00") }
        val particleJoint = if (context.mainHand) { 55 } else { 54 }
        val particleEffect = ParticleGenerator.create(ParticleGeneratorConfig(
            datId = particleName,
            header = ParticleGeneratorHeader(
                attachType = AttachType.SourceActorWeapon,
                attachedJoint0 = particleJoint,
                attachedJoint1 = 0,
                actorScaleParams = ActorScaleParams(
                    scalePosition = ActorScaleTarget.None, scalePositionAmount = 1f,
                    scaleSize = ActorScaleTarget.None, scaleSizeAmount = 1f
                ),
                framesPerEmission = 300,
                continuous = true,
            ),
            generatorUpdaters = listOf(
                ParticleGeneratorUpdaters.AssociationUpdater(followPosition = true, followFacing = true),
                ParticleGeneratorUpdaters.EndMarker,
            ),
            particleInitializers = listOf(
                ParticleInitializers.StandardParticleSetup(
                    positionOrientationFlags = 0,
                    renderStateFlags = 0,
                    basePosition = getParticlePositionOffset(),
                    linkedDataType = LinkedDataType.StaticMesh,
                    linkedDatId = particleMeshId,
                    lifeSpan = 300,
                    lifeSpanVariance = 0,
                ),
                ParticleInitializers.ProjectionBiasInitializer(-0.03f, 1f),
                ParticleInitializers.RotationInitializer(Vector3f.ZERO),
                ParticleInitializers.ScaleInitializer(getParticleScale()),
                ParticleInitializers.ColorInitializer(ByteColor(r = 70, g = 58, b = 18, a = 0)),
                ParticleInitializers.ColorAlphaKeyFrameSetup(refId = keyFrameValueId),
                ParticleInitializers.EndMarker,
            ),
            particleUpdaters = listOf(
                ParticleUpdaters.LifeTimeUpdater,
                ParticleUpdaters.TexCoordTranslateU(0.001f),
                ParticleUpdaters.TexCoordTranslateV(0.0005f),
                ParticleUpdaters.ColorAlphaKeyFrameUpdater,
                ParticleUpdaters.EndMarker,
            ),
            particleExpirationHandlers = listOf(
                ParticleExpirationHandlers.Repeat,
                ParticleExpirationHandlers.EndMarker,
            ),
        ))
        effectDirectory.addChild(particleEffect)

        val initRoutineName = if (context.mainHand) { DatId("!w00") } else { DatId("!w10") }
        effectDirectory.addChild(EffectRoutine.make(EffectRoutineConfig(
            datId = initRoutineName,
            effects = listOf(
                Effects.StartRoutine(delay = 0),
                Effects.ParticleDampenRoutine(delay = 0, duration = 0, refId = particleName),
                Effects.EndRoutine(delay = 0),
            )
        )))

        val engageRoutineName = if (context.mainHand) { DatId("!w01") } else { DatId("!w11") }
        effectDirectory.addChild(EffectRoutine.make(EffectRoutineConfig(
            datId = engageRoutineName,
            effects = listOf(
                Effects.StartRoutine(delay = 0),
                Effects.ParticleGenRoutine(delay = 0, duration = 0, refId = particleName),
                Effects.EndRoutine(delay = 0),
            )
        )))

        val disengageRoutineName = if (context.mainHand) { DatId("!w02") } else { DatId("!w12") }
        effectDirectory.addChild(EffectRoutine.make(EffectRoutineConfig(
            datId = disengageRoutineName,
            effects = listOf(
                Effects.StartRoutine(delay = 0),
                Effects.ParticleDampenRoutine(delay = 0, duration = 20, refId = particleName),
                Effects.EndRoutine(delay = 0),
            )
        )))

        effectDirectory.addChild(EndSection.make())
    }

    private fun getParticlePositionOffset(): Vector3f {
        return if (context.race == RaceGenderConfig.Galka) {
            Vector3f(0f, 0f, -0.0375f)
        } else {
            Vector3f.ZERO
        }
    }

    private fun getParticleScale(): Vector3f {
        return if (context.mainHand && context.race == RaceGenderConfig.ElvaanM) {
            Vector3f(1f, 0.825f, 0.875f)
        } else {
            Vector3f.ONE
        }
    }

}