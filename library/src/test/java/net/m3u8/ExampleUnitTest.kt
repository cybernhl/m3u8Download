package net.m3u8

import net.m3u8.download.M3u8DownloadFactory
import net.m3u8.listener.DownloadListener
import net.m3u8.utils.Constant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    private val M3U8URL =
        "https://ctv-s54.huishangqia.com/encrypt/s54/m3u8/y7/f9/12y7f942850fd1f11e4045ce68ae1dc80e275d4560.m3u8"
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun downloadTest() {
        val m3u8Download = M3u8DownloadFactory.getInstance(M3U8URL)
        //设置生成目录
        //设置生成目录
        m3u8Download.dir = "F://m3u8JavaTest"
        //设置视频名称
        //设置视频名称
        m3u8Download.fileName = "test"
        //设置线程数
        //设置线程数
        m3u8Download.threadCount = 100
        //设置重试次数
        //设置重试次数
        m3u8Download.retryCount = 100
        //设置连接超时时间（单位：毫秒）
        //设置连接超时时间（单位：毫秒）
        m3u8Download.timeoutMillisecond = 10000L
        /*
        设置日志级别
        可选值：NONE INFO DEBUG ERROR
        */
        /*
        设置日志级别
        可选值：NONE INFO DEBUG ERROR
        */m3u8Download.setLogLevel(Constant.INFO)
        //设置监听器间隔（单位：毫秒）
        //设置监听器间隔（单位：毫秒）
        m3u8Download.setInterval(500L)
        //添加额外请求头
        /*  Map<String, Object> headersMap = new HashMap<>();
        headersMap.put("Content-Type", "text/html;charset=utf-8");
        m3u8Download.addRequestHeaderMap(headersMap);*/
        //如果需要的话设置http代理
        //m3u8Download.setProxy("172.50.60.3",8090);
        //添加监听器
        //添加额外请求头
        /*  Map<String, Object> headersMap = new HashMap<>();
        headersMap.put("Content-Type", "text/html;charset=utf-8");
        m3u8Download.addRequestHeaderMap(headersMap);*/
        //如果需要的话设置http代理
        //m3u8Download.setProxy("172.50.60.3",8090);
        //添加监听器
        m3u8Download.addListener(object : DownloadListener {
            override fun start() {
                println("开始下载！")
            }

            override fun process(downloadUrl: String, finished: Int, sum: Int, percent: Float) {
                println("下载网址：" + downloadUrl + "\t已下载" + finished + "个\t一共" + sum + "个\t已完成" + percent + "%")
            }

            override fun speed(speedPerSecond: String) {
                println("下载速度：$speedPerSecond")
            }

            override fun end() {
                println("下载完毕")
            }
        })
        //开始下载
        //开始下载
        m3u8Download.start()
        assertEquals(4, 2 + 2)
    }
}