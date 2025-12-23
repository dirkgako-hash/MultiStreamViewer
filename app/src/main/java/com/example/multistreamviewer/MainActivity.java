package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

public class MainActivity extends AppCompatActivity {

    private WebView[] webViews = new WebView[4];
    private FrameLayout[] playerContainers = new FrameLayout[4];
    private LinearLayout[] loadingOverlays = new LinearLayout[4];
    private Button btnLoadAll, btnClearAll;
    private GridLayout gridLayout;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar tela cheia
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // Inicializar componentes
        gridLayout = findViewById(R.id.gridLayout);
        
        // IDs dos componentes
        int[] webViewIds = {R.id.webView1, R.id.webView2, R.id.webView3, R.id.webView4};
        int[] containerIds = {R.id.playerContainer1, R.id.playerContainer2, R.id.playerContainer3, R.id.playerContainer4};
        int[] overlayIds = {R.id.loadingOverlay1, R.id.loadingOverlay2, R.id.loadingOverlay3, R.id.loadingOverlay4};
        
        // Configurar cada WebView
        for (int i = 0; i < 4; i++) {
            final int playerIndex = i;
            
            playerContainers[i] = findViewById(containerIds[i]);
            loadingOverlays[i] = findViewById(overlayIds[i]);
            
            webViews[i] = findViewById(webViewIds[i]);
            setupWebView(webViews[i], playerIndex);
            
            // Configurar clique para carregar URL de exemplo
            playerContainers[i].setOnClickListener(v -> {
                String testUrl = "https://www.youtube.com";
                loadURL(playerIndex, testUrl);
                Toast.makeText(MainActivity.this, 
                    "Carregando exemplo no Player " + (playerIndex + 1), 
                    Toast.LENGTH_SHORT).show();
            });
        }
        
        // Configurar botões
        btnLoadAll = findViewById(R.id.btnLoadAll);
        btnClearAll = findViewById(R.id.btnClearAll);
        
        btnLoadAll.setOnClickListener(v -> loadAllExampleURLs());
        btnClearAll.setOnClickListener(v -> clearAllWebViews());
        
        // Carregar URLs de exemplo
        loadAllExampleURLs();
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView, int playerIndex) {
        WebSettings webSettings = webView.getSettings();
        
        // Configurações básicas
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        
        // Otimizações para vídeo
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        
        // User agent para compatibilidade
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        // CORREÇÃO CRÍTICA: Fundo preto
        webView.setBackgroundColor(Color.BLACK);
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        
        // WebViewClient personalizado
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                loadingOverlays[playerIndex].setVisibility(View.VISIBLE);
                
                // Injetar CSS para fundo preto imediatamente
                view.loadUrl("javascript:(function(){" +
                    "document.body.style.backgroundColor='#000000';" +
                    "document.body.style.color='#ffffff';" +
                    "})()");
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                loadingOverlays[playerIndex].setVisibility(View.GONE);
                
                // Reforçar fundo preto após carregamento
                view.loadUrl("javascript:(function(){" +
                    "document.body.style.backgroundColor='#000000';" +
                    "document.body.style.color='#ffffff';" +
                    "var videos=document.getElementsByTagName('video');" +
                    "for(var i=0;i<videos.length;i++){videos[i].style.backgroundColor='#000000';}" +
                    "})()");
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                loadingOverlays[playerIndex].setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, 
                    "Erro Player " + (playerIndex + 1) + ": " + description, 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        // WebChromeClient para vídeos
        webView.setWebChromeClient(new WebChromeClient());
    }
    
    private void loadURL(int playerIndex, String url) {
        if (url.isEmpty()) return;
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        webViews[playerIndex].loadUrl(url);
    }
    
    private void loadAllExampleURLs() {
        String[] exampleURLs = {
            "https://www.youtube.com",
            "https://www.twitch.tv",
            "https://vimeo.com",
            "https://www.dailymotion.com"
        };
        
        for (int i = 0; i < 4; i++) {
            if (i < exampleURLs.length) {
                loadURL(i, exampleURLs[i]);
            }
        }
        
        Toast.makeText(this, "Carregando exemplos em todos os players", Toast.LENGTH_SHORT).show();
    }
    
    private void clearAllWebViews() {
        for (int i = 0; i < 4; i++) {
            webViews[i].loadUrl("about:blank");
            loadingOverlays[i].setVisibility(View.GONE);
        }
        Toast.makeText(this, "Todos os players limpos", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onBackPressed() {
        // Verificar se algum WebView pode voltar
        for (WebView webView : webViews) {
            if (webView.canGoBack()) {
                webView.goBack();
                return;
            }
        }
        super.onBackPressed();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        for (WebView webView : webViews) {
            if (webView != null) webView.onPause();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        for (WebView webView : webViews) {
            if (webView != null) webView.onResume();
        }
    }
    
    @Override
    protected void onDestroy() {
        for (WebView webView : webViews) {
            if (webView != null) webView.destroy();
        }
        super.onDestroy();
    }
}
