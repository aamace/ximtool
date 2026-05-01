package ximtool.resource

import ximtool.misc.Log
import ximtool.misc.LogColor
import java.io.File

object TempFile {

    fun writeToTempFile(path: String, bytes: ByteArray): File {
        val file = makeFile(path)
        file.writeBytes(bytes)
        return file
    }

    fun writeToTempFile(path: String, lines: String): File {
        val file = makeFile(path)
        file.writeText(lines)
        return file
    }

    fun makeFile(path: String): File {
        val file = File("output/$path")
        file.parentFile.mkdirs()
        file.createNewFile()

        Log.info("Working with ${file.absolutePath}", LogColor.Blue)
        return file
    }

    fun getFile(path: String): File {
        val tempPath = if (path.startsWith("output/") || path.startsWith("output\\")) { path } else { "output/$path" }
        val file = File(tempPath)
        Log.info("Trying to fetch ${file.absolutePath}", LogColor.Blue)
        return file
    }

}