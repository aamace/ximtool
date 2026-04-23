package ximtool.misc

import ximtool.dat.RaceGenderConfig

class JointConfig(
    val mainHand: Int,
    val offHand: Int,
)

enum class WeaponType {
    Sword,
}

object WeaponJointMapping {

    private val swordMapping = mapOf(
        RaceGenderConfig.HumeM to JointConfig(mainHand = 5, offHand = 19),
        RaceGenderConfig.HumeF to JointConfig(mainHand = 73, offHand = 75),
        RaceGenderConfig.ElvaanM to JointConfig(mainHand = 79, offHand = 93),
        RaceGenderConfig.ElvaanF to JointConfig(mainHand = 33, offHand = 47),
        RaceGenderConfig.TaruM to JointConfig(mainHand = 52, offHand = 66),
        RaceGenderConfig.TaruF to JointConfig(mainHand = 52, offHand = 66),
        RaceGenderConfig.Mithra to JointConfig(mainHand = 84, offHand = 86),
        RaceGenderConfig.Galka to JointConfig(mainHand = 82, offHand = 96),
    )

    private val mappings = mapOf(
        WeaponType.Sword to swordMapping,
    )

    operator fun get(raceGenderConfig: RaceGenderConfig, type: WeaponType): JointConfig {
        return mappings[type]?.get(raceGenderConfig) ?: throw IllegalStateException("$type is not mapped for $raceGenderConfig.")
    }

}