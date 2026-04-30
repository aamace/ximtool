package ximtool.tools

import ximtool.dat.DatFile
import ximtool.dat.ItemModelSlot
import ximtool.dat.RaceGenderConfig
import ximtool.dat.restoreFromBackup
import ximtool.misc.Log
import ximtool.misc.LogColor

fun main(args: Array<String>) {
    RestoreFromBackup.runMainSub(args[0].toInt())
}

object RestoreFromBackup {
    fun runMainSub(modelId: Int) {
        run(ItemModelSlot.Main, modelId)
        run(ItemModelSlot.Sub, modelId)
    }

    fun run(itemModelSlot: ItemModelSlot, modelId: Int) {
        for (raceGender in RaceGenderConfig.entries) { run(raceGender, itemModelSlot, modelId) }
    }

    fun run(raceGender: RaceGenderConfig, itemModelSlot: ItemModelSlot, modelId: Int) {
        Log.info("Restoring $raceGender-$itemModelSlot-$modelId", LogColor.Green)
        DatFile.itemModel(raceGender, itemModelSlot, modelId).restoreFromBackup()
    }

}
