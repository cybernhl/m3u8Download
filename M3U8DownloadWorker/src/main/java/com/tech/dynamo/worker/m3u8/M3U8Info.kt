package com.tech.dynamo.worker.m3u8

import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist

data class M3U8Info(val list: HlsMediaPlaylist, val domain: String)