package com.tech.dynamo.worker.ts.merge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class TSMergeWorker {
    private val workscope = CoroutineScope(Dispatchers.IO)
    private var _stateFlow: MutableStateFlow<MergeState> = MutableStateFlow(MergeState(0,0,null))
    val stateFlow: StateFlow<MergeState> = _stateFlow
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
   fun mergeTStoMp4(tsFiles: List<File> ,target_folder: File,mp4_file_name:String) {
        workscope.launch {
            val total=tsFiles.size
            mergeTStoMp4Flow(tsFiles,target_folder,mp4_file_name).collect {
                val tsFile = File(it)
                if (tsFile.isFile&& tsFile.exists()){
                    _stateFlow.value= MergeState(total,total,tsFile.absolutePath)
                }
            }
        }
    }

    suspend fun mergeTStoMp4Flow(tsFiles: List<File> ,target_folder: File,mp4_file_name:String): Flow<String> = flow {
        val tsFile = File(target_folder, mp4_file_name)
        val total=tsFiles.size
        FileOutputStream(tsFile).use { outputstream ->
            for (current_file in tsFiles) {
                val index= tsFiles.indexOf(current_file)
                _stateFlow.value= MergeState(index,total,null )
                current_file.inputStream().use { inputStream ->
                    val buffer = ByteArray(1024 * 1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputstream.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
        if (tsFile.isFile&& tsFile.exists()){
            _stateFlow.value= MergeState(total,total,tsFile.absolutePath)
        }
        emit(tsFile.absolutePath)
    }.flowOn(Dispatchers.IO)
}