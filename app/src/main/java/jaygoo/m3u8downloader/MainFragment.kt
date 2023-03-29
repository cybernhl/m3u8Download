package jaygoo.m3u8downloader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tech.dynamo.worker.m3u8.M3U8ActionStep
import com.tech.dynamo.worker.ts.merge.MergeActionStep
import timber.log.Timber
import java.text.DecimalFormat

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

    }

    override fun onCreateView(  inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View {
        viewModel.dlstate.observe(viewLifecycleOwner) {
            when(it.currentStep){
                M3U8ActionStep.INIT.name->{
                    Timber.e("Show MainFragment INIT : " + it)
                    //TODO here can show start
                }
                M3U8ActionStep.FETCH_M3U8.name->{
                    Timber.e("Show MainFragment FETCH_M3U8 : " + it)
                }
                M3U8ActionStep.FETCH_ENCRYPTION_INFO.name->{
                    Timber.e("Show MainFragment FETCH_ENCRYPTION_INFO : " + it)
                }
                M3U8ActionStep.FETCH_TS.name->{
                    val percentage = DecimalFormat("0.00").format(it.currentCount.toDouble()/it.totalCount)

                    Timber.e("Show MainFragment FETCH_TS  percentage : $percentage"  )
                }
                MergeActionStep.MERGE_START.name->{
                    Timber.e("Show MainFragment MERGE_START"  )
                }
                MergeActionStep.MERGE_END.name->{
                    Timber.e("Show MainFragment MERGE_END"  )
                }
            }
        }
//"https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"
//        https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/level_0.m3u8
//            "https://ctv-s54.huishangqia.com/encrypt/s54/m3u8/y7/f9/12y7f942850fd1f11e4045ce68ae1dc80e275d4560.m3u8"
        viewModel.DemoDL(requireActivity().cacheDir,"https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/level_0.m3u8","dewqfqwfwef.mp4")
        return inflater.inflate(R.layout.fragment_main, container, false)
    }



}