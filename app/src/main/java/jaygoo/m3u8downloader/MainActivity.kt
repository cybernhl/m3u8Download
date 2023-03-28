package jaygoo.m3u8downloader;

import android.Manifest;
import android.os.Bundle;
import android.widget.Toast;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import java.util.List;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

//    private val m3u8worker=M3U8DownloadWorker.getWorker()
//    private val mergeworker=TSMergeWorker.getManager()
//    private val workscope = CoroutineScope(Dispatchers.IO)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestAppPermissions();
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //    //"https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"
//    fun DemoDL(cachedir: File) {
//        workscope.launch {
//            m3u8worker.stateFlow.collect {state->
//                val percentage=state.percentage
//                Log.e("DownloadManager","Show  get M3U8State : $state")
//                if (state.step==2 &&(state.current_fetch_file==state.total_fetch_files)&&percentage==100){
//                    Log.e("DownloadManager","Show  match M3U8State : $state")
//                    state.tsFiles?.let {files->
//                        mergeworker.mergeTStoMp4(files,cachedir,"de1r12e1r123r213wd.mp4")
//                    }
//                }
////                when(state.step){
////                    0->{
////
////                    }
////                    1->{
////
////                    }
////                    2->{
////
////                    }
////
////                }
//            }
//        }
//        workscope.launch {
//            mergeworker.stateFlow.collect{
//                Log.e("DownloadManager","Show  merget result : $it")
//            }
//        }
//        workscope.launch {
//            m3u8worker.startFetch(cachedir,"https://ctv-s54.huishangqia.com/encrypt/s54/m3u8/y7/f9/12y7f942850fd1f11e4045ce68ae1dc80e275d4560.m3u8")
//        }
//    }

    private void requestAppPermissions() {
        Dexter.withActivity(this)
                .withPermissions(PERMISSIONS)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {

                            Toast.makeText(getApplicationContext(),"权限获取成功",Toast.LENGTH_LONG).show();
                        }else {
                            Toast.makeText(getApplicationContext(),"权限获取失败",Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                    }
                })
                .check();
    }
}
