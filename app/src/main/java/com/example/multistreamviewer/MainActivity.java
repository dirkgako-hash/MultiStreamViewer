package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
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
    private boolean isBottomPanelFolded = false;
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
            
            // Configurar clique na box
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
        // Criar painel de controles VISÍVEL
        boxControlPanels[boxIndex] = new LinearLayout(this);
        boxControlPanels[boxIndex].setOrientation(LinearLayout.HORIZONTAL);
        boxControlPanels[boxIndex].setBackgroundColor(0xEE333333); // Cinza escuro mais visível
        boxControlPanels[boxIndex].setPadding(8, 6, 8, 6);
        boxControlPanels[boxIndex].setVisibility(View.GONE);
        
        // Layout params - posicionar no TOPO do container
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.TOP;
        boxContainers[boxIndex].addView(boxControlPanels[boxIndex], params);
        
        // Botões do painel COM CORES DIFERENTES E VISÍVEIS
        String[] buttonLabels = {"☰", "⛶", "+", "-", "↻", "←", "→"};
        String[] buttonActions = {"sidebar", "fullscreen", "zoomin", "zoomout", "refresh", "back", "forward"};
        int[] buttonColors = {
            0xFF4CAF50,  // Verde para sidebar
            0xFF2196F3,  // Azul para fullscreen
            0xFF9C27B0,  // Roxo para zoom in
            0xFF9C27B0,  // Roxo para zoom out
            0xFFFF9800,  // Laranja para refresh
            0xFF607D8B,  // Cinza para back
            0xFF607D8B   // Cinza para forward
        };
        
        for (int j = 0; j < buttonLabels.length; j++) {
            Button btn = new Button(this);
            btn.setText(buttonLabels[j]);
            btn.setBackgroundColor(buttonColors[j]);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(14);
            btn.setPadding(8, 4, 8, 4);
            
            final int actionIndex = j;
            final int currentBoxIndex = boxIndex;
            btn.setOnClickListener(v -> {
                handleBoxControlClick(currentBoxIndex, buttonActions[actionIndex]);
                resetBoxControlPanelTimer(currentBoxIndex);
            });
            
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            btnParams.setMargins(2, 0, 2, 0);
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
        
        // User agent
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        
        // Cor de fundo
        webView.setBackgroundColor(Color.BLACK);
        
        // WebViewClient simplificado
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });
        
        // WebChromeClient para fullscreen
        webView.setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;
            
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                mCustomView = view;
                mCustomViewCallback = callback;
                
                // Ocultar controles
                overlayControls.setVisibility(View.GONE);
                hideBoxControlPanel(boxIndex);
                
                // Adicionar a view customizada
                boxContainers[boxIndex].addView(view, 
                    new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                
                // Ocultar WebView original
                webView.setVisibility(View.GONE);
                
                // Forçar fullscreen
                getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
                Toast.makeText(MainActivity.this, 
                    "Fullscreen Box " + (boxIndex + 1), 
                    Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onHideCustomView() {
                if (mCustomView == null) return;
                
                // Mostrar WebView original
                webView.setVisibility(View.VISIBLE);
                
                // Remover view customizada
                boxContainers[boxIndex].removeView(mCustomView);
                
                if (mCustomViewCallback != null) {
                    mCustomViewCallback.onCustomViewHidden();
                }
                
                mCustomView = null;
                mCustomViewCallback = null;
                
                // Restaurar controles
                overlayControls.setVisibility(View.VISIBLE);
                
                // Sair do fullscreen
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        });
    }
    
    private void initEventListeners() {
        // Botão menu superior
        btnMenu.setOnClickListener(v -> {
            if (overlayControls.getVisibility() == View.VISIBLE) {
                overlayControls.setVisibility(View.GONE);
            } else {
                overlayControls.setVisibility(View.VISIBLE);
            }
        });
        
        // Botão sidebar
        btnToggleSidebar.setOnClickListener(v -> {
            if (sidebarMenu.getVisibility() == View.VISIBLE) {
                sidebarMenu.setVisibility(View.GONE);
                isSidebarVisible = false;
            } else {
                sidebarMenu.setVisibility(View.VISIBLE);
                isSidebarVisible = true;
            }
        });
        
        // Botão orientação REAL (agora funciona)
        btnOrientation.setOnClickListener(v -> {
            changeOrientation();
        });
        
        btnTogglePortrait.setOnClickListener(v -> {
            changeOrientation();
        });
        
        // Botão fold checks - FIXED!
        btnFoldChecks.setOnClickListener(v -> {
            toggleBottomPanel();
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
        
        // Toque no grid para fechar tudo
        gridLayout.setOnClickListener(v -> {
            if (isSidebarVisible) {
                sidebarMenu.setVisibility(View.GONE);
                isSidebarVisible = false;
            }
            
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
            case "sidebar":
                if (sidebarMenu.getVisibility() == View.VISIBLE) {
                    sidebarMenu.setVisibility(View.GONE);
                    isSidebarVisible = false;
                } else {
                    sidebarMenu.setVisibility(View.VISIBLE);
                    isSidebarVisible = true;
                }
                break;
            case "zoomin":
                webView.zoomIn();
                Toast.makeText(this, "Zoom in Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
                break;
            case "zoomout":
                webView.zoomOut();
                Toast.makeText(this, "Zoom out Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
                break;
            case "refresh":
                webView.reload();
                Toast.makeText(this, "Refreshing Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
                break;
            case "back":
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    Toast.makeText(this, "No back history", Toast.LENGTH_SHORT).show();
                }
                break;
            case "forward":
                if (webView.canGoForward()) {
                    webView.goForward();
                } else {
                    Toast.makeText(this, "No forward history", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Fullscreen Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
                break;
        }
    }
    
    private void toggleBoxControlPanel(int boxIndex) {
        if (!boxEnabled[boxIndex]) return;
        
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
        }
    }
    
    private void hideBoxControlPanel(int boxIndex) {
        boxControlPanels[boxIndex].setVisibility(View.GONE);
        controlPanelVisible[boxIndex] = false;
        boxHandlers[boxIndex].removeCallbacksAndMessages(null);
    }
    
    private void resetBoxControlPanelTimer(int boxIndex) {
        boxHandlers[boxIndex].removeCallbacksAndMessages(null);
        boxHandlers[boxIndex].postDelayed(() -> {
            if (controlPanelVisible[boxIndex]) {
                hideBoxControlPanel(boxIndex);
            }
        }, 10000); // 10 segundos
    }
    
    private void changeOrientation() {
        // Alternar entre portrait e landscape REAL
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
            // Forçar landscape
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            btnOrientation.setText("↻");
            btnTogglePortrait.setText("Landscape");
        } else {
            currentOrientation = Configuration.ORIENTATION_PORTRAIT;
            // Forçar portrait
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            btnOrientation.setText("📱");
            btnTogglePortrait.setText("Portrait");
        }
        updateLayout();
    }
    
    private void toggleBottomPanel() {
        LinearLayout checkboxContainer = findViewById(R.id.checkboxContainer);
        LinearLayout controlsRow = findViewById(R.id.controlsRow);
        
        if (isBottomPanelFolded) {
            // Mostrar tudo
            if (controlsRow != null) controlsRow.setVisibility(View.VISIBLE);
            if (checkboxContainer != null) checkboxContainer.setVisibility(View.VISIBLE);
            btnFoldChecks.setText("▲");
            isBottomPanelFolded = false;
        } else {
            // Esconder tudo
            if (controlsRow != null) controlsRow.setVisibility(View.GONE);
            if (checkboxContainer != null) checkboxContainer.setVisibility(View.GONE);
            btnFoldChecks.setText("▼");
            isBottomPanelFolded = true;
        }
    }
    
    private void updateLayout() {
        int activeBoxes = 0;
        for (boolean enabled : boxEnabled) {
            if (enabled) activeBoxes++;
        }
        
        if (activeBoxes == 0) {
            Toast.makeText(this, "No boxes active!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Layout baseado em boxes ativas e orientação
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
                rows = 1; cols = 3;
            } else {
                rows = 3; cols = 1;
            }
        } else { // 4 boxes
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                rows = 2; cols = 2;
            } else {
                rows = 4; cols = 1;
            }
        }
        
        // Aplicar layout
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
                params.setMargins(1, 1, 1, 1);
                
                boxContainers[i].setVisibility(View.VISIBLE);
                gridLayout.addView(boxContainers[i], params);
                position++;
            } else {
                boxContainers[i].setVisibility(View.GONE);
                hideBoxControlPanel(i);
            }
        }
        
        gridLayout.requestLayout();
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
        Toast.makeText(this, "Loading all URLs", Toast.LENGTH_SHORT).show();
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
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        webViews[boxIndex].loadUrl(url);
    }
    
    private void reloadAllWebViews() {
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i] && webViews[i] != null) {
                webViews[i].reload();
            }
        }
        Toast.makeText(this, "Reloading all", Toast.LENGTH_SHORT).show();
    }
    
    private void clearAllWebViews() {
        for (int i = 0; i < 4; i++) {
            if (webViews[i] != null) {
                webViews[i].loadUrl("about:blank");
            }
        }
        Toast.makeText(this, "Cleared all", Toast.LENGTH_SHORT).show();
    }
    
    private void applyWebViewSettings() {
        for (WebView webView : webViews) {
            if (webView != null) {
                WebSettings settings = webView.getSettings();
                settings.setJavaScriptEnabled(allowScripts);
            }
        }
        Toast.makeText(this, "Settings applied", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentOrientation = newConfig.orientation;
        updateLayout();
    }
    
    @Override
    public void onBackPressed() {
        if (isSidebarVisible) {
            sidebarMenu.setVisibility(View.GONE);
            isSidebarVisible = false;
            return;
        }
        
        if (overlayControls.getVisibility() == View.VISIBLE) {
            overlayControls.setVisibility(View.GONE);
            return;
        }
        
        super.onBackPressed();
    }
}