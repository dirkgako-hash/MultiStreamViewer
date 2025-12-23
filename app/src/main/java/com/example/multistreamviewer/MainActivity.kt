package com.example.multistreamviewer

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView1: WebView = findViewById(R.id.webView1)
        val webView2: WebView = findViewById(R.id.webView2)
        val webView3: WebView = findViewById(R.id.webView3)
        val webView4: WebView = findViewById(R.id.webView4)

        webView1.loadUrl("https://example.com/1")
        webView2.loadUrl("https://example.com/2")
        webView3.loadUrl("https://example.com/3")
        webView4.loadUrl("https://example.com/4")
    }
}
