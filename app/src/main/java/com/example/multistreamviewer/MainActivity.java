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
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private boolean isControlsFolded = false; // Controles principais dobrados
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

    // Foco / fullscreen
    private int lastFocusedBoxIndex = 0;

    // Fullscreen view container
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private FrameLayout fullscreenContainer;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

         // Configurar tela cheia (UI flags)
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

        // Carregar URLs iniciais com delay para evitar crash
        new Handler().postDelayed(this::loadInitialURLs, 800);

        // Sincronizar estados dos checkboxes visuais com variáveis
        for (int i = 0; i < 4; i++) {
            checkBoxes[i].setChecked(boxEnabled[i]);
        }
        cbAllowScripts.setChecked(allowScripts);
        cbAllowForms.setChecked(allowForms);
        cbAllowPopups.setChecked(allowPopups);
        cbBlockRedirects.setChecked(blockRedirects);
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

        // Fullscreen container (avulso) — criado dinamicamente e adicionado ao decor view
        fullscreenContainer = new FrameLayout(this);
        fullscreenContainer.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Inicialmente invisível; será adicionado ao root view quando necessário

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

            // Criar painel de controles da box (com cores melhoradas)
            createBoxControlPanel(boxIndex);

            // Configurar TOQUE DUPLO para mostrar controles (no container)
            boxContainers[i].setOnTouchListener(new View.OnTouchListener() {
                private long lastTouchTime = 0;
                private static final int DOUBLE_TAP_TIME_DELTA = 300;

                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                        long touchTime = System.currentTimeMillis();
                        if (touchTime - lastTouchTime < DOUBLE_TAP_TIME_DELTA) {
                            // Toque duplo detectado
                            toggleBoxControlPanel(boxIndex);
                            return true;
                        }
                        lastTouchTime = touchTime;
                    }
                    return false;
                }
            });

            // Interceptar toque direto no WebView para marcar foco
            webViews[i].setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    lastFocusedBoxIndex = boxIndex;
                }
                // devolver false pra permitir scroll / interação normal
                return false;
            });
        }

        // Adicionar containers ao grid (serão posicionados em updateLayout)
        for (int i = 0; i < 4; i++) {
            // adicionamos ao grid ao atualizar layout para garantir params corretos
        }
    }

    private void createBoxControlPanel(int boxIndex) {
        // Criar painel de controles com cores mais visíveis
        boxControlPanels[boxIndex] = new LinearLayout(this);
        boxControlPanels[boxIndex].setOrientation(LinearLayout.HORIZONTAL);
        boxControlPanels[boxIndex].setBackgroundColor(0xEE333333); // Mais opaco para melhor visibilidade
        boxControlPanels[boxIndex].setPadding(8, 4, 8, 4);
        boxControlPanels[boxIndex].setVisibility(View.GONE);

        // Layout params - posicionar no TOPO do container
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.TOP;
        params.topMargin = 8;
        boxContainers[boxIndex].addView(boxControlPanels[boxIndex], params);

        // Botões do painel com cores melhoradas
        String[] buttonLabels = {"+", "-", "↻", "←", "→", "⤢"};
        String[] buttonActions = {"zoomIn", "zoomOut", "refresh", "back", "forward", "fullscreen"};

        for (int j = 0; j < buttonLabels.length; j++) {
            Button btn = new Button(this);
            btn.setText(buttonLabels[j]);
            btn.setBackgroundColor(0xFF1e3c72); // Azul em vez de cinza
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(16); // Maior para melhor visibilidade
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
        settings.setJavaScriptEnabled(allowScripts);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Popups / múltiplas janelas
        settings.setSupportMultipleWindows(allowPopups);
        settings.setJavaScriptCanOpenWindowsAutomatically(allowPopups);

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

        // WebViewClient (moderno + fallback)
        webView.setWebViewClient(new WebViewClient() {
            // Para API >= 21
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (blockRedirects) {
                    String currentUrl = view.getUrl();
                    if (currentUrl != null && !isSameDomain(currentUrl, url)) {
                        Toast.makeText(MainActivity.this,
                                "Redirect blocked", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                // deixar WebView carregar normalmente
                return false;
            }

            // Fallback para versões antigas
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (blockRedirects) {
                    String currentUrl = view.getUrl();
                    if (currentUrl != null && !isSameDomain(currentUrl, url)) {
                        Toast.makeText(MainActivity.this,
                                "Redirect blocked", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                // Injetar CSS para fundo preto (somente se não for vimeo/youtube)
                if (!url.contains("vimeo") && !url.contains("youtube")) {
                    try {
                        String js = "document.body.style.backgroundColor='#000000';";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            view.evaluateJavascript("(function(){ " + js + " })()", null);
                        } else {
                            view.loadUrl("javascript:(function(){" + js + "})()");
                        }
                    } catch (Exception e) {
                        Log.w("WebViewInject", "JS inject failed: " + e.getMessage());
                    }
                }
            }
        });

        // WebChromeClient com fullscreen de vídeo
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Entrar em fullscreen nativo
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;

                ViewGroup root = (ViewGroup) getWindow().getDecorView();
                root.addView(customView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                // esconder a interface padrão (opcional)
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                ViewGroup root = (ViewGroup) getWindow().getDecorView();
                root.removeView(customView);
                customView = null;
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }
            }

            // Controlos de permissão (microfone/camera) — aceitar por padrão pode ser perigoso; aqui permitimos temporariamente
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                // Para controlar aprovações, inspecione request.getResources()
                // Exemplo simples: aprovar automaticamente (use com cautela)
                runOnUiThread(() -> {
                    try {
                        request.grant(request.getResources());
                    } catch (Exception e) {
                        Log.w("PermRequest", "Grant failed: " + e.getMessage());
                    }
                });
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

        // Botão toggle sidebar - MOSTRAR/ESCONDER sidebar
        btnToggleSidebar.setOnClickListener(v -> {
            if (sidebarMenu.getVisibility() == View.VISIBLE) {
                sidebarMenu.setVisibility(View.GONE);
                isSidebarVisible = false;
            } else {
                sidebarMenu.setVisibility(View.VISIBLE);
                isSidebarVisible = true;
            }
        });

        // Botão orientação - MUDAR layout manualmente (SEM CRASH)
        btnOrientation.setOnClickListener(v -> {
            changeOrientation();
        });

        btnTogglePortrait.setOnClickListener(v -> {
            changeOrientation();
        });

        // Botão fold checks - dobrar todos os controles
        btnFoldChecks.setOnClickListener(v -> {
            toggleAllControls();
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

        // Checkboxes de boxes - CORRIGIDO
        for (int i = 0; i < 4; i++) {
            final int index = i;
            checkBoxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                boxEnabled[index] = isChecked;
                Log.d("BoxCheck", "Box " + (index + 1) + " enabled: " + isChecked);
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

        try {
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
                    // tentar colocar o primeiro vídeo em fullscreen via JS pedir API
                    try {
                        webView.evaluateJavascript(
                                "(function(){var v=document.querySelector('video'); if(v){ if(v.requestFullscreen){v.requestFullscreen();} else if(v.webkitRequestFullscreen){v.webkitRequestFullscreen();} else if(v.mozRequestFullScreen){v.mozRequestFullScreen();}} })();"
                                , null);
                    } catch (Exception e) {
                        showToast("Fullscreen not supported");
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e("BoxControl", "Error handling control: " + e.getMessage());
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
        // Evitar crash: usar try-catch e mudança simplificada
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
            Log.e("Orientation", "Error changing orientation: " + e.getMessage());
            showToast("Error changing layout");
        }
    }

    private void toggleAllControls() {
        LinearLayout bottomPanel = findViewById(R.id.bottomPanel);
        LinearLayout controlsRow = findViewById(R.id.controlsRow);
        LinearLayout checkboxContainer = findViewById(R.id.checkboxContainer);

        if (isControlsFolded) {
            // Mostrar tudo
            if (controlsRow != null) controlsRow.setVisibility(View.VISIBLE);
            if (checkboxContainer != null) checkboxContainer.setVisibility(View.VISIBLE);
            btnFoldChecks.setText("▲");
            isControlsFolded = false;
        } else {
            // Esconder tudo
            if (controlsRow != null) controlsRow.setVisibility(View.GONE);
            if (checkboxContainer != null) checkboxContainer.setVisibility(View.GONE);
            btnFoldChecks.setText("▼");
            isControlsFolded = true;
        }
    }

    private void updateLayout() {
        try {
            int activeBoxes = 0;
            for (boolean enabled : boxEnabled) {
                if (enabled) activeBoxes++;
            }

            if (activeBoxes == 0) {
                showToast("No boxes active! Enabling all boxes.");
                for (int i = 0; i < 4; i++) {
                    boxEnabled[i] = true;
                    checkBoxes[i].setChecked(true);
                }
                activeBoxes = 4;
            }

            int rows, cols;

            switch (activeBoxes) {
                case 1:
                    rows = 1;
                    cols = 1;
                    break;
                case 2:
                    if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                        rows = 1;
                        cols = 2;
                    } else {
                        rows = 2;
                        cols = 1;
                    }
                    break;
                case 3:
                    if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                        rows = 2;
                        cols = 2; // 2x2 com uma célula vazia
                    } else {
                        rows = 2;
                        cols = 2;
                    }
                    break;
                default: // 4 boxes
                    if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                        rows = 2;
                        cols = 2;
                    } else {
                        rows = 4;
                        cols = 1;
                    }
                    break;
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
                    params.setMargins(1, 1, 1, 1);

                    // Garantir que o container tenha o webview e painel
                    boxContainers[i].setVisibility(View.VISIBLE);
                    gridLayout.addView(boxContainers[i], params);
                    position++;
                } else {
                    boxContainers[i].setVisibility(View.GONE);
                    hideBoxControlPanel(i);
                }
            }

            gridLayout.requestLayout();
            Log.d("Layout", "Updated: " + rows + "x" + cols + ", active: " + activeBoxes);

        } catch (Exception e) {
            Log.e("UpdateLayout", "Error updating layout: " + e.getMessage());
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
            if (webViews[boxIndex] != null) {
                webViews[boxIndex].loadUrl(url);
                Log.d("LoadURL", "Box " + (boxIndex + 1) + ": " + url);
            }
        } catch (Exception e) {
            Log.e("LoadURL", "Error loading URL: " + e.getMessage());
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
                settings.setSupportMultipleWindows(allowPopups);
                settings.setJavaScriptCanOpenWindowsAutomatically(allowPopups);
            }
        }
        showToast("Settings applied");
    }

    private boolean isSameDomain(String url1, String url2) {
        try {
            java.net.URL u1 = new java.net.URL(url1);
            java.net.URL u2 = new java.net.URL(url2);
            return u1.getHost().equalsIgnoreCase(u2.getHost());
        } catch (Exception e) {
            return false;
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onBackPressed() {
        // fechar sidebar / overlay primeiro
        if (isSidebarVisible) {
            sidebarMenu.setVisibility(View.GONE);
            isSidebarVisible = false;
            return;
        }

        if (overlayControls.getVisibility() == View.VISIBLE) {
            overlayControls.setVisibility(View.GONE);
            return;
        }

        // Se estiver em customView (fullscreen vídeo), fechá-lo
        if (customView != null) {
            // esconder custom view
            if (customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
            }
            // onHideCustomView será chamado pelo WebChromeClient
            return;
        }

        // Tentar voltar na última WebView focada (melhor UX)
        WebView focused = webViews[lastFocusedBoxIndex];
        if (focused != null && focused.canGoBack()) {
            focused.goBack();
            return;
        }

        // fallback: tentar qualquer webview
        for (WebView webView : webViews) {
            if (webView != null && webView.canGoBack()) {
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
        // remover views para prevenir leaks
        for (int i = 0; i < 4; i++) {
            WebView w = webViews[i];
            if (w != null) {
                try {
                    // remover do parent
                    ViewGroup parent = (ViewGroup) w.getParent();
                    if (parent != null) parent.removeView(w);
                    w.loadUrl("about:blank");
                    w.stopLoading();
                    w.getSettings().setJavaScriptEnabled(false);
                    w.clearHistory();
                    w.removeAllViews();
                    w.destroy();
                } catch (Exception e) {
                    Log.w("WebViewDestroy", "Error destroying webview: " + e.getMessage());
                }
                webViews[i] = null;
            }
        }
        for (Handler handler : boxHandlers) {
            if (handler != null) handler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }
}
