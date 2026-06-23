package com.deepseekpro.webview

import android.webkit.JavascriptInterface

class DeepSeekJavaScriptInterface(
    private val context: android.content.Context,
    private val onMessageReceived: (String) -> Unit = {}
) {
    
    @JavascriptInterface
    fun onMessageReceived(message: String) {
        onMessageReceived(message)
    }
    
    @JavascriptInterface
    fun sendToAndroid(message: String) {
        android.util.Log.d("DeepSeekJS", "Message from JS: $message")
    }
    
    @JavascriptInterface
    fun getDeviceInfo(): String {
        return "Android ${android.os.Build.VERSION.RELEASE}"
    }
    
    @JavascriptInterface
    fun saveToFile(content: String) {
        // Mentés a NoteManager-en keresztül
        val noteManager = com.deepseekpro.storage.NoteManager(context)
        noteManager.saveConversation(content)
    }
}
