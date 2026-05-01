package ximtool.gui

import ximtool.Environment
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.NORTHWEST
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.*
import javax.swing.JFileChooser.APPROVE_OPTION
import javax.swing.JFileChooser.DIRECTORIES_ONLY


object SettingsGui {

    private val inputSize = Dimension(300, 20)

    fun makePanel(): JPanel {

        val layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.anchor = NORTHWEST
        gbc.insets = Insets(10, 10, 0, 16)

        val formPanel = JPanel(layout)

        formPanel.add(JLabel("FFXI Directory:"), gbc)

        gbc.gridx = 1
        val ffxiBox = JTextField().also {
            it.preferredSize = inputSize
            it.text = Environment.ffxiDir
        }
        formPanel.add(ffxiBox, gbc)
        ffxiBox.isEnabled = false

        gbc.gridx = 2
        val ffxiSelect = JButton("...")
        formPanel.add(ffxiSelect, gbc)
        ffxiSelect.addActionListener {
            openFileChooser(ffxiBox) { Environment.ffxiDir = it.absolutePath }
        }

        gbc.weighty = 1.0
        gbc.gridy = 1
        gbc.gridx = 0
        formPanel.add(JLabel("<html>Import Destination&#9432;</html>").also {
            it.toolTipText = "The destination of .DAT files that are created by the glTF importer.\nIf this is the same as FFXI Directory, backups will be created automatically."
        }, gbc)

        gbc.gridx = 1
        val destBox = JTextField().also {
            it.preferredSize = inputSize
            it.text = Environment.importDestinationDir
        }
        formPanel.add(destBox, gbc)
        destBox.isEnabled = false

        gbc.gridx = 2
        gbc.weightx = 1.0
        val destSelect = JButton("...")
        formPanel.add(destSelect, gbc)
        destSelect.addActionListener {
            openFileChooser(destBox) { Environment.importDestinationDir = it.absolutePath }
        }

        return formPanel
    }

    private fun openFileChooser(start: JTextField, callback: (File) -> Unit) {
        val chooser = JFileChooser().also {
            it.fileSelectionMode = DIRECTORIES_ONLY
            it.currentDirectory = File(start.text)
        }

        if (chooser.showOpenDialog(null) == APPROVE_OPTION) {
            start.text = chooser.selectedFile.absolutePath
            callback.invoke(chooser.selectedFile)
        }
    }

}