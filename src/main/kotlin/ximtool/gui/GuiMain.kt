package ximtool.gui

import com.formdev.flatlaf.FlatDarkLaf
import ximtool.misc.Log
import ximtool.resource.ResourceReader
import java.awt.Dimension
import java.awt.Point
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities

val logListener by lazy {
    LogListener(System.out).also { Log.out = it }
}

fun main() {
    FlatDarkLaf.setup()
    SwingUtilities.invokeLater { makeFrame() }
}

private fun makeFrame() {
    val frame = JFrame()
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.title = "Xim Tools"

    frame.minimumSize = Dimension(600, 300)
    frame.iconImage = ImageIO.read(ResourceReader.getInputStream("Gui/icon.png"))
    frame.location = Point(200, 200)

    val tabbedPane = JTabbedPane()
    tabbedPane.add("Settings", SettingsGui.makePanel())
    tabbedPane.add("Export to glTF", ItemModelExportGui.makePanel(tabbedPane))
    tabbedPane.add("Import from glTF", ItemModelImportGui.makePanel(tabbedPane))
    frame.add(tabbedPane)

    frame.pack()
    frame.setVisible(true)
}

