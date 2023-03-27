package com.tech.dynamo.worker.m3u8

import java.io.File

data class M3U8State(
    val step: Int,
    val current_fetch_file: Int,
    val percentage: Int,
    val total_fetch_files: Int,
    val tsFiles: List<File>? = null
)