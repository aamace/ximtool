package ximtool.misc

import ximtool.misc.LogColor.None
import java.io.PrintStream

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

    var out: PrintStream = System.out

    fun debug(msg: String, color: LogColor = None) {
        out.println("${LogColor.Teal.code}[DEBUG] ${color.code}$msg${None.code}")
        out.flush()
    }

    fun info(msg: String, color: LogColor = None) {
        out.println("[INFO] ${color.code}$msg${None.code}")
        out.flush()
    }

    fun warn(msg: String, color: LogColor = None) {
        out.println("${LogColor.Yellow.code}[WARN] ${color.code}$msg${None.code}")
        out.flush()
    }

    fun error(msg: String, color: LogColor = None) {
        out.println("${LogColor.Red.code}[ERROR] ${color.code}$msg${None.code}")
        out.flush()
    }

}