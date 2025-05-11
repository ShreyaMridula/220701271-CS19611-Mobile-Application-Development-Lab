package com.example.fridgemanager1

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SuggestionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggestions)

        val recipeList = intent.getStringArrayListExtra("recipes")
        Log.d("DEBUG", "Received recipes: $recipeList")

        val textView = findViewById<TextView>(R.id.tvSuggestion)

        if (textView != null) {
            textView.text = recipeList?.joinToString("\n") ?: "No suggestions available"
        } else {
            // fallback if the view was not found (to avoid crash)
            println("tvSuggestion TextView not found in layout!")
        }
    }
}
