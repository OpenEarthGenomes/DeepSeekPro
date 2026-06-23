package com.deepseekpro.storage

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NoteManager(private val context: Context) {

    private val notesDirectory: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "DeepSeekNotes"
        ).apply { mkdirs() }

    // 📝 Mentés
    fun saveConversation(content: String, title: String = ""): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val fileName = if (title.isNotEmpty() && title.length < 50) {
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_áéíóöőúüűÁÉÍÓÖŐÚÜŰ]"), "_")
            "$timestamp-$cleanTitle.txt"
        } else {
            "$timestamp-beszelgetes.txt"
        }
        val file = File(notesDirectory, fileName)
        val fullContent = """
            ═══ DeepSeek Beszélgetés ═══
            📅 Dátum: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
            📄 Fájl: $fileName
            
            $content
            
            ═══ Vége ═══
        """.trimIndent()
        file.writeText(fullContent)
        return fileName
    }

    // 📖 Összes fájl listázása
    fun getAllConversations(): List<File> {
        return notesDirectory.listFiles()
            ?.filter { it.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    // 📖 Fájl tartalmának olvasása
    fun readConversation(fileName: String): String? {
        val file = File(notesDirectory, fileName)
        return if (file.exists()) file.readText() else null
    }

    // 🔍 Keresés az összes fájlban (1. szint)
    fun searchInAllFiles(query: String): Map<String, List<String>> {
        val results = mutableMapOf<String, List<String>>()
        val files = getAllConversations()
        
        files.forEach { file ->
            val content = file.readText()
            val lines = content.split("\n")
            val matches = lines.filter { 
                it.contains(query, ignoreCase = true) && 
                !it.startsWith("═══") && 
                !it.startsWith("📅") &&
                !it.startsWith("📄")
            }
            if (matches.isNotEmpty()) {
                results[file.name] = matches
            }
        }
        return results
    }

    // 🔍 Keresés egy adott fájlon belül (2. szint)
    fun searchInFile(fileName: String, query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val content = readConversation(fileName) ?: return results
        val lines = content.split("\n")
        
        var messageIndex = 0
        var currentRole = ""
        var currentMessage = StringBuilder()
        var isMessage = false
        
        lines.forEachIndexed { index, line ->
            when {
                line.contains("👤 Felhasználó:") -> {
                    if (currentMessage.isNotEmpty()) {
                        checkAndAddResult(
                            currentMessage.toString(), 
                            query, 
                            fileName, 
                            messageIndex, 
                            currentRole, 
                            results
                        )
                        messageIndex++
                    }
                    currentRole = "user"
                    currentMessage = StringBuilder(line)
                    isMessage = true
                }
                line.contains("🤖 DeepSeek:") -> {
                    if (currentMessage.isNotEmpty()) {
                        checkAndAddResult(
                            currentMessage.toString(), 
                            query, 
                            fileName, 
                            messageIndex, 
                            currentRole, 
                            results
                        )
                        messageIndex++
                    }
                    currentRole = "assistant"
                    currentMessage = StringBuilder(line)
                    isMessage = true
                }
                line.isNotBlank() && isMessage -> {
                    currentMessage.append("\n").append(line)
                }
            }
        }
        
        if (currentMessage.isNotEmpty() && isMessage) {
            checkAndAddResult(
                currentMessage.toString(), 
                query, 
                fileName, 
                messageIndex, 
                currentRole, 
                results
            )
        }
        
        return results
    }

    private fun checkAndAddResult(
        content: String,
        query: String,
        fileName: String,
        index: Int,
        role: String,
        results: MutableList<SearchResult>
    ) {
        if (content.contains(query, ignoreCase = true)) {
            val context = extractContext(content, query)
            results.add(
                SearchResult(
                    fileName = fileName,
                    messageIndex = index,
                    role = role,
                    content = content.trim(),
                    context = context,
                    lineNumber = index + 1
                )
            )
        }
    }

    private fun extractContext(text: String, query: String): String {
        val lowerText = text.lowercase(Locale.getDefault())
        val lowerQuery = query.lowercase(Locale.getDefault())
        val index = lowerText.indexOf(lowerQuery)
        if (index == -1) return text.take(100) + "..."
        
        val start = maxOf(0, index - 50)
        val end = minOf(text.length, index + query.length + 50)
        var context = text.substring(start, end)
        if (start > 0) context = "..." + context
        if (end < text.length) context += "..."
        return context
    }

    // 🗑️ Fájl törlése
    fun deleteConversation(fileName: String): Boolean {
        val file = File(notesDirectory, fileName)
        return if (file.exists()) file.delete() else false
    }

    // 📊 Fájl méretének lekérése
    fun getFileSize(fileName: String): Long {
        val file = File(notesDirectory, fileName)
        return if (file.exists()) file.length() else 0
    }

    // 📅 Fájl módosítási dátuma
    fun getFileDate(fileName: String): String? {
        val file = File(notesDirectory, fileName)
        return if (file.exists()) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(file.lastModified()))
        } else null
    }

    // 🔍 Találati osztály
    data class SearchResult(
        val fileName: String,
        val messageIndex: Int,
        val role: String,
        val content: String,
        val context: String,
        val lineNumber: Int
    )
}
