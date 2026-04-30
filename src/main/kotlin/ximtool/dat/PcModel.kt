package ximtool.dat

object PcModel {

    fun getRaceGenderResource(raceGender: RaceGenderConfig): Directory {
        val fileTableIndex = MainDll.getBaseRaceConfigIndex(raceGender)
        val file = DatFile.fileTableIndex(fileTableIndex)
        return DatTree.parse(file)
    }

}