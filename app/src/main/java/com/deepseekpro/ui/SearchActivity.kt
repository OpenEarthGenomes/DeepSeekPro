package com.deepseekpro.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.deepseekpro.R
import com.deepseekpro.storage.NoteManager
import kotlinx.coroutines.*

class SearchActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var searchResults: RecyclerView
    private lateinit var clearButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var fileFilterSpinner: Spinner

    private lateinit var noteManager: NoteManager
    private lateinit var adapter: SearchResultAdapter
    private var currentSearchResults = listOf<NoteManager.SearchResult>()
    private var allFiles = listOf<String>()
    private var selectedFile: String? = null

    private val searchScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        searchResults = findViewById(R.id.searchResults)
        clearButton = findViewById(R.id.clearButton)
        backButton = findViewById(R.id.backButton)
        loadingProgress = findViewById(R.id.loadingProgress)
        emptyState = findViewById(R.id.emptyState)
        fileFilterSpinner = findViewById(R.id.fileFilterSpinner)

        noteManager = NoteManager(this)
        
        adapter = SearchResultAdapter { result ->
            navigateToResult(result)
        }

        searchResults.layoutManager = LinearLayoutManager(this)
        searchResults.adapter = adapter

        loadFileList()
        setupListeners()
    }

    private fun loadFileList() {
        val files = noteManager.getAllConversations()
        allFiles = files.map { it.name }
        
        val spinnerItems = mutableListOf("🔍 Összes fájl")
        spinnerItems.addAll(allFiles)
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerItems)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fileFilterSpinner.adapter = adapter
        
        fileFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFile = if (position == 0) null else allFiles.getOrNull(position - 1)
                val query = searchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedFile = null
            }
        }
    }

    private fun setupListeners() {
        searchButton.setOnClickListener {
            performSearch()
        }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        clearButton.setOnClickListener {
            searchInput.text.clear()
            adapter.updateResults(emptyList())
            emptyState.visibility = View.VISIBLE
            emptyState.text = "🔍 Kezdj el keresni!"
            searchResults.visibility = View.GONE
            loadingProgress.visibility = View.GONE
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun performSearch() {
        val query = searchInput.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "📝 Kérlek, adj meg egy keresőkifejezést!", Toast.LENGTH_SHORT).show()
            return
        }

        loadingProgress.visibility = View.VISIBLE
        searchResults.visibility = View.GONE
        emptyState.visibility = View.GONE

        searchScope.launch {
            val results = withContext(Dispatchers.IO) {
                searchInFiles(query)
            }
            
            loadingProgress.visibility = View.GONE
            currentSearchResults = results
            
            if (results.isNotEmpty()) {
                adapter.updateResults(results)
                searchResults.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
                Toast.makeText(
                    this@SearchActivity,
                    "🔍 ${results.size} találat a(z) '$query' kifejezésre",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                adapter.updateResults(emptyList())
                searchResults.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
                emptyState.text = "😕 Nincs találat a(z) '$query' kifejezésre"
            }
        }
    }

    private suspend fun searchInFiles(query: String): List<NoteManager.SearchResult> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<NoteManager.SearchResult>()
            
            if (selectedFile != null) {
                results.addAll(noteManager.searchInFile(selectedFile!!, query))
            } else {
                val fileResults = noteManager.searchInAllFiles(query)
                fileResults.forEach { (fileName, _) ->
                    results.addAll(noteManager.searchInFile(fileName, query))
                }
            }
            
            results.sortedWith(compareBy({ it.fileName }, { it.messageIndex }))
        }
    }

    private fun navigateToResult(result: NoteManager.SearchResult) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_FILE", result.fileName)
            putExtra("SCROLL_TO_MESSAGE", result.messageIndex)
            putExtra("SEARCH_QUERY", searchInput.text.toString())
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        searchScope.cancel()
    }

    inner class SearchResultAdapter(
        private val onItemClick: (NoteManager.SearchResult) -> Unit
    ) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

        private var results = listOf<NoteManager.SearchResult>()

        fun updateResults(newResults: List<NoteManager.SearchResult>) {
            results = newResults
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_search_result, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val result = results[position]
            holder.bind(result)
        }

        override fun getItemCount(): Int = results.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val fileNameText: TextView = itemView.findViewById(R.id.fileNameText)
            private val roleText: TextView = itemView.findViewById(R.id.roleText)
            private val contentText: TextView = itemView.findViewById(R.id.contentText)
            private val contextText: TextView = itemView.findViewById(R.id.contextText)
            private val lineNumberText: TextView = itemView.findViewById(R.id.lineNumberText)
            private val matchCountText: TextView = itemView.findViewById(R.id.matchCountText)

            fun bind(result: NoteManager.SearchResult) {
                fileNameText.text = "📄 ${result.fileName}"
                roleText.text = if (result.role == "user") "👤 Felhasználó" else "🤖 DeepSeek"
                contentText.text = result.content.take(150) + if (result.content.length > 150) "..." else ""
                contextText.text = "💡 ${result.context}"
                lineNumberText.text = "📍 ${result.lineNumber}. üzenet"
                
                val matches = result.content.split(Regex(searchInput.text.toString()), ignoreCase = true).size - 1
                matchCountText.text = "🔍 $matches találat"
                matchCountText.visibility = if (matches > 0) View.VISIBLE else View.GONE

                itemView.setOnClickListener {
                    onItemClick(result)
                }
            }
        }
    }
}
