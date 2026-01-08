package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Componentes principais
    private GridLayout gridLayout;
    private WebView[] webViews = new WebView[4];
    private FrameLayout[] boxContainers = new FrameLayout[4];
    private LinearLayout bottomControls;
    private ScrollView sidebarMenu;
    private View sidebarOverlay;
    
    // Controles
    private Button btnMenu, btnOrientation, btnBack;
    private Button btnCloseMenu, btnLoadAll, btnReloadAll, btnClearAll;
    private Button btnSaveState, btnLoadState, btnSaveFavorites, btnLoadFavorites;
    private CheckBox[] checkBoxes = new CheckBox[4];
    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups, cbBlockRedirects, cbBlockAds;
    private EditText[] urlInputs = new EditText[4];
    private TextView tvFocusedBox;
    
    // Estado
    private boolean[] boxEnabled = {true, true, true, true};
    private boolean isSidebarVisible = false;
    private int currentOrientation = Configuration.ORIENTATION_PORTRAIT;
    private int focusedBoxIndex = 0; // Box em foco para o botão BACK
    
    // Favoritos
    private ArrayList<String> favoritesList = new ArrayList<>();
    private SharedPreferences preferences;
    
    // Lista de domínios de anúncios para bloquear
    private final List<String> adDomains = Arrays.asList(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "ads.google.com",
        "adservice.google.com",
        "ads.facebook.com",
        "ad.doubleclick.net",
        "adserver.google.com",
        "pagead2.googlesyndication.com",
        "ads.youtube.com",
        "ad.youtube.com",
        "pubads.g.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "www.googletagservices.com",
        "googletagservices.com",
        "ads-twitter.com",
        "ads.twitter.com",
        "ads.reddit.com",
        "ad.reddit.com"
    );

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar SharedPreferences
        preferences = getSharedPreferences("MultiStreamViewer", MODE_PRIVATE);
        
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
        
        // Carregar estado salvo AUTOMATICAMENTE
        loadSavedState(true);
        
        // Carregar lista de favoritos
        loadFavoritesList();
        
        // Configurar layout inicial
        updateLayout();
        updateFocusedBoxIndicator();
        
        // Configurar overlay para fechar sidebar
        setupSidebarOverlay();
        
        // Carregar URLs iniciais apenas se não tiver estado salvo
        if (!hasSavedState()) {
            new Handler().postDelayed(this::loadInitialURLs, 1000);
        }
    }
    
    private void initViews() {
        gridLayout = findViewById(R.id.gridLayout);
        bottomControls = findViewById(R.id.bottomControls);
        sidebarMenu = findViewById(R.id.sidebarMenu);
        sidebarOverlay = findViewById(R.id.sidebarOverlay);
        tvFocusedBox = findViewById(R.id.tvFocusedBox);
        
        // Botões na barra inferior
        btnMenu = findViewById(R.id.btnMenu);
        btnOrientation = findViewById(R.id.btnOrientation);
        btnBack = findViewById(R.id.btnBack);
        
        // Botões do menu lateral
        btnCloseMenu = findViewById(R.id.btnCloseMenu);
        btnLoadAll = findViewById(R.id.btnLoadAll);
        btnReloadAll = findViewById(R.id.btnReloadAll);
        btnClearAll = findViewById(R.id.btnClearAll);
        
        // Botões de estado e favoritos
        btnSaveState = findViewById(R.id.btnSaveState);
        btnLoadState = findViewById(R.id.btnLoadState);
        btnSaveFavorites = findViewById(R.id.btnSaveFavorites);
        btnLoadFavorites = findViewById(R.id.btnLoadFavorites);
        
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
        cbBlockAds = findViewById(R.id.cbBlockAds);
        
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
    
    private void setupSidebarOverlay() {
        sidebarOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSidebar();
            }
        });
        
        // Impedir que toques passem através do overlay
        sidebarOverlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true; // Consumir o evento
            }
        });
    }
    
    private void closeSidebar() {
        sidebarMenu.setVisibility(View.GONE);
        sidebarOverlay.setVisibility(View.GONE);
        isSidebarVisible = false;
    }
    
    private void openSidebar() {
        sidebarMenu.setVisibility(View.VISIBLE);
        sidebarOverlay.setVisibility(View.VISIBLE);
        isSidebarVisible = true;
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
            
            // Configurar clique duplo para fullscreen e clique simples para foco
            boxContainers[i].setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = 0;
                
                @Override
                public void onClick(View v) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < 300) {
                        // Clique duplo detectado - ativar fullscreen
                        activateFullscreen(boxIndex);
                    } else {
                        // Clique simples - mudar foco
                        focusedBoxIndex = boxIndex;
                        updateFocusedBoxIndicator();
                        Toast.makeText(MainActivity.this, 
                            "Box " + (boxIndex + 1) + " em foco", Toast.LENGTH_SHORT).show();
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
    
    private void updateFocusedBoxIndicator() {
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            tvFocusedBox.setVisibility(View.VISIBLE);
            tvFocusedBox.setText("Foco: " + (focusedBoxIndex + 1));
        } else {
            tvFocusedBox.setVisibility(View.GONE);
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
        
        // Bloquear anúncios
        settings.setBlockNetworkLoads(cbBlockAds.isChecked());
        settings.setBlockNetworkImage(cbBlockAds.isChecked());
        
        // User agent
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        
        // Cor de fundo
        webView.setBackgroundColor(Color.BLACK);
        
        // WebViewClient com bloqueio de redirecionamentos e anúncios
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrlLoading(view, request.getUrl().toString());
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrlLoading(view, url);
            }
            
            private boolean handleUrlLoading(WebView view, String url) {
                Log.d("URL_LOADING", "Tentando carregar: " + url);
                
                // 1. Bloqueio de redirecionamentos
                if (cbBlockRedirects.isChecked()) {
                    String currentUrl = view.getUrl();
                    if (currentUrl != null && !isSameDomain(currentUrl, url)) {
                        Toast.makeText(MainActivity.this, 
                            "Redirecionamento bloqueado: " + getDomain(url), 
                            Toast.LENGTH_SHORT).show();
                        return true; // Bloquear
                    }
                }
                
                // 2. Bloqueio de anúncios
                if (cbBlockAds.isChecked() && isAdUrl(url)) {
                    Toast.makeText(MainActivity.this, 
                        "Anúncio bloqueado: " + getDomain(url), 
                        Toast.LENGTH_SHORT).show();
                    return true; // Bloquear
                }
                
                return false; // Permitir carregamento
            }
            
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d("PAGE_STARTED", "Carregando: " + url);
                
                // Injetar script para remover anúncios
                if (cbBlockAds.isChecked()) {
                    injectAdBlocker(view);
                }
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
                
                // Injetar script para remover anúncios após carregamento
                if (cbBlockAds.isChecked()) {
                    injectAdBlocker(view);
                }
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
                
                // Ocultar controles inferiores
                bottomControls.setVisibility(View.GONE);
                
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
                bottomControls.setVisibility(View.VISIBLE);
                
                // Sair do fullscreen
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        });
    }
    
    private void injectAdBlocker(WebView view) {
        String adBlockJS = 
            "// Remover elementos de anúncios comuns" +
            "var selectors = [" +
            "   'div[class*=\"ad\"]'," +
            "   'div[id*=\"ad\"]'," +
            "   'iframe[src*=\"ad\"]'," +
            "   'ins.adsbygoogle'," +
            "   'div.ad-container'," +
            "   'div.advertisement'," +
            "   'div.ad-banner'," +
            "   'div.ad-wrapper'," +
            "   'div[data-ad-status]'," +
            "   'div.google-ad'," +
            "   'div#ad-wrap'," +
            "   'div#ad-container'" +
            "];" +
            "" +
            "selectors.forEach(function(selector) {" +
            "   var elements = document.querySelectorAll(selector);" +
            "   elements.forEach(function(el) {" +
            "       el.style.display = 'none';" +
            "       el.parentNode.removeChild(el);" +
            "   });" +
            "});" +
            "" +
            "// Remover scripts de anúncios" +
            "var scripts = document.getElementsByTagName('script');" +
            "for(var i = scripts.length - 1; i >= 0; i--) {" +
            "   var src = scripts[i].src || '';" +
            "   if(src.includes('doubleclick') || src.includes('googlesyndication') || src.includes('ads.google')) {" +
            "       scripts[i].parentNode.removeChild(scripts[i]);" +
            "   }" +
            "}";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(adBlockJS, null);
        } else {
            view.loadUrl("javascript:" + adBlockJS);
        }
    }
    
    private boolean isAdUrl(String url) {
        for (String domain : adDomains) {
            if (url.toLowerCase().contains(domain.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    private String getDomain(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String domain = uri.getHost();
            return domain != null ? domain.replace("www.", "") : url;
        } catch (Exception e) {
            return url;
        }
    }
    
    private boolean isSameDomain(String url1, String url2) {
        try {
            String domain1 = getDomain(url1);
            String domain2 = getDomain(url2);
            return domain1.equals(domain2);
        } catch (Exception e) {
            return false;
        }
    }
    
    private void initEventListeners() {
        // Botão BACK para retroceder na box em foco
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webViews[focusedBoxIndex].canGoBack()) {
                    webViews[focusedBoxIndex].goBack();
                    Toast.makeText(MainActivity.this, 
                        "Retrocedendo na Box " + (focusedBoxIndex + 1), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, 
                        "Não há páginas para retroceder na Box " + (focusedBoxIndex + 1), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Botão menu
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSidebarVisible) {
                    closeSidebar();
                } else {
                    openSidebar();
                }
            }
        });
        
        // Botão orientação - alternar entre portrait e landscape
        btnOrientation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    btnOrientation.setText("↻");
                } else {
                    setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    btnOrientation.setText("📱");
                }
            }
        });
        
        // Botão fechar menu lateral
        btnCloseMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSidebar();
            }
        });
        
        // Botões de ação no menu lateral
        btnLoadAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadAllURLs();
            }
        });
        
        btnReloadAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reloadAllWebViews();
            }
        });
        
        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAllWebViews();
            }
        });
        
        // Botões de estado e favoritos
        btnSaveState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentState();
            }
        });
        
        btnLoadState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSavedState(false);
            }
        });
        
        btnSaveFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveFavoriteDialog();
            }
        });
        
        btnLoadFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadFavoritesDialog();
            }
        });
        
        // Checkboxes de boxes
        for (int i = 0; i < 4; i++) {
            final int index = i;
            checkBoxes[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    boxEnabled[index] = isChecked;
                    updateLayout();
                }
            });
        }
        
        // Configurações de segurança
        cbAllowScripts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (WebView webView : webViews) {
                    if (webView != null) {
                        webView.getSettings().setJavaScriptEnabled(isChecked);
                    }
                }
                Toast.makeText(MainActivity.this, "JavaScript: " + (isChecked ? "Ativado" : "Desativado"), 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        cbBlockAds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (WebView webView : webViews) {
                    if (webView != null) {
                        WebSettings settings = webView.getSettings();
                        settings.setBlockNetworkLoads(isChecked);
                        settings.setBlockNetworkImage(isChecked);
                    }
                }
                Toast.makeText(MainActivity.this, "Bloqueio de anúncios: " + (isChecked ? "Ativado" : "Desativado"), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // ==================== ESTADO ====================
    
    private boolean hasSavedState() {
        return preferences.contains("url_0") || preferences.contains("box_enabled_0");
    }
    
    private void saveCurrentState() {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            
            // Salvar URLs
            for (int i = 0; i < 4; i++) {
                String currentUrl = urlInputs[i].getText().toString().trim();
                if (currentUrl.isEmpty()) {
                    currentUrl = getDefaultUrl(i);
                }
                editor.putString("url_" + i, currentUrl);
            }
            
            // Salvar estado das checkboxes
            for (int i = 0; i < 4; i++) {
                editor.putBoolean("box_enabled_" + i, boxEnabled[i]);
            }
            
            // Salvar orientação
            editor.putInt("orientation", currentOrientation);
            
            // Salvar configurações
            editor.putBoolean("allow_scripts", cbAllowScripts.isChecked());
            editor.putBoolean("allow_forms", cbAllowForms.isChecked());
            editor.putBoolean("allow_popups", cbAllowPopups.isChecked());
            editor.putBoolean("block_redirects", cbBlockRedirects.isChecked());
            editor.putBoolean("block_ads", cbBlockAds.isChecked());
            
            editor.apply();
            
            Toast.makeText(this, "✅ Estado guardado com sucesso!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "❌ Erro ao guardar estado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadSavedState(boolean silent) {
        try {
            // Carregar URLs
            boolean hasSavedUrls = false;
            for (int i = 0; i < 4; i++) {
                String savedUrl = preferences.getString("url_" + i, "");
                if (!savedUrl.isEmpty()) {
                    hasSavedUrls = true;
                    urlInputs[i].setText(savedUrl);
                    // Carregar a URL no WebView
                    if (boxEnabled[i]) {
                        loadURL(i, savedUrl);
                    }
                }
            }
            
            // Se não tiver URLs salvas, usar URLs padrão
            if (!hasSavedUrls) {
                for (int i = 0; i < 4; i++) {
                    urlInputs[i].setText(getDefaultUrl(i));
                }
            }
            
            // Carregar estado das checkboxes
            for (int i = 0; i < 4; i++) {
                boolean savedState = preferences.getBoolean("box_enabled_" + i, true);
                boxEnabled[i] = savedState;
                checkBoxes[i].setChecked(savedState);
            }
            
            // Carregar orientação
            int savedOrientation = preferences.getInt("orientation", Configuration.ORIENTATION_PORTRAIT);
            currentOrientation = savedOrientation;
            btnOrientation.setText(currentOrientation == Configuration.ORIENTATION_PORTRAIT ? "📱" : "↻");
            
            // Carregar configurações
            cbAllowScripts.setChecked(preferences.getBoolean("allow_scripts", true));
            cbAllowForms.setChecked(preferences.getBoolean("allow_forms", true));
            cbAllowPopups.setChecked(preferences.getBoolean("allow_popups", true));
            cbBlockRedirects.setChecked(preferences.getBoolean("block_redirects", false));
            cbBlockAds.setChecked(preferences.getBoolean("block_ads", false));
            
            if (!silent) {
                Toast.makeText(this, "✅ Estado carregado com sucesso!", Toast.LENGTH_SHORT).show();
            }
            
            updateLayout();
            updateFocusedBoxIndicator();
            
        } catch (Exception e) {
            if (!silent) {
                Toast.makeText(this, "❌ Erro ao carregar estado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private String getDefaultUrl(int boxIndex) {
        switch (boxIndex) {
            case 0: return "https://www.youtube.com";
            case 1: return "https://www.twitch.tv";
            case 2: return "https://vimeo.com";
            case 3: return "https://www.dailymotion.com";
            default: return "https://www.google.com";
        }
    }
    
    // ==================== FAVORITOS ====================
    
    private void loadFavoritesList() {
        try {
            String favoritesJson = preferences.getString("favorites_list", "[]");
            JSONArray jsonArray = new JSONArray(favoritesJson);
            
            favoritesList.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                String name = jsonArray.getString(i);
                favoritesList.add(name);
            }
            
        } catch (Exception e) {
            favoritesList.clear();
        }
    }
    
    private void saveFavoritesList() {
        try {
            JSONArray jsonArray = new JSONArray(favoritesList);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("favorites_list", jsonArray.toString());
            editor.apply();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void saveFavorite(String favoriteName) {
        try {
            // Verificar se já existe
            if (favoritesList.contains(favoriteName)) {
                Toast.makeText(this, "❌ Já existe um favorito com este nome!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Criar JSON com as URLs atuais
            JSONObject favoriteData = new JSONObject();
            favoriteData.put("name", favoriteName);
            
            JSONArray urlsArray = new JSONArray();
            for (int i = 0; i < 4; i++) {
                String url = urlInputs[i].getText().toString().trim();
                if (url.isEmpty()) {
                    url = getDefaultUrl(i);
                }
                urlsArray.put(url);
            }
            favoriteData.put("urls", urlsArray);
            
            // Adicionar à lista e salvar
            favoritesList.add(favoriteName);
            saveFavoritesList();
            
            // Salvar os dados do favorito
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("favorite_" + favoriteName, favoriteData.toString());
            editor.apply();
            
            Toast.makeText(this, "✅ Favorito '" + favoriteName + "' guardado!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "❌ Erro ao guardar favorito: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadFavorite(String favoriteName, int targetBox) {
        try {
            String favoriteJson = preferences.getString("favorite_" + favoriteName, "");
            if (favoriteJson.isEmpty()) {
                Toast.makeText(this, "❌ Favorito não encontrado!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            JSONObject favoriteData = new JSONObject(favoriteJson);
            JSONArray urlsArray = favoriteData.getJSONArray("urls");
            
            if (targetBox == -1) {
                // Carregar em todas as boxes
                for (int i = 0; i < 4 && i < urlsArray.length(); i++) {
                    String url = urlsArray.getString(i);
                    urlInputs[i].setText(url);
                    if (boxEnabled[i]) {
                        loadURL(i, url);
                    }
                }
                Toast.makeText(this, "✅ Favorito carregado em todas as boxes!", Toast.LENGTH_SHORT).show();
            } else {
                // Carregar apenas na box específica
                if (targetBox < urlsArray.length()) {
                    String url = urlsArray.getString(targetBox);
                    urlInputs[targetBox].setText(url);
                    if (boxEnabled[targetBox]) {
                        loadURL(targetBox, url);
                    }
                    Toast.makeText(this, 
                        "✅ Favorito carregado na Box " + (targetBox + 1), Toast.LENGTH_SHORT).show();
                }
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "❌ Erro ao carregar favorito!", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteFavorite(String favoriteName) {
        try {
            // Remover da lista
            favoritesList.remove(favoriteName);
            saveFavoritesList();
            
            // Remover dados
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("favorite_" + favoriteName);
            editor.apply();
            
            Toast.makeText(this, "✅ Favorito '" + favoriteName + "' removido!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "❌ Erro ao remover favorito: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // ==================== DIALOGS ====================
    
    private void showSaveFavoriteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guardar Favorito");
        
        // Configurar a view do dialog
        final EditText input = new EditText(this);
        input.setHint("Nome do favorito");
        builder.setView(input);
        
        builder.setPositiveButton("GUARDAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String favoriteName = input.getText().toString().trim();
                if (!favoriteName.isEmpty()) {
                    saveFavorite(favoriteName);
                } else {
                    Toast.makeText(MainActivity.this, "❌ Digite um nome para o favorito!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }
    
    private void showLoadFavoritesDialog() {
        if (favoritesList.isEmpty()) {
            Toast.makeText(this, "📭 Não há favoritos guardados!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Carregar Favorito");
        
        // Converter lista para array
        final String[] favoriteNames = favoritesList.toArray(new String[0]);
        
        builder.setItems(favoriteNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedFavorite = favoriteNames[which];
                showFavoriteOptionsDialog(selectedFavorite);
            }
        });
        
        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }
    
    private void showFavoriteOptionsDialog(final String favoriteName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Favorito: " + favoriteName);
        
        String[] options = {"Carregar em Todas", "Carregar em Box 1", "Carregar em Box 2", 
                           "Carregar em Box 3", "Carregar em Box 4", "Eliminar", "Cancelar"};
        
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Carregar em Todas
                        loadFavorite(favoriteName, -1);
                        break;
                    case 1: // Carregar em Box 1
                        loadFavorite(favoriteName, 0);
                        break;
                    case 2: // Carregar em Box 2
                        loadFavorite(favoriteName, 1);
                        break;
                    case 3: // Carregar em Box 3
                        loadFavorite(favoriteName, 2);
                        break;
                    case 4: // Carregar em Box 4
                        loadFavorite(favoriteName, 3);
                        break;
                    case 5: // Eliminar
                        showDeleteConfirmDialog(favoriteName);
                        break;
                    case 6: // Cancelar
                        dialog.dismiss();
                        break;
                }
            }
        });
        
        builder.show();
    }
    
    private void showDeleteConfirmDialog(final String favoriteName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Eliminação");
        builder.setMessage("Tem certeza que deseja eliminar o favorito '" + favoriteName + "'?");
        
        builder.setPositiveButton("ELIMINAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteFavorite(favoriteName);
            }
        });
        
        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        
        builder.show();
    }
    
    // ==================== OUTROS MÉTODOS ====================
    
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
                if (url.isEmpty()) {
                    url = getDefaultUrl(i);
                    urlInputs[i].setText(url);
                }
                loadURL(i, url);
            }
        }
        Toast.makeText(this, "Carregando todas as URLs", Toast.LENGTH_SHORT).show();
    }
    
    private void loadInitialURLs() {
        for (int i = 0; i < 4; i++) {
            String url = urlInputs[i].getText().toString().trim();
            if (url.isEmpty()) {
                url = getDefaultUrl(i);
                urlInputs[i].setText(url);
            }
            loadURL(i, url);
        }
    }
    
    private void loadURL(int boxIndex, String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            webViews[boxIndex].loadUrl(url);
            Toast.makeText(this, "Carregando Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "❌ Erro ao carregar Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
        }
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
        
        // Atualizar ícone do botão de orientação
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            btnOrientation.setText("↻");
            // Centralizar conteúdo da barra inferior em landscape
            bottomControls.setGravity(android.view.Gravity.CENTER);
        } else {
            btnOrientation.setText("📱");
            // Alinhar verticalmente em portrait
            bottomControls.setGravity(android.view.Gravity.CENTER_VERTICAL);
        }
        
        updateLayout();
        updateFocusedBoxIndicator();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Guardar estado automaticamente ao sair
        saveCurrentState();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recarregar favoritos
        loadFavoritesList();
    }
    
    @Override
    public void onBackPressed() {
        // 1. Fechar sidebar se aberto
        if (isSidebarVisible) {
            closeSidebar();
            return;
        }
        
        // 2. Tentar retroceder na box em foco
        if (webViews[focusedBoxIndex].canGoBack()) {
            webViews[focusedBoxIndex].goBack();
            return;
        }
        
        // 3. Tentar retroceder em qualquer WebView que possa
        for (WebView webView : webViews) {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
                return;
            }
        }
        
        // 4. Confirmar saída do app
        new AlertDialog.Builder(this)
            .setTitle("Sair do App")
            .setMessage("Deseja realmente sair?")
            .setPositiveButton("SIM", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .setNegativeButton("NÃO", null)
            .show();
    }
}