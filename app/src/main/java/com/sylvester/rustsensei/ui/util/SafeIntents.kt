package com.sylvester.rustsensei.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.sylvester.rustsensei.R

private const val TAG = "SafeIntents"

/**
 * Intent launches that cannot take the app down.
 *
 * ACTION_VIEW and ACTION_SEND both throw [ActivityNotFoundException] when nothing
 * on the device handles them. That is not exotic: managed work profiles, stripped
 * OEM images, emulators and kiosk devices routinely ship without a browser or any
 * share target, and every link in Settings was a one-tap crash there.
 */
fun Context.openUrlSafely(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No handler for $url")
        toastSafely(getString(R.string.no_browser_available))
    } catch (e: Exception) {
        Log.w(TAG, "Could not open $url: ${e.message}")
        toastSafely(getString(R.string.no_browser_available))
    }
}

/** Opens the Play listing, falling back to the web listing, then to a toast. */
fun Context.openPlayStoreSafely(appId: String) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(market)
    } catch (e: Exception) {
        openUrlSafely("https://play.google.com/store/apps/details?id=$appId")
    }
}

/** Shares plain text through the system chooser, or reports that nothing can. */
fun Context.shareTextSafely(text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        startActivity(
            Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        Log.w(TAG, "Could not share: ${e.message}")
        toastSafely(getString(R.string.no_share_target_available))
    }
}

private fun Context.toastSafely(message: String) {
    try {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.w(TAG, "Could not show toast: ${e.message}")
    }
}
