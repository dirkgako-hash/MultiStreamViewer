package com.example.multistreamviewer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val testButton: Button = findViewById(R.id.btn_test)
        testButton.setOnClickListener {
            Toast.makeText(this, "MultiStreamViewer Funcionando!", Toast.LENGTH_SHORT).show()
        }
    }
}
