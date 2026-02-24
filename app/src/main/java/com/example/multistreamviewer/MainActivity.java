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
import android.widget.RelativeLayout;
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

    private GridLayout gridLayout;
    private WebView[] webViews = new WebView[4];
    private FrameLayout[] boxContainers = new FrameLayout[4];
    private LinearLayout bottomControls;
    private LinearLayout expandedBottomBar;
    private FrameLayout sidebarContainer;
    private RelativeLayout mainLayout;
    
    private Button btnMenu;
    private Button btnToggleBottomBar, btnToggleSidebar;
    private Button btnCloseMenu, btnLoadAll, btnReloadAll, btnClearAll;
    private Button btnSaveState, btnLoadState, btnSaveFavorites, btnLoadFavorites;
    private Button btnToggleOrientation;
    private Button[] btnRefresh = new Button[4];
    private Button[] btnZoomIn = new Button[4];
    private Button[] btnZoomOut = new Button[4];
    private Button[] btnPrevious = new Button[4];
    private Button[] btnNext = new Button[4];
    private Button[] btnLoadUrl = new Button[4];
    
    private CheckBox[] checkBoxes = new CheckBox[4];
    private CheckBox[] checkBoxesKeepActive = new CheckBox[4];
    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups, cbBlockRedirects, cbBlockAds;
    private EditText[] urlInputs = new EditText[4];
    private TextView tvFocusedBox;
    
    private boolean[] boxEnabled = {true, true, true, true};
    private boolean[] boxKeepActive = {false, false, false, false};
    private boolean isSidebarVisible = false;
    private boolean isBottomBarExpanded = false;
    private int focusedBoxIndex = 0;
    private float[] zoomLevels = {1.0f, 1.0f, 1.0f, 1.0f};
    private int currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
    
    private ArrayList<String> favoritesList = new ArrayList<>();
    private SharedPreferences preferences;
    
    // Fullscreen video support
    private boolean[] boxInFullscreen = new boolean[4];
    private View[] fullscreenViews = new View[4];
    private WebChromeClient.CustomViewCallback[] fullscreenCallbacks = new WebChromeClient.CustomViewCallback[4];
    

    
    private final List<String> adDomains = Arrays.asList(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com"
    );

    private static final String TAG = "MultiStreamViewer";
    


    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Detectar tipo de dispositivo e definir orientação automática
        detectDeviceTypeAndSetOrientation();
        
        setContentView(R.layout.activity_main);
        
        // Inicializar a orientacao rastreada
        currentOrientation = getResources().getConfiguration().orientation;
        
        preferences = getSharedPreferences("MultiStreamViewer", MODE_PRIVATE);
        
        initViews();
        initWebViews();
        initEventListeners();
        
        loadSavedState(true);
        loadFavoritesList();
        
        // Confirmação visual de favoritos carregados
        new Handler().postDelayed(() -> {
            if (favoritesList.size() > 0) {
                Toast.makeText(MainActivity.this, 
                    "✅ " + favoritesList.size() + " Favoritos carregados!", 
                    Toast.LENGTH_SHORT).show();
            }
        }, 1500);
        
        updateLayout();
        updateFocusedBoxIndicator();
        
        if (!hasSavedState()) {
            new Handler().postDelayed(this::loadInitialURLs, 1000);
        }
    }
    
    /**
     * Detecta o tipo de dispositivo e define a orientação automaticamente
     * Fire Stick/Tablet: Landscape
     * Celular normal: Portrait
     */
    private void detectDeviceTypeAndSetOrientation() {
        if (isFireTVorTablet()) {
            // Fire Stick ou Tablet -> Landscape
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            // Celular normal -> Portrait
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }
    
    /**
     * Verifica se é Fire TV, Fire Stick ou Tablet
     */
    private boolean isFireTVorTablet() {
        // Verifica Fire TV/Stick
        String model = Build.MODEL;
        String device = Build.DEVICE;
        String product = Build.PRODUCT;
        
        // Check for Fire TV devices
        if (model != null && (model.contains("AFT") || model.contains("AFTA") || model.contains("AFTM"))) {
            return true;
        }
        if (device != null && device.contains("mt")) {
            return true;
        }
        if (product != null && product.contains("montoya")) {
            return true;
        }
        
        // Verifica se é Tablet pela densidade e tamanho de tela
        android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        
        float widthInches = displayMetrics.widthPixels / displayMetrics.xdpi;
        float heightInches = displayMetrics.heightPixels / displayMetrics.ydpi;
        double screenDiagonal = Math.sqrt((widthInches * widthInches) + (heightInches * heightInches));
        
        // Considerar tablet se diagonal > 5 polegadas (normalmente > 6 para tablets)
        return screenDiagonal >= 6.0;
    }
    
    /**
     * Limpa o cache do aplicativo quando fechado completamente
     */
    private void clearAppCache() {
        try {
            // Limpar cache do WebView
            for (WebView webView : webViews) {
                if (webView != null) {
                    webView.clearCache(true);
                    webView.clearHistory();
                }
            }
            
            // Limpar cache geral do app
            if (getCacheDir() != null) {
                deleteDir(getCacheDir());
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao limpar cache: " + e.getMessage());
        }
    }
    
    /**
     * Método auxiliar para deletar diretório e seus conteúdos
     */
    private boolean deleteDir(java.io.File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new java.io.File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }
    
    private void initViews() {
        gridLayout = findViewById(R.id.gridLayout);
        bottomControls = findViewById(R.id.bottomControls);
        expandedBottomBar = findViewById(R.id.expandedBottomBar);
        sidebarContainer = findViewById(R.id.sidebarContainer);
        mainLayout = findViewById(R.id.main_layout);
        tvFocusedBox = findViewById(R.id.tvFocusedBox);
        
        btnToggleBottomBar = findViewById(R.id.btnToggleBottomBar);
        btnToggleSidebar = findViewById(R.id.btnToggleSidebar);
        btnToggleOrientation = findViewById(R.id.btnToggleOrientation);
        btnCloseMenu = findViewById(R.id.btnCloseMenu);
        
        btnLoadAll = findViewById(R.id.btnLoadAll);
        btnReloadAll = findViewById(R.id.btnReloadAll);
        btnClearAll = findViewById(R.id.btnClearAll);
        
        btnSaveState = findViewById(R.id.btnSaveState);
        btnLoadState = findViewById(R.id.btnLoadState);
        btnSaveFavorites = findViewById(R.id.btnSaveFavorites);
        btnLoadFavorites = findViewById(R.id.btnLoadFavorites);
        
        // Inicializar CheckBoxes das boxes
        checkBoxes[0] = findViewById(R.id.checkBox1);
        checkBoxes[1] = findViewById(R.id.checkBox2);
        checkBoxes[2] = findViewById(R.id.checkBox3);
        checkBoxes[3] = findViewById(R.id.checkBox4);
        
        // Inicializar CheckBoxes Keep Active
        checkBoxesKeepActive[0] = findViewById(R.id.checkBoxKeepActive1);
        checkBoxesKeepActive[1] = findViewById(R.id.checkBoxKeepActive2);
        checkBoxesKeepActive[2] = findViewById(R.id.checkBoxKeepActive3);
        checkBoxesKeepActive[3] = findViewById(R.id.checkBoxKeepActive4);
        
        // Inicializar botões Refresh
        btnRefresh[0] = findViewById(R.id.btnRefresh1);
        btnRefresh[1] = findViewById(R.id.btnRefresh2);
        btnRefresh[2] = findViewById(R.id.btnRefresh3);
        btnRefresh[3] = findViewById(R.id.btnRefresh4);
        
        
        btnZoomIn[0] = findViewById(R.id.btnZoomIn1);
        btnZoomIn[1] = findViewById(R.id.btnZoomIn2);
        btnZoomIn[2] = findViewById(R.id.btnZoomIn3);
        btnZoomIn[3] = findViewById(R.id.btnZoomIn4);
        
        btnZoomOut[0] = findViewById(R.id.btnZoomOut1);
        btnZoomOut[1] = findViewById(R.id.btnZoomOut2);
        btnZoomOut[2] = findViewById(R.id.btnZoomOut3);
        btnZoomOut[3] = findViewById(R.id.btnZoomOut4);
        
        btnPrevious[0] = findViewById(R.id.btnPrevious1);
        btnPrevious[1] = findViewById(R.id.btnPrevious2);
        btnPrevious[2] = findViewById(R.id.btnPrevious3);
        btnPrevious[3] = findViewById(R.id.btnPrevious4);
        
        btnNext[0] = findViewById(R.id.btnNext1);
        btnNext[1] = findViewById(R.id.btnNext2);
        btnNext[2] = findViewById(R.id.btnNext3);
        btnNext[3] = findViewById(R.id.btnNext4);
        
        // Inicializar botões GO
        btnLoadUrl[0] = findViewById(R.id.btnLoadUrl1);
        btnLoadUrl[1] = findViewById(R.id.btnLoadUrl2);
        btnLoadUrl[2] = findViewById(R.id.btnLoadUrl3);
        btnLoadUrl[3] = findViewById(R.id.btnLoadUrl4);
        

        
        // Inicializar checkboxes de configuração web
        cbAllowScripts = findViewById(R.id.cbAllowScripts);
        cbAllowForms = findViewById(R.id.cbAllowForms);
        cbAllowPopups = findViewById(R.id.cbAllowPopups);
        cbBlockRedirects = findViewById(R.id.cbBlockRedirects);
        cbBlockAds = findViewById(R.id.cbBlockAds);
        
        urlInputs[0] = findViewById(R.id.urlInput1);
        urlInputs[1] = findViewById(R.id.urlInput2);
        urlInputs[2] = findViewById(R.id.urlInput3);
        urlInputs[3] = findViewById(R.id.urlInput4);
        
        String defaultUrl = "https://dzritv.com/sport/football/";
        for (EditText urlInput : urlInputs) {
            if (urlInput != null) {
                urlInput.setText(defaultUrl);
                urlInput.setCursorVisible(true);
                urlInput.setSelectAllOnFocus(true);
                
                urlInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            EditText et = (EditText) v;
                            et.selectAll();
                            showKeyboard(et);
                        }
                    }
                });
            }
        }
        
        // Configurar ação para FireTV
        for (int i = 0; i < 4; i++) {
            final int boxIndex = i;
            if (urlInputs[i] != null) {
                urlInputs[i].setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            String url = urlInputs[boxIndex].getText().toString().trim();
                            if (!url.isEmpty()) {
                                loadURL(boxIndex, url);
                                hideKeyboard();
                            }
                            return true;
                        }
                        return false;
                    }
                });
            }
        }
    }
    
    private void showKeyboard(View view) {
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                android.view.inputmethod.InputMethodManager imm = 
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 100);
    }
    
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
    
    public void closeSidebarFromOverlay(View view) {
        closeSidebar();
    }
    
    private void closeSidebar() {
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(
                sidebarContainer, "alpha", 1f, 0f);
        animator.setDuration(300);
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                sidebarContainer.setVisibility(View.GONE);
                isSidebarVisible = false;
                
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) gridLayout.getLayoutParams();
                params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
                params.removeRule(RelativeLayout.LEFT_OF);
                gridLayout.setLayoutParams(params);
                
                hideKeyboard();
                if (btnToggleSidebar != null) {
                    btnToggleSidebar.requestFocus();
                }
            }
        });
        animator.start();
    }
    
    private void openSidebar() {
        sidebarContainer.setVisibility(View.VISIBLE);
        sidebarContainer.setAlpha(0f);
        isSidebarVisible = true;
        
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) gridLayout.getLayoutParams();
        params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        params.addRule(RelativeLayout.LEFT_OF, R.id.sidebarContainer);
        gridLayout.setLayoutParams(params);
        
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(
                sidebarContainer, "alpha", 0f, 1f);
        animator.setDuration(300);
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (btnCloseMenu != null) {
                    btnCloseMenu.requestFocus();
                }
            }
        });
        animator.start();
    }

    private void toggleBottomBar() {
        if (isBottomBarExpanded) {
            closeBottomBar();
        } else {
            openBottomBar();
        }
    }
    
    private void closeBottomBar() {
        if (expandedBottomBar == null) return;
        
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(
                expandedBottomBar, "translationY", 0f, expandedBottomBar.getHeight());
        animator.setDuration(300);
        animator.setInterpolator(new android.view.animation.AccelerateInterpolator());
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                expandedBottomBar.setVisibility(View.GONE);
                isBottomBarExpanded = false;
                updateLayoutForBottomBar();
                hideKeyboard();
                if (btnToggleBottomBar != null) {
                    btnToggleBottomBar.requestFocus();
                }
            }
        });
        animator.start();
    }
    
    private void openBottomBar() {
        if (expandedBottomBar == null) return;
        
        expandedBottomBar.setVisibility(View.VISIBLE);
        expandedBottomBar.setTranslationY(expandedBottomBar.getHeight());
        isBottomBarExpanded = true;
        updateLayoutForBottomBar();
        
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(
                expandedBottomBar, "translationY", expandedBottomBar.getHeight(), 0f);
        animator.setDuration(300);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (btnCloseMenu != null) {
                    btnCloseMenu.requestFocus();
                }
            }
        });
        animator.start();
    }
    
    private void updateLayoutForBottomBar() {
        if (gridLayout == null || bottomControls == null || expandedBottomBar == null) return;
        
        RelativeLayout.LayoutParams gridParams = (RelativeLayout.LayoutParams) gridLayout.getLayoutParams();
        if (isBottomBarExpanded) {
            gridParams.addRule(RelativeLayout.ABOVE, R.id.expandedBottomBar);
            gridParams.removeRule(RelativeLayout.ABOVE);
            gridParams.addRule(RelativeLayout.ABOVE, R.id.expandedBottomBar);
        } else {
            gridParams.removeRule(RelativeLayout.ABOVE);
            gridParams.addRule(RelativeLayout.ABOVE, R.id.bottomControls);
        }
        gridLayout.setLayoutParams(gridParams);
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViews() {
        // Criar todos os containers
        for (int i = 0; i < 4; i++) {
            final int boxIndex = i;
            
            boxContainers[i] = new FrameLayout(this);
            boxContainers[i].setId(View.generateViewId());
            boxContainers[i].setBackgroundColor(Color.BLACK);
            boxContainers[i].setFocusable(true);
            boxContainers[i].setFocusableInTouchMode(true);
            
            boxContainers[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus && !isSidebarVisible) {
                        focusedBoxIndex = boxIndex;
                        updateFocusedBoxIndicator();
                        boxContainers[boxIndex].setBackgroundResource(R.drawable.box_focused_border);
                    } else if (!hasFocus) {
                        boxContainers[boxIndex].setBackgroundColor(Color.BLACK);
                    }
                }
            });
            
            boxContainers[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isSidebarVisible) return;
                    boxContainers[boxIndex].requestFocus();
                }
            });
        }
        
        // Criar e configurar TODOS os WebViews
        for (int i = 0; i < 4; i++) {
            webViews[i] = new WebView(this);
            webViews[i].setId(View.generateViewId());
            setupWebView(webViews[i], i);
            
            // Adicionar WebView ao container
            boxContainers[i].addView(webViews[i], 
                new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
        }
        
        // Adicionar todos os containers ao GridLayout
        for (int i = 0; i < 4; i++) {
            gridLayout.addView(boxContainers[i]);
        }
        
        updateLayout(); // Configurar layout inicial
        if (btnToggleSidebar != null) {
            btnToggleSidebar.requestFocus();
        }
    }
    
    private void updateFocusedBoxIndicator() {
        tvFocusedBox.setText("Foco: " + (focusedBoxIndex + 1));
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView, int boxIndex) {
        WebSettings settings = webView.getSettings();
        
        // CONFIGURAÇÕES BÁSICAS
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        String userAgent = "Mozilla/5.0 (Linux; Android 10; AFTMM) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Safari/537.36";
        settings.setUserAgentString(userAgent);
        
        if (cbBlockAds != null) {
            settings.setBlockNetworkLoads(cbBlockAds.isChecked());
            settings.setBlockNetworkImage(cbBlockAds.isChecked());
        }
        
        settings.setTextZoom((int)(zoomLevels[boxIndex] * 100));
        webView.setInitialScale((int)(zoomLevels[boxIndex] * 100));
        
        webView.setBackgroundColor(Color.BLACK);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        
        // CONFIGURAR WEBVIEWCLIENT
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
                if (cbBlockRedirects != null && cbBlockRedirects.isChecked()) {
                    String currentUrl = view.getUrl();
                    if (currentUrl != null && !isSameDomain(currentUrl, url)) {
                        return true;
                    }
                }
                
                if (cbBlockAds != null && cbBlockAds.isChecked() && isAdUrl(url)) {
                    return true;
                }
                
                if (url.contains("youtube.com") || url.contains("twitch.tv") || url.contains("stream")) {
                    view.loadUrl(url);
                    return true;
                }
                
                return false;
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                // Apply zoom
                applyZoom(boxIndex);
                
                // Ad blocking
                if (cbBlockAds != null && cbBlockAds.isChecked()) {
                    injectAdBlocker(view);
                }
            }
        });
        
        // CONFIGURAR WEBCHROMECLIENT COM SUPORTE A FULLSCREEN
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                Log.d(TAG, "Fullscreen on Box " + (boxIndex + 1));
                
                boxInFullscreen[boxIndex] = true;
                fullscreenViews[boxIndex] = view;
                fullscreenCallbacks[boxIndex] = callback;
                
                if (boxContainers[boxIndex].getChildCount() > 1) {
                    boxContainers[boxIndex].removeViewAt(1);
                }
                
                boxContainers[boxIndex].addView(view, 
                    new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                
                webView.setVisibility(View.GONE);
                
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
                
                Toast.makeText(MainActivity.this, 
                    "Box " + (boxIndex + 1) + " fullscreen", 
                    Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onHideCustomView() {
                if (fullscreenViews[boxIndex] == null) return;
                
                Log.d(TAG, "Exiting fullscreen Box " + (boxIndex + 1));
                
                if (boxContainers[boxIndex].indexOfChild(fullscreenViews[boxIndex]) != -1) {
                    boxContainers[boxIndex].removeView(fullscreenViews[boxIndex]);
                }
                
                webView.setVisibility(View.VISIBLE);
                
                if (fullscreenCallbacks[boxIndex] != null) {
                    fullscreenCallbacks[boxIndex].onCustomViewHidden();
                }
                
                fullscreenViews[boxIndex] = null;
                fullscreenCallbacks[boxIndex] = null;
                boxInFullscreen[boxIndex] = false;
                
                if (!isAnyBoxInFullscreen()) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    
                    // Atualizar layout quando sair de fullscreen para reorganizar boxes
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            updateLayout();
                        }
                    });
                }
            }
        });
    }

    private boolean isAnyBoxInFullscreen() {
        for (int i = 0; i < 4; i++) {
            if (boxInFullscreen[i]) {
                return true;
            }
        }
        return false;
    }
    
    private void exitFullscreen(int boxIndex) {
        if (boxInFullscreen[boxIndex] && fullscreenViews[boxIndex] != null) {
            if (boxContainers[boxIndex].indexOfChild(fullscreenViews[boxIndex]) != -1) {
                boxContainers[boxIndex].removeView(fullscreenViews[boxIndex]);
            }
            
            if (webViews[boxIndex] != null) {
                webViews[boxIndex].setVisibility(View.VISIBLE);
            }
            
            if (fullscreenCallbacks[boxIndex] != null) {
                fullscreenCallbacks[boxIndex].onCustomViewHidden();
            }
            
            fullscreenViews[boxIndex] = null;
            fullscreenCallbacks[boxIndex] = null;
            boxInFullscreen[boxIndex] = false;
            
            if (!isAnyBoxInFullscreen()) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
                // Agora que saiu de fullscreen, atualizar layout para a orientacao atual
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        updateLayout();
                    }
                });
            }
        }
    }

    private void applyZoom(int boxIndex) {
        WebView webView = webViews[boxIndex];
        if (webView != null) {
            webView.getSettings().setTextZoom((int)(zoomLevels[boxIndex] * 100));
        }
    }
    
    private void zoomIn(int boxIndex) {
        if (zoomLevels[boxIndex] < 2.0f) {
            zoomLevels[boxIndex] += 0.1f;
            applyZoom(boxIndex);
            Toast.makeText(this, "Box " + (boxIndex + 1) + " Zoom: " + String.format("%.0f", zoomLevels[boxIndex] * 100) + "%", 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void zoomOut(int boxIndex) {
        if (zoomLevels[boxIndex] > 0.5f) {
            zoomLevels[boxIndex] -= 0.1f;
            applyZoom(boxIndex);
            Toast.makeText(this, "Box " + (boxIndex + 1) + " Zoom: " + String.format("%.0f", zoomLevels[boxIndex] * 100) + "%", 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void injectAdBlocker(WebView view) {
        String adBlockJS = 
            "try {" +
            "   var selectors = [" +
            "       'div[class*=\"ad\"]', 'div[id*=\"ad\"]', 'iframe[src*=\"ad\"]'," +
            "       'ins.adsbygoogle', 'div.ad-container', 'div.advertisement'" +
            "   ];" +
            "   selectors.forEach(function(selector) {" +
            "       var elements = document.querySelectorAll(selector);" +
            "       elements.forEach(function(el) {" +
            "           el.style.display = 'none';" +
            "           el.parentNode.removeChild(el);" +
            "       });" +
            "   });" +
            "} catch(e) {}";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(adBlockJS, null);
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
        if (btnToggleBottomBar != null) {
            btnToggleBottomBar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleBottomBar();
                }
            });
        }
        
        if (btnToggleSidebar != null) {
            btnToggleSidebar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isSidebarVisible) {
                        closeSidebar();
                    } else {
                        openSidebar();
                    }
                }
            });
        }
        
        if (btnToggleOrientation != null) {
            btnToggleOrientation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleOrientation();
                }
            });
        }
        
        if (btnCloseMenu != null) {
            btnCloseMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isSidebarVisible) {
                        closeSidebar();
                    }
                    if (isBottomBarExpanded) {
                        closeBottomBar();
                    }
                }
            });
        }
        
        if (btnLoadAll != null) {
            btnLoadAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadAllURLs();
                }
            });
        }
        
        if (btnReloadAll != null) {
            btnReloadAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reloadAllWebViews();
                }
            });
        }
        
        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearAllWebViews();
                }
            });
        }
        
        if (btnSaveState != null) {
            btnSaveState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveCurrentState();
                }
            });
        }
        
        if (btnLoadState != null) {
            btnLoadState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadSavedState(false);
                }
            });
        }
        
        if (btnSaveFavorites != null) {
            btnSaveFavorites.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSaveFavoriteDialog();
                }
            });
        }
        
        if (btnLoadFavorites != null) {
            btnLoadFavorites.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLoadFavoritesDialog();
                }
            });
        }
        
        // Configurar listeners para controles individuais de cada box
        for (int i = 0; i < 4; i++) {
            final int boxIndex = i;
            
            // Botão GO
            if (btnLoadUrl[i] != null) {
                btnLoadUrl[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String url = urlInputs[boxIndex].getText().toString().trim();
                        if (!url.isEmpty()) {
                            loadURL(boxIndex, url);
                            Toast.makeText(MainActivity.this, "Box " + (boxIndex + 1) + " carregando...", 
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            
            // Botão Refresh
            if (btnRefresh[i] != null) {
                btnRefresh[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (webViews[boxIndex] != null) {
                            webViews[boxIndex].reload();
                            Toast.makeText(MainActivity.this, 
                                "Box " + (boxIndex + 1) + " recarregada", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            
            // Checkbox para ativar/desativar box
            if (checkBoxes[i] != null) {
                checkBoxes[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        boxEnabled[boxIndex] = isChecked;
                        updateLayout();
                    }
                });
            }
            
            // Checkbox para manter webview ativa mesmo quando não visível
            if (checkBoxesKeepActive[i] != null) {
                checkBoxesKeepActive[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        boxKeepActive[boxIndex] = isChecked;
                        updateLayout();
                    }
                });
            }
            
            // Botão Zoom In
            if (btnZoomIn[i] != null) {
                btnZoomIn[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        zoomIn(boxIndex);
                    }
                });
            }
            
            // Botão Zoom Out
            if (btnZoomOut[i] != null) {
                btnZoomOut[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        zoomOut(boxIndex);
                    }
                });
            }
            
            // Botão Previous
            if (btnPrevious[i] != null) {
                btnPrevious[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (webViews[boxIndex] != null && webViews[boxIndex].canGoBack()) {
                            webViews[boxIndex].goBack();
                        }
                    }
                });
            }
            
            // Botão Next
            if (btnNext[i] != null) {
                btnNext[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (webViews[boxIndex] != null && webViews[boxIndex].canGoForward()) {
                            webViews[boxIndex].goForward();
                        }
                    }
                });
            }
        }
        
        // Configurações Web
        if (cbAllowScripts != null) {
            cbAllowScripts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    for (WebView webView : webViews) {
                        if (webView != null) {
                            webView.getSettings().setJavaScriptEnabled(isChecked);
                        }
                    }
                }
            });
        }
        
        if (cbBlockAds != null) {
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
                }
            });
        }
    }
    
    private void updateLayout() {
        // Se algum box está em fullscreen, não fazer update do layout
        if (isAnyBoxInFullscreen()) {
            Log.d(TAG, "Box em fullscreen detectado, ignorando updateLayout");
            return;
        }
        
        int activeBoxes = 0;
        for (boolean enabled : boxEnabled) {
            if (enabled) activeBoxes++;
        }
        
        if (activeBoxes == 0) {
            for (int i = 0; i < 4; i++) {
                boxEnabled[i] = true;
                if (checkBoxes[i] != null) {
                    checkBoxes[i].setChecked(true);
                }
            }
            activeBoxes = 4;
        }
        
        int rows, cols;
        
        // Usar a orientação rastreada (ou obter da configuração se não foi rastreada)
        int orientation = currentOrientation;
        if (orientation == 0) {
            // Se não foi inicializada, obter da configuração
            orientation = getResources().getConfiguration().orientation;
        }
        
        boolean isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        
        Log.d(TAG, "updateLayout - Boxes ativos: " + activeBoxes + ", Portrait: " + isPortrait);
        
        if (isPortrait) {
            // PORTRAIT: prioridade vertical (coluna unica para 2-3 boxes)
            switch (activeBoxes) {
                case 1:
                    rows = 1; cols = 1;
                    break;
                case 2:
                    rows = 2; cols = 1;  // 2 linhas x 1 coluna
                    break;
                case 3:
                    rows = 3; cols = 1;  // 3 linhas x 1 coluna
                    break;
                case 4:
                    rows = 2; cols = 2;  // 2 linhas x 2 colunas
                    break;
                default:
                    rows = 1; cols = 1;
                    break;
            }
        } else {
            // LANDSCAPE: prioridade horizontal (linha unica para 2-3 boxes)
            switch (activeBoxes) {
                case 1:
                    rows = 1; cols = 1;
                    break;
                case 2:
                    rows = 1; cols = 2;  // 1 linha x 2 colunas
                    break;
                case 3:
                    rows = 1; cols = 3;  // 1 linha x 3 colunas
                    break;
                case 4:
                    rows = 2; cols = 2;  // 2 linhas x 2 colunas
                    break;
                default:
                    rows = 1; cols = 1;
                    break;
            }
        }
        
        gridLayout.removeAllViews();
        gridLayout.setRowCount(rows);
        gridLayout.setColumnCount(cols);
        
        int position = 0;
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i] || boxKeepActive[i]) {
                // Box visível OU box mantém-se ativa (mesmo se não visível)
                if (boxEnabled[i]) {
                    // Box ativa: adiciona ao grid para exibição
                    GridLayout.Spec rowSpec = GridLayout.spec(position / cols, 1f);
                    GridLayout.Spec colSpec = GridLayout.spec(position % cols, 1f);
                    
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
                    params.width = 0;
                    params.height = 0;
                    
                    if (activeBoxes == 1) {
                        params.setMargins(0, 0, 0, 0);
                    } else if (activeBoxes == 2) {
                        params.setMargins(2, 2, 2, 2);
                    } else {
                        params.setMargins(1, 1, 1, 1);
                    }
                    
                    if (boxContainers[i] != null) {
                        boxContainers[i].setVisibility(View.VISIBLE);
                        gridLayout.addView(boxContainers[i], params);
                        
                        if (webViews[i] != null) {
                            webViews[i].setVisibility(View.VISIBLE);
                        }
                    }
                    position++;
                } else if (boxKeepActive[i]) {
                    // Box mantém-se ativa mas não é exibida
                    // Manter o container invisível mas ativo para que a webview continue carregando
                    if (boxContainers[i] != null) {
                        boxContainers[i].setVisibility(View.INVISIBLE);
                        
                        if (webViews[i] != null) {
                            webViews[i].setVisibility(View.INVISIBLE);
                        }
                    }
                }
            } else {
                // Box desativada e sem Keep Active: remover da view
                if (boxContainers[i] != null) {
                    boxContainers[i].setVisibility(View.GONE);
                }
            }
        }
        
        gridLayout.requestLayout();
    }
    
    private void loadAllURLs() {
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i] || boxKeepActive[i]) {
                String url = urlInputs[i] != null ? urlInputs[i].getText().toString().trim() : "";
                if (url.isEmpty()) {
                    url = getDefaultUrl(i);
                    if (urlInputs[i] != null) {
                        urlInputs[i].setText(url);
                    }
                }
                loadURL(i, url);
            }
        }
        Toast.makeText(this, "Carregando todas as URLs", Toast.LENGTH_SHORT).show();
    }
    
    private void loadInitialURLs() {
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i] || boxKeepActive[i]) {
                String url = urlInputs[i] != null ? urlInputs[i].getText().toString().trim() : "";
                if (url.isEmpty()) {
                    url = getDefaultUrl(i);
                    if (urlInputs[i] != null) {
                        urlInputs[i].setText(url);
                    }
                }
                loadURL(i, url);
            }
        }
    }
    
    private void loadURL(int boxIndex, String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://")) {
                url = "https://" + url;
            }
            if (webViews[boxIndex] != null) {
                webViews[boxIndex].loadUrl(url);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar Box " + (boxIndex + 1), e);
            Toast.makeText(this, "Erro ao carregar Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
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
    
    private boolean hasSavedState() {
        return preferences.contains("url_0") || preferences.contains("box_enabled_0");
    }
    
    private void saveCurrentState() {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            
            for (int i = 0; i < 4; i++) {
                String currentUrl = urlInputs[i] != null ? urlInputs[i].getText().toString().trim() : "";
                if (currentUrl.isEmpty()) {
                    currentUrl = getDefaultUrl(i);
                }
                editor.putString("url_" + i, currentUrl);
            }
            
            for (int i = 0; i < 4; i++) {
                editor.putBoolean("box_enabled_" + i, boxEnabled[i]);
            }
            
            // Salvar estado Keep Active
            for (int i = 0; i < 4; i++) {
                editor.putBoolean("box_keep_active_" + i, boxKeepActive[i]);
            }
            
            // Salvar zoom levels
            for (int i = 0; i < 4; i++) {
                editor.putFloat("zoom_level_" + i, zoomLevels[i]);
            }
            
            if (cbAllowScripts != null) editor.putBoolean("allow_scripts", cbAllowScripts.isChecked());
            if (cbAllowForms != null) editor.putBoolean("allow_forms", cbAllowForms.isChecked());
            if (cbAllowPopups != null) editor.putBoolean("allow_popups", cbAllowPopups.isChecked());
            if (cbBlockRedirects != null) editor.putBoolean("block_redirects", cbBlockRedirects.isChecked());
            if (cbBlockAds != null) editor.putBoolean("block_ads", cbBlockAds.isChecked());
            
            editor.apply();
            Toast.makeText(this, "✅ Estado guardado!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Erro ao guardar estado", e);
            Toast.makeText(this, "❌ Erro ao guardar estado", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadSavedState(boolean silent) {
        try {
            // Carregar estados primeiro (boxEnabled e boxKeepActive) antes de carregar URLs
            for (int i = 0; i < 4; i++) {
                boolean savedState = preferences.getBoolean("box_enabled_" + i, true);
                boxEnabled[i] = savedState;
                if (checkBoxes[i] != null) {
                    checkBoxes[i].setChecked(savedState);
                }
            }
            
            // Carregar estado Keep Active
            for (int i = 0; i < 4; i++) {
                boolean savedKeepActive = preferences.getBoolean("box_keep_active_" + i, false);
                boxKeepActive[i] = savedKeepActive;
                if (checkBoxesKeepActive[i] != null) {
                    checkBoxesKeepActive[i].setChecked(savedKeepActive);
                }
            }
            
            // Agora carregar as URLs considerando boxEnabled e boxKeepActive
            boolean hasSavedUrls = false;
            for (int i = 0; i < 4; i++) {
                String savedUrl = preferences.getString("url_" + i, "");
                if (!savedUrl.isEmpty()) {
                    hasSavedUrls = true;
                    if (urlInputs[i] != null) {
                        urlInputs[i].setText(savedUrl);
                    }
                    if ((boxEnabled[i] || boxKeepActive[i]) && webViews[i] != null) {
                        loadURL(i, savedUrl);
                    }
                }
            }
            
            if (!hasSavedUrls) {
                for (int i = 0; i < 4; i++) {
                    if (urlInputs[i] != null) {
                        urlInputs[i].setText(getDefaultUrl(i));
                    }
                }
            }
            
            for (int i = 0; i < 4; i++) {
                zoomLevels[i] = preferences.getFloat("zoom_level_" + i, 1.0f);
                applyZoom(i);
            }
            
            if (cbAllowScripts != null) cbAllowScripts.setChecked(preferences.getBoolean("allow_scripts", true));
            if (cbAllowForms != null) cbAllowForms.setChecked(preferences.getBoolean("allow_forms", true));
            if (cbAllowPopups != null) cbAllowPopups.setChecked(preferences.getBoolean("allow_popups", true));
            if (cbBlockRedirects != null) cbBlockRedirects.setChecked(preferences.getBoolean("block_redirects", false));
            if (cbBlockAds != null) cbBlockAds.setChecked(preferences.getBoolean("block_ads", false));
            
            if (!silent) {
                Toast.makeText(this, "✅ Estado carregado!", Toast.LENGTH_SHORT).show();
            }
            
            updateLayout();
            updateFocusedBoxIndicator();
            
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar estado", e);
            if (!silent) {
                Toast.makeText(this, "❌ Erro ao carregar estado", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private String getDefaultUrl(int boxIndex) {
        return "https://dzritv.com/sport/football/";
    }
    
    private void loadFavoritesList() {
        try {
            String favoritesJson = preferences.getString("favorites_list", "[]");
            JSONArray jsonArray = new JSONArray(favoritesJson);
            
            favoritesList.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                String name = jsonArray.getString(i);
                favoritesList.add(name);
            }
            
            Log.d(TAG, "✅ Favoritos carregados com sucesso: " + favoritesList.size() + " itens");
            if (favoritesList.size() > 0) {
                StringBuilder sb = new StringBuilder("Favoritos: ");
                for (String fav : favoritesList) {
                    sb.append("[").append(fav).append("] ");
                }
                Log.d(TAG, sb.toString());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erro ao carregar lista de favoritos", e);
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
            Log.e(TAG, "Erro ao guardar lista de favoritos", e);
        }
    }
    
    private void saveFavorite(String favoriteName) {
        try {
            if (favoritesList.contains(favoriteName)) {
                Toast.makeText(this, "❌ Já existe um favorito com este nome!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            JSONObject favoriteData = new JSONObject();
            favoriteData.put("name", favoriteName);
            
            JSONArray urlsArray = new JSONArray();
            for (int i = 0; i < 4; i++) {
                String url = urlInputs[i] != null ? urlInputs[i].getText().toString().trim() : "";
                if (url.isEmpty()) {
                    url = getDefaultUrl(i);
                }
                urlsArray.put(url);
            }
            favoriteData.put("urls", urlsArray);
            
            favoritesList.add(favoriteName);
            saveFavoritesList();
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("favorite_" + favoriteName, favoriteData.toString());
            editor.apply();
            
            Toast.makeText(this, "✅ Favorito guardado!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Erro ao guardar favorito", e);
            Toast.makeText(this, "❌ Erro ao guardar favorito", Toast.LENGTH_SHORT).show();
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
                for (int i = 0; i < 4 && i < urlsArray.length(); i++) {
                    String url = urlsArray.getString(i);
                    if (urlInputs[i] != null) {
                        urlInputs[i].setText(url);
                    }
                    if (boxEnabled[i] && webViews[i] != null) {
                        loadURL(i, url);
                    }
                }
                Toast.makeText(this, "✅ Favorito carregado em todas as boxes!", Toast.LENGTH_SHORT).show();
            } else {
                if (targetBox < urlsArray.length()) {
                    String url = urlsArray.getString(targetBox);
                    if (urlInputs[targetBox] != null) {
                        urlInputs[targetBox].setText(url);
                    }
                    if (boxEnabled[targetBox] && webViews[targetBox] != null) {
                        loadURL(targetBox, url);
                    }
                    Toast.makeText(this, 
                        "✅ Favorito carregado na Box " + (targetBox + 1), Toast.LENGTH_SHORT).show();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar favorito", e);
            Toast.makeText(this, "❌ Erro ao carregar favorito!", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteFavorite(String favoriteName) {
        try {
            favoritesList.remove(favoriteName);
            saveFavoritesList();
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("favorite_" + favoriteName);
            editor.apply();
            
            Toast.makeText(this, "✅ Favorito removido!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Erro ao remover favorito", e);
            Toast.makeText(this, "❌ Erro ao remover favorito", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showSaveFavoriteDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Nome do favorito");
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        input.setBackgroundResource(android.R.drawable.edit_text);
        input.setCursorVisible(true);
        input.setSelectAllOnFocus(true);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guardar Favorito");
        builder.setView(input);
        
        builder.setPositiveButton("GUARDAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String favoriteName = input.getText().toString().trim();
                if (!favoriteName.isEmpty()) {
                    saveFavorite(favoriteName);
                }
            }
        });
        
        builder.setNegativeButton("CANCELAR", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        input.requestFocus();
        input.selectAll();
    }
    
    /**
     * Alterna a orientação entre portrait e landscape
     */
    private void toggleOrientation() {
        int requestedOrientation;
        
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Está em landscape, mudar para portrait
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            currentOrientation = Configuration.ORIENTATION_PORTRAIT;
            Toast.makeText(this, "📱 Portrait", Toast.LENGTH_SHORT).show();
        } else {
            // Está em portrait, mudar para landscape
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
            Toast.makeText(this, "📺 Landscape", Toast.LENGTH_SHORT).show();
        }
        
        // Forçar a orientação solicitada
        setRequestedOrientation(requestedOrientation);
        
        Log.d(TAG, "Toggle orientacao para: " + (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? "PORTRAIT" : "LANDSCAPE"));
        
        // Atualizar layout imediatamente
        updateLayout();
    }
    
    private void showLoadFavoritesDialog() {
        if (favoritesList.isEmpty()) {
            Toast.makeText(this, "📭 Não há favoritos guardados!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Carregar Favorito");
        
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
        
        String[] options = {"Carregar em Todas", "Carregar em Box 1", "Carregar em Box 2", 
                           "Carregar em Box 3", "Carregar em Box 4", "Eliminar", "Cancelar"};
        
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: loadFavorite(favoriteName, -1); break;
                    case 1: loadFavorite(favoriteName, 0); break;
                    case 2: loadFavorite(favoriteName, 1); break;
                    case 3: loadFavorite(favoriteName, 2); break;
                    case 4: loadFavorite(favoriteName, 3); break;
                    case 5: showDeleteConfirmDialog(favoriteName); break;
                }
            }
        });
        
        builder.show();
    }
    
    private void showDeleteConfirmDialog(final String favoriteName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Eliminação");
        builder.setMessage("Eliminar o favorito '" + favoriteName + "'?");
        
        builder.setPositiveButton("ELIMINAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteFavorite(favoriteName);
            }
        });
        
        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Atualizar a orientacao rastreada
        currentOrientation = newConfig.orientation;
        
        Log.d(TAG, "onConfigurationChanged - Orientacao agora: " + 
            (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ? "PORTRAIT" : "LANDSCAPE"));
        
        // Se há fullscreen ativo, preservar o estado dele
        if (isAnyBoxInFullscreen()) {
            Log.d(TAG, "Fullscreen detectado durante mudança de orientação - preservando estado");
            // Apenas atualizar o sistema UI se necessário
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            // Atualizar o layout com a nova orientacao apenas se não está em fullscreen
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    updateLayout();
                }
            });
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentState();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadFavoritesList();
        if (btnToggleSidebar != null) {
            btnToggleSidebar.requestFocus();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpar cache apenas quando app é completamente fechado
        clearAppCache();
        for (WebView webView : webViews) {
            if (webView != null) {
                webView.stopLoading();
                webView.setWebViewClient(null);
                webView.setWebChromeClient(null);
                webView.destroy();
            }
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (isSidebarVisible) {
                        closeSidebar();
                        return true;
                    }
                    
                    // Check if any box is in fullscreen
                    for (int i = 0; i < 4; i++) {
                        if (boxInFullscreen[i]) {
                            exitFullscreen(i);
                            return true;
                        }
                    }
                    
                    if (webViews[focusedBoxIndex] != null && webViews[focusedBoxIndex].canGoBack()) {
                        webViews[focusedBoxIndex].goBack();
                        return true;
                    }
                    break;
                    
                case KeyEvent.KEYCODE_MENU:
                    if (isSidebarVisible) {
                        closeSidebar();
                    } else {
                        openSidebar();
                    }
                    return true;
                    
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (!isSidebarVisible) {
                        scrollWebView(focusedBoxIndex, -100);
                        return true;
                    }
                    break;
                    
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (!isSidebarVisible) {
                        scrollWebView(focusedBoxIndex, 100);
                        return true;
                    }
                    break;
                    
                case KeyEvent.KEYCODE_PAGE_UP:
                    if (!isSidebarVisible) {
                        scrollWebView(focusedBoxIndex, -500);
                        return true;
                    }
                    break;
                    
                case KeyEvent.KEYCODE_PAGE_DOWN:
                    if (!isSidebarVisible) {
                        scrollWebView(focusedBoxIndex, 500);
                        return true;
                    }
                    break;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void scrollWebView(int boxIndex, int deltaY) {
        WebView webView = webViews[boxIndex];
        if (webView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String scrollJS = "window.scrollBy(0, " + deltaY + ");";
            webView.evaluateJavascript(scrollJS, null);
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isSidebarVisible) {
            closeSidebar();
            return;
        }
        
        // Check if any box is in fullscreen
        for (int i = 0; i < 4; i++) {
            if (boxInFullscreen[i]) {
                exitFullscreen(i);
                return;
            }
        }
        
        if (webViews[focusedBoxIndex] != null && webViews[focusedBoxIndex].canGoBack()) {
            webViews[focusedBoxIndex].goBack();
            return;
        }
        
        for (WebView webView : webViews) {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
                return;
            }
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Sair do App")
            .setMessage("Deseja sair?")
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