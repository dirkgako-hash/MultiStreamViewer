package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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
    private FrameLayout sidebarContainer;
    private RelativeLayout mainLayout;
    
    private Button btnMenu;
    private Button btnCloseMenu, btnLoadAll, btnReloadAll, btnClearAll;
    private Button btnSaveState, btnLoadState, btnSaveFavorites, btnLoadFavorites;
    private Button[] btnRefresh = new Button[4];
    private CheckBox[] cbAutoReload = new CheckBox[4];
    private Button[] btnZoomIn = new Button[4];
    private Button[] btnZoomOut = new Button[4];
    private Button[] btnPrevious = new Button[4];
    private Button[] btnNext = new Button[4];
    private Button[] btnLoadUrl = new Button[4];
    private CheckBox[] checkBoxes = new CheckBox[4];
    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups, cbBlockRedirects, cbBlockAds;
    private EditText[] urlInputs = new EditText[4];
    private TextView tvFocusedBox;
    
    private boolean[] boxEnabled = {true, true, true, true};
    private boolean[] autoReloadEnabled = {false, false, false, false};
    private boolean isSidebarVisible = false;
    private int focusedBoxIndex = 0;
    private boolean isVideoMuted = true;
    private float[] zoomLevels = {1.0f, 1.0f, 1.0f, 1.0f};
    
    private ArrayList<String> favoritesList = new ArrayList<>();
    private SharedPreferences preferences;
    
    private Handler autoReloadHandler = new Handler();
    private Runnable autoReloadRunnable;
    private final long AUTO_RELOAD_INTERVAL = 5000;
    
    // VARIÁVEIS PARA MONITORAMENTO DE BUFFER
    private Handler[] bufferHandlers = new Handler[4];
    private Runnable[] bufferCheckRunnables = new Runnable[4];
    private boolean[] isBuffering = new boolean[4];
    private float[] playbackRates = {1.0f, 1.0f, 1.0f, 1.0f};
    
    // VARIÁVEIS PARA CONTROLE DE SCROLL
    private int[] currentScrollY = {0, 0, 0, 0};
    private int SCROLL_STEP = 100;
    
    // CONFIGURAÇÕES DE BUFFER
    private static final long BUFFER_CHECK_INTERVAL = 2000; // 2 segundos
    private static final double BUFFER_CRITICAL_THRESHOLD = 1.5; // segundos
    private static final double BUFFER_LOW_THRESHOLD = 3.0; // segundos
    
    private final List<String> adDomains = Arrays.asList(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com"
    );

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
        
        preferences = getSharedPreferences("MultiStreamViewer", MODE_PRIVATE);
        
        // Configurar fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        
        initViews();
        initWebViews();
        initEventListeners();
        
        loadSavedState(true);
        loadFavoritesList();
        updateLayout();
        updateFocusedBoxIndicator();
        
        // Iniciar auto-reload monitoring
        startAutoReloadMonitoring();
        
        if (!hasSavedState()) {
            new Handler().postDelayed(this::loadInitialURLs, 1000);
        }
    }
    
    private void initViews() {
        gridLayout = findViewById(R.id.gridLayout);
        bottomControls = findViewById(R.id.bottomControls);
        sidebarContainer = findViewById(R.id.sidebarContainer);
        mainLayout = findViewById(R.id.main_layout);
        tvFocusedBox = findViewById(R.id.tvFocusedBox);
        
        btnMenu = findViewById(R.id.btnMenu);
        
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
        
        // Inicializar botões Refresh
        btnRefresh[0] = findViewById(R.id.btnRefresh1);
        btnRefresh[1] = findViewById(R.id.btnRefresh2);
        btnRefresh[2] = findViewById(R.id.btnRefresh3);
        btnRefresh[3] = findViewById(R.id.btnRefresh4);
        
        // Inicializar checkboxes auto-reload - COM VERIFICAÇÃO DE NULL
        cbAutoReload[0] = findViewById(R.id.cbAutoReload1);
        cbAutoReload[1] = findViewById(R.id.cbAutoReload2);
        cbAutoReload[2] = findViewById(R.id.cbAutoReload3);
        cbAutoReload[3] = findViewById(R.id.cbAutoReload4);
        
        // Verificar se as checkboxes auto-reload foram encontradas
        for (int i = 0; i < 4; i++) {
            if (cbAutoReload[i] == null) {
                Toast.makeText(this, "Aviso: Checkbox AutoReload " + (i+1) + " não encontrada", Toast.LENGTH_SHORT).show();
            }
        }
        
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
        
        // Verificar checkboxes de configuração
        if (cbAllowScripts == null) Toast.makeText(this, "cbAllowScripts não encontrada", Toast.LENGTH_SHORT).show();
        if (cbAllowForms == null) Toast.makeText(this, "cbAllowForms não encontrada", Toast.LENGTH_SHORT).show();
        if (cbAllowPopups == null) Toast.makeText(this, "cbAllowPopups não encontrada", Toast.LENGTH_SHORT).show();
        if (cbBlockRedirects == null) Toast.makeText(this, "cbBlockRedirects não encontrada", Toast.LENGTH_SHORT).show();
        if (cbBlockAds == null) Toast.makeText(this, "cbBlockAds não encontrada", Toast.LENGTH_SHORT).show();
        
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
    
    private void startAutoReloadMonitoring() {
        autoReloadRunnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 4; i++) {
                    if (boxEnabled[i] && autoReloadEnabled[i] && webViews[i] != null) {
                        checkAndFixStuckVideo(i);
                    }
                }
                autoReloadHandler.postDelayed(this, AUTO_RELOAD_INTERVAL);
            }
        };
        autoReloadHandler.postDelayed(autoReloadRunnable, AUTO_RELOAD_INTERVAL);
    }
    
    private void checkAndFixStuckVideo(final int boxIndex) {
        WebView webView = webViews[boxIndex];
        if (webView != null) {
            String checkVideoJS = 
                "try {" +
                "   var videos = document.getElementsByTagName('video');" +
                "   var videoStatus = {hasVideo: false, hasError: false, isPaused: false, isEnded: false, isPlaying: false, bufferAhead: 0};" +
                "   for(var i = 0; i < videos.length; i++) {" +
                "       var video = videos[i];" +
                "       videoStatus.hasVideo = true;" +
                "       if(video.error) {" +
                "           videoStatus.hasError = true;" +
                "       } else if(video.paused && !video.ended) {" +
                "           videoStatus.isPaused = true;" +
                "       } else if(video.ended) {" +
                "           videoStatus.isEnded = true;" +
                "       } else {" +
                "           videoStatus.isPlaying = true;" +
                "       }" +
                "       " +
                "       // Verificar buffer" +
                "       if(video.buffered.length > 0) {" +
                "           var bufferedEnd = video.buffered.end(video.buffered.length - 1);" +
                "           videoStatus.bufferAhead = bufferedEnd - video.currentTime;" +
                "       }" +
                "   }" +
                "   JSON.stringify(videoStatus);" +
                "} catch(e) { '{\"hasVideo\":false}' }";
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(checkVideoJS, value -> {
                    try {
                        String jsonStr = value.replace("\"", "").replace("\\\"", "\"");
                        JSONObject videoStatus = new JSONObject(jsonStr);
                        
                        boolean hasVideo = videoStatus.getBoolean("hasVideo");
                        boolean hasError = videoStatus.getBoolean("hasError");
                        boolean isPaused = videoStatus.getBoolean("isPaused");
                        double bufferAhead = videoStatus.optDouble("bufferAhead", 0);
                        
                        if (hasVideo && (hasError || (isPaused && bufferAhead < 1.0))) {
                            runOnUiThread(() -> {
                                reloadPageAndForceFullscreen(boxIndex);
                            });
                        } else if (hasVideo && bufferAhead < BUFFER_CRITICAL_THRESHOLD) {
                            // Buffer crítico, ajustar playback rate
                            adjustPlaybackRate(boxIndex, 0.8f);
                        }
                    } catch (Exception e) {
                        // Ignorar erros
                    }
                });
            }
        }
    }
    
    private void reloadPageAndForceFullscreen(int boxIndex) {
        WebView webView = webViews[boxIndex];
        if (webView != null) {
            String currentUrl = webView.getUrl();
            if (currentUrl != null && !currentUrl.equals("about:blank")) {
                webView.reload();
                
                new Handler().postDelayed(() -> {
                    String forceFullscreenJS = 
                        "try {" +
                        "   var videos = document.getElementsByTagName('video');" +
                        "   for(var i = 0; i < videos.length; i++) {" +
                        "       var video = videos[i];" +
                        "       video.style.width = '100%';" +
                        "       video.style.height = '100%';" +
                        "       video.style.position = 'absolute';" +
                        "       video.style.top = '0';" +
                        "       video.style.left = '0';" +
                        "       video.style.zIndex = '9999';" +
                        "       video.setAttribute('playsinline', 'false');" +
                        "       video.setAttribute('webkit-playsinline', 'false');" +
                        "       if(video.paused && !video.ended) {" +
                        "           video.play();" +
                        "       }" +
                        "   }" +
                        "   document.body.style.overflow = 'hidden';" +
                        "} catch(e) {}";
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        webView.evaluateJavascript(forceFullscreenJS, null);
                    }
                }, 2000);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Box " + (boxIndex + 1) + ": Auto-reload ativado", Toast.LENGTH_SHORT).show();
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
        sidebarContainer.setVisibility(View.GONE);
        isSidebarVisible = false;
        
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) gridLayout.getLayoutParams();
        params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        params.removeRule(RelativeLayout.LEFT_OF);
        gridLayout.setLayoutParams(params);
        
        hideKeyboard();
        btnMenu.requestFocus();
    }
    
    private void openSidebar() {
        sidebarContainer.setVisibility(View.VISIBLE);
        isSidebarVisible = true;
        
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) gridLayout.getLayoutParams();
        params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        params.addRule(RelativeLayout.LEFT_OF, R.id.sidebarContainer);
        gridLayout.setLayoutParams(params);
        
        btnCloseMenu.requestFocus();
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViews() {
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
            
            webViews[i] = new WebView(this);
            webViews[i].setId(View.generateViewId());
            setupWebView(webViews[i], boxIndex);
            
            boxContainers[i].addView(webViews[i], 
                new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            
            boxContainers[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isSidebarVisible) return;
                    boxContainers[boxIndex].requestFocus();
                }
            });
            
            gridLayout.addView(boxContainers[i]);
        }
        
        btnMenu.requestFocus();
    }
    
    private void updateFocusedBoxIndicator() {
        tvFocusedBox.setText("Foco: " + (focusedBoxIndex + 1));
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView, int boxIndex) {
        WebSettings settings = webView.getSettings();
        
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        // CONFIGURAÇÕES AVANÇADAS PARA STREAMING
        // Usar LOAD_DEFAULT que usa cache normalmente
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Habilitar banco de dados (se disponível)
        settings.setDatabaseEnabled(true);
        
        // OTIMIZAÇÕES PARA STREAMING
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // HABILITAR SUPPORT PARA MÍDIA
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(false);
        }
        
        // BUFFER PARA VÍDEO
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        
        // USER AGENT OTIMIZADO PARA STREAMING
        String userAgent = "Mozilla/5.0 (Linux; Android 10; AFTMM) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Safari/537.36";
        settings.setUserAgentString(userAgent);
        
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        
        if (cbBlockAds != null) {
            settings.setBlockNetworkLoads(cbBlockAds.isChecked());
            settings.setBlockNetworkImage(cbBlockAds.isChecked());
        }
        
        settings.setTextZoom((int)(zoomLevels[boxIndex] * 100));
        webView.setInitialScale((int)(zoomLevels[boxIndex] * 100));
        
        webView.setBackgroundColor(Color.BLACK);
        
        // HABILITAR SCROLL VERTICAL
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        
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
                
                return false;
            }
            
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // PRIORIZAR REQUISIÇÕES DE STREAMING
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString().toLowerCase();
                    if (url.contains(".m3u8") || url.contains(".mp4") || url.contains(".ts") || 
                        url.contains(".m4s") || url.contains(".mpd") || url.contains(".webm")) {
                        Log.d("StreamPriority", "Prioritizing streaming request: " + url);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
            
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (cbBlockAds != null && cbBlockAds.isChecked()) {
                    injectAdBlocker(view);
                }
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                // SCRIPT COMPLETO PARA CONTROLE DE BUFFER E SCROLL
                String videoOptimizationJS = 
                    "try {" +
                    "   // CONFIGURAR ELEMENTOS DE VÍDEO" +
                    "   var videos = document.getElementsByTagName('video');" +
                    "   for(var i = 0; i < videos.length; i++) {" +
                    "       var video = videos[i];" +
                    "       video.muted = " + isVideoMuted + ";" +
                    "       video.setAttribute('playsinline', 'false');" +
                    "       video.setAttribute('webkit-playsinline', 'false');" +
                    "       video.controls = true;" +
                    "       video.preload = 'auto';" +
                    "       video.autobuffer = true;" +
                    "       video.defaultPlaybackRate = 1.0;" +
                    "       video.playbackRate = 1.0;" +
                    "       video.style.width = '100%';" +
                    "       video.style.height = '100%';" +
                    "       video.style.position = 'absolute';" +
                    "       video.style.top = '0';" +
                    "       video.style.left = '0';" +
                    "       video.style.zIndex = '9999';" +
                    
                    // MONITORAR ESTADO DO BUFFER
                    "       video.addEventListener('waiting', function(e) {" +
                    "           console.log('Video waiting - buffer empty at ' + this.currentTime);" +
                    "           if(!this.paused) {" +
                    "               this.pause();" +
                    "               var self = this;" +
                    "               setTimeout(function() {" +
                    "                   if(self.readyState >= 2 && self.buffered.length > 0) {" +
                    "                       var bufferEnd = self.buffered.end(self.buffered.length - 1);" +
                    "                       if(bufferEnd - self.currentTime > 2) {" +
                    "                           self.play().catch(function(err) {" +
                    "                               console.log('Retry play failed:', err);" +
                    "                           });" +
                    "                       }" +
                    "                   }" +
                    "               }, 1500);" +
                    "           }" +
                    "       });" +
                    
                    "       video.addEventListener('playing', function() {" +
                    "           console.log('Video playing, buffer: ' + " +
                    "               (this.buffered.length > 0 ? this.buffered.end(this.buffered.length - 1) - this.currentTime : 0) + 's');" +
                    "       });" +
                    
                    "       video.addEventListener('progress', function() {" +
                    "           if(this.buffered.length > 0) {" +
                    "               var bufferedEnd = this.buffered.end(this.buffered.length - 1);" +
                    "               var currentTime = this.currentTime;" +
                    "               var bufferAhead = bufferedEnd - currentTime;" +
                    "               " +
                    "               // AJUSTE ADAPTATIVO DE QUALIDADE" +
                    "               if(bufferAhead < 2.0 && this.playbackRate > 0.7) {" +
                    "                   this.playbackRate = Math.max(0.7, this.playbackRate - 0.1);" +
                    "                   console.log('Reducing playback rate to ' + this.playbackRate + ', buffer: ' + bufferAhead + 's');" +
                    "               } else if(bufferAhead > 8.0 && this.playbackRate < 1.0) {" +
                    "                   this.playbackRate = Math.min(1.0, this.playbackRate + 0.05);" +
                    "                   console.log('Increasing playback rate to ' + this.playbackRate + ', buffer: ' + bufferAhead + 's');" +
                    "               }" +
                    "           }" +
                    "       });" +
                    
                    "       video.addEventListener('error', function(e) {" +
                    "           console.error('Video error:', this.error);" +
                    "           if(this.error && this.error.code === 4) {" +
                    "               setTimeout(function() {" +
                    "                   this.load();" +
                    "               }.bind(this), 3000);" +
                    "           }" +
                    "       });" +
                    
                    // TENTAR REPRODUZIR AUTOMATICAMENTE
                    "       if(video.paused && !video.ended && video.readyState >= 2) {" +
                    "           setTimeout(function() {" +
                    "               video.play().catch(function(e) {" +
                    "                   console.log('Auto-play failed, trying muted:', e);" +
                    "                   video.muted = true;" +
                    "                   video.play().catch(function(e2) {" +
                    "                       console.log('Muted play also failed:', e2);" +
                    "                   });" +
                    "               });" +
                    "           }, 1000);" +
                    "       }" +
                    "   }" +
                    
                    // CONFIGURAR ÁUDIO
                    "   var audios = document.getElementsByTagName('audio');" +
                    "   for(var i = 0; i < audios.length; i++) {" +
                    "       audios[i].preload = 'auto';" +
                    "       audios[i].autobuffer = true;" +
                    "   }" +
                    
                    // HABILITAR SCROLL VERTICAL
                    "   document.body.style.overflowY = 'auto';" +
                    "   document.body.style.overflowX = 'hidden';" +
                    "   document.body.style.height = 'auto';" +
                    "   document.body.style.minHeight = '100vh';" +
                    "   document.body.style.margin = '0';" +
                    "   document.body.style.padding = '0';" +
                    "   document.documentElement.style.overflowY = 'auto';" +
                    "   document.documentElement.style.overflowX = 'hidden';" +
                    
                    // INJETAR ADAPTIVE STREAMING CONTROLLER
                    "   window.StreamController = {" +
                    "       checkInterval: null," +
                    "       checkBuffer: function() {" +
                    "           var videos = document.getElementsByTagName('video');" +
                    "           for(var i = 0; i < videos.length; i++) {" +
                    "               var v = videos[i];" +
                    "               if(v.buffered.length > 0 && !v.paused && !v.ended) {" +
                    "                   var bufferEnd = v.buffered.end(v.buffered.length - 1);" +
                    "                   var bufferAhead = bufferEnd - v.currentTime;" +
                    "                   " +
                    "                   if(bufferAhead < 1.0) {" +
                    "                       console.warn('CRITICAL BUFFER: ' + bufferAhead + 's');" +
                    "                       v.playbackRate = 0.8;" +
                    "                       if(!v.paused) v.pause();" +
                    "                       setTimeout(function() {" +
                    "                           if(v.readyState >= 2) v.play();" +
                    "                       }, 2000);" +
                    "                   } else if(bufferAhead < 3.0 && v.playbackRate > 0.9) {" +
                    "                       v.playbackRate = 0.9;" +
                    "                   }" +
                    "               }" +
                    "           }" +
                    "       }," +
                    "       start: function() {" +
                    "           if(this.checkInterval) clearInterval(this.checkInterval);" +
                    "           this.checkInterval = setInterval(this.checkBuffer, 3000);" +
                    "       }," +
                    "       stop: function() {" +
                    "           if(this.checkInterval) clearInterval(this.checkInterval);" +
                    "       }" +
                    "   };" +
                    "   window.StreamController.start();" +
                    "} catch(e) { console.error('Video optimization error:', e); }";
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    view.evaluateJavascript(videoOptimizationJS, null);
                }
                
                applyZoom(boxIndex);
                
                if (cbBlockAds != null && cbBlockAds.isChecked()) {
                    injectAdBlocker(view);
                }
                
                // INICIAR MONITORAMENTO DE BUFFER
                startBufferMonitoring(boxIndex);
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;
            
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                mCustomView = view;
                mCustomViewCallback = callback;
                
                boxContainers[boxIndex].addView(view, 
                    new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                
                webView.setVisibility(View.GONE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            
            @Override
            public void onHideCustomView() {
                if (mCustomView == null) return;
                
                boxContainers[boxIndex].removeView(mCustomView);
                webView.setVisibility(View.VISIBLE);
                
                if (mCustomViewCallback != null) {
                    mCustomViewCallback.onCustomViewHidden();
                }
                
                mCustomView = null;
                mCustomViewCallback = null;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }
            
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
            }
        });
    }
    
    private void applyZoom(int boxIndex) {
        WebView webView = webViews[boxIndex];
        if (webView != null) {
            String zoomJS = "document.body.style.zoom = '" + (zoomLevels[boxIndex] * 100) + "%';";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(zoomJS, null);
            }
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
        // Verificar se btnMenu existe antes de adicionar listener
        if (btnMenu != null) {
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
        }
        
        if (btnCloseMenu != null) {
            btnCloseMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeSidebar();
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
            
            // Checkbox para ativar/desativar box - COM VERIFICAÇÃO DE NULL
            if (checkBoxes[i] != null) {
                checkBoxes[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        boxEnabled[boxIndex] = isChecked;
                        updateLayout();
                    }
                });
            }
            
            // Checkbox Auto Reload - COM VERIFICAÇÃO DE NULL
            if (cbAutoReload[i] != null) {
                cbAutoReload[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        autoReloadEnabled[boxIndex] = isChecked;
                        if (isChecked) {
                            startBufferMonitoring(boxIndex);
                        } else {
                            stopBufferMonitoring(boxIndex);
                        }
                        Toast.makeText(MainActivity.this, 
                            "Box " + (boxIndex + 1) + " auto-reload: " + (isChecked ? "ON" : "OFF"), 
                            Toast.LENGTH_SHORT).show();
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
        
        // Configurações Web - COM VERIFICAÇÃO DE NULL
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
        
        switch (activeBoxes) {
            case 1:
                rows = 1; cols = 1;
                break;
            case 2:
                rows = 1; cols = 2;
                break;
            case 3:
                rows = 1; cols = 3;
                break;
            case 4:
                rows = 2; cols = 2;
                break;
            default:
                rows = 1; cols = 1;
                break;
        }
        
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
                    
                    // Iniciar monitoramento de buffer se a box estiver ativa
                    if (autoReloadEnabled[i]) {
                        startBufferMonitoring(i);
                    }
                }
                position++;
            } else {
                if (boxContainers[i] != null) {
                    boxContainers[i].setVisibility(View.GONE);
                    // Parar monitoramento de buffer se a box estiver desativada
                    stopBufferMonitoring(i);
                }
            }
        }
        
        gridLayout.requestLayout();
    }
    
    // ==============================
    // MÉTODOS DE CONTROLE DE BUFFER
    // ==============================
    
    private void startBufferMonitoring(final int boxIndex) {
        if (!boxEnabled[boxIndex]) return;
        
        if (bufferHandlers[boxIndex] == null) {
            bufferHandlers[boxIndex] = new Handler();
        }
        
        // Parar monitoramento anterior se existir
        if (bufferCheckRunnables[boxIndex] != null) {
            bufferHandlers[boxIndex].removeCallbacks(bufferCheckRunnables[boxIndex]);
        }
        
        bufferCheckRunnables[boxIndex] = new Runnable() {
            @Override
            public void run() {
                if (boxEnabled[boxIndex] && autoReloadEnabled[boxIndex]) {
                    checkVideoBufferStatus(boxIndex);
                    bufferHandlers[boxIndex].postDelayed(this, BUFFER_CHECK_INTERVAL);
                }
            }
        };
        
        bufferHandlers[boxIndex].postDelayed(bufferCheckRunnables[boxIndex], BUFFER_CHECK_INTERVAL);
        Log.d("BufferMonitor", "Started buffer monitoring for box " + (boxIndex + 1));
    }
    
    private void stopBufferMonitoring(int boxIndex) {
        if (bufferHandlers[boxIndex] != null && bufferCheckRunnables[boxIndex] != null) {
            bufferHandlers[boxIndex].removeCallbacks(bufferCheckRunnables[boxIndex]);
            bufferCheckRunnables[boxIndex] = null;
            isBuffering[boxIndex] = false;
        }
    }
    
    private void checkVideoBufferStatus(final int boxIndex) {
        WebView webView = webViews[boxIndex];
        if (webView == null || !boxEnabled[boxIndex]) return;
        
        String checkBufferJS = 
            "try {" +
            "   var videos = document.getElementsByTagName('video');" +
            "   if(videos.length === 0) return null;" +
            "   " +
            "   var video = videos[0];" +
            "   var status = {" +
            "       hasVideo: true," +
            "       isPlaying: !video.paused && !video.ended," +
            "       isBuffering: video.readyState < 3," +
            "       currentTime: video.currentTime," +
            "       duration: video.duration," +
            "       playbackRate: video.playbackRate," +
            "       networkState: video.networkState," +
            "       readyState: video.readyState" +
            "   };" +
            "   " +
            "   if(video.buffered.length > 0) {" +
            "       var bufferedEnd = video.buffered.end(video.buffered.length - 1);" +
            "       status.bufferAhead = bufferedEnd - video.currentTime;" +
            "       status.bufferedEnd = bufferedEnd;" +
            "       status.bufferedLength = video.buffered.length;" +
            "   } else {" +
            "       status.bufferAhead = 0;" +
            "   }" +
            "   " +
            "   JSON.stringify(status);" +
            "} catch(e) { return null; }";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(checkBufferJS, value -> {
                try {
                    if (value == null || "null".equals(value)) return;
                    
                    String jsonStr = value.replace("\\\"", "\"");
                    if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                        jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
                    }
                    
                    JSONObject status = new JSONObject(jsonStr);
                    boolean hasVideo = status.optBoolean("hasVideo", false);
                    boolean isPlaying = status.optBoolean("isPlaying", false);
                    boolean isBufferingJS = status.optBoolean("isBuffering", false);
                    double bufferAhead = status.optDouble("bufferAhead", 0);
                    double playbackRate = status.optDouble("playbackRate", 1.0);
                    
                    playbackRates[boxIndex] = (float) playbackRate;
                    
                    runOnUiThread(() -> {
                        if (hasVideo && isPlaying) {
                            if (bufferAhead < BUFFER_CRITICAL_THRESHOLD && !isBuffering[boxIndex]) {
                                handleCriticalBuffer(boxIndex, bufferAhead);
                            } else if (bufferAhead < BUFFER_LOW_THRESHOLD && playbackRate > 0.8f) {
                                adjustPlaybackRate(boxIndex, 0.8f);
                            }
                        } else if (hasVideo && !isPlaying && bufferAhead > 5.0) {
                            // Tem buffer mas não está tocando
                            triggerVideoPlay(boxIndex);
                        }
                    });
                    
                } catch (Exception e) {
                    // Ignorar erros de parsing
                }
            });
        }
    }
    
    private void handleCriticalBuffer(int boxIndex, double bufferAhead) {
        if (isBuffering[boxIndex]) return;
        
        isBuffering[boxIndex] = true;
        Log.w("BufferMonitor", "Box " + (boxIndex + 1) + ": Critical buffer (" + bufferAhead + "s)");
        
        // Estratégia 1: Reduzir velocidade
        adjustPlaybackRate(boxIndex, 0.7f);
        
        // Estratégia 2: Pausar brevemente para acumular buffer
        injectJavaScript(boxIndex,
            "try {" +
            "   var v = document.getElementsByTagName('video')[0];" +
            "   if(v && !v.paused) {" +
            "       v.pause();" +
            "       console.log('Pausing for buffer accumulation');" +
            "       setTimeout(function() {" +
            "           if(v.readyState >= 2 && v.buffered.length > 0) {" +
            "               var bufferEnd = v.buffered.end(v.buffered.length - 1);" +
            "               if(bufferEnd - v.currentTime > 2) {" +
            "                   v.play().catch(function(e) {" +
            "                       console.log('Buffer recovery play failed:', e);" +
            "                   });" +
            "               }" +
            "           }" +
            "       }, 2000);" +
            "   }" +
            "} catch(e) {}");
        
        // Resetar flag após 5 segundos
        new Handler().postDelayed(() -> {
            isBuffering[boxIndex] = false;
        }, 5000);
    }
    
    private void adjustPlaybackRate(int boxIndex, float rate) {
        playbackRates[boxIndex] = rate;
        injectJavaScript(boxIndex,
            "try {" +
            "   var videos = document.getElementsByTagName('video');" +
            "   for(var i = 0; i < videos.length; i++) {" +
            "       videos[i].playbackRate = " + rate + ";" +
            "   }" +
            "} catch(e) {}");
        
        runOnUiThread(() -> {
            Toast.makeText(this, "Box " + (boxIndex + 1) + " playback: " + (rate * 100) + "%", 
                Toast.LENGTH_SHORT).show();
        });
    }
    
    private void triggerVideoPlay(int boxIndex) {
        injectJavaScript(boxIndex,
            "try {" +
            "   var videos = document.getElementsByTagName('video');" +
            "   for(var i = 0; i < videos.length; i++) {" +
            "       var v = videos[i];" +
            "       if(v.paused && !v.ended && v.readyState >= 2) {" +
            "           v.play().catch(function(e) {" +
            "               console.log('Auto-play trigger failed:', e);" +
            "           });" +
            "       }" +
            "   }" +
            "} catch(e) {}");
    }
    
    private void injectJavaScript(int boxIndex, String javascript) {
        WebView webView = webViews[boxIndex];
        if (webView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(javascript, null);
        }
    }
    
    // ==============================
    // MÉTODOS DE CONTROLE DE SCROLL
    // ==============================
    
    private void scrollWebView(int boxIndex, int deltaY) {
        WebView webView = webViews[boxIndex];
        if (webView == null) return;
        
        currentScrollY[boxIndex] += deltaY;
        if (currentScrollY[boxIndex] < 0) currentScrollY[boxIndex] = 0;
        
        String scrollJS = 
            "try {" +
            "   window.scrollTo(0, " + currentScrollY[boxIndex] + ");" +
            "} catch(e) {}";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(scrollJS, null);
        }
    }
    
    private void scrollToTop(int boxIndex) {
        currentScrollY[boxIndex] = 0;
        scrollWebView(boxIndex, 0);
    }
    
    private void scrollToBottom(int boxIndex) {
        injectJavaScript(boxIndex,
            "try {" +
            "   var height = Math.max(" +
            "       document.body.scrollHeight," +
            "       document.body.offsetHeight," +
            "       document.documentElement.clientHeight," +
            "       document.documentElement.scrollHeight," +
            "       document.documentElement.offsetHeight" +
            "   );" +
            "   window.scrollTo(0, height);" +
            "   return height;" +
            "} catch(e) { return 0; }");
    }
    
    private void loadAllURLs() {
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i]) {
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
    
    private void loadURL(int boxIndex, String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            if (webViews[boxIndex] != null) {
                webViews[boxIndex].loadUrl(url);
            }
        } catch (Exception e) {
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
                editor.putBoolean("auto_reload_" + i, autoReloadEnabled[i]);
                editor.putFloat("playback_rate_" + i, playbackRates[i]);
            }
            
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
            Toast.makeText(this, "❌ Erro ao guardar estado", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadSavedState(boolean silent) {
        try {
            boolean hasSavedUrls = false;
            for (int i = 0; i < 4; i++) {
                String savedUrl = preferences.getString("url_" + i, "");
                if (!savedUrl.isEmpty()) {
                    hasSavedUrls = true;
                    if (urlInputs[i] != null) {
                        urlInputs[i].setText(savedUrl);
                    }
                    if (boxEnabled[i] && webViews[i] != null) {
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
                boolean savedState = preferences.getBoolean("box_enabled_" + i, true);
                boxEnabled[i] = savedState;
                if (checkBoxes[i] != null) {
                    checkBoxes[i].setChecked(savedState);
                }
                
                boolean savedAutoReload = preferences.getBoolean("auto_reload_" + i, false);
                autoReloadEnabled[i] = savedAutoReload;
                if (cbAutoReload[i] != null) {
                    cbAutoReload[i].setChecked(savedAutoReload);
                }
                
                playbackRates[i] = preferences.getFloat("playback_rate_" + i, 1.0f);
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
        if (newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        updateLayout();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentState();
        if (autoReloadHandler != null && autoReloadRunnable != null) {
            autoReloadHandler.removeCallbacks(autoReloadRunnable);
        }
        
        // Parar todos os buffer monitors
        for (int i = 0; i < 4; i++) {
            stopBufferMonitoring(i);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadFavoritesList();
        if (btnMenu != null) {
            btnMenu.requestFocus();
        }
        if (autoReloadHandler != null && autoReloadRunnable != null) {
            autoReloadHandler.postDelayed(autoReloadRunnable, AUTO_RELOAD_INTERVAL);
        }
        
        // Reiniciar buffer monitors para boxes ativas
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i] && autoReloadEnabled[i]) {
                startBufferMonitoring(i);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoReloadHandler != null && autoReloadRunnable != null) {
            autoReloadHandler.removeCallbacks(autoReloadRunnable);
        }
        
        // Parar todos os buffer monitors
        for (int i = 0; i < 4; i++) {
            stopBufferMonitoring(i);
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
                    
                // CONTROLES DE SCROLL COM TECLADO
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (!isSidebarVisible) {
                        scrollWebView(focusedBoxIndex, -SCROLL_STEP);
                        return true;
                    }
                    break;
                    
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (!isSidebarVisible) {
                        scrollWebView(focusedBoxIndex, SCROLL_STEP);
                        return true;
                    }
                    break;
                    
                case KeyEvent.KEYCODE_PAGE_UP:
                    if (!isSidebarVisible) {
                        scrollWebView(focusedBoxIndex, -SCROLL_STEP * 5);
                        return true;
                    }
                    break;
                    
                case KeyEvent.KEYCODE_PAGE_DOWN:
                    if (!isSidebarVisible) {
                        scrollWebView(focusedBoxIndex, SCROLL_STEP * 5);
                        return true;
                    }
                    break;
                    
                case KeyEvent.KEYCODE_MOVE_HOME:
                    if (!isSidebarVisible) {
                        scrollToTop(focusedBoxIndex);
                        return true;
                    }
                    break;
                    
                case KeyEvent.KEYCODE_MOVE_END:
                    if (!isSidebarVisible) {
                        scrollToBottom(focusedBoxIndex);
                        return true;
                    }
                    break;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public void onBackPressed() {
        if (isSidebarVisible) {
            closeSidebar();
            return;
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
