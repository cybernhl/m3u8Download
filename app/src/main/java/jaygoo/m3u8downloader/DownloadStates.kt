package jaygoo.m3u8downloader

data class DownloadStates (val currentStep: String, val currentCount: Int=0 ,val currentPercentage: Int=0 ,val totalCount:Int=0 )