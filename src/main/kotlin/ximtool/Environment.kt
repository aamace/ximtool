package ximtool

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

object Environment {

    private const val path = "settings.ini"
    private val config by lazy { loadConfig() }

    var ffxiDir
        get() = config.ffxiDir
        set(value) { config.ffxiDir = value; persistConfig() }

    var importDestinationDir
        get() = config.importDestinationDir
        set(value) { config.importDestinationDir = value; persistConfig() }

    private fun loadConfig(): EnvironmentConfig {
        val settingsFile = File(path)
        if (!settingsFile.exists()) { return EnvironmentConfig() }

        return try {
            Json.decodeFromString<EnvironmentConfig>(settingsFile.readText())
        } catch (e: Exception) {
            EnvironmentConfig()
        }
    }

    private fun persistConfig() {
        val settingsFile = File(path)
        settingsFile.createNewFile()
        settingsFile.writeText(Json.encodeToString(config))
    }

}

@Serializable
private data class EnvironmentConfig(
    var ffxiDir: String = "C:/Program Files (x86)/PlayOnline/SquareEnix/FINAL FANTASY XI",
    var importDestinationDir: String = System.getProperty("user.dir"),
)