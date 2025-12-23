package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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

    // Componentes da UI
    private GridLayout gridLayout;
    private WebView[] webViews = new WebView[4];
    private FrameLayout[] playerContainers = new FrameLayout[4];
    private LinearLayout[] loadingOverlays = new LinearLayout[4];
    private LinearLayout[] playerControls = new LinearLayout[4];
    private EditText[] urlInputs = new EditText[4];
    private CheckBox[] checkBoxes = new CheckBox[4];
    private Button[] btnPrev = new Button[4];
    private Button[] btnNext = new Button[4];
    private Button btnLoadAll, btnReloadAll, btnClearAll, btnFullscreen;
    private Spinner layoutSpinner;

    // Estado da aplicação
    private Map<Integer, List<String>> playerHistory = new HashMap<>();
    private Map<Integer, Integer> currentHistoryIndex = new HashMap<>();
    private boolean isFullscreen = false;
    private String currentLayout = "2x2";
    private List<Integer> activePlayers = new ArrayList<>();

    // Configurações de layout
    private Map<String, int[]> layoutConfigs = new HashMap<String, int[]>() {{
        put("1x1", new int[]{1, 1});
        put("1x2", new int[]{1, 2});
        put("2x1", new int[]{2, 1});
        put("1x3", new int[]{1, 3});
        put("3x1", new int[]{3, 1});
        put("1x4", new int[]{1, 4});
        put("4x1", new int[]{4, 1});
        put("2x2", new int[]{2, 2});
    }};

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar arrays
        int[] webViewIds = {R.id.webView1, R.id.webView2, R.id.webView3, R.id.webView4};
        int[] containerIds = {R.id.playerContainer1, R.id.playerContainer2, R.id.playerContainer3, R.id.playerContainer4};
        int[] overlayIds = {R.id.loadingOverlay1, R.id.loadingOverlay2, R.id.loadingOverlay3, R.id.loadingOverlay4};
        int[] controlsIds = {R.id.playerControls1, R.id.playerControls2, R.id.playerControls3, R.id.playerControls4};
        int[] inputIds = {R.id.urlInput1, R.id.urlInput2, R.id.urlInput3, R.id.urlInput4};
        int[] checkBoxIds = {R.id.checkBox1, R.id.checkBox2, R.id.checkBox3, R.id.checkBox4};
        int[] prevBtnIds = {R.id.btnPrev1, R.id.btnPrev2, R.id.btnPrev3, R.id.btnPrev4};
        int[] nextBtnIds = {R.id.btnNext1, R.id.btnNext2, R.id.btnNext3, R.id.btnNext4};

        // Obter referências
        gridLayout = findViewById(R.id.gridLayout);
        btnLoadAll = findViewById(R.id.btnLoadAll);
        btnReloadAll = findViewById(R.id.btnReloadAll);
        btnClearAll = findViewById(R.id.btnClearAll);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        layoutSpinner = findViewById(R.id.layoutSpinner);

        // Inicializar componentes para cada player
        for (int i = 0; i < 4; i++) {
            final int playerIndex = i;
            
            // WebViews
            webViews[i] = findViewById(webViewIds[i]);
            setupWebView(webViews[i], playerIndex);
            
            // Containers
            playerContainers[i] = findViewById(containerIds[i]);
            
            // Overlays
            loadingOverlays[i] = findViewById(overlayIds[i]);
            
            // Controles
            playerControls[i] = findViewById(controlsIds[i]);
            
            // Inputs
            urlInputs[i] = findViewById(inputIds[i]);
            
            // CheckBoxes
            checkBoxes[i] = findViewById(checkBoxIds[i]);
            checkBoxes[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    updateActivePlayers();
                    applyLayout();
                }
            });
            
            // Botões de navegação
            btnPrev[i] = findViewById(prevBtnIds[i]);
            btnNext[i] = findViewById(nextBtnIds[i]);
            
            // Configurar listeners
            setupPlayerListeners(playerIndex);
            
            // Inicializar histórico
            playerHistory.put(i, new ArrayList<>());
            currentHistoryIndex.put(i, -1);
            
            // Adicionar aos players ativos inicialmente
            activePlayers.add(i);
        }

        // Configurar Spinner de layouts
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.layout_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        layoutSpinner.setAdapter(adapter);
        layoutSpinner.setSelection(7); // 2x2 é o item 7
        
        layoutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLayout = parent.getItemAtPosition(position).toString();
                applyLayout();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Configurar botões globais
        btnLoadAll.setOnClickListener(v -> loadAllURLs());
        btnReloadAll.setOnClickListener(v -> reloadAllWebViews());
        btnClearAll.setOnClickListener(v -> clearAllWebViews());
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());

        // Aplicar layout inicial
        applyLayout();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView, int playerIndex) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        
        // User agent personalizado para evitar bloqueios
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        // Habilitar hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // Configurar WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                loadingOverlays[playerIndex].setVisibility(View.VISIBLE);
                playerControls[playerIndex].setVisibility(View.VISIBLE);
                
                // Adicionar ao histórico
                if (!playerHistory.get(playerIndex).contains(url)) {
                    playerHistory.get(playerIndex).add(url);
                    currentHistoryIndex.put(playerIndex, playerHistory.get(playerIndex).size() - 1);
                }
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                loadingOverlays[playerIndex].setVisibility(View.GONE);
                
                // Auto-hide controls após 3 segundos
                playerControls[playerIndex].postDelayed(() -> {
                    playerControls[playerIndex].setVisibility(View.GONE);
                }, 3000);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                loadingOverlays[playerIndex].setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, 
                    "Player " + (playerIndex + 1) + " error: " + description, 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        // Configurar WebChromeClient para vídeos
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Handle fullscreen video
            }
            
            @Override
            public void onHideCustomView() {
                // Exit fullscreen video
            }
        });
    }

    private void setupPlayerListeners(int playerIndex) {
        // Botão Previous
        btnPrev[playerIndex].setOnClickListener(v -> navigateHistory(playerIndex, -1));
        
        // Botão Next
        btnNext[playerIndex].setOnClickListener(v -> navigateHistory(playerIndex, 1));
        
        // Input URL (ao pressionar Enter)
        urlInputs[playerIndex].setOnEditorActionListener((v, actionId, event) -> {
            loadURL(playerIndex, urlInputs[playerIndex].getText().toString().trim());
            return true;
        });
        
        // Tocar para mostrar controles
        playerContainers[playerIndex].setOnClickListener(v -> {
            playerControls[playerIndex].setVisibility(View.VISIBLE);
            // Auto-hide após 3 segundos
            playerControls[playerIndex].postDelayed(() -> {
                playerControls[playerIndex].setVisibility(View.GONE);
            }, 3000);
        });
    }

    private void loadURL(int playerIndex, String url) {
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Adicionar protocolo se necessário
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
        Toast.makeText(this, "Loading all active players", Toast.LENGTH_SHORT).show();
    }

    private void reloadAllWebViews() {
        for (int i = 0; i < 4; i++) {
            if (checkBoxes[i].isChecked() && webViews[i] != null) {
                webViews[i].reload();
            }
        }
        Toast.makeText(this, "Reloading all players", Toast.LENGTH_SHORT).show();
    }

    private void clearAllWebViews() {
        for (int i = 0; i < 4; i++) {
            if (webViews[i] != null) {
                webViews[i].loadUrl("about:blank");
                urlInputs[i].setText("");
                playerHistory.get(i).clear();
                currentHistoryIndex.put(i, -1);
            }
        }
        Toast.makeText(this, "Cleared all players", Toast.LENGTH_SHORT).show();
    }

    private void navigateHistory(int playerIndex, int direction) {
        List<String> history = playerHistory.get(playerIndex);
        int currentIndex = currentHistoryIndex.get(playerIndex);
        
        if (history.isEmpty()) return;
        
        int newIndex = currentIndex + direction;
        if (newIndex >= 0 && newIndex < history.size()) {
            String url = history.get(newIndex);
            webViews[playerIndex].loadUrl(url);
            urlInputs[playerIndex].setText(url);
            currentHistoryIndex.put(playerIndex, newIndex);
        }
    }

    private void updateActivePlayers() {
        activePlayers.clear();
        for (int i = 0; i < 4; i++) {
            if (checkBoxes[i].isChecked()) {
                activePlayers.add(i);
            }
        }
    }

    private void applyLayout() {
        if (activePlayers.isEmpty()) return;
        
        int[] config = layoutConfigs.get(currentLayout);
        if (config == null) config = new int[]{2, 2};
        
        int rows = config[0];
        int cols = config[1];
        
        gridLayout.setRowCount(rows);
        gridLayout.setColumnCount(cols);
        
        // Reset all containers
        for (int i = 0; i < 4; i++) {
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) playerContainers[i].getLayoutParams();
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1);
            params.width = 0;
            params.height = 0;
            
            // Show/hide based on checkbox
            if (checkBoxes[i].isChecked() && activePlayers.contains(i)) {
                playerContainers[i].setVisibility(View.VISIBLE);
            } else {
                playerContainers[i].setVisibility(View.GONE);
            }
        }
        
        // Position active players in grid
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (checkBoxes[i].isChecked() && activePlayers.contains(i)) {
                int row = count / cols;
                int col = count % cols;
                
                GridLayout.LayoutParams params = (GridLayout.LayoutParams) playerContainers[i].getLayoutParams();
                params.rowSpec = GridLayout.spec(row, 1, 1f);
                params.columnSpec = GridLayout.spec(col, 1, 1f);
                params.width = 0;
                params.height = 0;
                
                playerContainers[i].setLayoutParams(params);
                count++;
                
                if (count >= rows * cols) break;
            }
        }
        
        // Force layout update
        gridLayout.requestLayout();
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            // Exit fullscreen
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (getSupportActionBar() != null) {
                getSupportActionBar().show();
            }
            btnFullscreen.setText("⛶");
        } else {
            // Enter fullscreen
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
            btnFullscreen.setText("❐");
        }
        isFullscreen = !isFullscreen;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyLayout();
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

    @Override
    public void onBackPressed() {
        for (WebView webView : webViews) {
            if (webView.canGoBack()) {
                webView.goBack();
                return;
            }
        }
        super.onBackPressed();
    }
}
