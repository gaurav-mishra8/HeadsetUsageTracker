package com.headphonetracker.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class DriveSyncManager(private val context: Context) {

    companion object {
        const val BACKUP_FILENAME = "headphone_tracker_backup.json"
        private val SCOPES = listOf(DriveScopes.DRIVE_APPDATA)
    }

    fun getSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, signInOptions)
    }

    fun getSignedInAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    fun signOut(): Task<Void> = getSignInClient().signOut()

    suspend fun uploadBackup(jsonString: String): DriveFile = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
            ?: throw IllegalStateException("No Google account signed in")
        val driveService = buildDriveService(account)

        val content = ByteArrayContent("application/json", jsonString.toByteArray(Charsets.UTF_8))
        val existing = findBackupFile(driveService)

        if (existing != null) {
            driveService.files().update(existing.id, null, content)
                .setFields("id, modifiedTime")
                .execute()
        } else {
            val metadata = DriveFile().apply {
                name = BACKUP_FILENAME
                parents = listOf("appDataFolder")
            }

            driveService.files().create(metadata, content)
                .setFields("id, modifiedTime")
                .execute()
        }
    }

    suspend fun downloadBackup(): String? = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
            ?: throw IllegalStateException("No Google account signed in")
        val driveService = buildDriveService(account)

        val existing = findBackupFile(driveService) ?: return@withContext null
        val outputStream = ByteArrayOutputStream()
        driveService.files().get(existing.id).executeMediaAndDownloadTo(outputStream)
        outputStream.toString(Charsets.UTF_8.name())
    }

    suspend fun hasBackup(): Boolean = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
            ?: return@withContext false
        val driveService = buildDriveService(account)
        findBackupFile(driveService) != null
    }

    private fun buildDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(context, SCOPES).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Headphone Tracker").build()
    }

    private fun findBackupFile(driveService: Drive): DriveFile? {
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name='${BACKUP_FILENAME}' and trashed=false")
            .setFields("files(id, name, modifiedTime)")
            .execute()

        return result.files?.firstOrNull()
    }
}
