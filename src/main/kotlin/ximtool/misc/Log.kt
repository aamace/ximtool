package ximtool.misc

import ximtool.misc.LogColor.None

enum class LogColor(val code: String) {
    None("\u001B[0m"),
    Red("\u001B[0;91m"),
    Green("\u001B[0;92m"),
    Yellow("\u001B[0;93m"),
    Blue("\u001B[0;94m"),
    Pink("\u001B[0;95m"),
    Teal("\u001B[0;96m"),
}

object Log {

    fun debug(msg: String, color: LogColor = None) {
        println("${LogColor.Yellow.code}[DEBUG] ${color.code}$msg${None.code}")
    }

    fun info(msg: String, color: LogColor = None) {
        println("[INFO] ${color.code}$msg${None.code}")
    }

}