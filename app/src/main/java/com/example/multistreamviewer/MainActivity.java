package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Componentes
    private GridLayout gridLayout;
    private WebView[] webViews = new WebView[4];
    private FrameLayout[] playerContainers = new FrameLayout[4];
    private LinearLayout[] loadingOverlays = new LinearLayout[4];
    private LinearLayout[] controlPanels = new LinearLayout[4]; // Painéis de controle de cada box
    private EditText[] urlInputs = new EditText[4];
    private CheckBox[] checkBoxes = new CheckBox[4];
    
    // Botões de controle por box
    private Button[] btnZoomIn = new Button[4];
    private Button[] btnZoomOut = new Button[4];
    private Button[] btnRefresh = new Button[4];
    private Button[] btnBack = new Button[4];
    private Button[] btnForward = new Button[4];
    private Button[] btnFullscreen = new Button[4];
    
    // Menu e controles gerais
    private LinearLayout sideMenu;
    private LinearLayout bottomPanel; // Painel inferior foldable
    private Button btnToggleMenu, btnToggleOrientation, btnCloseMenu;
    private Button btnToggleBottomPanel; // Botão para mostrar/esconder painel inferior
    private Spinner layoutSpinner;
    private Button btnLoadAll, btnReloadAll, btnClearAll;
    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups;
    private CheckBox cbBlockRedirects; // Novo: bloquear redirecionamentos
    
    // Estado
    private boolean isMenuVisible = false;
    private boolean isBottomPanelVisible = false;
    private int currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
    private int activePlayersCount = 4;
    private boolean[] playerFullscreen = new boolean[4];
    private View fullscreenView = null;
    private int fullscreenPlayerIndex = -1;
    private boolean[] controlPanelVisible = new boolean[4];
    private Handler[] controlPanelHandlers = new Handler[4];
    private Runnable[] hideControlPanelRunnables = new Runnable[4];
    
    // Configurações WebView
    private boolean allowScripts = true;
    private boolean allowForms = true;
    private boolean allowPopups = true;
    private boolean blockRedirects = false; // Novo: bloquear redirecionamentos
    
    // Histórico de navegação por player
    private Map<Integer, List<String>> historyMap = new HashMap<>();
    private Map<Integer, Integer> historyIndexMap = new HashMap<>();

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar orientação
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        
        // Inicializar arrays de IDs
        int[] webViewIds = {R.id.webView1, R.id.webView2, R.id.webView3, R.id.webView4};
        int[] containerIds = {R.id.playerContainer1, R.id.playerContainer2, R.id.playerContainer3, R.id.playerContainer4};
        int[] overlayIds = {R.id.loadingOverlay1, R.id.loadingOverlay2, R.id.loadingOverlay3, R.id.loadingOverlay4};
        int[] controlPanelIds = {R.id.controlPanel1, R.id.controlPanel2, R.id.controlPanel3, R.id.controlPanel4};
        int[] inputIds = {R.id.urlInput1, R.id.urlInput2, R.id.urlInput3, R.id.urlInput4};
        int[] checkboxIds = {R.id.checkBox1, R.id.checkBox2, R.id.checkBox3, R.id.checkBox4};
        int[] zoomInIds = {R.id.btnZoomIn1, R.id.btnZoomIn2, R.id.btnZoomIn3, R.id.btnZoomIn4};
        int[] zoomOutIds = {R.id.btnZoomOut1, R.id.btnZoomOut2, R.id.btnZoomOut3, R.id.btnZoomOut4};
        int[] refreshIds = {R.id.btnRefresh1, R.id.btnRefresh2, R.id.btnRefresh3, R.id.btnRefresh4};
        int[] backIds = {R.id.btnBack1, R.id.btnBack2, R.id.btnBack3, R.id.btnBack4};
        int[] forwardIds = {R.id.btnForward1, R.id.btnForward2, R.id.btnForward3, R.id.btnForward4};
        int[] fullscreenIds = {R.id.btnFullscreen1, R.id.btnFullscreen2, R.id.btnFullscreen3, R.id.btnFullscreen4};
        
        // Obter referências
        gridLayout = findViewById(R.id.gridLayout);
        sideMenu = findViewById(R.id.sideMenu);
        bottomPanel = findViewById(R.id.bottomPanel);
        btnToggleMenu = findViewById(R.id.btnToggleMenu);
        btnToggleOrientation = findViewById(R.id.btnToggleOrientation);
        btnCloseMenu = findViewById(R.id.btnCloseMenu);
        btnToggleBottomPanel = findViewById(R.id.btnToggleBottomPanel);
        layoutSpinner = findViewById(R.id.layoutSpinner);
        btnLoadAll = findViewById(R.id.btnLoadAll);
        btnReloadAll = findViewById(R.id.btnReloadAll);
        btnClearAll = findViewById(R.id.btnClearAll);
        cbAllowScripts = findViewById(R.id.cbAllowScripts);
        cbAllowForms = findViewById(R.id.cbAllowForms);
        cbAllowPopups = findViewById(R.id.cbAllowPopups);
        cbBlockRedirects = findViewById(R.id.cbBlockRedirects);
        
        // Configurar checkboxes de permissões
        cbAllowScripts.setChecked(allowScripts);
        cbAllowForms.setChecked(allowForms);
        cbAllowPopups.setChecked(allowPopups);
        cbBlockRedirects.setChecked(blockRedirects);
        
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
        
        // Inicializar WebViews e controles
        for (int i = 0; i < 4; i++) {
            final int playerIndex = i;
            
            // Inicializar histórico
            historyMap.put(i, new ArrayList<>());
            historyIndexMap.put(i, -1);
            
            // Handlers para ocultar controles
            controlPanelHandlers[i] = new Handler();
            
            // WebViews
            webViews[i] = findViewById(webViewIds[i]);
            setupWebView(webViews[i], playerIndex);
            
            // Containers
            playerContainers[i] = findViewById(containerIds[i]);
            
            // Overlays
            loadingOverlays[i] = findViewById(overlayIds[i]);
            
            // Painéis de controle
            controlPanels[i] = findViewById(controlPanelIds[i]);
            controlPanels[i].setVisibility(View.GONE);
            controlPanelVisible[i] = false;
            
            // Inputs
            urlInputs[i] = findViewById(inputIds[i]);
            urlInputs[i].setOnEditorActionListener((v, actionId, event) -> {
                loadURL(playerIndex, urlInputs[playerIndex].getText().toString().trim());
                return true;
            });
            
            // CheckBoxes
            checkBoxes[i] = findViewById(checkboxIds[i]);
            checkBoxes[i].setChecked(true);
            checkBoxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateActivePlayers();
                applyAutoLayout();
            });
            
            // Botões de controle
            btnZoomIn[i] = findViewById(zoomInIds[i]);
            btnZoomOut[i] = findViewById(zoomOutIds[i]);
            btnRefresh[i] = findViewById(refreshIds[i]);
            btnBack[i] = findViewById(backIds[i]);
            btnForward[i] = findViewById(forwardIds[i]);
            btnFullscreen[i] = findViewById(fullscreenIds[i]);
            
            // Configurar listeners
            btnZoomIn[i].setOnClickListener(v -> {
                zoomWebView(playerIndex, 1.2f);
                resetControlPanelTimer(playerIndex);
            });
            
            btnZoomOut[i].setOnClickListener(v -> {
                zoomWebView(playerIndex, 0.8f);
                resetControlPanelTimer(playerIndex);
            });
            
            btnRefresh[i].setOnClickListener(v -> {
                refreshWebView(playerIndex);
                resetControlPanelTimer(playerIndex);
            });
            
            btnBack[i].setOnClickListener(v -> {
                goBack(playerIndex);
                resetControlPanelTimer(playerIndex);
            });
            
            btnForward[i].setOnClickListener(v -> {
                goForward(playerIndex);
                resetControlPanelTimer(playerIndex);
            });
            
            btnFullscreen[i].setOnClickListener(v -> {
                togglePlayerFullscreen(playerIndex);
                resetControlPanelTimer(playerIndex);
            });
            
            // Tocar no container para mostrar controles
            playerContainers[i].setOnClickListener(v -> {
                toggleControlPanel(playerIndex);
            });
            
            // CORREÇÃO: Criar variável final para usar na lambda
            final int finalPlayerIndex = i;
            
            // Criar runnable para esconder controles
            hideControlPanelRunnables[i] = new Runnable() {
                @Override
                public void run() {
                    if (controlPanelVisible[finalPlayerIndex] && !playerFullscreen[finalPlayerIndex]) {
                        controlPanels[finalPlayerIndex].setVisibility(View.GONE);
                        controlPanelVisible[finalPlayerIndex] = false;
                    }
                }
            };
        }
        
        // Configurar Spinner de layouts
        updateLayoutSpinner();
        layoutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String layout = parent.getItemAtPosition(position).toString();
                applyLayout(layout);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        
        // Configurar botões do menu
        btnToggleMenu.setOnClickListener(v -> toggleMenu());
        btnToggleOrientation.setOnClickListener(v -> toggleOrientation());
        btnCloseMenu.setOnClickListener(v -> toggleMenu());
        btnToggleBottomPanel.setOnClickListener(v -> toggleBottomPanel());
        
        btnLoadAll.setOnClickListener(v -> loadAllURLs());
        btnReloadAll.setOnClickListener(v -> reloadAllWebViews());
        btnClearAll.setOnClickListener(v -> clearAllWebViews());
        
        // Configurar orientação inicial
        currentOrientation = getResources().getConfiguration().orientation;
        updateOrientationUI();
        
        // Aplicar layout inicial
        updateActivePlayers();
        applyAutoLayout();
        
        // Esconder menu e painel inferior inicialmente
        sideMenu.setVisibility(View.GONE);
        bottomPanel.setVisibility(View.GONE);
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView, int playerIndex) {
        WebSettings webSettings = webView.getSettings();
        
        // Configurações básicas
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setLoadsImagesAutomatically(true);
        
        // Configurações de zoom
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        
        // User agent para vídeos
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        // Habilitar hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // Configurar WebViewClient personalizado para controlar redirecionamentos
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                loadingOverlays[playerIndex].setVisibility(View.VISIBLE);
                urlInputs[playerIndex].setText(url);
                
                // Adicionar ao histórico
                List<String> history = historyMap.get(playerIndex);
                if (history.isEmpty() || !history.get(history.size() - 1).equals(url)) {
                    history.add(url);
                    historyIndexMap.put(playerIndex, history.size() - 1);
                    updateNavigationButtons(playerIndex);
                }
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                loadingOverlays[playerIndex].setVisibility(View.GONE);
                updateNavigationButtons(playerIndex);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                loadingOverlays[playerIndex].setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, 
                    "Player " + (playerIndex + 1) + " error: " + description, 
                    Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Bloquear redirecionamentos se a opção estiver ativada
                if (blockRedirects) {
                    // Permite apenas carregar a URL se for do mesmo domínio ou se for uma ação do usuário
                    String currentUrl = view.getUrl();
                    if (currentUrl != null && !isSameDomain(currentUrl, url)) {
                        Toast.makeText(MainActivity.this, 
                            "Redirect blocked for Player " + (playerIndex + 1), 
                            Toast.LENGTH_SHORT).show();
                        return true; // Bloqueia o redirecionamento
                    }
                }
                return false; // Permite o redirecionamento
            }
        });
        
        // Configurar WebChromeClient para vídeos fullscreen
        webView.setWebChromeClient(new WebChromeClient() {
            private View customView;
            private WebChromeClient.CustomViewCallback customViewCallback;
            
            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                
                customView = view;
                customViewCallback = callback;
                
                // Adicionar a view personalizada ao container
                playerContainers[playerIndex].addView(customView, 
                    new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                
                // Esconder WebView
                webViews[playerIndex].setVisibility(View.GONE);
                
                // Ativar fullscreen
                playerFullscreen[playerIndex] = true;
                fullscreenView = view;
                fullscreenPlayerIndex = playerIndex;
                
                // Esconder todos os controles
                hideAllControlPanels();
            }
            
            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                
                // Mostrar WebView novamente
                webViews[playerIndex].setVisibility(View.VISIBLE);
                
                // Remover view personalizada
                playerContainers[playerIndex].removeView(customView);
                
                // Notificar callback
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }
                
                customView = null;
                customViewCallback = null;
                playerFullscreen[playerIndex] = false;
                fullscreenView = null;
                fullscreenPlayerIndex = -1;
            }
        });
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
    
    private void applyWebViewSettings() {
        for (WebView webView : webViews) {
            if (webView != null) {
                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(allowScripts);
                // As outras configurações são mantidas
            }
        }
    }
    
    private void loadURL(int playerIndex, String url) {
        if (url.isEmpty()) {
            Toast.makeText(this, "Enter URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        webViews[playerIndex].loadUrl(url);
        urlInputs[playerIndex].setText(url);
    }
    
    private void loadAllURLs() {
        for (int i = 0; i < 4; i++) {
            if (checkBoxes[i].isChecked()) {
                String url = urlInputs[i].getText().toString().trim();
                if (!url.isEmpty()) {
                    loadURL(i, url);
                }
            }
        }
        Toast.makeText(this, "Loading all", Toast.LENGTH_SHORT).show();
    }
    
    private void reloadAllWebViews() {
        for (int i = 0; i < 4; i++) {
            if (checkBoxes[i].isChecked() && webViews[i] != null) {
                webViews[i].reload();
            }
        }
        Toast.makeText(this, "Reloading all", Toast.LENGTH_SHORT).show();
    }
    
    private void refreshWebView(int playerIndex) {
        if (webViews[playerIndex] != null) {
            webViews[playerIndex].reload();
        }
    }
    
    private void goBack(int playerIndex) {
        if (webViews[playerIndex].canGoBack()) {
            webViews[playerIndex].goBack();
        } else {
            List<String> history = historyMap.get(playerIndex);
            int currentIndex = historyIndexMap.get(playerIndex);
            if (currentIndex > 0) {
                String url = history.get(currentIndex - 1);
                webViews[playerIndex].loadUrl(url);
                historyIndexMap.put(playerIndex, currentIndex - 1);
            }
        }
    }
    
    private void goForward(int playerIndex) {
        if (webViews[playerIndex].canGoForward()) {
            webViews[playerIndex].goForward();
        } else {
            List<String> history = historyMap.get(playerIndex);
            int currentIndex = historyIndexMap.get(playerIndex);
            if (currentIndex < history.size() - 1) {
                String url = history.get(currentIndex + 1);
                webViews[playerIndex].loadUrl(url);
                historyIndexMap.put(playerIndex, currentIndex + 1);
            }
        }
    }
    
    private void updateNavigationButtons(int playerIndex) {
        boolean canGoBack = webViews[playerIndex].canGoBack();
        boolean canGoForward = webViews[playerIndex].canGoForward();
        
        btnBack[playerIndex].setAlpha(canGoBack ? 1.0f : 0.5f);
        btnForward[playerIndex].setAlpha(canGoForward ? 1.0f : 0.5f);
    }
    
    private void clearAllWebViews() {
        for (int i = 0; i < 4; i++) {
            if (webViews[i] != null) {
                webViews[i].loadUrl("about:blank");
                urlInputs[i].setText("");
                historyMap.get(i).clear();
                historyIndexMap.put(i, -1);
                updateNavigationButtons(i);
            }
        }
        Toast.makeText(this, "Cleared all", Toast.LENGTH_SHORT).show();
    }
    
    private void zoomWebView(int playerIndex, float factor) {
        WebView webView = webViews[playerIndex];
        if (webView != null) {
            WebSettings settings = webView.getSettings();
            int currentZoom = settings.getTextZoom();
            int newZoom = (int)(currentZoom * factor);
            if (newZoom > 30 && newZoom < 500) { // Limites razoáveis
                settings.setTextZoom(newZoom);
                Toast.makeText(this, "Zoom: " + newZoom + "%", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void togglePlayerFullscreen(int playerIndex) {
        if (playerFullscreen[playerIndex]) {
            // Sair do fullscreen
            WebChromeClient chromeClient = (WebChromeClient) webViews[playerIndex].getWebChromeClient();
            if (chromeClient != null) {
                chromeClient.onHideCustomView();
            }
        } else {
            // Entrar no fullscreen - para páginas normais
            playerContainers[playerIndex].setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            playerFullscreen[playerIndex] = true;
            fullscreenPlayerIndex = playerIndex;
            hideAllControlPanels();
        }
    }
    
    private void toggleControlPanel(int playerIndex) {
        if (playerFullscreen[playerIndex]) return;
        
        if (controlPanelVisible[playerIndex]) {
            controlPanels[playerIndex].setVisibility(View.GONE);
            controlPanelVisible[playerIndex] = false;
            controlPanelHandlers[playerIndex].removeCallbacks(hideControlPanelRunnables[playerIndex]);
        } else {
            // Esconder outros painéis
            hideAllControlPanels();
            
            // Mostrar este painel
            controlPanels[playerIndex].setVisibility(View.VISIBLE);
            controlPanelVisible[playerIndex] = true;
            
            // Atualizar botões de navegação
            updateNavigationButtons(playerIndex);
            
            // Configurar timer para esconder
            resetControlPanelTimer(playerIndex);
        }
    }
    
    private void resetControlPanelTimer(int playerIndex) {
        controlPanelHandlers[playerIndex].removeCallbacks(hideControlPanelRunnables[playerIndex]);
        controlPanelHandlers[playerIndex].postDelayed(hideControlPanelRunnables[playerIndex], 10000); // 10 segundos
    }
    
    private void hideAllControlPanels() {
        for (int i = 0; i < 4; i++) {
            controlPanels[i].setVisibility(View.GONE);
            controlPanelVisible[i] = false;
            controlPanelHandlers[i].removeCallbacks(hideControlPanelRunnables[i]);
        }
    }
    
    private void updateActivePlayers() {
        activePlayersCount = 0;
        for (int i = 0; i < 4; i++) {
            if (checkBoxes[i].isChecked()) {
                activePlayersCount++;
                playerContainers[i].setVisibility(View.VISIBLE);
            } else {
                playerContainers[i].setVisibility(View.GONE);
                // Sair do fullscreen se estiver
                if (playerFullscreen[i]) {
                    togglePlayerFullscreen(i);
                }
                // Esconder painel de controle
                controlPanels[i].setVisibility(View.GONE);
                controlPanelVisible[i] = false;
                controlPanelHandlers[i].removeCallbacks(hideControlPanelRunnables[i]);
            }
        }
        
        // Atualizar spinner de layouts
        updateLayoutSpinner();
    }
    
    private void updateLayoutSpinner() {
        // Layouts disponíveis baseados no número de players ativos e orientação
        List<String> availableLayouts = new ArrayList<>();
        
        if (activePlayersCount == 1) {
            availableLayouts.add("1x1");
        } else if (activePlayersCount == 2) {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                availableLayouts.add("1x2");
                availableLayouts.add("2x1");
            } else {
                availableLayouts.add("2x1");
                availableLayouts.add("1x2");
            }
        } else if (activePlayersCount == 3) {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                availableLayouts.add("1x3");
                availableLayouts.add("3x1");
            } else {
                availableLayouts.add("3x1");
                availableLayouts.add("1x3");
            }
        } else if (activePlayersCount == 4) {
            availableLayouts.add("2x2");
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                availableLayouts.add("1x4");
                availableLayouts.add("4x1");
            } else {
                availableLayouts.add("4x1");
                availableLayouts.add("1x4");
            }
        }
        
        // Configurar adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, availableLayouts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        layoutSpinner.setAdapter(adapter);
        
        // Selecionar layout padrão
        if (!availableLayouts.isEmpty()) {
            layoutSpinner.setSelection(0);
            applyLayout(availableLayouts.get(0));
        }
    }
    
    private void applyAutoLayout() {
        updateLayoutSpinner();
    }
    
    private void applyLayout(String layout) {
        // Parse layout string (e.g., "2x2")
        String[] parts = layout.split("x");
        if (parts.length != 2) return;
        
        try {
            int rows = Integer.parseInt(parts[0]);
            int cols = Integer.parseInt(parts[1]);
            
            // Configurar grid
            gridLayout.setRowCount(rows);
            gridLayout.setColumnCount(cols);
            
            // Remover todas as views do grid
            gridLayout.removeAllViews();
            
            // Adicionar apenas os containers ativos na ordem correta
            int playerCount = 0;
            for (int i = 0; i < 4; i++) {
                if (checkBoxes[i].isChecked()) {
                    GridLayout.Spec rowSpec = GridLayout.spec(playerCount / cols, 1f);
                    GridLayout.Spec colSpec = GridLayout.spec(playerCount % cols, 1f);
                    
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
                    params.width = 0;
                    params.height = 0;
                    params.setMargins(2, 2, 2, 2);
                    
                    // Adicionar ao grid
                    gridLayout.addView(playerContainers[i], params);
                    playerCount++;
                    
                    if (playerCount >= rows * cols) break;
                }
            }
            
            // Forçar redesenho
            gridLayout.requestLayout();
            gridLayout.invalidate();
            
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
    
    private void toggleMenu() {
        isMenuVisible = !isMenuVisible;
        
        if (isMenuVisible) {
            sideMenu.setVisibility(View.VISIBLE);
            sideMenu.animate().translationX(0).setDuration(200).start();
            btnToggleMenu.setText("✕");
        } else {
            sideMenu.animate().translationX(-sideMenu.getWidth()).setDuration(200)
                .withEndAction(() -> {
                    sideMenu.setVisibility(View.GONE);
                    btnToggleMenu.setText("☰");
                }).start();
        }
    }
    
    private void toggleBottomPanel() {
        isBottomPanelVisible = !isBottomPanelVisible;
        
        if (isBottomPanelVisible) {
            bottomPanel.setVisibility(View.VISIBLE);
            bottomPanel.animate().translationY(0).setDuration(200).start();
            btnToggleBottomPanel.setText("▼");
        } else {
            bottomPanel.animate().translationY(bottomPanel.getHeight()).setDuration(200)
                .withEndAction(() -> {
                    bottomPanel.setVisibility(View.GONE);
                    btnToggleBottomPanel.setText("▲");
                }).start();
        }
    }
    
    private void toggleOrientation() {
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }
    
    private void updateOrientationUI() {
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            btnToggleOrientation.setText("📱 Portrait");
        } else {
            btnToggleOrientation.setText("📱 Landscape");
        }
        
        // Reaplicar layout para nova orientação
        updateLayoutSpinner();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentOrientation = newConfig.orientation;
        updateOrientationUI();
    }
    
    @Override
    public void onBackPressed() {
        // Se estiver em fullscreen, sair primeiro
        if (fullscreenPlayerIndex != -1 && playerFullscreen[fullscreenPlayerIndex]) {
            togglePlayerFullscreen(fullscreenPlayerIndex);
            return;
        }
        
        // Se menu estiver visível, fechar
        if (isMenuVisible) {
            toggleMenu();
            return;
        }
        
        // Se painel inferior estiver visível, fechar
        if (isBottomPanelVisible) {
            toggleBottomPanel();
            return;
        }
        
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
    protected void onResume() {
        super.onResume();
        for (WebView webView : webViews) {
            if (webView != null) {
                webView.onResume();
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        for (WebView webView : webViews) {
            if (webView != null) {
                webView.onPause();
            }
        }
        // Remover todos os handlers
        for (Handler handler : controlPanelHandlers) {
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
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
        // Remover todos os handlers
        for (Handler handler : controlPanelHandlers) {
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
            }
        }
        super.onDestroy();
    }
}
