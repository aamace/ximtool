package ximtool.dat

enum class RaceGenderConfig(val index: Int, val equipmentTableIndex: Int = index) {
    HumeM(index = 1),
    HumeF(index = 2),
    ElvaanM(index = 3),
    ElvaanF(index = 4),
    TaruM(index = 5),
    TaruF(index = 6),
    Mithra(index = 7),
    Galka(index = 8),
    ;
}

enum class ItemModelSlot(val index: Int, val prefix: Int = index * 0x1000) {
    Face(0),
    Head(1),
    Body(2),
    Hands(3),
    Legs(4),
    Feet(5),
    Main(6),
    Sub(7),
    Range(8),
    ;
}
