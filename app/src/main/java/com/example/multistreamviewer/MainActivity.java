package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Componentes
    private GridLayout gridLayout;
    private WebView[] webViews = new WebView[4];
    private FrameLayout[] playerContainers = new FrameLayout[4];
    private LinearLayout[] loadingOverlays = new LinearLayout[4];
    private EditText[] urlInputs = new EditText[4];
    private CheckBox[] checkBoxes = new CheckBox[4];
    
    // Menu
    private LinearLayout sideMenu;
    private Button btnToggleMenu, btnToggleOrientation, btnCloseMenu;
    private Spinner layoutSpinner;
    private Button btnLoadAll, btnReloadAll, btnClearAll;
    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups;
    
    // Controles de zoom
    private Button[] btnZoomIn = new Button[4];
    private Button[] btnZoomOut = new Button[4];
    private Button[] btnFullscreen = new Button[4];
    
    // Estado
    private boolean isMenuVisible = false;
    private int currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
    private int activePlayersCount = 4;
    private boolean[] playerFullscreen = new boolean[4];
    private View fullscreenView = null;
    private int fullscreenPlayerIndex = -1;
    
    // Configurações WebView
    private boolean allowScripts = true;
    private boolean allowForms = true;
    private boolean allowPopups = true;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Forçar sensor landscape inicialmente
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        
        // Inicializar arrays de IDs
        int[] webViewIds = {R.id.webView1, R.id.webView2, R.id.webView3, R.id.webView4};
        int[] containerIds = {R.id.playerContainer1, R.id.playerContainer2, R.id.playerContainer3, R.id.playerContainer4};
        int[] overlayIds = {R.id.loadingOverlay1, R.id.loadingOverlay2, R.id.loadingOverlay3, R.id.loadingOverlay4};
        int[] inputIds = {R.id.urlInput1, R.id.urlInput2, R.id.urlInput3, R.id.urlInput4};
        int[] checkboxIds = {R.id.checkBox1, R.id.checkBox2, R.id.checkBox3, R.id.checkBox4};
        int[] zoomInIds = {R.id.btnZoomIn1, R.id.btnZoomIn2, R.id.btnZoomIn3, R.id.btnZoomIn4};
        int[] zoomOutIds = {R.id.btnZoomOut1, R.id.btnZoomOut2, R.id.btnZoomOut3, R.id.btnZoomOut4};
        int[] fullscreenIds = {R.id.btnFullscreen1, R.id.btnFullscreen2, R.id.btnFullscreen3, R.id.btnFullscreen4};
        
        // Obter referências
        gridLayout = findViewById(R.id.gridLayout);
        sideMenu = findViewById(R.id.sideMenu);
        btnToggleMenu = findViewById(R.id.btnToggleMenu);
        btnToggleOrientation = findViewById(R.id.btnToggleOrientation);
        btnCloseMenu = findViewById(R.id.btnCloseMenu);
        layoutSpinner = findViewById(R.id.layoutSpinner);
        btnLoadAll = findViewById(R.id.btnLoadAll);
        btnReloadAll = findViewById(R.id.btnReloadAll);
        btnClearAll = findViewById(R.id.btnClearAll);
        cbAllowScripts = findViewById(R.id.cbAllowScripts);
        cbAllowForms = findViewById(R.id.cbAllowForms);
        cbAllowPopups = findViewById(R.id.cbAllowPopups);
        
        // Configurar checkboxes de permissões
        cbAllowScripts.setChecked(allowScripts);
        cbAllowForms.setChecked(allowForms);
        cbAllowPopups.setChecked(allowPopups);
        
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
        
        // Inicializar WebViews
        for (int i = 0; i < 4; i++) {
            final int playerIndex = i;
            
            // WebViews
            webViews[i] = findViewById(webViewIds[i]);
            setupWebView(webViews[i], playerIndex);
            
            // Containers
            playerContainers[i] = findViewById(containerIds[i]);
            
            // Overlays
            loadingOverlays[i] = findViewById(overlayIds[i]);
            
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
            
            // Botões de zoom
            btnZoomIn[i] = findViewById(zoomInIds[i]);
            btnZoomOut[i] = findViewById(zoomOutIds[i]);
            btnFullscreen[i] = findViewById(fullscreenIds[i]);
            
            // Configurar listeners de zoom
            btnZoomIn[i].setOnClickListener(v -> zoomWebView(playerIndex, 1.2f));
            btnZoomOut[i].setOnClickListener(v -> zoomWebView(playerIndex, 0.8f));
            
            // Configurar fullscreen
            btnFullscreen[i].setOnClickListener(v -> togglePlayerFullscreen(playerIndex));
            
            // Tocar para mostrar controles
            playerContainers[i].setOnClickListener(v -> {
                showPlayerControls(playerIndex);
            });
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
        
        btnLoadAll.setOnClickListener(v -> loadAllURLs());
        btnReloadAll.setOnClickListener(v -> reloadAllWebViews());
        btnClearAll.setOnClickListener(v -> clearAllWebViews());
        
        // Configurar orientação inicial
        currentOrientation = getResources().getConfiguration().orientation;
        updateOrientationUI();
        
        // Aplicar layout inicial
        updateActivePlayers();
        applyAutoLayout();
        
        // Esconder menu inicialmente
        sideMenu.setVisibility(View.GONE);
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
        
        // Configurar WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                loadingOverlays[playerIndex].setVisibility(View.VISIBLE);
                urlInputs[playerIndex].setText(url);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                loadingOverlays[playerIndex].setVisibility(View.GONE);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                loadingOverlays[playerIndex].setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, 
                    "Player " + (playerIndex + 1) + " error", 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        // Configurar WebChromeClient para vídeos fullscreen
        webView.setWebChromeClient(new WebChromeClient() {
            private View customView;
            private CustomViewCallback customViewCallback;
            
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                
                customView = view;
                customViewCallback = callback;
                
                // Adicionar a view personalizada ao container
                playerContainers[playerIndex].addView(customView, 
                    new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                
                // Esconder WebView
                webViews[playerIndex].setVisibility(View.GONE);
                
                // Ativar fullscreen
                playerFullscreen[playerIndex] = true;
                fullscreenView = view;
                fullscreenPlayerIndex = playerIndex;
                
                // Esconder controles
                hideAllControls();
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
    
    private void applyWebViewSettings() {
        for (WebView webView : webViews) {
            WebSettings webSettings = webView.getSettings();
            
            // Configurar sandbox baseado nas opções
            String sandbox = "allow-same-origin";
            if (allowScripts) sandbox += " allow-scripts";
            if (allowForms) sandbox += " allow-forms";
            if (allowPopups) sandbox += " allow-popups";
            
            try {
                webView.setWebChromeClient(null);
                webView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onShowCustomView(View view, CustomViewCallback callback) {
                        super.onShowCustomView(view, callback);
                    }
                    
                    @Override
                    public void onHideCustomView() {
                        super.onHideCustomView();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
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
    
    private void clearAllWebViews() {
        for (int i = 0; i < 4; i++) {
            if (webViews[i] != null) {
                webViews[i].loadUrl("about:blank");
                urlInputs[i].setText("");
            }
        }
        Toast.makeText(this, "Cleared all", Toast.LENGTH_SHORT).show();
    }
    
    private void zoomWebView(int playerIndex, float factor) {
        WebView webView = webViews[playerIndex];
        WebSettings settings = webView.getSettings();
        
        if (factor > 1.0f) {
            // Zoom in
            settings.setTextZoom((int)(settings.getTextZoom() * factor));
        } else {
            // Zoom out
            settings.setTextZoom((int)(settings.getTextZoom() * factor));
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
            // Entrar no fullscreen
            // Para vídeos, o próprio WebChromeClient gerencia via onShowCustomView
            // Para outras páginas, tentamos forçar fullscreen
            playerContainers[playerIndex].setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            playerFullscreen[playerIndex] = true;
            fullscreenPlayerIndex = playerIndex;
        }
    }
    
    private void showPlayerControls(int playerIndex) {
        // Mostrar controles temporariamente
        // Em uma implementação real, você teria controles visíveis
        // Aqui apenas mostramos um toast como exemplo
        Toast.makeText(this, "Player " + (playerIndex + 1) + " tapped", Toast.LENGTH_SHORT).show();
    }
    
    private void hideAllControls() {
        // Esconder todos os controles quando em fullscreen
        // Implementação básica
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
            availableLayouts.add("1x4");
            availableLayouts.add("4x1");
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
        
        int rows = Integer.parseInt(parts[0]);
        int cols = Integer.parseInt(parts[1]);
        
        // Configurar grid
        gridLayout.setRowCount(rows);
        gridLayout.setColumnCount(cols);
        
        // Redistribuir players ativos
        int playerIndex = 0;
        for (int i = 0; i < 4; i++) {
            if (checkBoxes[i].isChecked()) {
                GridLayout.Spec rowSpec = GridLayout.spec(playerIndex / cols, 1f);
                GridLayout.Spec colSpec = GridLayout.spec(playerIndex % cols, 1f);
                
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
                params.width = 0;
                params.height = 0;
                params.setMargins(1, 1, 1, 1);
                
                playerContainers[i].setLayoutParams(params);
                playerIndex++;
                
                if (playerIndex >= rows * cols) break;
            }
        }
        
        // Forçar redesenho
        gridLayout.requestLayout();
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
    }
    
    @Override
    protected void onDestroy() {
        for (WebView webView : webViews) {
            if (webView != null) {
                webView.destroy();
            }
        }
        super.onDestroy();
    }
}
