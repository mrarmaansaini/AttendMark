package com.example.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object WhatsAppHelper {
    const val PKG_WHATSAPP = "com.whatsapp"
    const val PKG_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun hasAnyWhatsAppInstalled(context: Context): Boolean {
        return isAppInstalled(context, PKG_WHATSAPP) || isAppInstalled(context, PKG_WHATSAPP_BUSINESS)
    }

    /**
     * Builds standard wa.me URL with correctly UTF-8 encoded text for emoji, Hindi, Punjabi, etc.
     */
    fun buildWaMeUrl(phoneE164WithoutPlus: String, messageText: String): String {
        val cleanPhone = phoneE164WithoutPlus.replace(Regex("[^0-9]"), "")
        val encodedMessage = URLEncoder.encode(messageText, StandardCharsets.UTF_8.name())
            .replace("+", "%20") // WhatsApp prefers %20 for space in some deep link parsers
        return "https://wa.me/$cleanPhone?text=$encodedMessage"
    }

    /**
     * Creates an Intent to launch WhatsApp chat.
     */
    fun createChatIntent(context: Context, phoneE164: String, messageText: String, preferredPackage: String = ""): Intent {
        val phoneClean = PhoneValidator.toWaMeFormat(phoneE164)
        val url = buildWaMeUrl(phoneClean, messageText)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        val targetPkg = when {
            preferredPackage.isNotEmpty() && isAppInstalled(context, preferredPackage) -> preferredPackage
            isAppInstalled(context, PKG_WHATSAPP) -> PKG_WHATSAPP
            isAppInstalled(context, PKG_WHATSAPP_BUSINESS) -> PKG_WHATSAPP_BUSINESS
            else -> null
        }

        if (targetPkg != null) {
            intent.setPackage(targetPkg)
        }
        return intent
    }
}
