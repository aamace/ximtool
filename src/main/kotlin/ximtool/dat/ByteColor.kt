package ximtool.dat

data class ByteColor(var r: Int, var g: Int, var b: Int, var a: Int) {

    companion object {
        val half = ByteColor(0x80, 0x80, 0x80, 0x80)
    }

    constructor(other: ByteColor): this(other.r, other.g, other.b, other.a)

}