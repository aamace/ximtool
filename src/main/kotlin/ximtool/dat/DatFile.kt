package ximtool.dat

import ximtool.Environment
import ximtool.misc.Log
import java.io.File

class DatFile private constructor(val path: String) {
    companion object {
        fun itemModel(raceGenderConfig: RaceGenderConfig, itemModelSlot: ItemModelSlot, itemModelId: Int): DatFile {
            val path = EquipmentModelTable.getItemModelPath(raceGenderConfig, itemModelSlot, itemModelId)
            return DatFile(path)
        }
    }
}

fun DatFile.readBytes(): ByteArray {
    Log.info("Reading from $path")
    return toFile().readBytes()
}

fun DatFile.writeBytes(byteArray: ByteArray) {
    val file = toFile()

    val backup = getBackupFile()
    if (!backup.exists()) {
        Log.info("Creating backup $path")
        file.copyTo(backup)
    }

    Log.info("Writing to $path")
    file.writeBytes(byteArray)
}

fun DatFile.restoreFromBackup() {
    val file = toFile()
    val backup = getBackupFile()
    if (!backup.exists()) { return }

    Log.info("Restoring $path from backup")
    backup.copyTo(file, overwrite = true)
}

private fun DatFile.toFile(): File {
    val filePath = Environment.ffxiDir + path
    return File(filePath)
}

private fun DatFile.getBackupFile(): File {
    val backupPath = Environment.ffxiDir + path + ".bak"
    return File(backupPath)
}
