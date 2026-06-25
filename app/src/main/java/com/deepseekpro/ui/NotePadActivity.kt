package com.deepseekpro.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.deepseekpro.R
import com.deepseekpro.storage.NoteManager
import java.io.File

class NotePadActivity : AppCompatActivity() {

    private lateinit var noteManager: NoteManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notepad)

        noteManager = NoteManager(this)
        recyclerView = findViewById(R.id.recyclerView)
        emptyState = findViewById(R.id.emptyState)

        recyclerView.layoutManager = LinearLayoutManager(this)
        loadConversations()
    }

    private fun loadConversations() {
        val files = noteManager.getAllConversations()
        if (files.isNotEmpty()) {
            recyclerView.visibility = android.view.View.VISIBLE
            emptyState.visibility = android.view.View.GONE
            // TODO: Adapter hozzáadása a következő lépésben
        } else {
            recyclerView.visibility = android.view.View.GONE
            emptyState.visibility = android.view.View.VISIBLE
            emptyState.text = "📭 Még nincs mentett beszélgetés"
        }
    }
}
