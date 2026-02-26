package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  DESIGN PRINCIPLES                                                   ║
 * ║                                                                      ║
 * ║  RULE 1 – WebViews are created ONCE in onCreate and never destroyed  ║
 * ║           or removed from their containers.  Video state is always   ║
 * ║           preserved regardless of orientation changes or boxes being ║
 * ║           shown/hidden.                                              ║
 * ║                                                                      ║
 * ║  RULE 2 – updateLayout() is the ONLY method that touches the grid.  ║
 * ║           It only changes GridLayout LayoutParams + setVisibility(). ║
 * ║           It never touches WebViews.                                 ║
 * ║                                                                      ║
 * ║  RULE 3 – "Fullbox" is NOT a special state. It simply means 1 box   ║
 * ║           is enabled. The WebView always fills its container 100%    ║
 * ║           (MATCH_PARENT × MATCH_PARENT).                             ║
 * ║                                                                      ║
 * ║  RULE 4 – AndroidManifest.xml MUST have on the <activity>:          ║
 * ║           android:configChanges="orientation|screenSize|             ║
 * ║               screenLayout|keyboardHidden|smallestScreenSize"        ║
 * ║           This prevents Activity recreation on rotation.             ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
public class MainActivity extends AppCompatActivity {





    // ── Views ──────────────────────────────────────────────────────────────────
    private GridLayout     gridLayout;
    private FrameLayout[]  boxContainers = new FrameLayout[4];
    private WebView[]      webViews      = new WebView[4];

    // 🔥 Fullscreen video tracking
    private View[] customViews = new View[4];
    private WebChromeClient.CustomViewCallback[] customCallbacks = new WebChromeClient.CustomViewCallback[4];

    private LinearLayout   bottomControls;
    private LinearLayout   expandedBottomBar;
    private FrameLayout    sidebarContainer;
    private RelativeLayout mainLayout;
    private TextView       tvFocusedBox;

    private Button btnToggleBottomBar, btnToggleSidebar, btnToggleOrientation;
    private Button btnCloseMenu, btnCloseSidebar;
    private Button btnLoadAll, btnReloadAll, btnClearAll;
    private Button btnSaveState, btnLoadState, btnSaveFavorites, btnLoadFavorites;

    // Per-box controls (bottom bar)
    private Button[]   btnRefresh  = new Button[4];
    private Button[]   btnZoomIn   = new Button[4];
    private Button[]   btnZoomOut  = new Button[4];
    private Button[]   btnPrevious = new Button[4];
    private Button[]   btnNext     = new Button[4];
    private Button[]   btnLoadUrl  = new Button[4];
    private EditText[] urlInputs   = new EditText[4];
    private CheckBox[] checkBoxes           = new CheckBox[4];
    private CheckBox[] checkBoxesKeepActive = new CheckBox[4];

    // Sidebar controls (unique IDs – no duplicate-ID build error)
    private EditText[] urlInputsSidebar  = new EditText[4];
    private Button[]   btnLoadUrlSidebar = new Button[4];
    private Button btnLoadAllSidebar, btnReloadAllSidebar, btnClearAllSidebar;
    private Button btnSaveStateSidebar, btnLoadStateSidebar;
    private Button btnSaveFavoritesSidebar, btnLoadFavoritesSidebar;

    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups, cbBlockRedirects, cbBlockAds;

    // ── State ──────────────────────────────────────────────────────────────────
    //
    //  boxEnabled[i]    = true  → container VISIBLE   (in grid)
    //  boxEnabled[i]    = false + boxKeepActive[i]=true  → INVISIBLE (playing bg)
    //  boxEnabled[i]    = false + boxKeepActive[i]=false → GONE
    //
    private boolean[] boxEnabled    = {true, true, true, true};
    private boolean[] boxKeepActive = {true, true, true, true};

    private boolean isSidebarVisible    = false;
    private boolean isBottomBarExpanded = false;
    private boolean isSyncingUI         = false;  // Flag para evitar disparar listeners durante sincronização
    private int     focusedBoxIndex     = 0;
    private float[] zoomLevels          = {1.0f, 1.0f, 1.0f, 1.0f};
    private int     currentOrientation  = Configuration.ORIENTATION_LANDSCAPE;

    private ArrayList<String> favoritesList = new ArrayList<>();
    private SharedPreferences preferences;

    private final List<String> adDomains = Arrays.asList(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com");

    private static final String TAG = "MSV";

    // ══════════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressLint({"SetJavaScriptEnabled","ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ① Inflate layout FIRST – views must exist before any orientation
        //   change fires (setRequestedOrientation can trigger it immediately)
        setContentView(R.layout.activity_main);

        // ② Read the real orientation AFTER layout inflation
        currentOrientation = getResources().getConfiguration().orientation;

        // ③ Request orientation AFTER setContentView (safe – views already exist)
        applyDefaultOrientation();

        preferences = getSharedPreferences("MultiStreamViewer", MODE_PRIVATE);

        initViews();

        // ④ Create all 4 WebViews exactly once.
        //   From this point on webViews[] are NEVER removed from boxContainers[].
        //   updateLayout() only moves boxContainers[] in/out of gridLayout.
        initWebViewsOnce();

        initEventListeners();
        loadSavedState(/*silent=*/true);
        loadFavoritesList();

        new Handler().postDelayed(() -> {
            if (!favoritesList.isEmpty())
                Toast.makeText(this, "✅ " + favoritesList.size() + " favoritos", Toast.LENGTH_SHORT).show();
        }, 1500);

        // First layout pass – post() so gridLayout is measured
        gridLayout.post(() -> {
            updateLayout();
            updateFocusedBoxIndicator();
            if (!hasSavedState())
                new Handler().postDelayed(this::loadInitialURLs, 500);
        });
    }

