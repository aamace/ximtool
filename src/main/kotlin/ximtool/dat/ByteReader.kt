package ximtool.dat

import ximtool.math.Vector2f
import ximtool.math.Vector3f
import java.lang.Float.floatToRawIntBits
import kotlin.experimental.xor

class ByteReader(val bytes: ByteArray) {

    var position: Int = 0

    fun nextDatId() : DatId {
        val id = nextString(0x4)
        return DatId(id)
    }

    fun nextString(length: Int): String {
        val id = StringBuilder()

        for (i in 0 until length) {
            id.append(nextChar())
        }

        return id.toString()
    }

    fun nextTextureName(): TextureName {
        return TextureName(nextString(0x10))
    }

    fun nextZeroTerminatedString(): String {
        val id = StringBuilder()

        while (true) {
            val next = nextChar()
            if (next == 0.toChar()) { break }
            id.append(next)
        }

        return id.toString()
    }

    fun nextStringWithMask(length: Int, mask: Int): String {
        val id = StringBuilder()

        for (i in 0 until length) {
            id.append(nextCharWithMask(mask))
        }

        return id.toString()
    }

    fun next8() : Int {
        val byte = bytes[position]
        position += 1
        return 0xFF and byte.toInt()
    }

    fun next8WithMask(mask: Int): Int {
        return next8() xor mask
    }

    fun next16() : Int {
        return (next8()) or (next8() shl 8)
    }

    fun next16Signed() : Int {
        return ((next8()) or (next8() shl 8)).toShort().toInt()
    }

    fun read16(offset: Int): Int {
        position = offset
        return next16()
    }

    fun next32() : Int {
        return ((next16()) or (next16() shl 16))
    }

    fun nextFloat(): Float {
        val readVal = next32()
        return Float.fromBits(readVal)
    }

    fun next32(amount: Int): IntArray {
        val dest = IntArray(amount) { 0 }
        next32(amount, dest)
        return dest
    }

    fun next16BE() : Int {
        return (next8() shl 8) or (next8())
    }

    fun next32BE() : Int {
        return ((next16BE() shl 16) or (next16BE()))
    }

    fun next64BE() : Long {
        return ((next32BE().toLong() shl 32) or (next32BE().toLong()))
    }

    fun nextFloat(amount: Int): FloatArray {
        val dest = FloatArray(amount) { 0f }
        nextFloat(amount, dest)
        return dest
    }

    fun next32(amount: Int, dest: IntArray) {
        for (i in 0 until amount) {
            dest[i] = next32()
        }
    }

    fun nextFloat(amount: Int, dest: FloatArray) {
        for (i in 0 until amount) {
            dest[i] = nextFloat()
        }
    }

    fun write8(value: Int) {
        bytes[position] = (value and 0xFF).toByte()
        position += 1
    }

    fun mask8(mask: Int) {
        val value = next8()
        position -= 1
        write8(value and mask)
    }

    fun mask16(mask: Int) {
        val value = next16()
        position -= 2
        write16(value and mask)
    }

    fun write16(value: Int) {
        write8(value and 0xFF)
        write8(value ushr 8)
    }

    fun write32(value: Int) {
        write16((value and 0xFFFF))
        write16(value ushr 16)
    }

    fun writeFloat(float: Float): Float {
        val rawValue = floatToRawIntBits(float)
        write32(rawValue)
        return float
    }

    fun writeBgra(byteColor: ByteColor): ByteReader {
        write8(byteColor.b)
        write8(byteColor.g)
        write8(byteColor.r)
        write8(byteColor.a)
        return this
    }

    fun writeRgba(byteColor: ByteColor): ByteReader {
        write8(byteColor.r)
        write8(byteColor.g)
        write8(byteColor.b)
        write8(byteColor.a)
        return this
    }

    fun write(floatArray: FloatArray): ByteReader {
        floatArray.forEach { writeFloat(it) }
        return this
    }

    fun write(vector: Vector3f): ByteReader {
        writeFloat(vector.x)
        writeFloat(vector.y)
        writeFloat(vector.z)
        return this
    }

    fun write(vector: Vector2f): ByteReader {
        writeFloat(vector.x)
        writeFloat(vector.y)
        return this
    }

    fun write(str: String) {
        str.toCharArray().forEach { write8(it.code) }
    }

    fun write(textureName: TextureName) {
        write(textureName.value)
    }

    fun write(array: ByteArray) {
        array.copyInto(destination = bytes, destinationOffset = position)
        position += array.size
    }

    fun write(datId: DatId) {
        write8(datId.id[0].code)
        write8(datId.id[1].code)
        write8(datId.id[2].code)
        write8(datId.id[3].code)
    }

    fun nextChar(): Char {
        return Char(next8())
    }

    fun nextCharWithMask(mask: Int): Char {
        return Char(next8WithMask(mask))
    }

    fun align0x10() {
        position = position.padTo16()
    }

    fun hasMore(): Boolean {
        return position < bytes.size
    }

    fun xorNext(mask: Byte): Byte {
        val masked = bytes[position] xor mask
        bytes[position] = masked
        position += 1
        return masked
    }

    fun swapNext8(offset: Int, repetitions: Int = 1) {
        for (i in 0 until repetitions) {
            val value = bytes[position]
            bytes[position] = bytes[position + offset]
            bytes[position + offset] = value
            position += 1
        }
    }

    fun subArray(length: Int, offset: Int = position): ByteArray {
        return bytes.sliceArray(offset until (offset + length))
    }

    fun remaining(): ByteArray {
        return subArray(length = bytes.size - position)
    }

    fun nextVector2f(): Vector2f {
        return Vector2f(nextFloat(), nextFloat())
    }

    fun nextVector3f(): Vector3f {
        return Vector3f(nextFloat(), nextFloat(), nextFloat())
    }

    fun nextRGBA(): ByteColor {
        return ByteColor(r = next8(), g = next8(), b = next8(), a = next8())
    }

    fun nextBGRA(): ByteColor {
        return ByteColor(b = next8(), g = next8(), r = next8(), a = next8())
    }

    fun rotateNext8(amount: Int) {
        bytes[position] = bytes[position].rotateRight(amount)
        position += 1
    }

    fun rotateAll(amount: Int) {
        for (i in bytes.indices) { bytes[i] = bytes[i].rotateRight(amount) }
    }

    override fun toString(): String {
        return "Pos: ${position.toString(0x10)} | Size: ${bytes.size.toString(0x10)}"
    }

}

fun Int.padTo16(): Int {
    return this + ((0x10 - (this % 0x10)) % 0x10)
}