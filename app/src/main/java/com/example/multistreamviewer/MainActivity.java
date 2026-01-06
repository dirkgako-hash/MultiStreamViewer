package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

public class MainActivity extends AppCompatActivity {

    // Componentes principais
    private GridLayout gridLayout;
    private WebView[] webViews = new WebView[4];
    private FrameLayout[] boxContainers = new FrameLayout[4];
    private LinearLayout topControls;
    private ScrollView sidebarMenu;
    
    // Controles
    private Button btnMenu, btnOrientation;
    private Button btnCloseMenu, btnLoadAll, btnReloadAll, btnClearAll;
    private CheckBox[] checkBoxes = new CheckBox[4];
    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups, cbBlockRedirects;
    private EditText[] urlInputs = new EditText[4];
    
    // Estado
    private boolean[] boxEnabled = {true, true, true, true};
    private boolean isSidebarVisible = false;
    private int currentOrientation = Configuration.ORIENTATION_PORTRAIT;

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
        topControls = findViewById(R.id.topControls);
        sidebarMenu = findViewById(R.id.sidebarMenu);
        
        // Botões superiores (COMPACTOS)
        btnMenu = findViewById(R.id.btnMenu);
        btnOrientation = findViewById(R.id.btnOrientation);
        
        // Botões do menu lateral
        btnCloseMenu = findViewById(R.id.btnCloseMenu);
        btnLoadAll = findViewById(R.id.btnLoadAll);
        btnReloadAll = findViewById(R.id.btnReloadAll);
        btnClearAll = findViewById(R.id.btnClearAll);
        
        // Checkboxes (COMPACTAS)
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
        
        // Configurar ENTER para carregar em cada EditText
        for (int i = 0; i < 4; i++) {
            final int boxIndex = i;
            urlInputs[i].setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE || 
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        // Carregar URL na box correspondente
                        String url = urlInputs[boxIndex].getText().toString().trim();
                        if (!url.isEmpty()) {
                            loadURL(boxIndex, url);
                        }
                        return true;
                    }
                    return false;
                }
            });
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
            
            // Configurar clique duplo para fullscreen
            boxContainers[i].setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = 0;
                
                @Override
                public void onClick(View v) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < 300) {
                        // Clique duplo detectado - ativar fullscreen
                        activateFullscreen(boxIndex);
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
        webView.setWebViewClient(new WebViewClient());
        
        // WebChromeClient para fullscreen
        webView.setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;
            
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                mCustomView = view;
                mCustomViewCallback = callback;
                
                // Ocultar controles superiores
                topControls.setVisibility(View.GONE);
                
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
                topControls.setVisibility(View.VISIBLE);
                
                // Sair do fullscreen
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        });
    }
    
    private void initEventListeners() {
        // Botão menu superior
        btnMenu.setOnClickListener(v -> {
            if (sidebarMenu.getVisibility() == View.VISIBLE) {
                sidebarMenu.setVisibility(View.GONE);
                isSidebarVisible = false;
            } else {
                sidebarMenu.setVisibility(View.VISIBLE);
                isSidebarVisible = true;
            }
        });
        
        // Botão orientação
        btnOrientation.setOnClickListener(v -> {
            changeOrientation();
        });
        
        // Botão fechar menu lateral
        btnCloseMenu.setOnClickListener(v -> {
            sidebarMenu.setVisibility(View.GONE);
            isSidebarVisible = false;
        });
        
        // Botões de ação no menu lateral
        btnLoadAll.setOnClickListener(v -> {
            loadAllURLs();
        });
        
        btnReloadAll.setOnClickListener(v -> {
            reloadAllWebViews();
        });
        
        btnClearAll.setOnClickListener(v -> {
            clearAllWebViews();
        });
        
        // Checkboxes de boxes (PERMANENTEMENTE VISÍVEIS)
        for (int i = 0; i < 4; i++) {
            final int index = i;
            checkBoxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                boxEnabled[index] = isChecked;
                updateLayout();
            });
        }
        
        // Configurações de segurança
        cbAllowScripts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (WebView webView : webViews) {
                if (webView != null) {
                    webView.getSettings().setJavaScriptEnabled(isChecked);
                }
            }
            Toast.makeText(this, "JavaScript: " + (isChecked ? "Ativado" : "Desativado"), 
                          Toast.LENGTH_SHORT).show();
        });
    }
    
    private void activateFullscreen(int boxIndex) {
        // Ativar fullscreen via JavaScript
        WebView webView = webViews[boxIndex];
        String fullscreenJS = 
            "var videos = document.getElementsByTagName('video');" +
            "if (videos.length > 0) {" +
            "   videos[0].requestFullscreen();" +
            "} else {" +
            "   document.documentElement.requestFullscreen();" +
            "}";
        
        webView.loadUrl("javascript:" + fullscreenJS);
    }
    
    private void changeOrientation() {
        // Alternar entre portrait e landscape
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            btnOrientation.setText("↻");
        } else {
            currentOrientation = Configuration.ORIENTATION_PORTRAIT;
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            btnOrientation.setText("📱");
        }
        updateLayout();
    }
    
    private void updateLayout() {
        int activeBoxes = 0;
        for (boolean enabled : boxEnabled) {
            if (enabled) activeBoxes++;
        }
        
        if (activeBoxes == 0) {
            // Nenhuma box ativa, mostrar todas
            for (int i = 0; i < 4; i++) {
                boxEnabled[i] = true;
                checkBoxes[i].setChecked(true);
            }
            activeBoxes = 4;
            Toast.makeText(this, "Todas as boxes ativadas", Toast.LENGTH_SHORT).show();
        }
        
        int rows, cols;
        
        // Layout automático baseado em boxes ativas e orientação
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
                params.setMargins(2, 2, 2, 2);
                
                boxContainers[i].setVisibility(View.VISIBLE);
                gridLayout.addView(boxContainers[i], params);
                position++;
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
        Toast.makeText(this, "Carregando todas as URLs", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Carregando Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
    }
    
    private void reloadAllWebViews() {
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i] && webViews[i] != null) {
                webViews[i].reload();
            }
        }
        Toast.makeText(this, "Recarregando todas", Toast.LENGTH_SHORT).show();
    }
    
    private void clearAllWebViews() {
        for (int i = 0; i < 4; i++) {
            if (webViews[i] != null) {
                webViews[i].loadUrl("about:blank");
            }
        }
        Toast.makeText(this, "Limpando todas", Toast.LENGTH_SHORT).show();
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
        
        // Tentar sair do fullscreen
        for (WebView webView : webViews) {
            if (webView != null && webView.getVisibility() == View.GONE) {
                webView.setVisibility(View.VISIBLE);
                topControls.setVisibility(View.VISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                return;
            }
        }
        
        super.onBackPressed();
    }
}