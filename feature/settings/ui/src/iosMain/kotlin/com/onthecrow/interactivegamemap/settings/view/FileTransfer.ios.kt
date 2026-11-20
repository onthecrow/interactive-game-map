package com.onthecrow.interactivegamemap.settings.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject

/**
 * The document pickers of Files: the file is written to the temporary folder first and copied where the user picks;
 * a file picked is copied in, so it can be read without asking for access.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
internal actual fun rememberFileTransfer(onSaved: (Boolean) -> Unit, onOpened: (String) -> Unit): FileTransfer {
    val viewController = LocalUIViewController.current
    val currentOnSaved by rememberUpdatedState(onSaved)
    val currentOnOpened by rememberUpdatedState(onOpened)

    // The pickers keep weak references to their delegates, these live as long as the screen
    val saveDelegate = remember { PickerDelegate { currentOnSaved(true) } }
    val openDelegate = remember {
        PickerDelegate { urls ->
            val url = urls.firstOrNull() ?: return@PickerDelegate
            val content = NSString.stringWithContentsOfURL(url, encoding = NSUTF8StringEncoding, error = null)
            currentOnOpened(content ?: "")
        }
    }

    return remember(viewController) {
        object : FileTransfer {
            override fun save(fileName: String, content: String) {
                val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
                val isWritten = NSString.create(string = content)
                    .writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
                if (!isWritten) {
                    currentOnSaved(false)
                    return
                }
                val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
                picker.delegate = saveDelegate
                viewController.topmost().presentViewController(picker, animated = true, completion = null)
            }

            override fun open() {
                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypeJSON, UTTypePlainText, UTTypeData),
                    asCopy = true,
                )
                picker.delegate = openDelegate
                viewController.topmost().presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

private class PickerDelegate(private val onPicked: (List<NSURL>) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onPicked(didPickDocumentsAtURLs.filterIsInstance<NSURL>())
    }

    // A cancel does nothing
    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = Unit
}

// A dialog over the screen presents the picker, not the screen under it
private fun UIViewController.topmost(): UIViewController {
    var controller = this
    while (true) controller = controller.presentedViewController ?: return controller
}
