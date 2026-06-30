package dev.azora.sdk.core.presentation.image

import dev.azora.sdk.core.presentation.util.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDictionary
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

/**
 * iOS implementation of [PhotoPickerLauncher] using [UIImagePickerController].
 *
 * Presents the system photo library picker on the key window's root view controller and delivers
 * the chosen image as JPEG bytes to the callback supplied to [launch].
 */
actual class PhotoPickerLauncher {

    // Held strongly while the picker is presented; the delegate is otherwise only weakly referenced.
    private var delegate: PhotoPickerDelegate? = null

    @OptIn(ExperimentalForeignApi::class)
    actual fun launch(onPhotoPicked: (ByteArray) -> Unit) {
        val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return

        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary

        val pickerDelegate = PhotoPickerDelegate(
            onPicked = { bytes ->
                delegate = null
                onPhotoPicked(bytes)
            },
            onDismiss = { delegate = null }
        )
        delegate = pickerDelegate
        picker.delegate = pickerDelegate

        rootController.presentViewController(picker, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PhotoPickerDelegate(
    private val onPicked: (ByteArray) -> Unit,
    private val onDismiss: () -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        if (image != null) {
            UIImageJPEGRepresentation(image, 0.9)?.let { onPicked(it.toByteArray()) }
        } else {
            onDismiss()
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onDismiss()
    }
}
