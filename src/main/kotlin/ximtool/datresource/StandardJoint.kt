package ximtool.datresource

import ximtool.dat.ItemModelSlot
import ximtool.misc.WeaponType

enum class StandardPosition(val index: Int) {
    AboveHead(2),
    RightFoot(8),
    LeftFoot(9),
    EquippedSwordSub(54),
    EquippedSwordMain(55),
    LeftHand(126),
    RightHand(127),
}

object StandardJointPositions {

    operator fun get(weaponType: WeaponType, itemModelSlot: ItemModelSlot): Int {
        return if (itemModelSlot == ItemModelSlot.Main) {
            getMainHandPositions(weaponType).index
        } else {
            getOffHandPositions(weaponType).index
        }
    }

    private fun getMainHandPositions(weaponType: WeaponType): StandardPosition {
        return when (weaponType) {
            WeaponType.Sword -> StandardPosition.EquippedSwordMain
        }
    }

    private fun getOffHandPositions(weaponType: WeaponType): StandardPosition {
        return when (weaponType) {
            WeaponType.Sword -> StandardPosition.EquippedSwordSub
        }
    }

}