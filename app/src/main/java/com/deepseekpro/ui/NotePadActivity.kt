package com.deepseekpro.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.deepseekpro.R

class NotePadActivity : AppCompatActivity() {

    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notepad)

        textView = findViewById(R.id.textView)
        textView.text = "Jegyzettömb - Működik!"
    }
}
