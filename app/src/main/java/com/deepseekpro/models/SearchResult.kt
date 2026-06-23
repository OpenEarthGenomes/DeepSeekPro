package com.deepseekpro.models

data class SearchResult(
    val fileName: String,
    val fileDate: String,
    val messageIndex: Int,
    val role: String,
    val content: String,
    val context: String,
    val lineNumber: Int,
    val matchCount: Int = 1
)