    // ── Device / orientation ─────────────────────────────────────────────────

    private boolean isFireTVorTablet() {
        try { if (getPackageManager().hasSystemFeature("amazon.hardware.fire_tv")) return true; }
        catch (Exception ignored) {}
        String model  = Build.MODEL  != null ? Build.MODEL.toUpperCase()  : "";
        String device = Build.DEVICE != null ? Build.DEVICE.toUpperCase() : "";
        if (model.startsWith("AFT") || device.contains("MONTOYA")) return true;
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        double diag = Math.sqrt(Math.pow(dm.widthPixels / dm.xdpi, 2)
                              + Math.pow(dm.heightPixels / dm.ydpi, 2));
        return diag >= 6.0;
    }

    private void applyDefaultOrientation() {
        setRequestedOrientation(isFireTVorTablet()
            ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void toggleOrientation() {
        int newOrientation = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) 
            ? Configuration.ORIENTATION_PORTRAIT 
            : Configuration.ORIENTATION_LANDSCAPE;
        
        int screenOrientation = (newOrientation == Configuration.ORIENTATION_PORTRAIT)
            ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        
        setRequestedOrientation(screenOrientation);
        Toast.makeText(this, newOrientation == Configuration.ORIENTATION_PORTRAIT ? "📱 Portrait" : "📺 Landscape", Toast.LENGTH_SHORT).show();
        // onConfigurationChanged() vai ser chamado automaticamente pelo Android
        // Lá é que se atualiza currentOrientation e se chama updateLayout()
    }


@Override

public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    currentOrientation = newConfig.orientation;

    Log.d(TAG, "Rotation → " +
            (currentOrientation == Configuration.ORIENTATION_PORTRAIT ? "PORTRAIT" : "LANDSCAPE"));

    if (gridLayout != null) {
        gridLayout.post(this::updateLayout);
    }
}

// ══════════════════════════════════════════════════════════════════════════
    //  INIT VIEWS
    // ══════════════════════════════════════════════════════════════════════════

    private void initViews() {
        gridLayout        = findViewById(R.id.gridLayout);
        bottomControls    = findViewById(R.id.bottomControls);
        expandedBottomBar = findViewById(R.id.expandedBottomBar);
        sidebarContainer  = findViewById(R.id.sidebarContainer);
        mainLayout        = findViewById(R.id.main_layout);
        tvFocusedBox      = findViewById(R.id.tvFocusedBox);

        btnToggleBottomBar   = findViewById(R.id.btnToggleBottomBar);
        btnToggleSidebar     = findViewById(R.id.btnToggleSidebar);
        btnToggleOrientation = findViewById(R.id.btnToggleOrientation);
        btnCloseMenu         = findViewById(R.id.btnCloseMenu);
        btnCloseSidebar      = findViewById(R.id.btnCloseSidebar);

        btnLoadAll       = findViewById(R.id.btnLoadAll);
        btnReloadAll     = findViewById(R.id.btnReloadAll);
        btnClearAll      = findViewById(R.id.btnClearAll);
        btnSaveState     = findViewById(R.id.btnSaveState);
        btnLoadState     = findViewById(R.id.btnLoadState);
        btnSaveFavorites = findViewById(R.id.btnSaveFavorites);
        btnLoadFavorites = findViewById(R.id.btnLoadFavorites);

        btnSaveStateSidebar     = findViewById(R.id.btnSaveStateSidebar);
        btnLoadStateSidebar     = findViewById(R.id.btnLoadStateSidebar);
        btnSaveFavoritesSidebar = findViewById(R.id.btnSaveFavoritesSidebar);
        btnLoadFavoritesSidebar = findViewById(R.id.btnLoadFavoritesSidebar);
        btnLoadAllSidebar       = findViewById(R.id.btnLoadAllSidebar);
        btnReloadAllSidebar     = findViewById(R.id.btnReloadAllSidebar);
        btnClearAllSidebar      = findViewById(R.id.btnClearAllSidebar);

        cbAllowScripts   = findViewById(R.id.cbAllowScripts);
        cbAllowForms     = findViewById(R.id.cbAllowForms);
        cbAllowPopups    = findViewById(R.id.cbAllowPopups);
        cbBlockRedirects = findViewById(R.id.cbBlockRedirects);
        cbBlockAds       = findViewById(R.id.cbBlockAds);

        int[] cbIds  = {R.id.checkBox1, R.id.checkBox2, R.id.checkBox3, R.id.checkBox4};
        int[] kaIds  = {R.id.checkBoxKeepActive1, R.id.checkBoxKeepActive2,
                        R.id.checkBoxKeepActive3, R.id.checkBoxKeepActive4};
        int[] rfIds  = {R.id.btnRefresh1,  R.id.btnRefresh2,  R.id.btnRefresh3,  R.id.btnRefresh4};
        int[] ziIds  = {R.id.btnZoomIn1,   R.id.btnZoomIn2,   R.id.btnZoomIn3,   R.id.btnZoomIn4};
        int[] zoIds  = {R.id.btnZoomOut1,  R.id.btnZoomOut2,  R.id.btnZoomOut3,  R.id.btnZoomOut4};
        int[] pvIds  = {R.id.btnPrevious1, R.id.btnPrevious2, R.id.btnPrevious3, R.id.btnPrevious4};
        int[] nxIds  = {R.id.btnNext1,     R.id.btnNext2,     R.id.btnNext3,     R.id.btnNext4};
        int[] goIds  = {R.id.btnLoadUrl1,  R.id.btnLoadUrl2,  R.id.btnLoadUrl3,  R.id.btnLoadUrl4};
        int[] ulIds  = {R.id.urlInput1,    R.id.urlInput2,    R.id.urlInput3,    R.id.urlInput4};
        int[] usbIds = {R.id.urlInputSidebar1, R.id.urlInputSidebar2,
                        R.id.urlInputSidebar3, R.id.urlInputSidebar4};
        int[] gsbIds = {R.id.btnLoadUrlSidebar1, R.id.btnLoadUrlSidebar2,
                        R.id.btnLoadUrlSidebar3, R.id.btnLoadUrlSidebar4};

        String def = "https://dzritv.com/sport/football/";
        for (int i = 0; i < 4; i++) {
            checkBoxes[i]           = findViewById(cbIds[i]);
            checkBoxesKeepActive[i] = findViewById(kaIds[i]);
            btnRefresh[i]           = findViewById(rfIds[i]);
            btnZoomIn[i]            = findViewById(ziIds[i]);
            btnZoomOut[i]           = findViewById(zoIds[i]);
            btnPrevious[i]          = findViewById(pvIds[i]);
            btnNext[i]              = findViewById(nxIds[i]);
            btnLoadUrl[i]           = findViewById(goIds[i]);
            urlInputs[i]            = findViewById(ulIds[i]);
            urlInputsSidebar[i]     = findViewById(usbIds[i]);
            btnLoadUrlSidebar[i]    = findViewById(gsbIds[i]);
            setupUrlInput(urlInputs[i],        def, i, false);
            setupUrlInput(urlInputsSidebar[i], def, i, true);
        }
    }

