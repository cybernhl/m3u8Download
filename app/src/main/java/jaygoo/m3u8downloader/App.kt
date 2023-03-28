package jaygoo.m3u8downloader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber


@HiltAndroidApp
class App : Application(){
    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugLogTree("M3U8"))
    }
}
