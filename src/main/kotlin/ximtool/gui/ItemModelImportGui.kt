package ximtool.gui

import ximtool.dat.ItemModelSlot
import ximtool.dat.RaceGenderConfig
import ximtool.misc.Log
import ximtool.tools.ItemModelGltfImporter
import ximtool.tools.ModelGltfImporterConfig
import java.awt.*
import java.awt.GridBagConstraints.NORTHWEST
import java.text.NumberFormat
import java.util.concurrent.ForkJoinPool
import javax.swing.*

object ItemModelImportGui {

    private val inputSize = Dimension(120, 20)

    fun makePanel(owner: JTabbedPane): JComponent {
        val split = JSplitPane( JSplitPane.HORIZONTAL_SPLIT)

        val layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.anchor = NORTHWEST
        gbc.insets = Insets(10, 10, 0, 16)

        val formPanel = JPanel(layout)
        split.add(formPanel)

        gbc.gridx = 0
        formPanel.add(JLabel("Race:"), gbc)
        gbc.gridx = 1

        val raceBox = JComboBox(RaceGenderConfig.entries.toTypedArray()).also { it.preferredSize = inputSize }
        formPanel.add(raceBox, gbc)

        gbc.gridx = 0
        gbc.gridy = gbc.gridy++
        formPanel.add(JLabel("Item Slot:"), gbc)

        gbc.gridx = 1
        val slotBox = JComboBox(ItemModelSlot.entries.toTypedArray()).also { it.preferredSize = inputSize }
        formPanel.add(slotBox, gbc)

        gbc.gridx = 0
        gbc.gridy = gbc.gridy++
        formPanel.add(JLabel("Item Model ID:"), gbc)

        gbc.gridx = 1
        val idBox = JFormattedTextField(NumberFormat.INTEGER_FIELD).also { it.preferredSize = inputSize }
        formPanel.add(idBox, gbc)

        gbc.gridx = 0
        gbc.gridy = gbc.gridy++
        formPanel.add(JLabel("glTF File Name:"), gbc)

        gbc.gridx = 1
        val fileNameBox = JTextField().also { it.preferredSize = inputSize }
        fileNameBox.text = "model.gltf"
        formPanel.add(fileNameBox, gbc)

        gbc.gridx = 0
        gbc.gridy = gbc.gridy++
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        val importButton = JButton("Import from glTF")
        formPanel.add(importButton, gbc)

        val textPane = JTextPane()
        textPane.isEditable = false
        textPane.background = Color(0x1E, 0x1E, 0x1E)
        textPane.preferredSize = Dimension(600, 400)

        logListener.register { if (owner.selectedComponent == split) { textPane } else { null } }

        split.add(JScrollPane(textPane))

        importButton.addActionListener {
            textPane.text = ""
            val race = raceBox.selectedItem as RaceGenderConfig
            val slot = slotBox.selectedItem as ItemModelSlot
            val id = (idBox.value as Int)
            val fileName = fileNameBox.text

            val config = ModelGltfImporterConfig(
                modelFileName = fileName,
            )

            importButton.isEnabled = false
            ForkJoinPool.commonPool().submit( {
                executeImport(race, slot, id, config)
                importButton.isEnabled = true
            })
        }

        return split
    }

    private fun executeImport(raceGenderConfig: RaceGenderConfig, itemModelSlot: ItemModelSlot, modelId: Int, config: ModelGltfImporterConfig) {
        try {
            ItemModelGltfImporter(race = raceGenderConfig, itemModelSlot = itemModelSlot, itemModelId = modelId, config = config).apply()
        } catch (e: Exception) {
            Log.error("An error occurred: ${e.message}")
        }
    }

}