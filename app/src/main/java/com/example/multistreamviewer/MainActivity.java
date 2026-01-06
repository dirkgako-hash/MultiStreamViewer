package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
        
        // Mostrar overlay inicialmente para testes
        overlayControls.setVisibility(View.VISIBLE);
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
            
            // Criar painel de controles
            createBoxControlPanel(boxIndex);
            
            // Configurar TOQUE LONGO na box (em vez de clique simples)
            boxContainers[i].setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    toggleBoxControlPanel(boxIndex);
                    return true;
                }
            });
            
            // Configurar clique simples para esconder controles
            boxContainers[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (controlPanelVisible[boxIndex]) {
                        hideBoxControlPanel(boxIndex);
                    }
                }
            });
        }
        
        // Adicionar containers ao grid
        for (int i = 0; i < 4; i++) {
            gridLayout.addView(boxContainers[i]);
        }
    }
    
    private void createBoxControlPanel(int boxIndex) {
        // Criar painel de controles
        boxControlPanels[boxIndex] = new LinearLayout(this);
        boxControlPanels[boxIndex].setOrientation(LinearLayout.HORIZONTAL);
        boxControlPanels[boxIndex].setBackgroundColor(0xCC1a1a1a);
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
            btn.setBackgroundColor(0xFF555555);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(12);
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
        settings.setJavaScriptEnabled(allowScripts);
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
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        // Cor de fundo
        webView.setBackgroundColor(Color.BLACK);
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        
        // WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (blockRedirects) {
                    String url = request.getUrl().toString();
                    String currentUrl = view.getUrl();
                    
                    if (currentUrl != null && !isSameDomain(currentUrl, url)) {
                        Toast.makeText(MainActivity.this, 
                            "Redirect blocked", 
                            Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                return false;
            }
            
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                // Injetar CSS para fundo preto
                view.loadUrl("javascript:(function(){" +
                    "document.body.style.backgroundColor='#000000';" +
                    "})()");
            }
        });
        
        // WebChromeClient para fullscreen
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Fullscreen dentro da box
                boxContainers[boxIndex].addView(view, 
                    new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                webView.setVisibility(View.GONE);
            }
            
            @Override
            public void onHideCustomView() {
                // Sair do fullscreen
                webView.setVisibility(View.VISIBLE);
                View customView = boxContainers[boxIndex].getChildAt(boxContainers[boxIndex].getChildCount() - 1);
                if (customView != webView && customView != boxControlPanels[boxIndex]) {
                    boxContainers[boxIndex].removeView(customView);
                }
            }
        });
    }
    
    private void initEventListeners() {
        // Botão menu - MOSTRAR overlay
        btnMenu.setOnClickListener(v -> {
            Log.d("MainActivity", "Menu button clicked");
            overlayControls.setVisibility(View.VISIBLE);
        });
        
        // Botão toggle sidebar - MOSTRAR sidebar
        btnToggleSidebar.setOnClickListener(v -> {
            Log.d("MainActivity", "Toggle sidebar button clicked");
            sidebarMenu.setVisibility(View.VISIBLE);
            isSidebarVisible = true;
        });
        
        // Botão orientação - MUDAR layout manualmente
        btnOrientation.setOnClickListener(v -> {
            Log.d("MainActivity", "Orientation button clicked");
            toggleOrientation();
        });
        
        btnTogglePortrait.setOnClickListener(v -> {
            Log.d("MainActivity", "Toggle portrait button clicked");
            toggleOrientation();
        });
        
        // Botão fold checks
        btnFoldChecks.setOnClickListener(v -> {
            Log.d("MainActivity", "Fold checks button clicked");
            toggleBottomPanel();
        });
        
        // Botão fechar menu
        btnCloseMenu.setOnClickListener(v -> {
            Log.d("MainActivity", "Close menu button clicked");
            sidebarMenu.setVisibility(View.GONE);
            isSidebarVisible = false;
        });
        
        // Botões de ação
        btnLoadAll.setOnClickListener(v -> {
            Log.d("MainActivity", "Load all button clicked");
            loadAllURLs();
        });
        
        btnReloadAll.setOnClickListener(v -> {
            Log.d("MainActivity", "Reload all button clicked");
            reloadAllWebViews();
        });
        
        btnClearAll.setOnClickListener(v -> {
            Log.d("MainActivity", "Clear all button clicked");
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
        
        // Toque no overlay para esconder (apenas se tocar na área vazia)
        overlayControls.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Não fazer nada, deixar os botões funcionarem
                return false;
            }
        });
        
        // Toque fora do sidebar para fechar
        findViewById(R.id.gridLayout).setOnClickListener(v -> {
            if (isSidebarVisible) {
                sidebarMenu.setVisibility(View.GONE);
                isSidebarVisible = false;
            }
        });
    }
    
    private void handleBoxControlClick(int boxIndex, String action) {
        WebView webView = webViews[boxIndex];
        
        switch (action) {
            case "zoomIn":
                webView.zoomIn();
                Toast.makeText(this, "Zoom in Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
                break;
            case "zoomOut":
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
                // Tentar ativar fullscreen
                webView.loadUrl("javascript:(function(){" +
                    "var videos=document.getElementsByTagName('video');" +
                    "if(videos.length>0){videos[0].requestFullscreen();}" +
                    "})()");
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
        }, 5000); // Reduzido para 5 segundos
    }
    
    private void toggleOrientation() {
        int newOrientation = (currentOrientation == Configuration.ORIENTATION_PORTRAIT) ?
            Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
        
        // Atualizar ícone do botão
        btnOrientation.setText(newOrientation == Configuration.ORIENTATION_PORTRAIT ? "📱" : "🔄");
        btnTogglePortrait.setText(newOrientation == Configuration.ORIENTATION_PORTRAIT ? "Portrait" : "Landscape");
        
        // Atualizar orientação e layout
        currentOrientation = newOrientation;
        updateLayout();
        
        Toast.makeText(this, 
            newOrientation == Configuration.ORIENTATION_PORTRAIT ? "Portrait Mode" : "Landscape Mode", 
            Toast.LENGTH_SHORT).show();
    }
    
    private void toggleBottomPanel() {
        LinearLayout checkboxContainer = findViewById(R.id.checkboxContainer);
        
        if (isBottomPanelFolded) {
            checkboxContainer.setVisibility(View.VISIBLE);
            btnFoldChecks.setText("▼");
            isBottomPanelFolded = false;
        } else {
            checkboxContainer.setVisibility(View.GONE);
            btnFoldChecks.setText("▲");
            isBottomPanelFolded = true;
        }
    }
    
    private void updateLayout() {
        int activeBoxes = 0;
        for (boolean enabled : boxEnabled) {
            if (enabled) activeBoxes++;
        }
        
        int rows, cols;
        
        if (activeBoxes == 1) {
            rows = 1; cols = 1;
        } else if (activeBoxes == 2) {
            // Sempre 1x2 em landscape, 2x1 em portrait
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
        
        gridLayout.setRowCount(rows);
        gridLayout.setColumnCount(cols);
        gridLayout.removeAllViews();
        
        int added = 0;
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i]) {
                GridLayout.Spec rowSpec = GridLayout.spec(added / cols, 1f);
                GridLayout.Spec colSpec = GridLayout.spec(added % cols, 1f);
                
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
                params.width = 0;
                params.height = 0;
                params.setMargins(2, 2, 2, 2);
                
                boxContainers[i].setVisibility(View.VISIBLE);
                gridLayout.addView(boxContainers[i], params);
                added++;
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
        Toast.makeText(this, "Loading Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
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
                settings.setAllowFileAccess(allowForms);
                settings.setAllowContentAccess(allowForms);
            }
        }
        Toast.makeText(this, "Settings applied", Toast.LENGTH_SHORT).show();
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
        for (Handler handler : boxHandlers) {
            if (handler != null) handler.removeCallbacksAndMessages(null);
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
        for (Handler handler : boxHandlers) {
            if (handler != null) handler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }
}