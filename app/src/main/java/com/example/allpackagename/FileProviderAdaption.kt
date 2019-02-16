package com.example.allpackagename

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File


class FileProviderAdaption {
    companion object {
        fun getUriFile(context: Context, file: File, isShareSystem: Boolean = true): Uri? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getUriForFileIfN(context, file, isShareSystem)
            } else {
                Uri.fromFile(file)
            }
        }

        private fun getUriForFileIfN(
            context: Context,
            file: File,
            isShareSystem: Boolean = true
        ): Uri? {
            return if (isShareSystem) FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            else getFileContentUri(context, file)
        }


        fun setIntentDataAndType(
            context: Context,
            intent: Intent,
            type: String,
            file: File,
            writeAble: Boolean = false
        ): Intent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setDataAndType(getUriFile(context, file, false), type)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (writeAble)
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            } else {
                intent.setDataAndType(Uri.fromFile(file), type)
            }
            return intent
        }

        private fun getFileContentUri(context: Context, file: File): Uri? {
            val volumeName = "external"
            val filePath = file.absolutePath
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID
            )
            var uri: Uri? = null
            val cursor = context.contentResolver.query(
                MediaStore.Files.getContentUri(volumeName), projection,
                MediaStore.Images.Media.DATA + "=? ", arrayOf(filePath), null
            )

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val id = cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                    uri = MediaStore.Files.getContentUri(volumeName, id.toLong())
                }
                cursor.close()
            }
            if (uri == null)
                uri = forceGetFileUri(file)
            return uri
        }

        private fun forceGetFileUri(shareFile: File): Uri {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    @SuppressLint("PrivateApi")
                    val rMethod = StrictMode::class.java.getDeclaredMethod("disableDeathOnFileUriExposure")
                    rMethod.invoke(null)
                } catch (e: Exception) {
                    Log.e(TAG, Log.getStackTraceString(e))
                }
            }

            return Uri.parse("file://" + shareFile.absolutePath)
        }
    }
}