    private void setupUrlInput(EditText et, String def, final int idx, final boolean sidebar) {
        if (et == null) return;
        et.setText(def);
        et.setCursorVisible(true);
        et.setSelectAllOnFocus(true);
        et.setOnFocusChangeListener((v, focus) -> { if (focus) { et.selectAll(); showKeyboard(et); } });
        et.setOnEditorActionListener((v, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String url = et.getText().toString().trim();
                if (!url.isEmpty()) {
                    if (sidebar) syncToBottomBar(idx, url); else syncToSidebar(idx, url);
                    loadURL(idx, url);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });
    }

    private void syncToSidebar(int i, String url)   { if (urlInputsSidebar[i] != null) urlInputsSidebar[i].setText(url); }
    private void syncToBottomBar(int i, String url) { if (urlInputs[i]        != null) urlInputs[i].setText(url); }

    // ══════════════════════════════════════════════════════════════════════════
    //  INIT WEBVIEWS  –  called exactly ONCE from onCreate
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewsOnce() {
        for (int i = 0; i < 4; i++) {
            final int idx = i;

            // Container
            boxContainers[i] = new FrameLayout(this);
            boxContainers[i].setId(View.generateViewId());
            boxContainers[i].setBackgroundColor(Color.BLACK);
            boxContainers[i].setFocusable(true);
            boxContainers[i].setFocusableInTouchMode(true);
            boxContainers[i].setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && !isSidebarVisible) {
                    focusedBoxIndex = idx;
                    updateFocusedBoxIndicator();
                    setFocusBorder(idx, true);
                } else if (!hasFocus) {
                    setFocusBorder(idx, false);
                }
            });
            boxContainers[i].setOnClickListener(v -> {
                if (!isSidebarVisible) boxContainers[idx].requestFocus();
            });

            // WebView – MATCH_PARENT so it always fills its container (= "fullbox")
            webViews[i] = new WebView(this);
            webViews[i].setId(View.generateViewId());
            setupWebView(webViews[i], i);
            boxContainers[i].addView(webViews[i], new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

            // NOT adding to gridLayout here – updateLayout() will do it with proper LayoutParams
        }

        if (btnToggleSidebar != null) btnToggleSidebar.requestFocus();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView wv, final int idx) {
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(buildUserAgent());
        s.setTextZoom((int)(zoomLevels[idx] * 100));
        wv.setInitialScale((int)(zoomLevels[idx] * 100));
        wv.setBackgroundColor(Color.BLACK);
        wv.setVerticalScrollBarEnabled(true);
        wv.setHorizontalScrollBarEnabled(false);
        wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        wv.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return handleUrl(v, r.getUrl().toString()); }
            @Override public boolean shouldOverrideUrlLoading(WebView v, String url)           { return handleUrl(v, url); }
            private boolean handleUrl(WebView v, String url) {
                if (cbBlockRedirects != null && cbBlockRedirects.isChecked()) {
                    String cur = v.getUrl();
                    if (cur != null && !isSameDomain(cur, url)) return true;
                }
                if (cbBlockAds != null && cbBlockAds.isChecked() && isAdUrl(url)) return true;
                return false;
            }
            @Override public void onPageFinished(WebView v, String url) {
                applyZoom(idx);
                if (cbBlockAds != null && cbBlockAds.isChecked()) injectAdBlocker(v);
            }
        });

wv.setWebChromeClient(new WebChromeClient() {

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {

        customViews[idx] = view;
        customCallbacks[idx] = callback;

        boxContainers[idx].addView(view,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        webViews[idx].setVisibility(View.GONE);
    }

    @Override
    public void onHideCustomView() {

        if (customViews[idx] != null) {
            boxContainers[idx].removeView(customViews[idx]);
            customViews[idx] = null;
        }

        webViews[idx].setVisibility(View.VISIBLE);
    }
});
    }

    private String buildUserAgent() {
        return isFireTVorTablet()
            ? "Mozilla/5.0 (Linux; Android 9; AFTMM Build/PS7233; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.120 Mobile Safari/537.36"
            : "Mozilla/5.0 (Linux; Android 11; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36";
    }

