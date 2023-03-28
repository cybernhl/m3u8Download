package jaygoo.m3u8downloader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.dynamo.worker.m3u8.M3U8DownloadWorker
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

        //"https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"
        fun DemoDL(cachedir: File) {
            viewModelScope.launch {
                m3u8worker.stateFlow.collectLatest {state->
                    val percentage = state.percentage
                    Timber.e("Show  get M3U8State : " + state)
                    if (state.step == 2 && (state.current_fetch_file == state.total_fetch_files) && percentage == 100) {
                        Timber.e("Show  match M3U8State : " + state)
                        state.tsFiles?.let { files ->
                            mergeworker.mergeTStoMp4(files, cachedir, "de1r12e1r123r213wd.mp4")
                        }
                    }
                    //                when(state.step){
                    //                    0->{
                    //
                    //                    }
                    //                    1->{
                    //
                    //                    }
                    //                    2->{
                    //
                    //                    }
                    //
                    //                }
                }
            }
            viewModelScope.launch {
                mergeworker.stateFlow.collectLatest {
                    Timber.e("Show  merget result : " + it)
                }
            }
            viewModelScope.launch {
                m3u8worker.startFetch(cachedir,"https://ctv-s54.huishangqia.com/encrypt/s54/m3u8/y7/f9/12y7f942850fd1f11e4045ce68ae1dc80e275d4560.m3u8")
            }
        }

}