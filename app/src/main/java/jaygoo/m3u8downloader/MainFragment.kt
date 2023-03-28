package jaygoo.m3u8downloader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.io.File

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
        viewModel.DemoDL(requireActivity().cacheDir)
        return inflater.inflate(R.layout.fragment_main, container, false)
    }



}