package ximtool.mods

import ximtool.dat.*
import ximtool.datresource.KeyFrameEntry
import ximtool.datresource.SkeletonMeshInstructions
import ximtool.datresource.StandardJointPositions
import ximtool.datresource.addSubDirectory
import ximtool.datresource.effectroutine.EffectRoutineSerializer
import ximtool.datresource.effectroutine.Effects
import ximtool.datresource.particle.*
import ximtool.math.Matrix4f
import ximtool.math.PI_f
import ximtool.math.Vector3f
import ximtool.misc.Log
import ximtool.misc.LogColor
import ximtool.misc.WeaponJointMapping
import ximtool.misc.WeaponType
import ximtool.resource.*
import ximtool.tools.RestoreFromBackup
import java.io.File

private const val texturePath  = "MogBlade/diffuse.dds"
private const val shinyObjPath = "MogBlade/shiny.obj"
private const val matteObjPath = "MogBlade/matte.obj"

private const val particleTexturePath = "MogBlade/particle.dds"
private const val particleObjPath = "MogBlade/particle.obj"

private const val destinationModelId = 268 // [Bronze Sword]

fun main() {
    RestoreFromBackup.runMainSub(destinationModelId)
    MogBlade.invoke()
}

private object MogBlade {

    fun invoke() {
        for (race in RaceGenderConfig.values()) {
            if (race == RaceGenderConfig.TaruF) { continue }
            val jointConfig = WeaponJointMapping[race, WeaponType.Sword]

            Log.info("Working on ${race.name} main-hand", LogColor.Green)
            MogBladeApplier(WeaponModContext(race, jointConfig.mainHand, mainHand = true, ItemModelSlot.Main)).apply()

            Log.info("Working on ${race.name} off-hand", LogColor.Green)
            MogBladeApplier(WeaponModContext(race, jointConfig.offHand, mainHand = false, ItemModelSlot.Sub)).apply()
        }
    }

}

private class MogBladeApplier(val context: WeaponModContext) {

    fun apply() {
        val outputFile = DatFile.itemModel(context.race, context.itemModelSlot, destinationModelId)
        val rootDirectory = DatTree.parse(outputFile)

        rootDirectory.deleteRecursive { it.sectionType == SectionType.S20_Texture }
        rootDirectory.deleteRecursive { it.sectionType == SectionType.S2A_SkeletonMesh }

        val textureName = TextureName("tim     mogblade")
        rootDirectory.addChild(DdsToTexture.convert(path = texturePath, datId = DatId("mogt"), name = textureName))

        rootDirectory.addChild(makeShinySkeletonMesh(textureName))
        rootDirectory.addChild(makeMatteSkeletonMesh(textureName))

        addParticleEffect(rootDirectory)

        val output = rootDirectory.serialize()
        outputFile.writeBytes(output)

        val localFile = File("./output/MogBlade/${context.race}-${context.itemModelSlot}.DAT")
        localFile.parentFile.mkdirs()
        localFile.createNewFile()
        localFile.writeBytes(output)
    }

    private fun makeShinySkeletonMesh(textureName: TextureName): SkeletonMesh {
        val obj = ObjLoader.load(shinyObjPath, config = ObjLoaderConfig(verticalFlipUvs = true))
        applyObjTransform(obj)

        return ObjToSkeletonMeshConverter.convert(ObjToSkeletonMeshConfig(
            datId = StandardDatIds.weaponMesh(context.mainHand),
            objData = obj,
            joint = context.joint,
            materialInstruction = SkeletonMeshInstructions.MaterialInstruction(
                ambientMultiplier = 0.6f,
                specularHighlightEnabled = true,
                specularHighlightPower = 50f,
            ),
            textureInstruction = SkeletonMeshInstructions.TextureInstruction(textureName = textureName)
        ))
    }

    private fun makeMatteSkeletonMesh(textureName: TextureName): SkeletonMesh {
        val obj = ObjLoader.load(matteObjPath, config = ObjLoaderConfig(verticalFlipUvs = true))
        applyObjTransform(obj)

        return ObjToSkeletonMeshConverter.convert(ObjToSkeletonMeshConfig(
            datId = StandardDatIds.weaponMesh(context.mainHand),
            objData = obj,
            joint = context.joint,
            materialInstruction = SkeletonMeshInstructions.MaterialInstruction(
                ambientMultiplier = 0.80f,
            ),
            textureInstruction = SkeletonMeshInstructions.TextureInstruction(textureName = textureName),
        ))
    }

