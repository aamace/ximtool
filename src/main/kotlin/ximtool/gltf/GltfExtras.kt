@file:Suppress("UNCHECKED_CAST")

package ximtool.gltf

import ximtool.dat.DatId
import ximtool.dat.TextureName
import ximtool.dat.TextureName.Companion.blank
import ximtool.misc.Log
import ximtool.misc.LogColor

data class MeshExtras(
    var datId: DatId? = null,
    var occludeType: Int = 0,
    var displayType: Int = 0,
    var textureName: TextureName? = null,
    var ambientMultiplier: Float = 1f,
    var specularPower: Float = 0f,
) {

    companion object {
        fun deserialize(raw: Any?): MeshExtras {
            val extras = MeshExtras()
            val data = (raw as? Map<*, *>) ?: return extras

            for ((key, value) in data) {
                if (key !is String || value !is String) {
                    Log.warn("\tUnknown extra: $key -> $value", LogColor.Yellow)
                    continue
                }

                when (key) {
                    "datId" -> extras.datId = DatId(value)
                    "occludeType" -> extras.occludeType = value.toIntOrNull() ?: 0
                    "displayType" -> extras.displayType = value.toIntOrNull() ?: 0
                    "textureName" -> extras.textureName = TextureName(value)
                    "ambientMultiplier" -> extras.ambientMultiplier = value.toFloatOrNull() ?: 1f
                    "specularPower" -> extras.specularPower = value.toFloatOrNull() ?: 0f
                    else -> Log.warn("\tUnknown extra: $key -> $value", LogColor.Yellow)
                }
            }

            return extras
        }
    }

    fun serialize(): Map<String, String> {
        return mapOf(
            "datId" to (datId?.id ?: "    "),
            "occludeType" to occludeType.toString(),
            "displayType" to displayType.toString(),
            "textureName" to (textureName ?: blank).value,
            "ambientMultiplier" to String.format("%.4f", ambientMultiplier),
            "specularPower" to String.format("%.4f", specularPower),
        )
    }

}

class JointExtras(
    val index: Int,
) {

    companion object {
        fun deserialize(raw: Any?): JointExtras {
            val data = (raw as? Map<String, String>) ?: return JointExtras(0)
            return JointExtras(index = data["index"]?.toIntOrNull() ?: 0)
        }
    }

    fun serialize(): Map<String, String> {
        return mapOf(
            "index" to index.toString(),
        )
    }

}