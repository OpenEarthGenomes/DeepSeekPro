package com.deepseekpro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.ValueCallback
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.deepseekpro.storage.NoteManager
import com.deepseekpro.ui.NotePadActivity
import com.deepseekpro.ui.SearchActivity
import com.deepseekpro.utils.ScrollHelper
import com.deepseekpro.webview.DeepSeekJavaScriptInterface

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var seekBar: SeekBar
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var r1Toggle: ImageButton
    private lateinit var attachButton: ImageButton
    private lateinit var progressBar: ProgressBar

    private var isR1Mode = false
    private var currentConversationTitle = ""
    private lateinit var noteManager: NoteManager
    private lateinit var scrollHelper: ScrollHelper

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val REQUEST_CODE_FILE_CHOOSER = 1001

    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        noteManager = NoteManager(this)

        webView = findViewById(R.id.webView)
        seekBar = findViewById(R.id.seekBar)
        inputMessage = findViewById(R.id.inputMessage)
        sendButton = findViewById(R.id.sendButton)
        r1Toggle = findViewById(R.id.r1Toggle)
        attachButton = findViewById(R.id.attachButton)
        progressBar = findViewById(R.id.progressBar)

        checkPermissions()
        setupWebView()

        scrollHelper = ScrollHelper(webView, seekBar)
        setupSeekBar()

        setupButtons()
        setupWindowInsets()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val fileName = it.getStringExtra("OPEN_FILE")
            val messageIndex = it.getIntExtra("SCROLL_TO_MESSAGE", -1)
            val searchQuery = it.getStringExtra("SEARCH_QUERY")

            if (!fileName.isNullOrEmpty()) {
                Toast.makeText(this, "📂 Betöltés: $fileName", Toast.LENGTH_LONG).show()
                loadConversationFromFile(fileName, messageIndex, searchQuery)
            }
        }
    }

    private fun loadConversationFromFile(fileName: String, messageIndex: Int, searchQuery: String?) {
        val content = noteManager.readConversation(fileName)
        if (content != null) {
            val cleanContent = content
                .replace("=== DeepSeek Beszélgetés ===", "")
                .replace(Regex("Dátum: .*\\n"), "")
                .trim()

            webView.evaluateJavascript(
                "document.querySelector('textarea').value = '${cleanContent.take(1000)}...';",
                null
            )

            if (!searchQuery.isNullOrEmpty()) {
                webView.evaluateJavascript(
                    "highlightText('$searchQuery');",
                    null
                )
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    }
                }
            }

            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissions.toTypedArray(),
                    REQUEST_STORAGE_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "✅ Tárhely engedélyezve", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "⚠️ Tárhely engedély szükséges a mentéshez", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 DeepSeekPro"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                injectJavaScriptHelpers()
                forceHungarianLanguage()
                setupScrollSync()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(
                    Intent.createChooser(intent, "Válassz fájlt"),
                    REQUEST_CODE_FILE_CHOOSER
                )
                return true
            }
        }

        webView.loadUrl("https://chat.deepseek.com")

        webView.addJavascriptInterface(
            DeepSeekJavaScriptInterface(this) {
                runOnUiThread {
                    saveCurrentConversation()
                }
            },
            "Android"
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_FILE_CHOOSER -> {
                if (resultCode == RESULT_OK) {
                    val uris = if (data?.data != null) {
                        arrayOf(data.data!!)
                    } else {
                        null
                    }
                    filePathCallback?.onReceiveValue(uris)
                } else {
                    filePathCallback?.onReceiveValue(null)
                }
                filePathCallback = null
            }
        }
    }

    private fun injectJavaScriptHelpers() {
        val jsCode = """
            function sendDeepSeekMessage(text) {
                const textarea = document.querySelector('textarea');
                const button = document.querySelector('button[type="submit"]');
                if (textarea && button) {
                    textarea.value = text;
                    textarea.dispatchEvent(new Event('input', { bubbles: true }));
                    button.click();
                    return true;
                }
                return false;
            }
            
            function getLastDeepSeekMessage() {
                const messages = document.querySelectorAll('.message:last-child');
                if (messages.length > 0) {
                    return messages[messages.length-1].innerText;
                }
                return '';
            }
            
            function getFullDeepSeekChat() {
                const messages = document.querySelectorAll('.message');
                let result = '';
                let role = '';
                messages.forEach((msg, index) => {
                    const text = msg.innerText.trim();
                    if (text) {
                        if (index % 2 === 0) {
                            role = '👤 Felhasználó';
                        } else {
                            role = '🤖 DeepSeek';
                        }
                        result += role + ':\n' + text + '\n\n';
                    }
                });
                return result;
            }
            
            function clearDeepSeekChat() {
                const newChatBtn = document.querySelector('a[href="/"]');
                if (newChatBtn) {
                    newChatBtn.click();
                    return true;
                }
                return false;
            }
            
            function highlightText(text) {
                const messages = document.querySelectorAll('.message');
                let found = false;
                messages.forEach(msg => {
                    const original = msg.innerHTML;
                    const highlighted = original.replace(
                        new RegExp(text, 'gi'),
                        match => '<span style="background-color: yellow; color: black;">' + match + '</span>'
                    );
                    if (original !== highlighted) {
                        msg.innerHTML = highlighted;
                        found = true;
                    }
                });
                return found;
            }
            
            function getScrollPosition() {
                return window.scrollY;
            }
            
            function setScrollPosition(position) {
                window.scrollTo(0, position);
            }
            
            console.log('DeepSeek Pro helper injected!');
        """.trimIndent()

        webView.evaluateJavascript(jsCode, null)
    }

    private fun forceHungarianLanguage() {
        webView.evaluateJavascript(
            "sendDeepSeekMessage('Kérlek, válaszolj mindig magyar nyelven!');",
            null
        )
    }

    private fun setupScrollSync() {
        webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
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

    private fun setupSeekBar() {
        scrollHelper.setup()  // 🔥 EZ HIÁNYZOTT!
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    scrollHelper.scrollToProgress(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtons() {
        sendButton.setOnClickListener {
            val message = inputMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessageToDeepSeek(message)
                inputMessage.text.clear()
            }
        }

        inputMessage.setOnEditorActionListener { _, _, _ ->
            val message = inputMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessageToDeepSeek(message)
                inputMessage.text.clear()
            }
            true
        }

        r1Toggle.setOnClickListener {
            isR1Mode = !isR1Mode
            if (isR1Mode) {
                r1Toggle.setImageResource(R.drawable.ic_r1_on)
                Toast.makeText(this, "🧠 DeepThink (R1) bekapcsolva", Toast.LENGTH_SHORT).show()
                webView.evaluateJavascript(
                    "document.querySelector('[data-testid=\"deep-think-toggle\"]')?.click();",
                    null
                )
            } else {
                r1Toggle.setImageResource(R.drawable.ic_r1_off)
                Toast.makeText(this, "⚡ Gyors mód (V3)", Toast.LENGTH_SHORT).show()
                webView.evaluateJavascript(
                    "document.querySelector('[data-testid=\"deep-think-toggle\"]')?.click();",
                    null
                )
            }
        }

        attachButton.setOnClickListener {
            Toast.makeText(this, "📎 Válassz fájlt a feltöltéshez", Toast.LENGTH_SHORT).show()
            webView.evaluateJavascript(
                "document.querySelector('input[type=\"file\"]')?.click();",
                null
            )
        }
    }

    private fun sendMessageToDeepSeek(message: String) {
        val escapedMessage = message.replace("'", "\\'").replace("\n", "\\n")
        progressBar.visibility = View.VISIBLE

        webView.evaluateJavascript(
            "sendDeepSeekMessage('$escapedMessage');",
            { result ->
                if (result == "true") {
                    webView.evaluateJavascript(
                        "setTimeout(function() { getLastDeepSeekMessage(); }, 2000);",
                        {
                            progressBar.visibility = View.GONE
                            saveCurrentConversation()
                            updateSeekBar()  // 🔥 CSÚSZKA FRISSÍTÉSE
                        }
                    )
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "❌ Hiba az üzenet küldésekor!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun updateSeekBar() {
        webView.evaluateJavascript("document.body.scrollHeight;") { heightResult ->
            try {
                val totalHeight = heightResult.toFloatOrNull() ?: 1f
                val currentScroll = webView.scrollY
                val progress = (currentScroll / totalHeight * 100).toInt().coerceIn(0, 100)
                seekBar.progress = progress
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun saveCurrentConversation() {
        webView.evaluateJavascript("getFullDeepSeekChat();") { chatContent ->
            if (chatContent.isNotEmpty() && chatContent != "null" && chatContent != "\"\"") {
                var content = chatContent
                if (content.startsWith("\"") && content.endsWith("\"")) {
                    content = content.substring(1, content.length - 1)
                }
                content = content.replace("\\n", "\n").replace("\\\"", "\"")

                val fileName = noteManager.saveConversation(
                    content = content,
                    title = currentConversationTitle
                )
                Toast.makeText(this, "💾 Mentés: $fileName", Toast.LENGTH_SHORT).show()
                currentConversationTitle = fileName
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                saveCurrentConversation()
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }
            R.id.action_notepad -> {
                startActivity(Intent(this, NotePadActivity::class.java))
                true
            }
            R.id.action_save -> {
                saveCurrentConversation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
