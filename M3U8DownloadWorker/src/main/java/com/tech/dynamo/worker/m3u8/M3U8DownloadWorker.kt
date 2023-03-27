package com.tech.dynamo.worker.m3u8

import com.google.android.exoplayer.hls.HlsMediaPlaylist
import com.google.android.exoplayer.hls.HlsPlaylistParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import net.m3u8.utils.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.InvalidAlgorithmParameterException
import java.security.NoSuchAlgorithmException
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.management.openmbean.InvalidKeyException

class M3U8DownloadWorker {
    private val workscope = CoroutineScope(Dispatchers.IO)
    private val client = OkHttpClient.Builder().build()
    companion object {
        private val LOCK = Any()
        private var instance: M3U8DownloadWorker? = null

        @JvmStatic
        fun getManager(): M3U8DownloadWorker {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = M3U8DownloadWorker()
                }
            }
            return instance!!
        }
    }

    suspend fun fetchM3U8(url: String): Flow<M3U8Info> = flow {
        val domainPath = url.substringBeforeLast("/") + "/"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            response.body?.let { body ->
                (HlsPlaylistParser().parse(
                    url,
                    body.byteStream()
                ) as? HlsMediaPlaylist)?.let { playlist ->
                    emit(M3U8Info(playlist, domainPath))
                }
            }
        } else {
            throw IOException("Unexpected code $response")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetchEncryptionTSFiles(
        cachedir: File,
        baseUri: String,
        segments: List<HlsMediaPlaylist.Segment>
    ): Flow<List<File>> = flow {
        val tsFiles = mutableListOf<File>()
        segments.forEach { segment ->
            fetchTSEncryptionInfo(baseUri, segment).flatMapConcat { info ->
                fetchEncryptionTSFile(cachedir, baseUri, segment, info)
            }.collect {
                Log.e("DownloadManager", "Show  finish  file : $it")
                tsFiles.add(it)
            }
        }
        emit(tsFiles)
    }

    suspend fun fetchTSEncryptionInfo(
        baseUri: String,
        segment: HlsMediaPlaylist.Segment
    ): Flow<EncryptionInfo> = flow {
//        val keyUri = UriUtil.resolveToUri(baseUri, segment.encryptionKeyUri)
        val keyUri = segment.encryptionKeyUri
        val request = Request.Builder().url(keyUri.toString()).build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            response.body?.let { body ->
                val result = body.bytes()
                emit(EncryptionInfo(segment.encryptionIV, result))
            }
        }
    }

    suspend fun fetchEncryptionTSFile(
        cachedir: File,
        baseUri: String,
        segment: HlsMediaPlaylist.Segment,
        encryptioninfo: EncryptionInfo
    ): Flow<File> = flow {
        val domainPath = baseUri.substringBeforeLast("/") + "/"
        val request = Request.Builder().url(domainPath + segment.url).build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val tsFileName = segment.url.replace(domainPath, "")
            val tsfile = File(cachedir, tsFileName)
            response.body?.byteStream()?.use { encryption_inputstream ->
                val cipher: Cipher = try {
                    Cipher.getInstance("AES/CBC/PKCS7Padding")
                } catch (e: NoSuchAlgorithmException) {
                    throw RuntimeException(e)
                } catch (e: NoSuchPaddingException) {
                    throw RuntimeException(e)
                }
                val iv: AlgorithmParameterSpec = IvParameterSpec(getEncryptionData(encryptioninfo.encryptionIV))
                val key  = SecretKeySpec(encryptioninfo.key , KeyProperties.KEY_ALGORITHM_AES)
                try {
                    cipher.init(Cipher.DECRYPT_MODE, key, iv)
                } catch (e: InvalidKeyException) {
                    throw java.lang.RuntimeException(e)
                } catch (e: InvalidAlgorithmParameterException) {
                    throw java.lang.RuntimeException(e)
                }
                CipherInputStream(encryption_inputstream, cipher).use { decrypt_inputstream->
                    FileOutputStream(tsfile).use { output ->
                        val buffer = ByteArray(4 * 1024)
                        var read: Int
                        var total: Long = 0
                        val contentLength = response.body?.contentLength() ?: 0
                        while (decrypt_inputstream.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            total += read.toLong()
                        }
                    }
                }
            }
            emit(tsfile)
        }
    }

    private fun getEncryptionData(iv: String): ByteArray {
        Log.d("DownloadManager", "Show getEncryptionData")
        val lowercase = iv.toLowerInvariant()
        val trimmedIv = if (lowercase != null && lowercase.startsWith("0x")) {
            iv.substring(2)
        } else {
            iv
        }
        Log.d("DownloadManager", "Show getEncryptionData trimmedIv  $trimmedIv ")
        val ivData = BigInteger(trimmedIv, 16).toByteArray()
        val ivDataWithPadding = ByteArray(16)
        val offset = if (ivData.size > 16) ivData.size - 16 else 0
        System.arraycopy(
            ivData, offset, ivDataWithPadding, ivDataWithPadding.size - ivData.size
                    + offset, ivData.size - offset
        )
        Log.d("DownloadManager", "Show getEncryptionData ivDataWithPadding  $ivDataWithPadding")
        return ivDataWithPadding
    }

    private fun String.toLowerInvariant() : String? {
        return this?.lowercase()
    }
}