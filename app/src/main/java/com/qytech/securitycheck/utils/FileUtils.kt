package com.qytech.securitycheck.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.AtomicFile
import androidx.core.content.FileProvider
import com.qytech.securitycheck.BuildConfig
import timber.log.Timber
import java.io.*
import java.lang.reflect.InvocationTargetException

object FileUtils {
    private const val BUFFER_SIZE = 8192

    fun getUriFromFile(context: Context?, file: File?): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context!!,
                BuildConfig.APPLICATION_ID + ".fileProvider",
                file!!
            )
        } else {
            Uri.fromFile(file)
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    fun getFilePathByUri(context: Context, uri: Uri): String? {
        var path: String? = null
        // 以 file:// 开头的
        if (ContentResolver.SCHEME_FILE == uri.scheme) {
            path = uri.path
            return path
        }
        // 以 content:// 开头的，比如 content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT == uri.scheme && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DATA),
                null,
                null,
                null
            )
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex)
                    }
                }
                cursor.close()
            }
            return path
        }
        // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT == uri.scheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) { // ExternalStorageProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        path = Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                        return path
                    }
                } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                    path = getDataColumn(context, contentUri, null, null)
                    return path
                } else if (isMediaDocument(uri)) { // MediaProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val contentUri: Uri =
                        when (split[0]) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])
                    path = getDataColumn(context, contentUri, selection, selectionArgs)
                    return path
                }
            }
        }
        return null
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        val column = "_data"
        val projection = arrayOf(column)
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            .use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(columnIndex)
                }
            }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    //2019/01/24 修复获取文件夹大小时没有深度遍历文件夹问题
    fun getFileSize(file: File?): Long {
        if (file == null || !file.exists()) return 0
        if (!file.isDirectory) {
            return file.length()
        }
        var size = 0L
        val files = file.listFiles()
        for (item in files) {
            size += getFileSize(item)
        }
        return size
    }

    fun delete(file: File?): Boolean {
        if (file == null || !file.exists()) return false
        if (file.isFile) {
            return file.delete()
        }
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                delete(child)
            }
            return file.delete()
        }
        return false
    }

    fun move(source: File, target: File): Boolean {
        if (!source.renameTo(target)) {
            if (copy(source, target)) {
                return delete(source)
            }
        }
        return false
    }

    fun copy(source: File, target: File): Boolean {
        if (!source.exists()) return false
        if (source.isDirectory) {
            if (createOrExistsDir(target)) {
                return source.list().all {
                    Timber.d("copy date: 2019-12-23 $it")
                    copy(File(source, it), File(target, it))
                }
            }
        } else if (source.isFile) {
            return copyFile(source, target)
        }
        return false
    }

    private fun copyFile(source: File, target: File): Boolean {
        if (!source.exists() || !source.isFile) return false
        if (!target.exists()) {
            createOrExistsFile(target)
        }
        val fileInputStream = FileInputStream(source)
        return writeFileFromIS(target, fileInputStream)
    }

    /**
     * 读取文件
     *
     * @param file
     * @return 字符串
     */
    fun readFromFile(file: File?): String? {
        return if (file != null && file.exists()) {
            try {
                val fin = FileInputStream(file)
                val reader = BufferedReader(InputStreamReader(fin))
                val value = reader.readLine()
                fin.close()
                value
            } catch (e: IOException) {
                null
            }
        } else null
    }

    /**
     * 文件中写入字符串
     */
    fun write2File(file: File?, value: String?): Boolean {
        return if (file?.exists() == true) try {
            val fout = FileOutputStream(file)
            val pWriter = PrintWriter(fout)
            pWriter.println(value)
            pWriter.flush()
            pWriter.close()
            fout.close()
            true
        } catch (re: IOException) {
            Timber.d("write file fail ${re.message}")
            false
        } else false
    }

    fun getStoragePath(context: Context): String? {
        val mStorageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolumeClazz: Class<*>
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            val getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
            val getPath = storageVolumeClazz.getMethod("getPath")
            val isRemovable = storageVolumeClazz.getMethod("isRemovable")
            val result = getVolumeList.invoke(mStorageManager)
            val length = java.lang.reflect.Array.getLength(result)
            for (i in 0 until length) {
                val storageVolumeElement = java.lang.reflect.Array.get(result, i)
                val path = getPath.invoke(storageVolumeElement) as String
                val removable = isRemovable.invoke(storageVolumeElement) as Boolean
                if (removable && !TextUtils.isEmpty(path)) {
                    return path.replace("/storage", "/mnt/media_rw")
//                    return path
                }
            }
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return null
    }

    fun copyFileFromAssets(
        context: Context,
        assetsFilePath: String,
        destFilePath: String
    ): Boolean {
        var res = true
        try {
            val assets = context.assets.list(assetsFilePath)
            if (!assets.isNullOrEmpty()) {
                for (asset in assets) {
                    res = res and copyFileFromAssets(
                        context,
                        "$assetsFilePath/$asset",
                        "$destFilePath/$asset"
                    )
                }
            } else {
                res =
                    writeFileFromIS(File(destFilePath), context.assets.open(assetsFilePath), false)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            res = false
        }
        return res
    }


    private fun writeFileFromIS(
        file: File,
        inputStream: InputStream?,
        append: Boolean = false
    ): Boolean {
        if (!createOrExistsFile(file) || inputStream == null) return false
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()

        return try {
            val data = ByteArray(BUFFER_SIZE)
            var len = -1
            while (inputStream.read(data, 0, BUFFER_SIZE).also { len = it } != -1) {
                stream.write(data, 0, len)
            }
//            stream.write(inputStream.readBytes())
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                atomicFile.finishWrite(stream)
            } catch (e: Exception) {
                e.printStackTrace()
                atomicFile.failWrite(stream)
            }
            try {
                stream.flush()
                stream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createOrExistsFile(file: File?): Boolean {
        if (file == null) return false
        if (file.exists()) return file.isFile
        return if (!createOrExistsDir(file.parentFile)) false else try {
            file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun createOrExistsDir(file: File?): Boolean {
        return file != null && if (file.exists()) file.isDirectory else file.mkdirs()
    }

    fun notificationScanFile(context: Context, source: File, target: File? = null) {
        Timber.d("notificationScanFile date: ${source.path} ")
        var paths = arrayOf<String>(source.absolutePath)
        if (target != null) {
            val list = paths.toMutableList()
            list.add(target.absolutePath)
            paths = list.toTypedArray()
        }
        MediaScannerConnection.scanFile(context, paths, null, null)
    }

    fun notificationDeleteFile(context: Context, file: File) {
        Timber.d("notificationDeleteFile date: delete file ")
        context.contentResolver.delete(getUriFromFile(context, file), null, null)
    }
}