package ximtool.dat

import ximtool.datresource.KeyFrameValueSection
import ximtool.datresource.SkeletonAnimationSection
import ximtool.datresource.SkeletonMeshSection
import ximtool.datresource.SkeletonSection
import ximtool.datresource.effectroutine.EffectRoutineSection
import ximtool.datresource.particle.ParticleGeneratorSection
import ximtool.misc.Log
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

object DatTree {

    fun parse(datFile: DatFile): Directory {
        return parse(datFile.readBytes())
    }

    private fun parse(rawDat: ByteArray): Directory {
        var root: Directory? = null
        var currentDirectory: Directory? = null
        val parentLink = HashMap<Directory, Directory>()

        val byteReader = ByteReader(rawDat)

        while (true) {
            if (!byteReader.hasMore()) {
                break
            }

            val sectionStart = byteReader.position
            val header = SectionHeader.read(byteReader)
            val data = byteReader.subArray(length = header.sectionSize, offset = sectionStart)

            val node = try {
                when (header.sectionType) {
                    SectionType.S00_End -> End(header.sectionId)
                    SectionType.S01_Directory -> Directory(header.sectionId)
                    SectionType.S05_ParticleGenerator -> ParticleGeneratorSection.read(data)
                    SectionType.S07_EffectRoutine -> EffectRoutineSection.read(data)
                    SectionType.S19_ParticleKeyFrameData -> KeyFrameValueSection.read(data)
                    SectionType.S20_Texture -> TextureSection.read(data)
                    SectionType.S29_Skeleton -> SkeletonSection.read(data)
                    SectionType.S2A_SkeletonMesh -> SkeletonMeshSection.read(data)
                    SectionType.S2B_SkeletonAnimation -> SkeletonAnimationSection.read(data)
                    else -> UnimplementedResource(header.sectionId, header.sectionType, data)
                }
            } catch (e: Exception) {
                Log.warn("Failed to parse ${header.sectionType} ${header.sectionId}: ${e.message}. Using direct ser/des for it.")
                UnimplementedResource(header.sectionId, header.sectionType, data)
            }

            currentDirectory?.children?.add(node)

            if (node is Directory) {
                if (root == null) { root = node }
                parentLink[node] = currentDirectory ?: node
                currentDirectory = node
            } else if (node is End) {
                currentDirectory = parentLink[currentDirectory]
            }

            byteReader.position = sectionStart + header.sectionSize
        }

        return root!!
    }

    fun fromItemModel(raceGenderConfig: RaceGenderConfig, itemModelSlot: ItemModelSlot, itemModelId: Int): Directory {
        val file = DatFile.itemModel(raceGenderConfig, itemModelSlot, itemModelId)
        return parse(file)
    }

}

fun Directory.delete(datResource: DatResource) {
    delete(datResource.sectionType, datResource.datId)
}

fun Directory.delete(sectionType: SectionType, datId: DatId) {
    children.removeAll { it.sectionType == sectionType && it.datId == datId }
}

fun Directory.deleteRecursive(sectionType: SectionType) {
    deleteRecursive { it.sectionType == sectionType }
}

fun Directory.deleteRecursive(filter: (DatResource) -> Boolean) {
    children.filterIsInstance<Directory>().forEach { it.deleteRecursive(filter) }
    children.removeAll { filter(it) }
}

fun <T: DatResource> Directory.addChild(child: T): T {
    children += child
    sortChildren()
    return child
}

fun Directory.addChild(raw: ByteArray): DatResource {
    val header = SectionHeader.read(ByteReader(raw))
    return addChild(UnimplementedResource(header.sectionId, header.sectionType, raw))
}

fun <T: DatResource> Directory.getChild(childType: KClass<T>, childId: DatId): T {
    val match = children.firstOrNull { it.datId == childId && childType.isInstance(it) }
    return childType.safeCast(match) ?: throw IllegalStateException("[${datId}] Did not have a child matching $childType/$childId")
}

fun Directory.getChild(childType: SectionType, childId: DatId): DatResource {
    return children.firstOrNull { it.datId == childId && it.sectionType == childType }
        ?: throw IllegalStateException("[${datId}] Did not have a child matching $childType/$childId")
}

fun <T: DatResource> Directory.getChildRecursive(childType: KClass<T>, childId: DatId): T {
    var match = children.firstOrNull { it.datId == childId && childType.isInstance(it) }

    if (match == null) {
        val subdirectories = getChildren(Directory::class)
        match = subdirectories.firstNotNullOfOrNull { it.getChildRecursive(childType, childId) }
    }

    return childType.safeCast(match) ?: throw IllegalStateException("[${datId}] Did not have a child matching $childType/$childId")
}

fun Directory.getChildren(sectionType: SectionType): List<DatResource> {
    return getChildren { it.sectionType == sectionType }
}

fun <T: DatResource> Directory.getChildren(type: KClass<T>): List<T> {
    return children.mapNotNull { type.safeCast(it) }
}

fun Directory.getChildren(filter: (DatResource) -> Boolean): List<DatResource> {
    return children.filter(filter)
}

fun <T : DatResource> Directory.getChildrenRecursive(childType: KClass<T>): List<T> {
    val match = children.filter { childType.isInstance(it) }.toMutableList()

    val subdirectories = getChildren(Directory::class)
    match += subdirectories.flatMap { it.getChildrenRecursive(childType) }

    return children.mapNotNull { childType.safeCast(it) }
}

private fun Directory.sortChildren() {
    children.sortBy {
        when (it.sectionType) {
            SectionType.S00_End -> Int.MAX_VALUE
            SectionType.S01_Directory -> Int.MAX_VALUE - 1
            else -> it.sectionType.code
        }
    }
}