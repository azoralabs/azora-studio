package dev.azora.studio.settings

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Desktop [LibraryFilePicker]. Library bundles are either directories
 * (containing `library.json`) or `.azlib`/`.zip` archives, so this uses a
 * Swing [JFileChooser] in FILES_AND_DIRECTORIES mode (AWT's native dialog
 * cannot select directories on all platforms), falling back to the native
 * [FileDialog] for archive files.
 */
class DesktopLibraryFilePicker : LibraryFilePicker {

    override fun pickLibraryBundle(): String? {
        val chooser = JFileChooser().apply {
            dialogTitle = "Select Library Bundle (folder or .azlib)"
            fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
            fileFilter = FileNameExtensionFilter("Azora Library (*.azlib, *.zip)", "azlib", "zip")
            isAcceptAllFileFilterUsed = true
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return chooser.selectedFile?.absolutePath
        }

        // Fallback: native file dialog (archives only).
        val dialog = FileDialog(Frame(), "Select Library Archive", FileDialog.LOAD).apply {
            isMultipleMode = false
            setFilenameFilter { _, name ->
                name.endsWith(".azlib", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)
            }
            isVisible = true
        }
        val file = dialog.file ?: return null
        val chosen = File(dialog.directory, file)
        return chosen.takeIf { it.exists() }?.absolutePath
    }
}
