package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

public class MainActivity extends AppCompatActivity {

    // Componentes principais
    private GridLayout gridLayout;
    private WebView[] webViews = new WebView[4];
    private FrameLayout[] boxContainers = new FrameLayout[4];
    private LinearLayout overlayControls;
    private ScrollView sidebarMenu;
    
    // Controles
    private Button btnMenu, btnOrientation, btnFoldChecks, btnToggleSidebar, btnTogglePortrait;
    private Button btnCloseMenu, btnLoadAll, btnReloadAll, btnClearAll;
    private CheckBox[] checkBoxes = new CheckBox[4];
    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups, cbBlockRedirects;
    private EditText[] urlInputs = new EditText[4];
    
    // Estado
    private boolean[] boxEnabled = {true, true, true, true};
    private boolean isSidebarVisible = false;
    private boolean isControlsFolded = false;
    private int currentOrientation = Configuration.ORIENTATION_PORTRAIT;
    
    // Controles individuais por box
    private LinearLayout[] boxControlPanels = new LinearLayout[4];
    private boolean[] controlPanelVisible = new boolean[4];
    private Handler[] boxHandlers = new Handler[4];
    
    // Configurações WebView
    private boolean allowScripts = true;
    private boolean allowForms = true;
    private boolean allowPopups = true;
    private boolean blockRedirects = false;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
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
        initViews();
        initWebViews();
        initEventListeners();
        
        // Configurar layout inicial
        updateLayout();
        
        // Mostrar overlay por padrão
        overlayControls.setVisibility(View.VISIBLE);
        
