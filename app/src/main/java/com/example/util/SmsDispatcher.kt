package com.example.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

object SmsDispatcher {

    /**
     * Safely launches the system's default SMS application pre-filled with the selected canned text.
     * Uses zero dangerous permissions (no SEND_SMS or READ_SMS required).
     */
    fun launchSms(
        context: Context,
        selectedText: String,
        onError: (String) -> Unit = {}
    ): Boolean {
        // Construct standard SMS intent as specified in requirements
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", selectedText)
            putExtra(Intent.EXTRA_TEXT, selectedText)
        }

        // Check if there is an SMS app registered to handle smsto:
        val packageManager = context.packageManager
        val canResolve = try {
            intent.resolveActivity(packageManager) != null
        } catch (e: Exception) {
            false
        }

        if (canResolve) {
            return try {
                context.startActivity(intent)
                true
            } catch (e: ActivityNotFoundException) {
                onError("No SMS messaging application found on this device.")
                false
            } catch (e: Exception) {
                onError("Could not open messaging app: ${e.localizedMessage ?: "Unknown error"}")
                false
            }
        }

        // Fallback check with "sms:" URI scheme
        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:")
            putExtra("sms_body", selectedText)
            putExtra(Intent.EXTRA_TEXT, selectedText)
        }

        val canResolveFallback = try {
            fallbackIntent.resolveActivity(packageManager) != null
        } catch (e: Exception) {
            false
        }

        if (canResolveFallback) {
            return try {
                context.startActivity(fallbackIntent)
                true
            } catch (e: ActivityNotFoundException) {
                onError("No SMS messaging application found on this device.")
                false
            } catch (e: Exception) {
                onError("Could not open messaging app: ${e.localizedMessage ?: "Unknown error"}")
                false
            }
        }

        // As a last attempt, try launching directly within a try-catch block
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            onError("No SMS messaging app installed on this device.")
            false
        } catch (e: Exception) {
            onError("Could not open messaging app: ${e.localizedMessage ?: "Unknown error"}")
            false
        }
    }

    fun copyToClipboard(context: Context, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Canned Response", text)
            clipboard?.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }
}
