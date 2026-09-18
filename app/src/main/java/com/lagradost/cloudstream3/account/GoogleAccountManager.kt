package com.lagradost.cloudstream3.account

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.lagradost.cloudstream3.BuildConfig

/**
 * Optional Google sign-in for the Rei Stream profile. The local library is never sent to Google.
 *
 * Google requires a Web OAuth client ID registered for the application's signing certificate.
 * The ID is deliberately supplied by local.properties/CI, never committed to the source tree.
 */
class GoogleAccountManager(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    val signedInEmail: String? get() = preferences.getString(EMAIL, null)
    val signedInName: String? get() = preferences.getString(NAME, null)

    suspend fun signIn(): GoogleProfile {
        check(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            "Google sign-in is not configured. Set google.web_client_id in local.properties."
        }
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        try {
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val profile = GoogleProfile(
                email = googleCredential.id,
                displayName = googleCredential.displayName,
                avatarUrl = googleCredential.profilePictureUri?.toString(),
            )
            // ID tokens are intentionally not persisted. A backend must verify a token if cloud sync is added.
            preferences.edit().putString(EMAIL, profile.email).putString(NAME, profile.displayName).apply()
            return profile
        } catch (error: GetCredentialException) {
            throw GoogleSignInException("Não foi possível entrar com o Google.", error)
        }
    }

    fun signOut() = preferences.edit().clear().apply()

    private companion object {
        const val PREFERENCES = "rei_stream_google_account"
        const val EMAIL = "email"
        const val NAME = "name"
    }
}

data class GoogleProfile(val email: String, val displayName: String?, val avatarUrl: String?)
class GoogleSignInException(message: String, cause: Throwable) : Exception(message, cause)
