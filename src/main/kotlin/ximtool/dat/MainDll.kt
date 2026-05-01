package ximtool.dat

import ximtool.Environment
import ximtool.dat.DllOffsetHints.actionAnimationFileTableOffsetHint
import ximtool.dat.DllOffsetHints.battleAnimationFileTableOffsetHint
import ximtool.dat.DllOffsetHints.battleSkirtAnimationFileTableOffsetHint
import ximtool.dat.DllOffsetHints.battleSkirtDwAnimationFileTableOffsetHint
import ximtool.dat.DllOffsetHints.danceSkillAnimationFileTableOffsetHint
import ximtool.dat.DllOffsetHints.dualWieldMainHandFileTableOffsetHint
import ximtool.dat.DllOffsetHints.dualWieldOffHandFileTableOffsetHint
import ximtool.dat.DllOffsetHints.emoteAnimationOffsetHint
import ximtool.dat.DllOffsetHints.equipmentLookupTableOffsetHint
import ximtool.dat.DllOffsetHints.fishingRodFileTableOffsetHint
import ximtool.dat.DllOffsetHints.raceConfigLookupTableOffsetHint
import ximtool.dat.DllOffsetHints.weaponSkillAnimationFileTableOffsetHint
import ximtool.dat.DllOffsetHints.zoneDecryptTable1OffsetHint
import ximtool.dat.DllOffsetHints.zoneDecryptTable2OffsetHint
import ximtool.dat.DllOffsetHints.zoneMapTableOffsetHint
import ximtool.misc.Log
import java.io.File

private class DllOffsets(
    val battleAnimationFileTableOffset: Int,
    val dualWieldMainHandFileTableOffset: Int,
    val dualWieldOffHandFileTableOffset: Int,
    val battleSkirtAnimationFileTableOffset: Int,
    val battleSkirtDwAnimationFileTableOffset: Int,
    val emoteAnimationOffset: Int,
    val equipmentLookupTableOffset: Int,
    val raceConfigLookupTableOffset: Int,
    val weaponSkillAnimationFileTableOffset: Int,
    val danceSkillAnimationFileTableOffset: Int,
    val actionAnimationFileTableOffset: Int,
    val fishingRodFileTableOffset: Int,
    val zoneDecryptTable1Offset: Int,
    val zoneDecryptTable2Offset: Int,
    val zoneMapTableOffset: Int,
)

private object DllOffsetHints {
    val battleAnimationFileTableOffsetHint: UInt = 0xC825C825U
    val dualWieldMainHandFileTableOffsetHint: UInt = 0x6F9F6F9FU
    val dualWieldOffHandFileTableOffsetHint: UInt = 0xEF9DEF9DU
    val battleSkirtAnimationFileTableOffsetHint: UInt = 0x4826C826U
    val battleSkirtDwAnimationFileTableOffsetHint: UInt = 0xEF9F6FA0U
    val emoteAnimationOffsetHint: UInt = 0x48274827U
    val equipmentLookupTableOffsetHint: UInt = 0xA81B0000U
    val raceConfigLookupTableOffsetHint: UInt = 0xA01BA01BU
    val weaponSkillAnimationFileTableOffsetHint: UInt = 0xCB81CB81U
    val danceSkillAnimationFileTableOffsetHint: UInt = 0xB9E2B9E2U
    val actionAnimationFileTableOffsetHint: UInt = 0xCB96CB96U
    val fishingRodFileTableOffsetHint: UInt = 0x8B998B99U
    val zoneDecryptTable1OffsetHint: UInt = 0xE2E506A9U
    val zoneDecryptTable2OffsetHint: UInt = 0xB8C5F784U
    val zoneMapTableOffsetHint: ULong = 0x6400000100010100U
}

object MainDll {

    private lateinit var dll: ByteReader
    private lateinit var offsets: DllOffsets

    fun getZoneDecryptTable1(): ByteArray {
        load()
        return getBytes(offset = offsets.zoneDecryptTable1Offset, length = 0x100)
    }

    fun getZoneDecryptTable2(): ByteArray {
        load()
        return getBytes(offset = offsets.zoneDecryptTable2Offset, length = 0x100)
    }

    fun getZoneMapTableReader(): ByteReader {
        load()
        return getByteReader(offset = offsets.zoneMapTableOffset, size = 0x2D64)
    }

    fun getEquipmentLookupTable(): ByteReader {
        load()
        return getByteReader(offset = offsets.equipmentLookupTableOffset, size = 0x1B0 * 0x10)
    }

    fun getBaseBattleAnimationIndex(raceGenderConfig: RaceGenderConfig): Int {
        load()
        return dll.read16(offsets.battleAnimationFileTableOffset + raceGenderConfig.index * 2)
    }

    fun getBaseSkirtAnimationIndex(raceGenderConfig: RaceGenderConfig, dualWield: Boolean): Int {
        load()
        val offset = if (dualWield) { offsets.battleSkirtDwAnimationFileTableOffset } else { offsets.battleSkirtAnimationFileTableOffset }
        return dll.read16(offset + raceGenderConfig.index * 4 + 2)
    }

