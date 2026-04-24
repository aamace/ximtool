package ximtool.mods

import ximtool.dat.DatFile
import ximtool.dat.ItemModelSlot
import ximtool.dat.RaceGenderConfig
import ximtool.dat.restoreFromBackup
import ximtool.misc.Log
import ximtool.misc.LogColor

fun main(args: Array<String>) {
    RestoreFromBackup.run(args[0].toInt())
}

object RestoreFromBackup {
    fun run(modelId: Int) {
        for (raceGender in RaceGenderConfig.values()) {
            Log.info("Restoring: $raceGender", LogColor.Green)
            DatFile.itemModel(raceGender, ItemModelSlot.Main, modelId).restoreFromBackup()
            DatFile.itemModel(raceGender, ItemModelSlot.Sub, modelId).restoreFromBackup()
        }
    }
}