        // Carregar URLs iniciais
        new Handler().postDelayed(this::loadInitialURLs, 1000);
    }
    
    private void initViews() {
        gridLayout = findViewById(R.id.gridLayout);
        overlayControls = findViewById(R.id.overlayControls);
        sidebarMenu = findViewById(R.id.sidebarMenu);
        
        // Botões principais
        btnMenu = findViewById(R.id.btnMenu);
        btnOrientation = findViewById(R.id.btnOrientation);
        btnFoldChecks = findViewById(R.id.btnFoldChecks);
        btnToggleSidebar = findViewById(R.id.btnToggleSidebar);
        btnTogglePortrait = findViewById(R.id.btnTogglePortrait);
        btnCloseMenu = findViewById(R.id.btnCloseMenu);
        btnLoadAll = findViewById(R.id.btnLoadAll);
        btnReloadAll = findViewById(R.id.btnReloadAll);
        btnClearAll = findViewById(R.id.btnClearAll);
        
        // Checkboxes
        checkBoxes[0] = findViewById(R.id.checkBox1);
        checkBoxes[1] = findViewById(R.id.checkBox2);
        checkBoxes[2] = findViewById(R.id.checkBox3);
        checkBoxes[3] = findViewById(R.id.checkBox4);
        
        // Configurações
        cbAllowScripts = findViewById(R.id.cbAllowScripts);
        cbAllowForms = findViewById(R.id.cbAllowForms);
        cbAllowPopups = findViewById(R.id.cbAllowPopups);
        cbBlockRedirects = findViewById(R.id.cbBlockRedirects);
        
        // URLs
        urlInputs[0] = findViewById(R.id.urlInput1);
        urlInputs[1] = findViewById(R.id.urlInput2);
        urlInputs[2] = findViewById(R.id.urlInput3);
        urlInputs[3] = findViewById(R.id.urlInput4);
        
        // Inicializar handlers
        for (int i = 0; i < 4; i++) {
            boxHandlers[i] = new Handler();
            controlPanelVisible[i] = false;
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViews() {
        // Criar containers e WebViews dinamicamente
        for (int i = 0; i < 4; i++) {
            final int boxIndex = i;
            
            // Criar container
            boxContainers[i] = new FrameLayout(this);
            boxContainers[i].setId(View.generateViewId());
            boxContainers[i].setBackgroundColor(Color.BLACK);
            
            // Criar WebView
            webViews[i] = new WebView(this);
            webViews[i].setId(View.generateViewId());
            setupWebView(webViews[i], boxIndex);
            
            // Adicionar WebView ao container
            boxContainers[i].addView(webViews[i], 
                new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            
            // Criar painel de controles da box
            createBoxControlPanel(boxIndex);
            
            // Configurar clique duplo para mostrar controles
            boxContainers[i].setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = 0;
                
                @Override
                public void onClick(View v) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < 300) {
                        // Clique duplo detectado
                        toggleBoxControlPanel(boxIndex);
                    }
                    lastClickTime = clickTime;
                }
            });
        }
        
        // Adicionar containers ao grid
        for (int i = 0; i < 4; i++) {
            gridLayout.addView(boxContainers[i]);
        }
    }
    
    private void createBoxControlPanel(int boxIndex) {
        // Criar painel de controles com cores visíveis
        boxControlPanels[boxIndex] = new LinearLayout(this);
        boxControlPanels[boxIndex].setOrientation(LinearLayout.HORIZONTAL);
        boxControlPanels[boxIndex].setBackgroundColor(0xEE333333);
        boxControlPanels[boxIndex].setPadding(12, 8, 12, 8);
        boxControlPanels[boxIndex].setVisibility(View.GONE);
        
        // Layout params - posicionar no TOPO do container
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.TOP;
        boxContainers[boxIndex].addView(boxControlPanels[boxIndex], params);
        
        // Botões do painel
        String[] buttonLabels = {"+", "-", "↻", "←", "→", "⤢"};
        String[] buttonActions = {"zoomIn", "zoomOut", "refresh", "back", "forward", "fullscreen"};
        
        for (int j = 0; j < buttonLabels.length; j++) {
            Button btn = new Button(this);
            btn.setText(buttonLabels[j]);
            btn.setBackgroundColor(0xFF1e3c72);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(14);
            btn.setPadding(12, 8, 12, 8);
            
            final int actionIndex = j;
            final int currentBoxIndex = boxIndex;
            btn.setOnClickListener(v -> {
                handleBoxControlClick(currentBoxIndex, buttonActions[actionIndex]);
                resetBoxControlPanelTimer(currentBoxIndex);
            });
            
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            btnParams.setMargins(4, 0, 4, 0);
            boxControlPanels[boxIndex].addView(btn, btnParams);
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView, int boxIndex) {
        WebSettings settings = webView.getSettings();
        
        // Configurações básicas
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        // Configurações de zoom
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        
        // Otimizações
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Permissões especiais para vídeos
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // User agent
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");
        
        // Cor de fundo
        webView.setBackgroundColor(Color.BLACK);
        
        // Hardware acceleration
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        
        // WebViewClient para navegação
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // Injetar JavaScript para permitir fullscreen
                String fullscreenJS = 
                    "var videos = document.getElementsByTagName('video');" +
                    "for(var i = 0; i < videos.length; i++) {" +
                    "   videos[i].setAttribute('playsinline', 'false');" +
                    "   videos[i].setAttribute('webkit-playsinline', 'false');" +
                    "   videos[i].controls = true;" +
                    "}";
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    view.evaluateJavascript(fullscreenJS, null);
                } else {
                    view.loadUrl("javascript:" + fullscreenJS);
                }
            }
        });
        
        // WebChromeClient para fullscreen - ESSENCIAL!
        webView.setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;
            
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Guardar a view e callback
                mCustomView = view;
                mCustomViewCallback = callback;
                
                // Ocultar controles
                overlayControls.setVisibility(View.GONE);
                hideBoxControlPanel(boxIndex);
                
                // Adicionar a view customizada ao container da box
                boxContainers[boxIndex].addView(view, 
                    new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                
                // Ocultar WebView original
                webView.setVisibility(View.GONE);
                
                // Forçar fullscreen no sistema
                getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
                Toast.makeText(MainActivity.this, 
                    "Fullscreen Box " + (boxIndex + 1), 
                    Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onHideCustomView() {
                // Sair do fullscreen
                if (mCustomView == null) return;
                
                // Mostrar WebView original
                webView.setVisibility(View.VISIBLE);
                
                // Remover view customizada
                boxContainers[boxIndex].removeView(mCustomView);
                
                // Notificar callback
                if (mCustomViewCallback != null) {
                    mCustomViewCallback.onCustomViewHidden();
                }
                
                // Limpar referências
                mCustomView = null;
                mCustomViewCallback = null;
                
                // Restaurar controles
                overlayControls.setVisibility(View.VISIBLE);
                
                // Sair do fullscreen do sistema
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
                Toast.makeText(MainActivity.this, 
                    "Exited fullscreen", 
                    Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // Log para debug
                if (newProgress == 100) {
                    Log.d("WebView", "Box " + boxIndex + " loaded: " + view.getUrl());
                }
            }
        });
    }
    
    private void initEventListeners() {
        // Botão menu - MOSTRAR/ESCONDER overlay
        btnMenu.setOnClickListener(v -> {
            if (overlayControls.getVisibility() == View.VISIBLE) {
                overlayControls.setVisibility(View.GONE);
            } else {
                overlayControls.setVisibility(View.VISIBLE);
            }
        });
        
        // Botão toggle sidebar - MOSTRAR sidebar
        btnToggleSidebar.setOnClickListener(v -> {
            if (sidebarMenu.getVisibility() == View.VISIBLE) {
                sidebarMenu.setVisibility(View.GONE);
                isSidebarVisible = false;
            } else {
                sidebarMenu.setVisibility(View.VISIBLE);
                isSidebarVisible = true;
                // Esconder outros controles
                for (int i = 0; i < 4; i++) {
                    hideBoxControlPanel(i);
                }
            }
        });
        
        // Botão orientação - MUDAR layout
        btnOrientation.setOnClickListener(v -> {
            changeOrientation();
        });
        
        btnTogglePortrait.setOnClickListener(v -> {
            changeOrientation();
        });
        
        // Botão fold checks - dobrar controles inferiores
        btnFoldChecks.setOnClickListener(v -> {
            toggleBottomControls();
        });
        
        // Botão fechar menu
        btnCloseMenu.setOnClickListener(v -> {
            sidebarMenu.setVisibility(View.GONE);
            isSidebarVisible = false;
        });
        
        // Botões de ação
        btnLoadAll.setOnClickListener(v -> {
            loadAllURLs();
        });
        
        btnReloadAll.setOnClickListener(v -> {
            reloadAllWebViews();
        });
        
        btnClearAll.setOnClickListener(v -> {
            clearAllWebViews();
        });
        
        // Checkboxes de boxes
        for (int i = 0; i < 4; i++) {
            final int index = i;
            checkBoxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                boxEnabled[index] = isChecked;
                Log.d("Box", "Box " + (index + 1) + ": " + isChecked);
                updateLayout();
            });
        }
        
        // Configurações de segurança
        cbAllowScripts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            allowScripts = isChecked;
            applyWebViewSettings();
        });
        
        cbAllowForms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            allowForms = isChecked;
            applyWebViewSettings();
        });
        
        cbAllowPopups.setOnCheckedChangeListener((buttonView, isChecked) -> {
            allowPopups = isChecked;
            applyWebViewSettings();
        });
        
        cbBlockRedirects.setOnCheckedChangeListener((buttonView, isChecked) -> {
            blockRedirects = isChecked;
        });
        
        // Toque fora do sidebar para fechar
        findViewById(R.id.gridLayout).setOnClickListener(v -> {
            if (isSidebarVisible) {
                sidebarMenu.setVisibility(View.GONE);
                isSidebarVisible = false;
            }
            
            // Esconder todos os controles das boxes
            for (int i = 0; i < 4; i++) {
                if (controlPanelVisible[i]) {
                    hideBoxControlPanel(i);
                }
            }
        });
    }
    
    private void handleBoxControlClick(int boxIndex, String action) {
        WebView webView = webViews[boxIndex];
        
        switch (action) {
            case "zoomIn":
                webView.zoomIn();
                showToast("Zoom in Box " + (boxIndex + 1));
                break;
            case "zoomOut":
                webView.zoomOut();
                showToast("Zoom out Box " + (boxIndex + 1));
                break;
            case "refresh":
                webView.reload();
                showToast("Refreshing Box " + (boxIndex + 1));
                break;
            case "back":
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    showToast("No back history");
                }
                break;
            case "forward":
                if (webView.canGoForward()) {
                    webView.goForward();
                } else {
                    showToast("No forward history");
                }
                break;
            case "fullscreen":
                // Ativar fullscreen via JavaScript
                String fullscreenJS = 
                    "var videos = document.getElementsByTagName('video');" +
                    "if (videos.length > 0) {" +
                    "   videos[0].requestFullscreen();" +
                    "} else {" +
                    "   document.documentElement.requestFullscreen();" +
                    "}";
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(fullscreenJS, null);
                } else {
                    webView.loadUrl("javascript:" + fullscreenJS);
                }
                showToast("Fullscreen Box " + (boxIndex + 1));
                break;
        }
    }
    
    private void toggleBoxControlPanel(int boxIndex) {
        if (!boxEnabled[boxIndex]) {
            showToast("Box " + (boxIndex + 1) + " is disabled");
            return;
        }
        
        if (controlPanelVisible[boxIndex]) {
            hideBoxControlPanel(boxIndex);
        } else {
            // Esconder outros painéis
            for (int i = 0; i < 4; i++) {
                if (i != boxIndex && controlPanelVisible[i]) {
                    hideBoxControlPanel(i);
                }
            }
            
            // Mostrar este painel
            boxControlPanels[boxIndex].setVisibility(View.VISIBLE);
            controlPanelVisible[boxIndex] = true;
            resetBoxControlPanelTimer(boxIndex);
            showToast("Controls Box " + (boxIndex + 1));
        }
    }
    
    private void hideBoxControlPanel(int boxIndex) {
        if (boxControlPanels[boxIndex] != null) {
            boxControlPanels[boxIndex].setVisibility(View.GONE);
        }
        controlPanelVisible[boxIndex] = false;
        if (boxHandlers[boxIndex] != null) {
            boxHandlers[boxIndex].removeCallbacksAndMessages(null);
        }
    }
    
    private void resetBoxControlPanelTimer(int boxIndex) {
        if (boxHandlers[boxIndex] != null) {
            boxHandlers[boxIndex].removeCallbacksAndMessages(null);
            boxHandlers[boxIndex].postDelayed(() -> {
                if (controlPanelVisible[boxIndex]) {
                    hideBoxControlPanel(boxIndex);
                }
            }, 5000);
        }
    }
    
    private void changeOrientation() {
        try {
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
                btnOrientation.setText("↻");
                btnTogglePortrait.setText("Landscape");
                showToast("Landscape Mode");
            } else {
                currentOrientation = Configuration.ORIENTATION_PORTRAIT;
                btnOrientation.setText("📱");
                btnTogglePortrait.setText("Portrait");
                showToast("Portrait Mode");
            }
            updateLayout();
        } catch (Exception e) {
            Log.e("Orientation", "Error: " + e.getMessage());
            showToast("Error changing layout");
        }
    }
    
    private void toggleBottomControls() {
        LinearLayout checkboxContainer = findViewById(R.id.checkboxContainer);
        LinearLayout controlsRow = findViewById(R.id.controlsRow);
        
        if (isControlsFolded) {
            // Mostrar tudo
            if (checkboxContainer != null) checkboxContainer.setVisibility(View.VISIBLE);
            if (controlsRow != null) controlsRow.setVisibility(View.VISIBLE);
            btnFoldChecks.setText("▲");
            isControlsFolded = false;
            showToast("Controls shown");
        } else {
            // Esconder tudo
            if (checkboxContainer != null) checkboxContainer.setVisibility(View.GONE);
            if (controlsRow != null) controlsRow.setVisibility(View.GONE);
            btnFoldChecks.setText("▼");
            isControlsFolded = true;
            showToast("Controls hidden");
        }
    }
    
    private void updateLayout() {
        try {
            int activeBoxes = 0;
            for (boolean enabled : boxEnabled) {
                if (enabled) activeBoxes++;
            }
            
            if (activeBoxes == 0) {
                showToast("No boxes active!");
                return;
            }
            
            int rows, cols;
            
            if (activeBoxes == 1) {
                rows = 1; cols = 1;
            } else if (activeBoxes == 2) {
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    rows = 1; cols = 2;
                } else {
                    rows = 2; cols = 1;
                }
            } else if (activeBoxes == 3) {
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    rows = 2; cols = 2; // Um espaço vazio
                } else {
                    rows = 2; cols = 2;
                }
            } else {
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    rows = 2; cols = 2;
                } else {
                    rows = 4; cols = 1;
                }
            }
            
            // Limpar e reconfigurar o grid
            gridLayout.removeAllViews();
            gridLayout.setRowCount(rows);
            gridLayout.setColumnCount(cols);
            
            int position = 0;
            for (int i = 0; i < 4; i++) {
                if (boxEnabled[i]) {
                    GridLayout.Spec rowSpec = GridLayout.spec(position / cols, 1f);
                    GridLayout.Spec colSpec = GridLayout.spec(position % cols, 1f);
                    
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
                    params.width = 0;
                    params.height = 0;
                    params.setMargins(2, 2, 2, 2);
                    
                    boxContainers[i].setVisibility(View.VISIBLE);
                    gridLayout.addView(boxContainers[i], params);
                    position++;
                } else {
                    boxContainers[i].setVisibility(View.GONE);
                    hideBoxControlPanel(i);
                }
            }
            
            gridLayout.requestLayout();
            
        } catch (Exception e) {
            Log.e("UpdateLayout", "Error: " + e.getMessage());
        }
    }
    
    private void loadAllURLs() {
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i]) {
                String url = urlInputs[i].getText().toString().trim();
                if (!url.isEmpty()) {
                    loadURL(i, url);
                }
            }
        }
        showToast("Loading all URLs");
    }
    
    private void loadInitialURLs() {
        for (int i = 0; i < 4; i++) {
            String url = urlInputs[i].getText().toString().trim();
            if (!url.isEmpty()) {
                loadURL(i, url);
            }
        }
    }
    
    private void loadURL(int boxIndex, String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            webViews[boxIndex].loadUrl(url);
            Log.d("LoadURL", "Box " + (boxIndex + 1) + ": " + url);
        } catch (Exception e) {
            Log.e("LoadURL", "Error: " + e.getMessage());
        }
    }
    
    private void reloadAllWebViews() {
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i] && webViews[i] != null) {
                webViews[i].reload();
            }
        }
        showToast("Reloading all");
    }
    
    private void clearAllWebViews() {
        for (int i = 0; i < 4; i++) {
            if (webViews[i] != null) {
                webViews[i].loadUrl("about:blank");
            }
        }
        showToast("Cleared all");
    }
    
    private void applyWebViewSettings() {
        for (WebView webView : webViews) {
            if (webView != null) {
                WebSettings settings = webView.getSettings();
                settings.setJavaScriptEnabled(allowScripts);
                settings.setDomStorageEnabled(true);
            }
        }
        showToast("Settings applied");
    }
    
    private boolean isSameDomain(String url1, String url2) {
        try {
            java.net.URL u1 = new java.net.URL(url1);
            java.net.URL u2 = new java.net.URL(url2);
            return u1.getHost().equals(u2.getHost());
        } catch (Exception e) {
            return false;
        }
    }
    
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
    
    @Override
    public void onBackPressed() {
        // Primeiro, tentar sair do fullscreen
        for (WebView webView : webViews) {
            if (webView != null && webView.getVisibility() == View.GONE) {
                // Está em fullscreen, tentar sair
                webView.setVisibility(View.VISIBLE);
                showToast("Exited fullscreen");
                return;
            }
        }
        
        // Segundo, fechar sidebar se aberto
        if (isSidebarVisible) {
            sidebarMenu.setVisibility(View.GONE);
            isSidebarVisible = false;
            return;
        }
        
        // Terceiro, mostrar/ocultar overlay
        if (overlayControls.getVisibility() == View.VISIBLE) {
            overlayControls.setVisibility(View.GONE);
            return;
        } else {
            overlayControls.setVisibility(View.VISIBLE);
            return;
        }
        
        // Por último, voltar no WebView
        // for (WebView webView : webViews) {
        //     if (webView.canGoBack()) {
        //         webView.goBack();
        //         return;
        //     }
        // }
        
        // super.onBackPressed();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        for (WebView webView : webViews) {
            if (webView != null) {
                webView.onPause();
            }
        }
        for (Handler handler : boxHandlers) {
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        for (WebView webView : webViews) {
            if (webView != null) {
                webView.onResume();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        for (WebView webView : webViews) {
            if (webView != null) {
                webView.destroy();
            }
        }
        for (Handler handler : boxHandlers) {
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
            }
        }
        super.onDestroy();
    }
}