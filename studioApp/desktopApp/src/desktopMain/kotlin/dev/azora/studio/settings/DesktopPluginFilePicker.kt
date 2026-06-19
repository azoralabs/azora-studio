package dev.azora.studio.settings

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Desktop [PluginFilePicker] using a native AWT [FileDialog] (LOAD) filtered to `.jar` files.
 * Falls back to a [JFileChooser] if the native dialog returns nothing usable.
 */
class DesktopPluginFilePicker : PluginFilePicker {

    override fun pickPluginJar(): String? {
        // Native FileDialog is the most reliable cross-platform native picker.
        val dialog = FileDialog(Frame(), "Select Plugin JAR", FileDialog.LOAD).apply {
            isMultipleMode = false
            // FilenameFilter works on some LAFs; we also validate the extension afterwards.
            setFilenameFilter { _, name -> name.endsWith(".jar", ignoreCase = true) }
            isVisible = true
        }
        val file = dialog.file
        val dir = dialog.directory
        if (file != null) {
            val chosen = File(dir, file)
            if (chosen.extension.equals("jar", ignoreCase = true) && chosen.exists()) {
                return chosen.absolutePath
            }
        }

        // Fallback: Swing JFileChooser.
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            fileFilter = FileNameExtensionFilter("Azora Plugin (*.jar)", "jar")
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.absolutePath
        } else null
    }
}
