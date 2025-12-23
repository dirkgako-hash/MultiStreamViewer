package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

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
    private int currentOrientation;
    private Handler controlPanelHandler = new Handler();
    private Runnable hideControlPanelRunnable;
    
    // Configurações WebView
    private boolean allowScripts = true;
    private boolean allowForms = true;
    private boolean allowPopups = true;
    private boolean blockRedirects = false;
    
    // Controles individuais por box
    private LinearLayout[] boxControlPanels = new LinearLayout[4];
    private boolean[] controlPanelVisible = new boolean[4];
    private Handler[] boxHandlers = new Handler[4];

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
        currentOrientation = getResources().getConfiguration().orientation;
        updateLayout();
        
        // Carregar URLs iniciais após um breve delay
        new Handler().postDelayed(this::loadInitialURLs, 1000);
    }
    
    private void initViews() {
        gridLayout = findViewById(R.id.gridLayout);
        overlayControls = findViewById(R.id.overlayControls);
        sidebarMenu = findViewById(R.id.sidebarMenu);
        
        // Botões
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
        
        // Inicializar handlers para boxes
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
            
            // Criar container para a box
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
            
            // Criar painel de controles para esta box
            createBoxControlPanel(boxIndex);
            
            // Configurar clique na box para mostrar controles
            boxContainers[i].setOnClickListener(v -> toggleBoxControlPanel(boxIndex));
        }
        
        // Adicionar todos os containers ao grid
        for (int i = 0; i < 4; i++) {
            gridLayout.addView(boxContainers[i]);
        }
    }
    
    private void createBoxControlPanel(int boxIndex) {
        // Criar painel de controles para a box
        boxControlPanels[boxIndex] = new LinearLayout(this);
        boxControlPanels[boxIndex].setOrientation(LinearLayout.HORIZONTAL);
        boxControlPanels[boxIndex].setBackgroundColor(0xCC1a1a1a);
        boxControlPanels[boxIndex].setPadding(8, 8, 8, 8);
        boxControlPanels[boxIndex].setVisibility(View.GONE);
        
        // Layout params para posicionar no topo da box
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        boxContainers[boxIndex].addView(boxControlPanels[boxIndex], params);
        
        // Botões do painel de controle
        String[] buttonLabels = {"⤊", "⤋", "↻", "←", "→", "⤢"};
        String[] buttonActions = {"zoomIn", "zoomOut", "refresh", "back", "forward", "fullscreen"};
        
        for (int j = 0; j < buttonLabels.length; j++) {
            Button btn = new Button(this);
            btn.setText(buttonLabels[j]);
            btn.setTag(boxIndex + "_" + buttonActions[j]);
            btn.setBackgroundColor(0xFF555555);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(12);
            btn.setPadding(8, 4, 8, 4);
            
            final int actionIndex = j;
            final int currentBoxIndex = boxIndex;
            btn.setOnClickListener(v -> handleBoxControlClick(currentBoxIndex, buttonActions[actionIndex]));
            
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
        
        // Otimizações para vídeo
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        
        // User agent para compatibilidade
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        // Cor de fundo
        webView.setBackgroundColor(Color.BLACK);
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        
        // WebViewClient personalizado
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Bloquear redirecionamentos se configurado
                if (blockRedirects) {
                    String url = request.getUrl().toString();
                    String currentUrl = view.getUrl();
                    
                    // Permitir apenas URLs do mesmo domínio
                    if (currentUrl != null && !isSameDomain(currentUrl, url)) {
                        Toast.makeText(MainActivity.this, 
                            "Redirect blocked for Box " + (boxIndex + 1), 
                            Toast.LENGTH_SHORT).show();
                        return true; // Bloquear
                    }
                }
                return false; // Permitir
            }
            
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                // Injetar CSS para fundo preto
                view.loadUrl("javascript:(function(){" +
                    "document.body.style.backgroundColor='#000000';" +
                    "document.body.style.color='#ffffff';" +
                    "})()");
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                // Reforçar CSS após carregamento
                view.loadUrl("javascript:(function(){" +
                    "document.body.style.backgroundColor='#000000';" +
                    "var videos=document.getElementsByTagName('video');" +
                    "for(var i=0;i<videos.length;i++){" +
                    "videos[i].style.backgroundColor='#000000';" +
                    "}" +
                    "})()");
            }
        });
        
        // WebChromeClient para fullscreen dentro da box
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Fullscreen dentro da própria box
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
        // Botão menu (☰)
        btnMenu.setOnClickListener(v -> toggleOverlayControls());
        btnToggleSidebar.setOnClickListener(v -> toggleSidebar());
        
        // Botão orientação (📱)
        btnOrientation.setOnClickListener(v -> toggleOrientation());
        btnTogglePortrait.setOnClickListener(v -> toggleOrientation());
        
        // Botão fold checks (▼)
        btnFoldChecks.setOnClickListener(v -> toggleBottomPanel());
        
        // Botão fechar menu
        btnCloseMenu.setOnClickListener(v -> toggleSidebar());
        
        // Botões de ação
        btnLoadAll.setOnClickListener(v -> loadAllURLs());
        btnReloadAll.setOnClickListener(v -> reloadAllWebViews());
        btnClearAll.setOnClickListener(v -> clearAllWebViews());
        
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
        
        // Toque fora do menu para fechá-lo
        overlayControls.setOnClickListener(v -> {
            // Se clicar no overlay (fora dos controles), esconder controles
            hideOverlayControls();
        });
        
        // Prevenir que o clique nos controles propague para o overlay
        View[] controlViews = {btnMenu, btnOrientation, btnFoldChecks, btnToggleSidebar, 
                              btnTogglePortrait, btnCloseMenu, btnLoadAll, btnReloadAll, btnClearAll};
        for (View view : controlViews) {
            if (view != null) {
                view.setOnClickListener(v -> {
                    // Não fazer nada, apenas prevenir propagação
                });
        view.setOnTouchListener((v, event) -> {
            v.performClick();
            return true; // Consumir o evento
        });
            }
        }
    }
    
    private void handleBoxControlClick(int boxIndex, String action) {
        WebView webView = webViews[boxIndex];
        
        switch (action) {
            case "zoomIn":
                webView.zoomIn();
                break;
            case "zoomOut":
                webView.zoomOut();
                break;
            case "refresh":
                webView.reload();
                break;
            case "back":
                if (webView.canGoBack()) {
                    webView.goBack();
                }
                break;
            case "forward":
                if (webView.canGoForward()) {
                    webView.goForward();
                }
                break;
            case "fullscreen":
                // Simular clique no botão fullscreen do player (se houver)
                webView.loadUrl("javascript:" +
                    "var videos=document.getElementsByTagName('video');" +
                    "if(videos.length>0){" +
                    "if(videos[0].requestFullscreen){videos[0].requestFullscreen();}" +
                    "else if(videos[0].webkitRequestFullscreen){videos[0].webkitRequestFullscreen();}" +
                    "else if(videos[0].mozRequestFullScreen){videos[0].mozRequestFullScreen();}" +
                    "}");
                break;
        }
        
        // Resetar timer para esconder controles
        resetBoxControlPanelTimer(boxIndex);
    }
    
    private void toggleBoxControlPanel(int boxIndex) {
        if (!boxEnabled[boxIndex]) return;
        
        if (controlPanelVisible[boxIndex]) {
            // Esconder
            boxControlPanels[boxIndex].setVisibility(View.GONE);
            controlPanelVisible[boxIndex] = false;
            boxHandlers[boxIndex].removeCallbacksAndMessages(null);
        } else {
            // Esconder outros painéis primeiro
            for (int i = 0; i < 4; i++) {
                if (i != boxIndex && controlPanelVisible[i]) {
                    boxControlPanels[i].setVisibility(View.GONE);
                    controlPanelVisible[i] = false;
                    boxHandlers[i].removeCallbacksAndMessages(null);
                }
            }
            
            // Mostrar este painel
            boxControlPanels[boxIndex].setVisibility(View.VISIBLE);
            controlPanelVisible[boxIndex] = true;
            
            // Configurar timer para esconder após 10 segundos
            resetBoxControlPanelTimer(boxIndex);
        }
    }
    
    private void resetBoxControlPanelTimer(int boxIndex) {
        boxHandlers[boxIndex].removeCallbacksAndMessages(null);
        boxHandlers[boxIndex].postDelayed(() -> {
            if (controlPanelVisible[boxIndex]) {
                boxControlPanels[boxIndex].setVisibility(View.GONE);
                controlPanelVisible[boxIndex] = false;
            }
        }, 10000); // 10 segundos
    }
    
    private void toggleOverlayControls() {
        if (overlayControls.getVisibility() == View.VISIBLE) {
            hideOverlayControls();
        } else {
            overlayControls.setVisibility(View.VISIBLE);
        }
    }
    
    private void hideOverlayControls() {
        overlayControls.setVisibility(View.GONE);
        // Também esconder todos os painéis de controle das boxes
        for (int i = 0; i < 4; i++) {
            if (controlPanelVisible[i]) {
                boxControlPanels[i].setVisibility(View.GONE);
                controlPanelVisible[i] = false;
                boxHandlers[i].removeCallbacksAndMessages(null);
            }
        }
    }
    
    private void toggleSidebar() {
        if (isSidebarVisible) {
            sidebarMenu.setVisibility(View.GONE);
            isSidebarVisible = false;
        } else {
            sidebarMenu.setVisibility(View.VISIBLE);
            isSidebarVisible = true;
        }
    }
    
    private void toggleOrientation() {
        int newOrientation = (currentOrientation == Configuration.ORIENTATION_PORTRAIT) ?
            Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
        
        // Atualizar botão
        btnOrientation.setText(newOrientation == Configuration.ORIENTATION_PORTRAIT ? "📱" : "🔄");
        btnTogglePortrait.setText(newOrientation == Configuration.ORIENTATION_PORTRAIT ? "Portrait" : "Landscape");
        
        // Atualizar layout
        currentOrientation = newOrientation;
        updateLayout();
    }
    
    private void toggleBottomPanel() {
        LinearLayout checkboxContainer = findViewById(R.id.checkboxContainer);
        
        if (isBottomPanelFolded) {
            // Mostrar
            checkboxContainer.setVisibility(View.VISIBLE);
            btnFoldChecks.setText("▼");
            isBottomPanelFolded = false;
        } else {
            // Esconder
            checkboxContainer.setVisibility(View.GONE);
            btnFoldChecks.setText("▲");
            isBottomPanelFolded = true;
        }
    }
    
    private void updateLayout() {
        // Contar boxes ativos
        int activeBoxes = 0;
        for (boolean enabled : boxEnabled) {
            if (enabled) activeBoxes++;
        }
        
        // Determinar layout baseado na orientação e boxes ativos
        int rows, cols;
        
        if (activeBoxes == 0) {
            rows = 1; cols = 1;
        } else if (activeBoxes == 1) {
            rows = 1; cols = 1;
        } else if (activeBoxes == 2) {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                rows = 1; cols = 2; // 1x2
            } else {
                rows = 2; cols = 1; // 2x1
            }
        } else if (activeBoxes == 3) {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                rows = 1; cols = 3; // 1x3
            } else {
                rows = 3; cols = 1; // 3x1
            }
        } else { // 4 boxes
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                rows = 2; cols = 2; // 2x2
            } else {
                rows = 4; cols = 1; // 4x1
            }
        }
        
        // Configurar grid
        gridLayout.setRowCount(rows);
        gridLayout.setColumnCount(cols);
        gridLayout.removeAllViews();
        
        // Adicionar boxes ativos na ordem
        int added = 0;
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i]) {
                GridLayout.Spec rowSpec = GridLayout.spec(added / cols, 1f);
                GridLayout.Spec colSpec = GridLayout.spec(added % cols, 1f);
                
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
                params.width = 0;
                params.height = 0;
                params.setMargins(1, 1, 1, 1);
                
                boxContainers[i].setVisibility(View.VISIBLE);
                gridLayout.addView(boxContainers[i], params);
                added++;
                
                if (added >= rows * cols) break;
            } else {
                boxContainers[i].setVisibility(View.GONE);
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
                // Outras configurações podem ser aplicadas aqui
            }
        }
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentOrientation = newConfig.orientation;
        updateLayout();
    }
    
    @Override
    public void onBackPressed() {
        // Verificar se sidebar está visível
        if (isSidebarVisible) {
            toggleSidebar();
            return;
        }
        
        // Verificar se overlay está visível
        if (overlayControls.getVisibility() == View.VISIBLE) {
            hideOverlayControls();
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
    protected void onPause() {
        super.onPause();
        for (WebView webView : webViews) {
            if (webView != null) webView.onPause();
        }
        // Limpar handlers
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
        // Limpar handlers
        for (Handler handler : boxHandlers) {
            if (handler != null) handler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }
}
