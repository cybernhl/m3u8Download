package com.tech.dynamo.worker.ts.merge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream

class TSMergeWorker {
    private val workscope = CoroutineScope(Dispatchers.IO)
    private var _stateFlow: MutableStateFlow<File?> = MutableStateFlow(null)
    val stateFlow: StateFlow<File?> = _stateFlow
    companion object {
        private val LOCK = Any()
        private var instance: TSMergeWorker? = null

        @JvmStatic
        fun getManager(): TSMergeWorker {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = TSMergeWorker()
                }
            }
            return instance!!
        }
    }

    suspend fun mergeTStoMp4(tsFiles: List<File> ,target_folder: File,mp4_file_name:String): Flow<String> = flow {
        val tsFile = File(target_folder, mp4_file_name)
        FileOutputStream(tsFile).use { outputstream ->
            for (tsFile in tsFiles) {
                tsFile.inputStream().use { inputStream ->
                    val buffer = ByteArray(1024 * 1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputstream.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
        if (tsFile.isFile&& tsFile.exists()){
            _stateFlow.value=tsFile
        }
        emit(tsFile.absolutePath)
    }.flowOn(Dispatchers.IO)
}