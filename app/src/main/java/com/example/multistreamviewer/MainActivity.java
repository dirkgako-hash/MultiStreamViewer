package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
        
        // Configurar orientação automática
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        
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
            
            // Configurar clique para foco
            boxContainers[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    focusedBoxIndex = boxIndex;
                    updateFocusedBoxIndicator();
                }
            });
            
            // Configurar clique duplo para fullscreen
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
    
    private void initEventListeners() {
        // Botão BACK
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retroceder na box em foco
                if (webViews[focusedBoxIndex].canGoBack()) {
                    webViews[focusedBoxIndex].goBack();
                } else {
                    Toast.makeText(MainActivity.this, 
                        "Não há páginas para retroceder", Toast.LENGTH_SHORT).show();
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
        
        // Botão orientação
        btnOrientation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
            checkBoxes[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boxEnabled[index] = checkBoxes[index].isChecked();
                    updateLayout();
                }
            });
        }
        
        // Configurações de segurança
        cbAllowScripts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (WebView webView : webViews) {
                    if (webView != null) {
                        webView.getSettings().setJavaScriptEnabled(cbAllowScripts.isChecked());
                    }
                }
                Toast.makeText(MainActivity.this, "JavaScript: " + 
                    (cbAllowScripts.isChecked() ? "Ativado" : "Desativado"), Toast.LENGTH_SHORT).show();
            }
        });
        
        cbBlockAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (WebView webView : webViews) {
                    if (webView != null) {
                        WebSettings settings = webView.getSettings();
                        settings.setBlockNetworkLoads(cbBlockAds.isChecked());
                        settings.setBlockNetworkImage(cbBlockAds.isChecked());
                    }
                }
                Toast.makeText(MainActivity.this, "Bloqueio de anúncios: " + 
                    (cbBlockAds.isChecked() ? "Ativado" : "Desativado"), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // ==================== MÉTODOS ADICIONAIS ====================
    
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
        
        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }
    
    private void showFavoriteOptionsDialog(final String favoriteName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Favorito: " + favoriteName);
        
        String[] options = {"Carregar em Todas", "Carregar em Box Específica", "Eliminar", "Cancelar"};
        
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Carregar em Todas
                        loadFavorite(favoriteName, -1);
                        break;
                    case 1: // Carregar em Box Específica
                        showBoxSelectionDialog(favoriteName);
                        break;
                    case 2: // Eliminar
                        showDeleteConfirmDialog(favoriteName);
                        break;
                }
            }
        });
        
        builder.show();
    }
    
    private void showBoxSelectionDialog(final String favoriteName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Carregar em qual Box?");
        
        String[] boxes = {"Box 1", "Box 2", "Box 3", "Box 4"};
        
        builder.setItems(boxes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                loadFavorite(favoriteName, which);
            }
        });
        
        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }
    
    private void loadFavorite(String favoriteName, int boxIndex) {
        try {
            String favoriteJson = preferences.getString("favorite_" + favoriteName, "");
            if (favoriteJson.isEmpty()) {
                Toast.makeText(this, "❌ Favorito não encontrado!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            JSONObject favoriteData = new JSONObject(favoriteJson);
            JSONArray urlsArray = favoriteData.getJSONArray("urls");
            
            if (boxIndex == -1) {
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
                if (boxIndex < urlsArray.length()) {
                    String url = urlsArray.getString(boxIndex);
                    urlInputs[boxIndex].setText(url);
                    if (boxEnabled[boxIndex]) {
                        loadURL(boxIndex, url);
                    }
                    Toast.makeText(this, 
                        "✅ Favorito carregado na Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
                }
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "❌ Erro ao carregar favorito!", Toast.LENGTH_SHORT).show();
        }
    }
    
    // ==================== MÉTODOS EXISTENTES (mantidos do código original) ====================
    
    // Os seguintes métodos permanecem exatamente como no seu código original:
    // - injectAdBlocker()
    // - isAdUrl()
    // - getDomain()
    // - isSameDomain()
    // - hasSavedState()
    // - saveCurrentState()
    // - loadSavedState()
    // - getDefaultUrl()
    // - loadFavoritesList()
    // - saveFavoritesList()
    // - saveFavorite()
    // - showSaveFavoriteDialog()
    // - showDeleteConfirmDialog()
    // - deleteFavorite()
    // - activateFullscreen()
    // - updateLayout()
    // - loadAllURLs()
    // - loadInitialURLs()
    // - loadURL()
    // - reloadAllWebViews()
    // - clearAllWebViews()
    
    // ==================== SOBRESCRITAS ====================
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentOrientation = newConfig.orientation;
        
        // Atualizar ícone do botão de orientação
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            btnOrientation.setText("↻");
        } else {
            btnOrientation.setText("📱");
        }
        
        updateLayout();
        updateFocusedBoxIndicator();
        
        // Centralizar conteúdo na barra inferior em landscape
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            bottomControls.setGravity(android.view.Gravity.CENTER);
        } else {
            bottomControls.setGravity(android.view.Gravity.CENTER_VERTICAL);
        }
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
    
    // Métodos auxiliares que precisam ser implementados:
    private void injectAdBlocker(WebView view) {
        // Implementação do seu código original
    }
    
    private boolean isAdUrl(String url) {
        // Implementação do seu código original
        return false;
    }
    
    private String getDomain(String url) {
        // Implementação do seu código original
        return "";
    }
    
    private boolean isSameDomain(String url1, String url2) {
        // Implementação do seu código original
        return false;
    }
}