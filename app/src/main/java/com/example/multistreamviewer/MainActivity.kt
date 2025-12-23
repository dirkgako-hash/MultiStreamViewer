package com.example.multistreamviewer

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity() {

    private lateinit var webViews: Array<WebView?>
    private lateinit var containers: Array<FrameLayout?>
    private lateinit var statusTexts: Array<TextView?>
    
    private lateinit var urlInput: EditText
    private lateinit var btnLoad: Button
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var controlsContainer: LinearLayout
    private lateinit var fabToggle: com.google.android.material.floatingactionbutton.FloatingActionButton
    
    private var activeWebViewIndex = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar fullscreen imediatamente
        setupFullscreen()
        
        setContentView(R.layout.activity_main)
        
        setupViews()
        setupWebViews()
        setupListeners()
        
        // Carregar URLs padrão
        loadDefaultUrls()
    }

    private fun setupFullscreen() {
        // Esconder a barra de status e navegação
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        
        // Manter tela ligada
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupViews() {
        urlInput = findViewById(R.id.url_input)
        btnLoad = findViewById(R.id.btn_load)
        btnBack = findViewById(R.id.btn_back)
        btnForward = findViewById(R.id.btn_forward)
        btnRefresh = findViewById(R.id.btn_refresh)
        btnHome = findViewById(R.id.btn_home)
        tvStatus = findViewById(R.id.tv_status)
        controlsContainer = findViewById(R.id.controls_container)
        fabToggle = findViewById(R.id.fab_toggle)
        
        // Inicializar arrays
        webViews = arrayOf(
            findViewById(R.id.webview_1),
            findViewById(R.id.webview_2),
            findViewById(R.id.webview_3),
            findViewById(R.id.webview_4)
        )
        
        containers = arrayOf(
            findViewById(R.id.container_1),
            findViewById(R.id.container_2),
            findViewById(R.id.container_3),
            findViewById(R.id.container_4)
        )
        
        statusTexts = arrayOf(
            findViewById(R.id.tv_webview_1),
            findViewById(R.id.tv_webview_2),
            findViewById(R.id.tv_webview_3),
            findViewById(R.id.tv_webview_4)
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViews() {
        for (i in webViews.indices) {
            webViews[i]?.apply {
                // Configurações do WebView
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                
                // Otimizações para vídeo
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                
                // Client personalizado
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Esconder texto de status quando a página carregar
                        statusTexts[i]?.visibility = View.GONE
                        updateNavigationButtons()
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (newProgress == 100) {
                            tvStatus.text = "Carregado: ${view?.url?.take(30)}..."
                        } else {
                            tvStatus.text = "Carregando: $newProgress%"
                        }
                    }
                }
                
                // Listener para clique (selecionar WebView ativo)
                setOnClickListener {
                    selectWebView(i)
                }
            }
        }
        
        // Selecionar primeiro WebView por padrão
        selectWebView(0)
    }

    private fun setupListeners() {
        btnLoad.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                loadUrlInActiveWebView(url)
            }
        }
        
        btnBack.setOnClickListener {
            webViews[activeWebViewIndex]?.takeIf { it.canGoBack() }?.goBack()
        }
        
        btnForward.setOnClickListener {
            webViews[activeWebViewIndex]?.takeIf { it.canGoForward() }?.goForward()
        }
        
        btnRefresh.setOnClickListener {
            webViews[activeWebViewIndex]?.reload()
        }
        
        btnHome.setOnClickListener {
            // Fechar controles
            controlsContainer.visibility = View.GONE
            fabToggle.visibility = View.VISIBLE
        }
        
        fabToggle.setOnClickListener {
            // Mostrar/ocultar controles
            if (controlsContainer.isVisible) {
                controlsContainer.visibility = View.GONE
            } else {
                controlsContainer.visibility = View.VISIBLE
                urlInput.requestFocus()
            }
        }
        
        // Toque fora dos controles para escondê-los
        findViewById<View>(R.id.webview_grid).setOnClickListener {
            if (controlsContainer.isVisible) {
                controlsContainer.visibility = View.GONE
            }
        }
    }

    private fun loadDefaultUrls() {
        // URLs padrão para teste (podem ser alteradas)
        val defaultUrls = arrayOf(
            "https://www.youtube.com",
            "https://www.twitch.tv",
            "https://vimeo.com",
            "https://www.dailymotion.com"
        )
        
        for (i in webViews.indices) {
            if (i < defaultUrls.size) {
                webViews[i]?.loadUrl(defaultUrls[i])
                statusTexts[i]?.text = "Carregando ${defaultUrls[i].take(20)}..."
            }
        }
    }

    private fun selectWebView(index: Int) {
        // Remover seleção anterior
        containers[activeWebViewIndex]?.setBackgroundColor(Color.BLACK)
        
        // Nova seleção
        activeWebViewIndex = index
        containers[index]?.setBackgroundColor(Color.parseColor("#CC6200EE"))
        
        // Atualizar URL no input
        webViews[index]?.url?.let { url ->
            urlInput.setText(url)
        }
        
        updateNavigationButtons()
        tvStatus.text = "WebView ${index + 1} selecionado"
    }

    private fun loadUrlInActiveWebView(url: String) {
        val formattedUrl = if (!url.startsWith("http")) {
            "https://$url"
        } else {
            url
        }
        
        statusTexts[activeWebViewIndex]?.apply {
            text = "Carregando..."
            visibility = View.VISIBLE
        }
        
        webViews[activeWebViewIndex]?.loadUrl(formattedUrl)
        tvStatus.text = "Carregando: $formattedUrl"
    }

    private fun updateNavigationButtons() {
        val currentWebView = webViews[activeWebViewIndex]
        btnBack.isEnabled = currentWebView?.canGoBack() == true
        btnForward.isEnabled = currentWebView?.canGoForward() == true
    }

    override fun onBackPressed() {
        if (controlsContainer.isVisible) {
            controlsContainer.visibility = View.GONE
            return
        }
        
        if (webViews[activeWebViewIndex]?.canGoBack() == true) {
            webViews[activeWebViewIndex]?.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pausar todos os WebViews quando a app perde foco
        webViews.forEach { it?.onPause() }
    }

    override fun onResume() {
        super.onResume()
        // Retomar WebViews quando a app ganha foco
        webViews.forEach { it?.onResume() }
    }

    override fun onDestroy() {
        // Limpar WebViews para evitar memory leaks
        webViews.forEach { it?.destroy() }
        super.onDestroy()
    }
}
