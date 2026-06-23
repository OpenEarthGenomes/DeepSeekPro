package com.deepseekpro.webview

import android.webkit.WebView
import android.webkit.WebViewClient

class DeepSeekWebViewClient(
    private val onPageLoaded: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) : WebViewClient() {
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageLoaded()
    }
    
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        if (description != null) {
            onError(description)
        }
    }
    
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return if (url != null && url.startsWith("https://chat.deepseek.com")) {
            false
        } else {
            true
        }
    }
}
