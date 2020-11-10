package com.qytech.securitycheck.extensions

/**
 * Created by Jax on 2020/11/10.
 * Description :
 * Version : V1.0.0
 */

import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

val File.fileType: String?
    get() = name.split(".").lastOrNull()

/**
 * check file is image type
 */
val File.isImage: Boolean
    get() {
        val list = listOf(".PNG", ".JPG", ".JPEG", ".BMP", ".GIF", ".WEBP")
        return ".${fileType}".toUpperCase(Locale.getDefault()) in list
    }

/**
 *check file is video type
 */
val File.isVideo: Boolean
    get() {
        val list = listOf(".AVI", ".MP4", ".FLV", ".ASF", ".MKV", ".MOV")
        return ".${fileType}".toUpperCase(Locale.getDefault()) in list
    }

fun File.getFileMD5(): String? {
    if (!this.isFile || !this.exists()) {
        return ""
    }
    var digest: MessageDigest? = null
    var fis: FileInputStream? = null
    val buffer = ByteArray(1024)
    var len: Int
    try {
        digest = MessageDigest.getInstance("MD5")
        fis = FileInputStream(this)
        while (fis.read(buffer, 0, 1024).also { len = it } != -1) {
            digest.update(buffer, 0, len)
        }
        fis.close()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        return null
    }
    val bigInt = BigInteger(1, digest.digest())
    return bigInt.toString(16)
}