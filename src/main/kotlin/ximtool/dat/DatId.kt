package ximtool.dat

data class DatId(val id: String) {

    companion object {
        val end = DatId("end${0.toChar()}")
        val zero = DatId("${0.toChar()}${0.toChar()}${0.toChar()}${0.toChar()}")
    }

    init {
        check(id.toCharArray().size == 4) { "ID must consist of 4 characters" }
    }

    override fun toString(): String {
        return id
    }

    fun isZero(): Boolean {
        return id == zero.id
    }

    fun toNullIfZero(): DatId? {
        return if (isZero()) { null } else { this }
    }

    fun incrementLastDigit(amount: Int = 1): DatId {
        val new = id.last().digitToInt() + amount
        val newId = id.take(3) + new.toString(0x10)
        return DatId(newId)
    }

}

object StandardDatIds {

    fun weaponMesh(mainHand: Boolean): DatId {
        return if (mainHand) { DatId("wep0") } else { DatId("wep1") }
    }

    fun weaponOnAttack(mainHand: Boolean): DatId {
        return if (mainHand) { DatId("skaz") } else { DatId("skal") }
    }

    fun weaponInitRoutine(mainHand: Boolean): DatId {
        return if (mainHand) { DatId("!w00") } else { DatId("!w10") }
    }

    fun weaponEngageRoutine(mainHand: Boolean): DatId {
        return if (mainHand) { DatId("!w01") } else { DatId("!w11") }
    }

    fun weaponDisengageRoutine(mainHand: Boolean): DatId {
        return if (mainHand) { DatId("!w02") } else { DatId("!w12") }
    }

}