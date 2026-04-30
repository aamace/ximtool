package ximtool.dat

import ximtool.Environment
import ximtool.misc.Log
import ximtool.misc.LogColor
import java.io.File

class DatFile private constructor(val path: String) {
    companion object {
        fun itemModel(raceGenderConfig: RaceGenderConfig, itemModelSlot: ItemModelSlot, itemModelId: Int): DatFile {
            val path = EquipmentModelTable.getItemModelPath(raceGenderConfig, itemModelSlot, itemModelId)
            return DatFile(path)
        }

        fun romFile(version: Int = 1, folder: Int, file: Int): DatFile {
            val romNumber = if (version == 1) { "" } else { version.toString() }
            return DatFile("ROM$romNumber/$folder/$file.DAT")
        }

        fun fileTableIndex(fileTableIndex: Int): DatFile {
            val path = FileTables.getFilePath(fileTableIndex) ?: throw IllegalStateException("No file for index $fileTableIndex")
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
        Log.info("Creating backup $path", LogColor.Blue)
        file.copyTo(backup)
    }

    Log.info("Writing to $path", LogColor.Blue)
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
