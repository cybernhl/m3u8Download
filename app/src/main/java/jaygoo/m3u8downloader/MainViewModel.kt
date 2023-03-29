package jaygoo.m3u8downloader

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.dynamo.worker.m3u8.M3U8ActionStep
import com.tech.dynamo.worker.m3u8.M3U8DownloadWorker
import com.tech.dynamo.worker.ts.merge.MergeActionStep
import com.tech.dynamo.worker.ts.merge.TSMergeWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class MainViewModel : ViewModel() {
    private val m3u8worker = M3U8DownloadWorker.getWorker()
    private val mergeworker = TSMergeWorker.getManager()
    private val _dlstate = MutableLiveData<DownloadStates>()
    val dlstate: LiveData<DownloadStates> = _dlstate


    fun DemoDL(cachedir: File,m3u8:String,result_file_name_with_ext:String) {
        viewModelScope.launch {
            m3u8worker.stateFlow.collectLatest { state ->
                val percentage = state.percentage
                when (state.step) {
                    M3U8ActionStep.INIT -> {
                        _dlstate.postValue(DownloadStates(M3U8ActionStep.INIT.name))
                    }
                    M3U8ActionStep.FETCH_M3U8 -> {
//                        Timber.e("Show  FETCH_M3U8 $state")
                        _dlstate.postValue(DownloadStates(M3U8ActionStep.FETCH_M3U8.name ))
                    }
                    M3U8ActionStep.FETCH_ENCRYPTION_INFO -> {
                        _dlstate.postValue(DownloadStates(M3U8ActionStep.FETCH_ENCRYPTION_INFO.name,state.current_fetch_file,state.percentage,state.total_fetch_files))
                    }
                    M3U8ActionStep.FETCH_TS -> {
                        Timber.e("Show  FETCH_TS  info current_fetch_file ${state.current_fetch_file} percentage : $percentage total_fetch_files ${state.total_fetch_files}"  )
                        _dlstate.postValue(DownloadStates(M3U8ActionStep.FETCH_TS.name,state.current_fetch_file,percentage,state.total_fetch_files))
                        if (percentage==100 &&(state.current_fetch_file == state.total_fetch_files)){
                            state.tsFiles?.let { files ->
                                //TODO show start merge
                                _dlstate.postValue(DownloadStates(MergeActionStep.MERGE_START.name,state.current_fetch_file,percentage,state.total_fetch_files))
                                mergeworker.mergeTStoMp4(files, cachedir, result_file_name_with_ext)
                            }
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            mergeworker.stateFlow.collectLatest {
                it.merge_result_file?.let {
                    //TODO Show all done
                    _dlstate.postValue(DownloadStates(MergeActionStep.MERGE_END.name))
                    Timber.e("Show  merget result : " + it)
                }
            }
        }
        viewModelScope.launch {
            m3u8worker.startFetch(
                cachedir,m3u8
            )
        }
    }
}