    fun getBaseWeaponSkillAnimationIndex(raceGenderConfig: RaceGenderConfig): Int {
        load()
        return dll.read16(offsets.weaponSkillAnimationFileTableOffset + raceGenderConfig.index * 2)
    }

    fun getBaseRaceConfigIndex(raceGenderConfig: RaceGenderConfig): Int {
        load()
        return dll.read16(offsets.raceConfigLookupTableOffset + raceGenderConfig.index * 2)
    }

    fun getBaseDanceSkillAnimationIndex(raceGenderConfig: RaceGenderConfig): Int {
        load()
        return dll.read16(offsets.danceSkillAnimationFileTableOffset + raceGenderConfig.index * 2)
    }

    fun getBaseDualWieldMainHandAnimationIndex(raceGenderConfig: RaceGenderConfig): Int {
        load()
        return dll.read16(offsets.dualWieldMainHandFileTableOffset + raceGenderConfig.index * 2)
    }

    fun getBaseDualWieldOffHandAnimationIndex(raceGenderConfig: RaceGenderConfig): Int {
        load()
        return dll.read16(offsets.dualWieldOffHandFileTableOffset + raceGenderConfig.index * 2)
    }

    fun getBaseEmoteAnimationIndex(raceGenderConfig: RaceGenderConfig): Int {
        load()
        return dll.read16(offsets.emoteAnimationOffset + raceGenderConfig.index * 2)
    }

    fun getActionAnimationIndex(raceGenderConfig: RaceGenderConfig): Int {
        load()
        return dll.read16(offsets.actionAnimationFileTableOffset + raceGenderConfig.index * 2)
    }

    fun getBaseFishingRodIndex(raceGenderConfig: RaceGenderConfig): Int {
        load()
        return dll.read16(offsets.fishingRodFileTableOffset + raceGenderConfig.index * 2)
    }

    private fun getBytes(offset: Int, length: Int): ByteArray {
        load()
        return dll.subArray(offset = offset, length = length)
    }

    private fun load() {
        if (this::dll.isInitialized) { return }
        val file = File("${Environment.ffxiDir}/FFXiMain.dll")
        try {
            dll = ByteReader(file.readBytes())
            offsets = loadOffsets()
        } catch (t: Throwable) {
            Log.error("Failed to load ${file.canonicalPath}. Is the FFXI Directory correct?")
            throw t
        }
    }

    private fun getByteReader(offset: Int, size: Int): ByteReader {
        return ByteReader(getBytes(offset, size))
    }

    private fun loadOffsets(): DllOffsets {
        return DllOffsets(
            battleAnimationFileTableOffset = findOffset(battleAnimationFileTableOffsetHint),
            dualWieldMainHandFileTableOffset = findOffset(dualWieldMainHandFileTableOffsetHint),
            dualWieldOffHandFileTableOffset = findOffset(dualWieldOffHandFileTableOffsetHint),
            battleSkirtAnimationFileTableOffset = findOffset(battleSkirtAnimationFileTableOffsetHint),
            battleSkirtDwAnimationFileTableOffset = findOffset(battleSkirtDwAnimationFileTableOffsetHint),
            emoteAnimationOffset = findOffset(emoteAnimationOffsetHint),
            equipmentLookupTableOffset = findOffset(equipmentLookupTableOffsetHint),
            raceConfigLookupTableOffset = findOffset(raceConfigLookupTableOffsetHint),
            weaponSkillAnimationFileTableOffset = findOffset(weaponSkillAnimationFileTableOffsetHint),
            danceSkillAnimationFileTableOffset = findOffset(danceSkillAnimationFileTableOffsetHint),
            actionAnimationFileTableOffset = findOffset(actionAnimationFileTableOffsetHint),
            fishingRodFileTableOffset = findOffset(fishingRodFileTableOffsetHint),
            zoneDecryptTable1Offset = findOffset(zoneDecryptTable1OffsetHint),
            zoneDecryptTable2Offset = findOffset(zoneDecryptTable2OffsetHint),
            zoneMapTableOffset = findOffset(zoneMapTableOffsetHint),
        )
    }

    private fun findOffset(hint: UInt): Int {
        dll.position = 0x30000
        val signedHint = hint.toInt()

        for (i in 0 until 0xC000) {
            if (dll.next32BE() == signedHint) {
                return dll.position - 0x04
            }
        }

        throw IllegalStateException("Failed to find offset for ${hint.toString(0x10)}")
    }

    private fun findOffset(hint: ULong): Int {
        dll.position = 0x30000
        val signedHint = hint.toLong()

        for (i in 0 until 0x6000) {
            if (dll.next64BE() == signedHint) {
                return dll.position - 0x08
            }
        }

        throw IllegalStateException("Failed to find offset for ${hint.toString(0x10)}")
    }

}