private void reapplyFullscreenViews() {

    for (int i = 0; i < 4; i++) {

        if (customViews[i] != null) {

            boxContainers[i].removeView(customViews[i]);

            boxContainers[i].addView(customViews[i],
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));

            boxContainers[i].requestLayout();
        }
    }
}



    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE LAYOUT  –  single source of truth for grid arrangement
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Rebuilds the GridLayout.
     *
     * CONTRACT:
     *  • Reads: boxEnabled[], boxKeepActive[], currentOrientation, gridLayout dimensions
     *  • Writes: GridLayout child list + LayoutParams, container visibility
     *  • NEVER touches webViews[] – videos keep playing through any layout change
     *
     * Visibility:
     *  enabled=true             → VISIBLE, added to grid
     *  enabled=false keepActive → INVISIBLE, NOT in grid (WebView plays in bg)
     *  enabled=false !keepActive → GONE
     *
     * IMPORTANTE:
     *  • NÃO há fullscreen automático. Com 1 box ativa, a grid usa layout 1×1.
     *  • Quando o sidebar abre, o espaço é dividido: grid + sidebar no mesmo ecrã.
     *  • Ao rodar o ecrã, o estado das boxes (enabled/keepActive) mantém-se intacto.
     *  • Ao abrir sidebar, a grid reajusta width mas estado das boxes NÃO muda.
     *  • KeepAlive é ativado por padrão (boxKeepActive[] inicializado com true).
     */
private void updateLayout() {

    int visibleCount = 0;
    for (boolean e : boxEnabled) if (e) visibleCount++;

    if (visibleCount == 0) {
        boxEnabled[0] = true;
        visibleCount = 1;
    }

    final boolean portrait = (currentOrientation == Configuration.ORIENTATION_PORTRAIT);

    int rows, cols;

    if (portrait) {
        switch (visibleCount) {
            case 1: rows = 1; cols = 1; break;
            case 2: rows = 2; cols = 1; break;
            case 3: rows = 3; cols = 1; break;
            default: rows = 2; cols = 2; break;
        }
    } else {
        switch (visibleCount) {
            case 1: rows = 1; cols = 1; break;
            case 2: rows = 1; cols = 2; break;
            case 3: rows = 1; cols = 3; break;
            default: rows = 2; cols = 2; break;
        }
    }

    // 🔥 Tornar final para uso dentro do lambda
    final int fVisibleCount = visibleCount;
    final int fRows = rows;
    final int fCols = cols;

    for (int i = 0; i < 4; i++) {
        if (boxContainers[i] == null) continue;

        if (boxEnabled[i]) {
            boxContainers[i].setVisibility(View.VISIBLE);
        } else if (boxKeepActive[i]) {
            boxContainers[i].setVisibility(View.INVISIBLE);
        } else {
            boxContainers[i].setVisibility(View.GONE);
        }
    }

    gridLayout.post(() -> {

        int gridW = gridLayout.getMeasuredWidth();
        int gridH = gridLayout.getMeasuredHeight();

        if (gridW <= 0 || gridH <= 0) return;

        int margin = fVisibleCount == 1 ? 0 : 2;
        int cellW = (gridW - margin * 2 * fCols) / fCols;
        int cellH = (gridH - margin * 2 * fRows) / fRows;

        gridLayout.removeAllViews();
        gridLayout.setRowCount(fRows);
        gridLayout.setColumnCount(fCols);

        int pos = 0;

        for (int i = 0; i < 4; i++) {
            if (!boxEnabled[i]) continue;

            GridLayout.Spec rowSpec = GridLayout.spec(pos / fCols, 1f);
            GridLayout.Spec colSpec = GridLayout.spec(pos % fCols, 1f);

            GridLayout.LayoutParams p =
                    new GridLayout.LayoutParams(rowSpec, colSpec);

            p.width = cellW;
            p.height = cellH;
            p.setMargins(margin, margin, margin, margin);

            gridLayout.addView(boxContainers[i], p);
            pos++;
        }

        gridLayout.requestLayout();

        // 🔥 Reaplicar fullscreen após reorganização
        reapplyFullscreenViews();
    });
}


