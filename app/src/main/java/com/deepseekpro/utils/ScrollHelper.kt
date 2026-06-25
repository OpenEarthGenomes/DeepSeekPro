package com.deepseekpro.utils

import android.webkit.WebView
import android.widget.SeekBar

class ScrollHelper(
    private val webView: WebView,
    private val seekBar: SeekBar
) {

    private var isSeekBarChanging = false

    fun setup() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    isSeekBarChanging = true
                    scrollToProgress(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeekBarChanging = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeekBarChanging = false
            }
        })

        webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (!isSeekBarChanging) {
                updateSeekBarFromScroll(scrollY)
            }
        }
    }

    fun scrollToProgress(progress: Int) {
        webView.evaluateJavascript("document.body.scrollHeight;") { heightResult ->
            try {
                val totalHeight = heightResult.toFloatOrNull() ?: 1f
                val targetY = (totalHeight * progress / 100f).toInt()
                webView.scrollTo(0, targetY)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun updateSeekBarFromScroll(scrollY: Int) {
        webView.evaluateJavascript("document.body.scrollHeight;") { heightResult ->
            try {
                val totalHeight = heightResult.toFloatOrNull() ?: 1f
                val progress = (scrollY / totalHeight * 100).toInt().coerceIn(0, 100)
                seekBar.progress = progress
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
