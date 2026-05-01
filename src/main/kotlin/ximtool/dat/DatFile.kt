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
    val file = toFile()
    Log.info("Reading from ${file.absolutePath}")
    return file.readBytes()
}

fun DatFile.writeBytes(byteArray: ByteArray) {
    val readFile = toFile()
    val writeFile = toWriteFile()

    val backup = getBackupFile()
    if (!backup.exists() && readFile == writeFile) {
        Log.info("Creating backup ${backup.absolutePath}", LogColor.Blue)
        writeFile.copyTo(backup)
    }

    Log.info("Writing to ${writeFile.absolutePath}", LogColor.Blue)
    writeFile.writeBytes(byteArray)
}

fun DatFile.restoreFromBackup() {
    val file = toFile()
    val backup = getBackupFile()
    if (!backup.exists()) { return }

    Log.info("Restoring $path from backup")
    backup.copyTo(file, overwrite = true)
}

private fun DatFile.toFile(): File {
    val filePath = "${Environment.ffxiDir}/${path}"
    return File(filePath)
}

private fun DatFile.toWriteFile(): File {
    val filePath = "${Environment.importDestinationDir}/$path"
    return File(filePath).also { it.parentFile.mkdirs() }
}

private fun DatFile.getBackupFile(): File {
    val backupPath = "${Environment.ffxiDir}/${path}.bak"
    return File(backupPath)
}
