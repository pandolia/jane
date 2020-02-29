package net.pandolia.jane.libs

import java.awt.Desktop
import java.net.URI

object Desk {
    fun openBrowser(uri: String) {
        if (!Desktop.isDesktopSupported()) {
            Log.info("Desktop is not supported, cancle openning browser")
            return
        }

        try {
            Desktop.getDesktop().browse(URI.create(uri))
        } catch (ex: Exception) {
            Log.error("Failed to open broswer: ${ex.detail}")
        }
    }
}