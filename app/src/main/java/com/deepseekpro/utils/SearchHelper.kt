package com.deepseekpro.utils

import android.webkit.WebView
import com.deepseekpro.storage.NoteManager

class SearchHelper(
    private val webView: WebView,
    private val noteManager: NoteManager
) {
    
    fun searchInWebView(query: String): Boolean {
        var result = false
        webView.evaluateJavascript(
            "highlightText('$query');",
            { found ->
                result = found.toBoolean()
            }
        )
        return result
    }
    
    fun searchInFiles(query: String): Map<String, List<String>> {
        return noteManager.searchInAllFiles(query)
    }
    
    fun searchInFile(fileName: String, query: String): List<NoteManager.SearchResult> {
        return noteManager.searchInFile(fileName, query)
    }
    
    fun clearHighlights() {
        webView.evaluateJavascript(
            "document.querySelectorAll('.message span').forEach(el => el.replaceWith(el.textContent));",
            null
        )
    }
}
