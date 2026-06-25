package com.deepseekpro.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.deepseekpro.R
import java.io.File

class NoteAdapter(
    private val files: List<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val file = files[position]
        holder.bind(file)
    }

    override fun getItemCount(): Int = files.size

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileNameText: TextView = itemView.findViewById(R.id.fileNameText)

        fun bind(file: File) {
            fileNameText.text = file.name
            itemView.setOnClickListener { onItemClick(file) }
        }
    }
}
