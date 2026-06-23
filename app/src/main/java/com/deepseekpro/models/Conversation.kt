package com.deepseekpro.models

data class Conversation(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val messages: List<Message> = emptyList(),
    val fileName: String = ""
)

data class Message(
    val role: String = "", // "user" vagy "assistant"
    val content: String = "",
    val timestamp: String = ""
)