// ══════════════════════════════════════════════════════════════════════════
    //  EVENT LISTENERS
    // ══════════════════════════════════════════════════════════════════════════

    private void initEventListeners() {
        if (btnToggleBottomBar   != null) btnToggleBottomBar.setOnClickListener(v -> toggleBottomBar());
        if (btnToggleSidebar     != null) btnToggleSidebar.setOnClickListener(v -> { if (isSidebarVisible) closeSidebar(); else openSidebar(); });
        if (btnToggleOrientation != null) btnToggleOrientation.setOnClickListener(v -> toggleOrientation());
        if (btnCloseSidebar      != null) btnCloseSidebar.setOnClickListener(v -> closeSidebar());
        if (btnCloseMenu         != null) btnCloseMenu.setOnClickListener(v -> closeBottomBar());

        if (btnLoadAll       != null) btnLoadAll.setOnClickListener(v -> loadAllURLs());
        if (btnReloadAll     != null) btnReloadAll.setOnClickListener(v -> reloadAll());
        if (btnClearAll      != null) btnClearAll.setOnClickListener(v -> clearAll());
        if (btnSaveState     != null) btnSaveState.setOnClickListener(v -> saveCurrentState());
        if (btnLoadState     != null) btnLoadState.setOnClickListener(v -> loadSavedState(false));
        if (btnSaveFavorites != null) btnSaveFavorites.setOnClickListener(v -> showSaveFavoriteDialog());
        if (btnLoadFavorites != null) btnLoadFavorites.setOnClickListener(v -> showLoadFavoritesDialog());

        if (btnSaveStateSidebar     != null) btnSaveStateSidebar.setOnClickListener(v -> saveCurrentState());
        if (btnLoadStateSidebar     != null) btnLoadStateSidebar.setOnClickListener(v -> loadSavedState(false));
        if (btnSaveFavoritesSidebar != null) btnSaveFavoritesSidebar.setOnClickListener(v -> showSaveFavoriteDialog());
        if (btnLoadFavoritesSidebar != null) btnLoadFavoritesSidebar.setOnClickListener(v -> showLoadFavoritesDialog());
        if (btnLoadAllSidebar       != null) btnLoadAllSidebar.setOnClickListener(v -> loadAllURLs());
        if (btnReloadAllSidebar     != null) btnReloadAllSidebar.setOnClickListener(v -> reloadAll());
        if (btnClearAllSidebar      != null) btnClearAllSidebar.setOnClickListener(v -> clearAll());

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            if (btnLoadUrl[i]        != null) btnLoadUrl[i].setOnClickListener(v -> { String u = urlInputs[idx].getText().toString().trim(); if (!u.isEmpty()) { syncToSidebar(idx,u); loadURL(idx,u); } });
            if (btnLoadUrlSidebar[i] != null) btnLoadUrlSidebar[i].setOnClickListener(v -> { String u = urlInputsSidebar[idx].getText().toString().trim(); if (!u.isEmpty()) { syncToBottomBar(idx,u); loadURL(idx,u); } });
            if (btnRefresh[i]        != null) btnRefresh[i].setOnClickListener(v -> { if (webViews[idx]!=null) webViews[idx].reload(); });
            if (btnZoomIn[i]         != null) btnZoomIn[i].setOnClickListener(v -> zoomIn(idx));
            if (btnZoomOut[i]        != null) btnZoomOut[i].setOnClickListener(v -> zoomOut(idx));
            if (btnPrevious[i]       != null) btnPrevious[i].setOnClickListener(v -> { if (webViews[idx]!=null&&webViews[idx].canGoBack())    webViews[idx].goBack(); });
            if (btnNext[i]           != null) btnNext[i].setOnClickListener(v ->     { if (webViews[idx]!=null&&webViews[idx].canGoForward()) webViews[idx].goForward(); });

            if (checkBoxes[i] != null) {
                checkBoxes[i].setOnCheckedChangeListener((b, checked) -> {
                    if (isSyncingUI) return;   // ⛔ BLOQUEIA durante loadSavedState
                    boxEnabled[idx] = checked;
                    updateLayout();
                });
            }

            if (checkBoxesKeepActive[i] != null) {
                checkBoxesKeepActive[i].setOnCheckedChangeListener((b, checked) -> {
                    if (isSyncingUI) return;   // ⛔ BLOQUEIA durante loadSavedState
                    boxKeepActive[idx] = checked;
                    updateLayout();
                });
            }
        }

        if (cbAllowScripts != null) cbAllowScripts.setOnCheckedChangeListener((b, c) -> { for (WebView wv : webViews) if (wv!=null) wv.getSettings().setJavaScriptEnabled(c); });
        if (cbBlockAds     != null) cbBlockAds.setOnCheckedChangeListener((b, c) -> { for (WebView wv : webViews) if (wv!=null) { wv.getSettings().setBlockNetworkLoads(c); wv.getSettings().setBlockNetworkImage(c); } });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  URL LOADING
    // ══════════════════════════════════════════════════════════════════════════

    private void loadURL(int idx, String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://"))
                url = "https://" + url;
            if (webViews[idx] != null) webViews[idx].loadUrl(url);
        } catch (Exception e) { Log.e(TAG, "loadURL " + idx, e); }
    }

    private void loadAllURLs() {
        for (int i = 0; i < 4; i++) {
            if (!boxEnabled[i] && !boxKeepActive[i]) continue;
            String url = urlInputs[i] != null ? urlInputs[i].getText().toString().trim() : "";
            if (url.isEmpty()) { url = defaultUrl(); if (urlInputs[i]!=null) urlInputs[i].setText(url); }
            syncToSidebar(i, url); loadURL(i, url);
        }
        Toast.makeText(this, "Carregando todas as URLs", Toast.LENGTH_SHORT).show();
    }

    private void loadInitialURLs() {
        for (int i = 0; i < 4; i++) {
            if (!boxEnabled[i] && !boxKeepActive[i]) continue;
            String url = urlInputs[i] != null ? urlInputs[i].getText().toString().trim() : "";
            if (url.isEmpty()) { url = defaultUrl(); if (urlInputs[i]!=null) urlInputs[i].setText(url); }
            syncToSidebar(i, url); loadURL(i, url);
        }
    }

    private void reloadAll() { for (int i=0;i<4;i++) if (webViews[i]!=null) webViews[i].reload(); Toast.makeText(this,"Recarregando todas",Toast.LENGTH_SHORT).show(); }
    private void clearAll()  { for (int i=0;i<4;i++) if (webViews[i]!=null) webViews[i].loadUrl("about:blank"); Toast.makeText(this,"Limpando todas",Toast.LENGTH_SHORT).show(); }
    private String defaultUrl() { return "https://dzritv.com/sport/football/"; }

    // ══════════════════════════════════════════════════════════════════════════
    //  ZOOM
    // ══════════════════════════════════════════════════════════════════════════

    private void applyZoom(int i) { if (webViews[i]!=null) webViews[i].getSettings().setTextZoom((int)(zoomLevels[i]*100)); }
    private void zoomIn(int i)  { if (zoomLevels[i]<2.0f) { zoomLevels[i]+=0.1f; applyZoom(i); Toast.makeText(this,"Box "+(i+1)+" Zoom: "+Math.round(zoomLevels[i]*100)+"%",Toast.LENGTH_SHORT).show(); } }
    private void zoomOut(int i) { if (zoomLevels[i]>0.5f) { zoomLevels[i]-=0.1f; applyZoom(i); Toast.makeText(this,"Box "+(i+1)+" Zoom: "+Math.round(zoomLevels[i]*100)+"%",Toast.LENGTH_SHORT).show(); } }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATE PERSISTENCE
    // ══════════════════════════════════════════════════════════════════════════

    private boolean hasSavedState() { return preferences.contains("url_0") || preferences.contains("box_enabled_0"); }

    private void saveCurrentState() {
        try {
            SharedPreferences.Editor ed = preferences.edit();
            for (int i=0;i<4;i++) {
                String url = urlInputs[i]!=null ? urlInputs[i].getText().toString().trim() : "";
                ed.putString("url_"+i, url.isEmpty()?defaultUrl():url);
                ed.putBoolean("box_enabled_"+i, boxEnabled[i]);
                ed.putBoolean("box_keep_active_"+i, boxKeepActive[i]);
                ed.putFloat("zoom_level_"+i, zoomLevels[i]);
            }
            if (cbAllowScripts   !=null) ed.putBoolean("allow_scripts",   cbAllowScripts.isChecked());
            if (cbAllowForms     !=null) ed.putBoolean("allow_forms",     cbAllowForms.isChecked());
            if (cbAllowPopups    !=null) ed.putBoolean("allow_popups",    cbAllowPopups.isChecked());
            if (cbBlockRedirects !=null) ed.putBoolean("block_redirects", cbBlockRedirects.isChecked());
            if (cbBlockAds       !=null) ed.putBoolean("block_ads",       cbBlockAds.isChecked());
            ed.apply();
            Toast.makeText(this,"✅ Estado guardado!",Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this,"❌ Erro ao guardar estado",Toast.LENGTH_SHORT).show(); }
    }

    private void loadSavedState(boolean silent) {
        try {
            isSyncingUI = true;  // Desabilitar listeners durante carregamento
            for (int i=0;i<4;i++) {
                boxEnabled[i]    = preferences.getBoolean("box_enabled_"+i, true);
                boxKeepActive[i] = preferences.getBoolean("box_keep_active_"+i, true);
                zoomLevels[i]    = preferences.getFloat("zoom_level_"+i, 1.0f);
                if (checkBoxes[i]           !=null) checkBoxes[i].setChecked(boxEnabled[i]);
                if (checkBoxesKeepActive[i] !=null) checkBoxesKeepActive[i].setChecked(boxKeepActive[i]);
                applyZoom(i);
            }
            boolean hasUrls = false;
            for (int i=0;i<4;i++) {
                String url = preferences.getString("url_"+i,"");
                if (!url.isEmpty()) {
                    hasUrls = true;
                    if (urlInputs[i]!=null) urlInputs[i].setText(url);
                    syncToSidebar(i, url);
                    if ((boxEnabled[i]||boxKeepActive[i])&&webViews[i]!=null) loadURL(i, url);
                }
            }
            if (!hasUrls) { String d=defaultUrl(); for (int i=0;i<4;i++) { if (urlInputs[i]!=null) urlInputs[i].setText(d); syncToSidebar(i,d); } }
            if (cbAllowScripts   !=null) cbAllowScripts.setChecked(preferences.getBoolean("allow_scripts",true));
            if (cbAllowForms     !=null) cbAllowForms.setChecked(preferences.getBoolean("allow_forms",true));
            if (cbAllowPopups    !=null) cbAllowPopups.setChecked(preferences.getBoolean("allow_popups",true));
            if (cbBlockRedirects !=null) cbBlockRedirects.setChecked(preferences.getBoolean("block_redirects",false));
            if (cbBlockAds       !=null) cbBlockAds.setChecked(preferences.getBoolean("block_ads",false));
            if (!silent) Toast.makeText(this,"✅ Estado carregado!",Toast.LENGTH_SHORT).show();
        } catch (Exception e) { if (!silent) Toast.makeText(this,"❌ Erro ao carregar",Toast.LENGTH_SHORT).show(); }
        finally {
            isSyncingUI = false;
            updateLayout();   // 🔥 Força layout final consistente
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FAVORITES
    // ══════════════════════════════════════════════════════════════════════════

    private void loadFavoritesList() { try { JSONArray a=new JSONArray(preferences.getString("favorites_list","[]")); favoritesList.clear(); for(int i=0;i<a.length();i++) favoritesList.add(a.getString(i)); } catch(Exception e){favoritesList.clear();} }
    private void saveFavoritesList() { try { preferences.edit().putString("favorites_list",new JSONArray(favoritesList).toString()).apply(); } catch(Exception ignored){} }

    private void saveFavorite(String name) {
        try {
            if (favoritesList.contains(name)) { Toast.makeText(this,"❌ Nome já existe!",Toast.LENGTH_SHORT).show(); return; }
            JSONArray urls=new JSONArray();
            for (int i=0;i<4;i++) { String u=urlInputs[i]!=null?urlInputs[i].getText().toString().trim():""; urls.put(u.isEmpty()?defaultUrl():u); }
            JSONObject obj=new JSONObject(); obj.put("name",name); obj.put("urls",urls);
            favoritesList.add(name); saveFavoritesList();
            preferences.edit().putString("favorite_"+name,obj.toString()).apply();
            Toast.makeText(this,"✅ Favorito guardado!",Toast.LENGTH_SHORT).show();
        } catch(Exception e){Toast.makeText(this,"❌ Erro ao guardar favorito",Toast.LENGTH_SHORT).show();}
    }

    private void loadFavorite(String name, int target) {
        try {
            String json=preferences.getString("favorite_"+name,"");
            if (json.isEmpty()) { Toast.makeText(this,"❌ Não encontrado!",Toast.LENGTH_SHORT).show(); return; }
            JSONArray urls=new JSONObject(json).getJSONArray("urls");
            if (target==-1) { for(int i=0;i<4&&i<urls.length();i++){String u=urls.getString(i);if(urlInputs[i]!=null)urlInputs[i].setText(u);syncToSidebar(i,u);if((boxEnabled[i]||boxKeepActive[i])&&webViews[i]!=null)loadURL(i,u);} Toast.makeText(this,"✅ Carregado em todas!",Toast.LENGTH_SHORT).show(); }
            else if (target<urls.length()) { String u=urls.getString(target);if(urlInputs[target]!=null)urlInputs[target].setText(u);syncToSidebar(target,u);if(webViews[target]!=null)loadURL(target,u);Toast.makeText(this,"✅ Box "+(target+1),Toast.LENGTH_SHORT).show(); }
        } catch(Exception e){Toast.makeText(this,"❌ Erro ao carregar favorito",Toast.LENGTH_SHORT).show();}
    }

    private void deleteFavorite(String name) { favoritesList.remove(name); saveFavoritesList(); preferences.edit().remove("favorite_"+name).apply(); Toast.makeText(this,"✅ Removido!",Toast.LENGTH_SHORT).show(); }

    private void showSaveFavoriteDialog() {
        EditText input=new EditText(this); input.setHint("Nome do favorito"); input.setTextColor(Color.BLACK); input.setBackgroundResource(android.R.drawable.edit_text);
        new AlertDialog.Builder(this).setTitle("Guardar Favorito").setView(input)
            .setPositiveButton("GUARDAR",(d,w)->{String n=input.getText().toString().trim();if(!n.isEmpty())saveFavorite(n);})
            .setNegativeButton("CANCELAR",null).show(); input.requestFocus();
    }
    private void showLoadFavoritesDialog() {
        if (favoritesList.isEmpty()){Toast.makeText(this,"🔭 Sem favoritos!",Toast.LENGTH_SHORT).show();return;}
        String[]names=favoritesList.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Carregar Favorito").setItems(names,(d,w)->showFavoriteOptionsDialog(names[w])).setNegativeButton("CANCELAR",null).show();
    }
    private void showFavoriteOptionsDialog(String name) {
        String[]opts={"Carregar em Todas","Box 1","Box 2","Box 3","Box 4","Eliminar","Cancelar"};
        new AlertDialog.Builder(this).setTitle("Favorito: "+name).setItems(opts,(d,w)->{if(w<5)loadFavorite(name,w==0?-1:w-1);else if(w==5)showDeleteConfirmDialog(name);}).show();
    }
    private void showDeleteConfirmDialog(String name) {
        new AlertDialog.Builder(this).setTitle("Eliminar '"+name+"'?")
            .setPositiveButton("ELIMINAR",(d,w)->deleteFavorite(name)).setNegativeButton("CANCELAR",null).show();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SIDEBAR
    // ══════════════════════════════════════════════════════════════════════════

    public void closeSidebarFromOverlay(View v) { closeSidebar(); }

    private void closeSidebar() {
        android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofFloat(sidebarContainer,"alpha",1f,0f);
        a.setDuration(250);
        a.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator an){
            sidebarContainer.setVisibility(View.GONE); isSidebarVisible=false;
            // Restaurar gridLayout para ocupar width total (sem sidebar)
            adjustGridLayoutForSidebar(false);
            hideKeyboard(); if(btnToggleSidebar!=null)btnToggleSidebar.requestFocus();
        }}); a.start();
    }

    private void openSidebar() {
        sidebarContainer.setVisibility(View.VISIBLE); sidebarContainer.setAlpha(0f); isSidebarVisible=true;
        // Reduzir gridLayout width para dar espaço ao sidebar
        adjustGridLayoutForSidebar(true);
        android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofFloat(sidebarContainer,"alpha",0f,1f);
        a.setDuration(250);
        a.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator an){
            for(int i=0;i<4;i++) if(urlInputs[i]!=null&&urlInputsSidebar[i]!=null) urlInputsSidebar[i].setText(urlInputs[i].getText());
            if(btnCloseSidebar!=null)btnCloseSidebar.requestFocus();
        }}); a.start();
    }

    /**
     * Ajusta a largura do gridLayout quando sidebar abre/fecha.
     * O sidebar tem width 200dp, então gridLayout reduz para liberar espaço.
     * O estado das boxes (enabled/keepActive) mantém-se inalterado.
     * A grid divide o espaço disponível entre as boxes visíveis.
     */
    private void adjustGridLayoutForSidebar(boolean sidebarOpen) {
        if(gridLayout==null) return;
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int sidebarWidth = sidebarOpen ? (int)(200 * dm.density) : 0;
        RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams)gridLayout.getLayoutParams();
        p.rightMargin = sidebarWidth;
        gridLayout.setLayoutParams(p);
        // Recalcular layout imediatamente
        gridLayout.post(this::updateLayout);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOTTOM BAR
    // ══════════════════════════════════════════════════════════════════════════

    private void toggleBottomBar() { if(isBottomBarExpanded) closeBottomBar(); else openBottomBar(); }

    private void closeBottomBar() {
        if(expandedBottomBar==null) return;
        android.animation.ObjectAnimator a=android.animation.ObjectAnimator.ofFloat(expandedBottomBar,"translationY",0f,expandedBottomBar.getHeight());
        a.setDuration(250); a.setInterpolator(new android.view.animation.AccelerateInterpolator());
        a.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator an){
            expandedBottomBar.setVisibility(View.GONE); isBottomBarExpanded=false; updateLayoutForBottomBar();
            hideKeyboard(); if(btnToggleBottomBar!=null)btnToggleBottomBar.requestFocus();
        }}); a.start();
    }

    private void openBottomBar() {
        if(expandedBottomBar==null) return;
        expandedBottomBar.setVisibility(View.VISIBLE); expandedBottomBar.setTranslationY(expandedBottomBar.getHeight());
        isBottomBarExpanded=true; updateLayoutForBottomBar();
        android.animation.ObjectAnimator a=android.animation.ObjectAnimator.ofFloat(expandedBottomBar,"translationY",expandedBottomBar.getHeight(),0f);
        a.setDuration(250); a.setInterpolator(new android.view.animation.DecelerateInterpolator());
        a.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator an){if(btnCloseMenu!=null)btnCloseMenu.requestFocus();}}); a.start();
    }

    private void updateLayoutForBottomBar() {
        if(gridLayout==null) return;
        RelativeLayout.LayoutParams p=(RelativeLayout.LayoutParams)gridLayout.getLayoutParams();
        p.removeRule(RelativeLayout.ABOVE);
        p.addRule(RelativeLayout.ABOVE, isBottomBarExpanded ? R.id.expandedBottomBar : R.id.bottomControls);
        gridLayout.setLayoutParams(p);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void updateFocusedBoxIndicator() { if(tvFocusedBox!=null) tvFocusedBox.setText("Foco: "+(focusedBoxIndex+1)); }

    /** Programmatic focus border – no drawable resource needed. */
    private void setFocusBorder(int idx, boolean focused) {
        if(idx<0||idx>=4||boxContainers[idx]==null) return;
        if(focused) { GradientDrawable gd=new GradientDrawable(); gd.setColor(Color.BLACK); gd.setStroke(4,Color.YELLOW); boxContainers[idx].setBackground(gd); }
        else        { boxContainers[idx].setBackgroundColor(Color.BLACK); }
    }

    private void scrollWebView(int i,int dy) { if(webViews[i]!=null&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT) webViews[i].evaluateJavascript("window.scrollBy(0,"+dy+");",null); }

    private void showKeyboard(View v) { v.postDelayed(()->{InputMethodManager imm=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(imm!=null)imm.showSoftInput(v,InputMethodManager.SHOW_IMPLICIT);},100); }
    private void hideKeyboard() { View v=getCurrentFocus(); if(v!=null){InputMethodManager imm=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(imm!=null)imm.hideSoftInputFromWindow(v.getWindowToken(),0);} }

    private void injectAdBlocker(WebView v) { if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT) v.evaluateJavascript("try{['div[class*=\"ad\"]','ins.adsbygoogle'].forEach(s=>document.querySelectorAll(s).forEach(e=>e.style.display='none'));}catch(e){}",null); }
    private boolean isAdUrl(String url) { for(String d:adDomains) if(url.toLowerCase().contains(d)) return true; return false; }
    private String getDomain(String url) { try{String h=new java.net.URI(url).getHost();return h!=null?h.replace("www.",""):url;}catch(Exception e){return url;} }
    private boolean isSameDomain(String a,String b) { try{return getDomain(a).equals(getDomain(b));}catch(Exception e){return false;} }

    private void clearAppCache() { try{for(WebView wv:webViews)if(wv!=null){wv.clearCache(true);wv.clearHistory();}if(getCacheDir()!=null)deleteDir(getCacheDir());}catch(Exception ignored){} }
    private boolean deleteDir(java.io.File dir) { if(dir.isDirectory()){String[]c=dir.list();if(c!=null)for(String s:c)if(!deleteDir(new java.io.File(dir,s)))return false;}return dir.delete(); }

    // ══════════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════════

    @Override protected void onPause()   { super.onPause();   saveCurrentState(); }
    @Override protected void onResume()  { super.onResume();  loadFavoritesList(); if(btnToggleSidebar!=null)btnToggleSidebar.requestFocus(); }
    @Override protected void onDestroy() { super.onDestroy(); clearAppCache(); for(WebView wv:webViews)if(wv!=null){wv.stopLoading();wv.setWebViewClient(null);wv.setWebChromeClient(null);wv.destroy();} }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction()==KeyEvent.ACTION_DOWN) {
            switch(keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if(isSidebarVisible){closeSidebar();return true;}
                    if(webViews[focusedBoxIndex]!=null&&webViews[focusedBoxIndex].canGoBack()){webViews[focusedBoxIndex].goBack();return true;}
                    break;
                case KeyEvent.KEYCODE_MENU:
                    if(isSidebarVisible)closeSidebar();else openSidebar(); return true;
                case KeyEvent.KEYCODE_DPAD_UP:    if(!isSidebarVisible){scrollWebView(focusedBoxIndex,-100);return true;}break;
                case KeyEvent.KEYCODE_DPAD_DOWN:  if(!isSidebarVisible){scrollWebView(focusedBoxIndex, 100);return true;}break;
                case KeyEvent.KEYCODE_PAGE_UP:    if(!isSidebarVisible){scrollWebView(focusedBoxIndex,-500);return true;}break;
                case KeyEvent.KEYCODE_PAGE_DOWN:  if(!isSidebarVisible){scrollWebView(focusedBoxIndex, 500);return true;}break;
            }
        }
        return super.onKeyDown(keyCode,event);
    }

    @Override
    public void onBackPressed() {
        if(isSidebarVisible){closeSidebar();return;}
        if(webViews[focusedBoxIndex]!=null&&webViews[focusedBoxIndex].canGoBack()){webViews[focusedBoxIndex].goBack();return;}
        for(WebView wv:webViews)if(wv!=null&&wv.canGoBack()){wv.goBack();return;}
        new AlertDialog.Builder(this).setTitle("Sair?").setMessage("Deseja sair do app?")
            .setPositiveButton("SIM",(d,w)->finish()).setNegativeButton("NÃO",null).show();
    }
}
