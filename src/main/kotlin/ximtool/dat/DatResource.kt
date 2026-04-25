package ximtool.dat

import ximtool.datresource.*
import ximtool.datresource.effectroutine.*
import ximtool.datresource.particle.*

sealed interface DatResource {
    val datId: DatId
    val sectionType: SectionType

    fun serialize(): ByteArray

}

class Directory(override var datId: DatId, var children: MutableList<DatResource> = ArrayList()): DatResource {
    override val sectionType: SectionType = SectionType.S01_Directory
    override fun serialize() = DirectorySection.serialize(this)
}

class ParticleGenerator(
    override var datId: DatId,
    var header: ParticleGeneratorHeader,
    var generatorUpdaters: MutableList<ParticleGeneratorUpdater>,
    var particleInitializers: MutableList<ParticleInitializer>,
    var particleUpdaters: MutableList<ParticleUpdater>,
    var particleExpirationHandlers: MutableList<ParticleExpirationHandler>,
): DatResource {
    override val sectionType: SectionType = SectionType.S05_ParticleGenerator
    override fun serialize() = ParticleGeneratorSerializer.serialize(this)
}

class EffectRoutine(
    override var datId: DatId,
    var onInitializeEffects: MutableList<OnInitializeEffect> = mutableListOf(OnInitializeEffects.EndEffect),
    var effects: MutableList<Effect>,
    var onCompleteEffects: MutableList<OnCompleteEffect> = mutableListOf(OnCompleteEffects.EndEffect),
): DatResource {
    override val sectionType: SectionType = SectionType.S07_EffectRoutine
    override fun serialize() = EffectRoutineSerializer.serialize(this)
}

data class ParticleMesh(
    override var datId: DatId,
    val entries: List<ParticleMeshEntry>
): DatResource {
    override val sectionType: SectionType = SectionType.S1F_ParticleMesh
    override fun serialize() = ParticleMeshSerializer.serialize(this)
}

data class KeyFrameValue(
    override val datId: DatId,
    var entries: List<KeyFrameEntry>
): DatResource {
    override val sectionType = SectionType.S19_ParticleKeyFrameData
    override fun serialize() = KeyFrameValueSerializer.serialize(datId, entries)
}

data class SkeletonMesh(
    override val datId: DatId,
    val flagHeader: SkeletonMeshData.FlagHeader,
    val instructions: List<SkeletonMeshInstructions.Instruction>,
    val jointList: SkeletonMeshData.JointListSection,
    val vertexCountSection: SkeletonMeshData.VertexCountSection,
    val jointReferenceBuffer: List<SkeletonMeshData.JointReferenceEntry>,
    val vertexBuffer: SkeletonMeshData.VertexBuffer,
): DatResource {
    override val sectionType: SectionType = SectionType.S2A_SkeletonMesh
    override fun serialize() = SkeletonMeshSerializer.serialize(this)
}


class End(override val datId: DatId = DatId.end): DatResource {
    override val sectionType: SectionType = SectionType.S00_End
    override fun serialize() = EndSection.serialize(datId)
}

class UnimplementedResource(override val datId: DatId, override val sectionType: SectionType, val raw: ByteArray): DatResource {
    override fun serialize() = raw
}
