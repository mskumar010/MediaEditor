package com.mediaeditor.core.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Create output file in app-specific directory or user-chosen SAF directory
    fun createOutputFile(
        name: String,
        extension: String,
        parentUri: Uri? = null // null = app cache dir
    ): Uri {
        return if (parentUri != null) {
            // SAF — user picked a folder
            val docTree = DocumentFile.fromTreeUri(context, parentUri)
                ?: throw java.io.IOException("Failed to access DocumentFile from URI")
            val file = docTree.createFile("*/*", "$name.$extension")
                ?: throw java.io.IOException("Failed to create file in SAF directory")
            file.uri
        } else {
            // App cache — always writable
            val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
            val file = File(dir, "$name.$extension")
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        }
    }

    // Copy finished export to MediaStore so it appears in gallery
    fun publishToMediaStore(sourceUri: Uri, name: String, mimeType: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH,
                    if (mimeType.startsWith("audio")) Environment.DIRECTORY_MUSIC
                    else Environment.DIRECTORY_MOVIES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val collection = if (mimeType.startsWith("audio"))
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val outputUri = context.contentResolver.insert(collection, values) ?: return null

        context.contentResolver.openOutputStream(outputUri)?.use { out ->
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.copyTo(out)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(outputUri, values, null, null)
        }
        return outputUri
    }

    // Get real file path from URI for FFmpeg (FFmpeg needs actual path)
    fun getRealPath(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path

        if (uri.scheme == "content") {
            // Check if it's our own FileProvider URI
            if (uri.authority == "${context.packageName}.provider") {
                val exportsDir = File(context.cacheDir, "exports")
                // FileProvider path is usually /exports/filename
                val segments = uri.pathSegments
                if (segments.isNotEmpty()) {
                    val file = File(exportsDir, segments.last())
                    if (file.exists()) return file.absolutePath
                }
            }

            // Try MediaStore
            val proj = arrayOf(MediaStore.MediaColumns.DATA)
            try {
                context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (idx != -1 && cursor.moveToFirst()) {
                        val path = cursor.getString(idx)
                        if (!path.isNullOrBlank()) return path
                    }
                }
            } catch (e: Exception) {
                // Ignore query failures
            }

            // Fallback: copy to cache and return that path
            return copyToCacheAndGetPath(uri)
        }
        return null
    }

    private fun copyToCacheAndGetPath(uri: Uri): String? {
        val fileName = getFileName(uri) ?: "temp_${System.currentTimeMillis()}"
        val cacheFile = File(context.cacheDir, "input/$fileName").also {
            it.parentFile?.mkdirs()
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { out -> input.copyTo(out) }
            }
            cacheFile.absolutePath
        } catch (e: Exception) { null }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        return name
    }
}
