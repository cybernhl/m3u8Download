package com.tech.dynamo.worker.ts.merge

data class MergeState(val current_processing_file: Int, val total_files: Int,val merge_result_file:String?)