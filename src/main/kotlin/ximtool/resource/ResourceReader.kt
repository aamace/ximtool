package ximtool.resource

object ResourceReader {

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