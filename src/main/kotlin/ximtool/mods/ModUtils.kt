package ximtool.mods

import ximtool.dat.ItemModelSlot
import ximtool.dat.RaceGenderConfig

class WeaponModContext(
    val race: RaceGenderConfig,
    val joint: Int,
    val mainHand: Boolean,
    val itemModelSlot: ItemModelSlot,
)