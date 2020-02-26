package net.pandolia.jane.libs

import java.awt.Desktop
import java.net.URI

object Desk {
    fun openBrowser(uri: String) {
        if (!Desktop.isDesktopSupported()) {
            return
        }

        Desktop.getDesktop().browse(URI.create(uri))
    }
}