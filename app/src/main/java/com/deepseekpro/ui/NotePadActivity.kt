package com.deepseekpro.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.deepseekpro.R
import com.deepseekpro.storage.NoteManager
import java.io.File

class NotePadActivity : AppCompatActivity() {

    private lateinit var noteManager: NoteManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var adapter: NoteAdapter
    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var sortSpinner: Spinner

    private var currentFiles = listOf<File>()
    private var sortOrder = SortOrder.DATE_DESC

    enum class SortOrder {
        DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_ASC, SIZE_DESC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notepad)

        noteManager = NoteManager(this)
        recyclerView = findViewById(R.id.recyclerView)
        emptyState = findViewById(R.id.emptyState)
        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        sortSpinner = findViewById(R.id.sortSpinner)

        setupRecyclerView()
        setupSortSpinner()
        loadConversations()
        setupToolbar()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onItemClick = { file ->
                showFilePreview(file)
            },
            onItemDelete = { file ->
                deleteConversation(file)
            },
            onItemShare = { file ->
                shareConversation(file)
            },
            onItemExport = { file ->
                exportConversation(file)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSortSpinner() {
        val items = arrayOf(
            "📅 Friss először",
            "📅 Régi először",
            "📁 Név (A-Z)",
            "📁 Név (Z-A)",
            "📊 Kicsi először",
            "📊 Nagy először"
        )
        
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = spinnerAdapter
        
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortOrder = when (position) {
                    0 -> SortOrder.DATE_DESC
                    1 -> SortOrder.DATE_ASC
                    2 -> SortOrder.NAME_ASC
                    3 -> SortOrder.NAME_DESC
                    4 -> SortOrder.SIZE_ASC
                    5 -> SortOrder.SIZE_DESC
                    else -> SortOrder.DATE_DESC
                }
                applyFiltersAndSort()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadConversations() {
        val files = noteManager.getAllConversations()
        currentFiles = files
        
        if (currentFiles.isNotEmpty()) {
            adapter.updateFiles(currentFiles)
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        } else {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            emptyState.text = getString(R.string.empty_state)
        }
    }

    private fun sortFiles(files: List<File>): List<File> {
        return when (sortOrder) {
            SortOrder.DATE_DESC -> files.sortedByDescending { it.lastModified() }
            SortOrder.DATE_ASC -> files.sortedBy { it.lastModified() }
            SortOrder.NAME_ASC -> files.sortedBy { it.name }
            SortOrder.NAME_DESC -> files.sortedByDescending { it.name }
            SortOrder.SIZE_ASC -> files.sortedBy { it.length() }
            SortOrder.SIZE_DESC -> files.sortedByDescending { it.length() }
        }
    }

    private fun applyFiltersAndSort() {
        val query = searchInput.text.toString().trim()
        val filteredFiles = if (query.isNotEmpty()) {
            currentFiles.filter { it.name.contains(query, ignoreCase = true) }
        } else {
            currentFiles
        }
        
        val sortedFiles = sortFiles(filteredFiles)
        
        if (sortedFiles.isNotEmpty()) {
            adapter.updateFiles(sortedFiles)
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        } else {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            emptyState.text = if (query.isNotEmpty()) {
                "😕 Nincs találat: '$query'"
            } else {
                getString(R.string.empty_state)
            }
        }
    }

    private fun setupListeners() {
        searchButton.setOnClickListener {
            applyFiltersAndSort()
        }
        
        searchInput.setOnEditorActionListener { _, _, _ ->
            applyFiltersAndSort()
            true
        }
    }

    private fun showFilePreview(file: File) {
        val content = noteManager.readConversation(file.name)
        if (content != null) {
            val scrollView = ScrollView(this)
            val textView = TextView(this).apply {
                text = content
                textSize = 14f
                setPadding(24, 24, 24, 24)
                isTextSelectable = true
            }
            scrollView.addView(textView)
            
            AlertDialog.Builder(this)
                .setTitle("📄 ${file.name}")
                .setView(scrollView)
                .setPositiveButton("Bezár") { dialog, _ -> dialog.dismiss() }
                .setNeutralButton("📤 Megosztás") { _, _ ->
                    shareConversation(file)
                }
                .setNegativeButton("🗑️ Törlés") { _, _ ->
                    deleteConversation(file)
                }
                .show()
        }
    }

    private fun deleteConversation(file: File) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Fájl törlése")
            .setMessage("Biztosan törölni szeretnéd a(z) ${file.name} fájlt?")
            .setPositiveButton("Igen") { _, _ ->
                if (noteManager.deleteConversation(file.name)) {
                    Toast.makeText(this, "✅ Fájl törölve", Toast.LENGTH_SHORT).show()
                    loadConversations()
                }
            }
            .setNegativeButton("Mégse", null)
            .show()
    }

    private fun shareConversation(file: File) {
        val content = noteManager.readConversation(file.name)
        if (content != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, content)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "📤 Megosztás"))
        }
    }

    private fun exportConversation(file: File) {
        val content = noteManager.readConversation(file.name)
        if (content != null) {
            val exportFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "DeepSeek_${file.name}"
            )
            exportFile.writeText(content)
            
            Toast.makeText(this, "📁 Exportálva: ${exportFile.absolutePath}", Toast.LENGTH_LONG).show()
            
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    exportFile
                )
            } else {
                Uri.fromFile(exportFile)
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/plain")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "📂 Megnyitás"))
        }
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "📁 Jegyzettömb"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    inner class NoteAdapter(
        private val onItemClick: (File) -> Unit,
        private val onItemDelete: (File) -> Unit,
        private val onItemShare: (File) -> Unit,
        private val onItemExport: (File) -> Unit
    ) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {

        private var files = listOf<File>()

        fun updateFiles(newFiles: List<File>) {
            files = newFiles
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_notepad, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = files[position]
            holder.bind(file)
        }

        override fun getItemCount(): Int = files.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val fileNameText: TextView = itemView.findViewById(R.id.fileNameText)
            private val fileDateText: TextView = itemView.findViewById(R.id.fileDateText)
            private val fileSizeText: TextView = itemView.findViewById(R.id.fileSizeText)
            private val filePreviewText: TextView = itemView.findViewById(R.id.filePreviewText)
            private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
            private val shareButton: ImageButton = itemView.findViewById(R.id.shareButton)
            private val exportButton: ImageButton = itemView.findViewById(R.id.exportButton)

            fun bind(file: File) {
                fileNameText.text = file.name
                fileDateText.text = noteManager.getFileDate(file.name) ?: "Ismeretlen"
                var size = noteManager.getFileSize(file.name)
                fileSizeText.text = if (size > 1024) {
                    "${size / 1024} KB"
                } else {
                    "$size B"
                }
                
                var content = noteManager.readConversation(file.name)
                filePreviewText.text = content?.take(100)?.replace("\n", " ")?.plus("...") 
                    ?: "📄 Üres fájl"
                filePreviewText.visibility = View.VISIBLE

                itemView.setOnClickListener { onItemClick(file) }
                deleteButton.setOnClickListener { onItemDelete(file) }
                shareButton.setOnClickListener { onItemShare(file) }
                exportButton.setOnClickListener { onItemExport(file) }
            }
        }
    }
}
