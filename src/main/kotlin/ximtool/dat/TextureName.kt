package ximtool.dat

@JvmInline
value class TextureName(val value: String) {
    init { check(value.length == 0x10) { "Texture names must be 16 characters in length" } }
}