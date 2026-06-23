package com.deepseekpro.utils

import android.webkit.WebView
import android.widget.SeekBar

class ScrollHelper(
    private val webView: WebView,
    private val seekBar: SeekBar
) {
    
    private var isScrolling = false
    private var totalHeight = 0f
    
    fun scrollToProgress(progress: Int) {
        if (isScrolling) return
        isScrolling = true
        
        webView.evaluateJavascript("document.body.scrollHeight;") { heightResult ->
            try {
                totalHeight = heightResult.toFloatOrNull() ?: 1f
                val targetY = (totalHeight * progress / 100f).toInt()
                webView.scrollTo(0, targetY)
            } catch (e: Exception) {
                // ignore
            } finally {
                isScrolling = false
            }
        }
    }
    
    fun syncSeekBarWithScroll(scrollY: Int) {
        if (isScrolling) return
        webView.evaluateJavascript("document.body.scrollHeight;") { heightResult ->
            try {
                totalHeight = heightResult.toFloatOrNull() ?: 1f
                val progress = (scrollY / totalHeight * 100).toInt().coerceIn(0, 100)
                seekBar.progress = progress
            } catch (e: Exception) {
                // ignore
            }
        }
    }
    
    fun getCurrentScrollPosition(): Int {
        return webView.scrollY
    }
}