    private fun applyObjTransform(obj: ObjData) {
        // Fixes the forward/up axis, and ensures the moogle is facing outward.
        // (Alternatively, this could've been fixed in Blender)
        val zRotateSign = if (context.mainHand) { -1f } else { 1f }
        val rotation = Matrix4f().rotateZInPlace(zRotateSign * PI_f / 2f).rotateXInPlace(-PI_f / 2f)

        obj.vertices.forEach {
            rotation.transformInPlace(it)
        }

        obj.normals.forEach {
            rotation.transformInPlace(it)
        }
    }

    private fun addParticleEffect(rootNode: Directory) {
        val directoryName = if (context.mainHand) { DatId("effr") } else { DatId("effl") }
        val effectDirectory = rootNode.addSubDirectory(directoryName)

        val particleTextureName = TextureName("tim     mogglow ")
        val particleTexture = DdsToTexture.convert(particleTexturePath, DatId("ptxt"), particleTextureName)
        effectDirectory.addChild(particleTexture)

        val particleMeshId = DatId("pmsh")
        val particleMesh = ObjToParticleMeshConverter.convert(ObjToParticleMeshConfig(
            datId = particleMeshId,
            objData = ObjLoader.load(particleObjPath).also { applyObjTransform(it) },
            textureName = particleTextureName,
        ))
        effectDirectory.addChild(particleMesh)

        val keyFrameValueId = DatId("alph")
        effectDirectory.addChild(KeyFrameValue(keyFrameValueId, listOf(
            KeyFrameEntry(0f, 0.0f),
            KeyFrameEntry(0.5f, 0.15f),
            KeyFrameEntry(1f, 0.0f),
        )))

        val particleName = if (context.mainHand) { DatId("gr00") } else { DatId("gl00") }
        val particleJoint = StandardJointPositions[WeaponType.Sword, context.itemModelSlot]
        val particleEffect = ParticleGeneratorSerializer.serialize(ParticleGenerator(
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
            generatorUpdaters = mutableListOf(
                ParticleGeneratorUpdaters.AssociationUpdater(followPosition = true, followFacing = true),
                ParticleGeneratorUpdaters.EndMarker,
            ),
            particleInitializers = mutableListOf(
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
                ParticleInitializers.ColorAlphaKeyFrameSetup(config = KeyFrameConfig(keyFrameValueId)),
                ParticleInitializers.EndMarker,
            ),
            particleUpdaters = mutableListOf(
                ParticleUpdaters.LifeTimeUpdater,
                ParticleUpdaters.TexCoordTranslateU(0.001f),
                ParticleUpdaters.TexCoordTranslateV(0.0005f),
                ParticleUpdaters.ColorAlphaKeyFrameUpdater,
                ParticleUpdaters.EndMarker,
            ),
            particleExpirationHandlers = mutableListOf(
                ParticleExpirationHandlers.Repeat,
                ParticleExpirationHandlers.EndMarker,
            ),
        ))
        effectDirectory.addChild(particleEffect)

        val initRoutineName = StandardDatIds.weaponInitRoutine(context.mainHand)
        effectDirectory.addChild(EffectRoutineSerializer.serialize(EffectRoutine(
            datId = initRoutineName,
            effects = mutableListOf(
                Effects.StartRoutine(delay = 0),
                Effects.ParticleDampenRoutine(delay = 0, duration = 0, refId = particleName),
                Effects.EndRoutine(delay = 0),
            )
        )))

        val engageRoutineName = StandardDatIds.weaponEngageRoutine(context.mainHand)
        effectDirectory.addChild(EffectRoutineSerializer.serialize(EffectRoutine(
            datId = engageRoutineName,
            effects = mutableListOf(
                Effects.StartRoutine(delay = 0),
                Effects.ParticleGenRoutine(delay = 0, duration = 0, refId = particleName),
                Effects.EndRoutine(delay = 0),
            )
        )))

        val disengageRoutineName = StandardDatIds.weaponDisengageRoutine(context.mainHand)
        effectDirectory.addChild(EffectRoutineSerializer.serialize(EffectRoutine(
            datId = disengageRoutineName,
            effects = mutableListOf(
                Effects.StartRoutine(delay = 0),
                Effects.ParticleDampenRoutine(delay = 0, duration = 20, refId = particleName),
                Effects.EndRoutine(delay = 0),
            )
        )))

        effectDirectory.addChild(End())
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