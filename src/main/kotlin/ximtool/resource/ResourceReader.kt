package ximtool.resource

import java.io.InputStream

object ResourceReader {

    fun getInputStream(name: String): InputStream {
        return object {}.javaClass.getResource("/$name")
            ?.openStream()
            ?: throw IllegalStateException("No such resource: $name")
    }

    fun readLines(name: String): List<String> {
        return object {}.javaClass.getResourceAsStream("/$name")
            ?.use { it.bufferedReader().readLines() }
            ?: throw IllegalStateException("No such resource: $name")
    }

    fun readBytes(name: String): ByteArray {
        return object {}.javaClass.getResourceAsStream("/$name")
            ?.use { it.readBytes() }
            ?: throw IllegalStateException("No such resource: $name")
    }

}