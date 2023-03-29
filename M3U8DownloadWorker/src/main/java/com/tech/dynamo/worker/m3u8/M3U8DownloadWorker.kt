package com.tech.dynamo.worker.m3u8

import android.net.Uri
import android.security.keystore.KeyProperties
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistParser
import com.google.android.exoplayer2.util.UriUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.InvalidAlgorithmParameterException
import java.security.NoSuchAlgorithmException
import java.security.spec.AlgorithmParameterSpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class M3U8DownloadWorker {
    private val workscope = CoroutineScope(Dispatchers.IO)
    private val client = OkHttpClient.Builder().build()
    private var _stateFlow: MutableStateFlow<M3U8ProcessState> =
        MutableStateFlow(M3U8ProcessState(M3U8ActionStep.INIT, 0, 0, 0))
    val stateFlow: StateFlow<M3U8ProcessState> = _stateFlow

    companion object {
        private val LOCK = Any()
        private var instance: M3U8DownloadWorker? = null

        @JvmStatic
        fun getWorker(): M3U8DownloadWorker {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = M3U8DownloadWorker()
                }
            }
            return instance!!
        }
    }
    fun startFetch(cachedir: File, url: String) {
        workscope.launch {
            fetchM3U8(url).flatMapConcat { info ->
                val durationUs = info.list.durationUs //TODO notify time
                val segments = info.list.segments
                val firstcheck = segments.first()
                val isEncryption =
                    (!firstcheck.encryptionIV.isNullOrBlank() && !firstcheck.fullSegmentEncryptionKeyUri.isNullOrBlank())
                fetchTSFiles(cachedir, info.domain, segments, isEncryption)
            }.collect {
                val total = it.size
                _stateFlow.value = M3U8ProcessState(M3U8ActionStep.FETCH_TS, total, 100, total, it)
                println("M3U8DownloadWorker at startFetch Show  startFetch finish  file : $it")
            }
        }
    }
    private suspend fun fetchM3U8(url: String): Flow<M3U8Info> = flow {
        println("M3U8DownloadWorker at fetchM3U8 Show input url : $url")
        val domainPath = url.substringBeforeLast("/") + "/"
        val request = Request.Builder().url(url).build()
        _stateFlow.value = M3U8ProcessState(M3U8ActionStep.FETCH_M3U8, 1, 0, 1)
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            response.body()?.let { body ->
                when(val playlist=HlsPlaylistParser().parse( Uri.parse(url), body.byteStream())){
                    is HlsMediaPlaylist->{
                        println("M3U8DownloadWorker at fetchM3U8 Show playlist variants : ${playlist }")
                        _stateFlow.value = M3U8ProcessState(M3U8ActionStep.FETCH_M3U8, 1, 100, 1)
                        emit(M3U8Info(playlist, domainPath))
                    }
                    is  HlsMasterPlaylist->{
                        println("M3U8DownloadWorker at fetchM3U8 Show HlsMasterPlaylist variants : ${playlist.variants }")
                        playlist.variants.forEach {
                            println("M3U8DownloadWorker at fetchM3U8 Show HlsMasterPlaylist variant value  : ${it}")
                        }
                    }
                 }
            }
        } else {
            throw IOException("Unexpected code $response")
        }
    }.flowOn(Dispatchers.IO)
    private suspend fun fetchTSEncryptionInfo(
        baseUri: String,
        segment: HlsMediaPlaylist.Segment
    ): Flow<EncryptionInfo> = flow {
        val keyUri = UriUtil.resolveToUri(baseUri, segment.fullSegmentEncryptionKeyUri)
        val request = Request.Builder().url(keyUri.toString()).build()
        _stateFlow.value = M3U8ProcessState(M3U8ActionStep.FETCH_ENCRYPTION_INFO, 1, 0, 1)
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            response.body()?.let { body ->
                val result = body.bytes()
                _stateFlow.value = M3U8ProcessState(M3U8ActionStep.FETCH_ENCRYPTION_INFO, 1, 100, 1)
                emit(EncryptionInfo(segment.encryptionIV, result))
            }
        }
    }
    private suspend fun fetchTSFiles(
        cachedir: File,
        baseUri: String,
        segments: List<HlsMediaPlaylist.Segment>,
        isencryption: Boolean
    ): Flow<List<File>> = flow {
        val tsFiles = mutableListOf<File>()
        val total_count = segments.size
        segments.forEach { segment ->
            val index = segments.indexOf(segment)
            if (isencryption) {
                fetchTSEncryptionInfo(baseUri, segment).flatMapConcat { info ->
                    _stateFlow.value =
                        M3U8ProcessState(M3U8ActionStep.FETCH_TS, index, 0, total_count)
                    fetchTSFile(cachedir, baseUri, segment, info)
                }.collect {
                    println("M3U8DownloadWorker at fetchEncryptionTSFiles encryption type Show finish  file : $it")
                    _stateFlow.value =
                        M3U8ProcessState(M3U8ActionStep.FETCH_TS, index, 100, total_count)
                    tsFiles.add(it)
                }
            } else {
                _stateFlow.value = M3U8ProcessState(M3U8ActionStep.FETCH_TS, index, 0, total_count)
                fetchTSFile(cachedir, baseUri, segment, null).collect {
                    println("M3U8DownloadWorker at fetchEncryptionTSFiles Show finish  file : $it")
                    _stateFlow.value =
                        M3U8ProcessState(M3U8ActionStep.FETCH_TS, index, 100, total_count)
                    tsFiles.add(it)
                }
            }
        }
        emit(tsFiles)
    }

    private suspend fun fetchTSFile(
        cachedir: File,
        baseUri: String,
        segment: HlsMediaPlaylist.Segment,
        encryptioninfo: EncryptionInfo?
    ): Flow<File> = flow {
        val domainPath = baseUri.substringBeforeLast("/") + "/"
        val request = Request.Builder().url(domainPath + segment.url).build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val tsFileName = segment.url.replace(domainPath, "")
            val tsfile = File(cachedir, tsFileName)
            response.body()?.byteStream()?.use { encryption_inputstream ->
                if (encryptioninfo != null) {
                    val cipher: Cipher = try {
                        Cipher.getInstance("AES/CBC/PKCS7Padding")
                    } catch (e: NoSuchAlgorithmException) {
                        throw RuntimeException(e)
                    } catch (e: NoSuchPaddingException) {
                        throw RuntimeException(e)
                    }
                    val iv: AlgorithmParameterSpec =
                        IvParameterSpec(getEncryptionData(encryptioninfo.encryptionIV))
                    val key = SecretKeySpec(encryptioninfo.key, KeyProperties.KEY_ALGORITHM_AES)
                    try {
                        cipher.init(Cipher.DECRYPT_MODE, key, iv)
                    } catch (e: java.security.InvalidKeyException) {
                        throw java.lang.RuntimeException(e)
                    } catch (e: InvalidAlgorithmParameterException) {
                        throw java.lang.RuntimeException(e)
                    }
                    CipherInputStream(encryption_inputstream, cipher).use { decrypt_inputstream ->
                        FileOutputStream(tsfile).use { output ->
                            val buffer = ByteArray(4 * 1024)
                            var read: Int
                            var total: Long = 0
                            val contentLength = response.body()?.contentLength() ?: 0
                            while (decrypt_inputstream.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                total += read.toLong()
                            }
                        }
                    }
                } else {
                    FileOutputStream(tsfile).use { output ->
                        val buffer = ByteArray(4 * 1024)
                        var read: Int
                        var total: Long = 0
                        val contentLength = response.body()?.contentLength() ?: 0
                        while (encryption_inputstream.read(buffer).also { read = it } != -1) {
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
        println("M3U8DownloadWorker at getEncryptionData")
        val lowercase = iv.toLowerInvariant()
        val trimmedIv = if (lowercase != null && lowercase.startsWith("0x")) {
            iv.substring(2)
        } else {
            iv
        }
        println("M3U8DownloadWorker at getEncryptionData Show getEncryptionData trimmedIv  $trimmedIv ")
        val ivData = BigInteger(trimmedIv, 16).toByteArray()
        val ivDataWithPadding = ByteArray(16)
        val offset = if (ivData.size > 16) ivData.size - 16 else 0
        System.arraycopy(
            ivData, offset, ivDataWithPadding, ivDataWithPadding.size - ivData.size
                    + offset, ivData.size - offset
        )
        println("M3U8DownloadWorker at getEncryptionData Show getEncryptionData ivDataWithPadding  $ivDataWithPadding")
        return ivDataWithPadding
    }

    private fun String.toLowerInvariant(): String? {
        return this?.toLowerCase(Locale.getDefault())
    }
}