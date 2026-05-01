package ximtool.gui

import ximtool.misc.LogColor
import java.awt.Color
import java.io.OutputStream
import java.io.PrintStream
import javax.swing.JTextPane
import javax.swing.text.StyleConstants

class LogListener(outputStream: OutputStream): PrintStream(outputStream) {

    private val listeners = ArrayList<() -> JTextPane?>()

    fun register(provider: () -> JTextPane?) {
        listeners += provider
    }

    override fun println(x: String?) {
        val area = listeners.firstNotNullOfOrNull { it.invoke() } ?: return

        var cleaned = x ?: return
        LogColor.entries.forEach { cleaned = cleaned.replace(it.code, "") }

        val document = area.styledDocument

        val style = area.addStyle("line", null)

        if (cleaned.contains("[WARN]")) {
            StyleConstants.setForeground(style, Color.YELLOW)
        } else if (cleaned.contains("[ERROR]")) {
            StyleConstants.setForeground(style, Color.RED)
        } else if (cleaned.contains("[DEBUG]")) {
            StyleConstants.setForeground(style, Color(0x40, 0x40, 0x40))
        }

        document.insertString(document.length, cleaned + "\n", style)
    }

}