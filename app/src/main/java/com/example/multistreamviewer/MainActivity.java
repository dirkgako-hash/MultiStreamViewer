package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int MAX = 6;

    // Views
    private FrameLayout gridLayout;
    private FrameLayout[] boxContainers = new FrameLayout[MAX];
    private WebView[] webViews = new WebView[MAX];

    // Fullscreen video tracking
    private View[] customViews = new View[MAX];
    private WebChromeClient.CustomViewCallback[] customCallbacks = new WebChromeClient.CustomViewCallback[MAX];

    private LinearLayout bottomControls;
    private FrameLayout sidebarContainer;
    private RelativeLayout mainLayout;
    private TextView tvFocusedBox;
    private TextView tvLayoutName;

    private Button btnToggleBottomBar, btnToggleSidebar;
    private Button btnSetPortrait, btnSetLandscape;
    private Button btnCloseSidebar;
    private Button btnCycleLayout;
    private Button btnExportSidebar, btnImportSidebar, btnClearCacheSidebar;

    private Button[] btnRefresh = new Button[MAX];
    private Button[] btnZoomIn = new Button[MAX];
    private Button[] btnZoomOut = new Button[MAX];
    private Button[] btnPrevious = new Button[MAX];
    private Button[] btnNext = new Button[MAX];
    private CheckBox[] checkBoxes = new CheckBox[MAX];
    private CheckBox[] checkBoxesKeepActive = new CheckBox[MAX];
    private CheckBox[] checkBoxFullVideo = new CheckBox[MAX];
    private boolean[] fullscreenActive = new boolean[MAX];

    private EditText[] urlInputsSidebar = new EditText[MAX];
    private Button[] btnLoadUrlSidebar = new Button[MAX];
    private Button btnLoadAllSidebar, btnReloadAllSidebar, btnClearAllSidebar;
    private Button btnSaveStateSidebar, btnLoadStateSidebar;
    private Button btnSaveFavoritesSidebar, btnLoadFavoritesSidebar;

    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups, cbBlockRedirects, cbBlockAds;
    private CheckBox cbKeepScreenOn, cbAllowHttp, cbBoxCap;
    private View[] boxGroups = new View[MAX];
    private int maxBoxes = MAX;

    private boolean[] boxEnabled = {true, true, true, true, false, false};
    private boolean[] boxKeepActive = {false, false, false, false, false, false};

    private boolean isSidebarVisible = false;
    private boolean isBottomControlsVisible = false; // Estado da barra inferior
    private boolean isSyncingUI = false;
    private int focusedBoxIndex = 0;
    private float[] zoomLevels = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    private int currentOrientation = Configuration.ORIENTATION_LANDSCAPE;

    private ArrayList<String> favoritesList = new ArrayList<>();
    private SharedPreferences preferences;

    private GridLayoutEngine engine;

    private static final String PREFS = "MultiStreamViewer";
    private static final String KEY_WEIGHTS = "layout_weights";
    private static final String KEY_PRESET_PREFIX = "layout_";
    private static final String KEY_BOX_CAP = "box_cap";

    private final List<String> adDomains = Arrays.asList(
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "google-analytics.com", "googletagmanager.com", "adservice.google",
            "amazon-adsystem.com", "taboola.com", "outbrain.com", "criteo.com",
            "criteo.net", "adnxs.com", "rubiconproject.com", "pubmatic.com",
            "openx.net", "smartadserver.com", "adform.net", "adcash.com",
            "popads.net", "propellerads.com", "juicyads.com", "exoclick.com",
            "onclickads.net", "directnavbt.com", "ntv.io", "zoneid.com");

    private static final String TAG = "MSV";

    // ── Export/Import JSON via SAF ────────────────────────────────────────────
    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),
                    uri -> { if (uri != null) writeConfigTo(uri); });

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> { if (uri != null) readConfigFrom(uri); });

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentOrientation = getResources().getConfiguration().orientation;
        applyDefaultOrientation();
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);

        float dp = getResources().getDisplayMetrics().density;
        engine = new GridLayoutEngine(this, (int) (8 * dp), (int) (60 * dp));

        initViews();
        initWebViewsOnce();
        initEventListeners();
        applyPressHighlights(bottomControls);
        applyPressHighlights(sidebarContainer);
        loadSavedState(true);
        loadFavoritesList();
        applyKeepScreenOn(cbKeepScreenOn == null || cbKeepScreenOn.isChecked());

        new Handler().postDelayed(() -> {
            if (!favoritesList.isEmpty())
                Toast.makeText(this, "✅ " + favoritesList.size() + " favoritos", Toast.LENGTH_SHORT).show();
        }, 1500);

        gridLayout.post(() -> {
            updateLayout();
            updateFocusedBoxIndicator();
            if (!hasSavedState())
                new Handler().postDelayed(this::loadInitialURLs, 500);
        });
    }

    // ── keep screen on ────────────────────────────────────────────────────────
    private void applyKeepScreenOn(boolean on) {
        if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // ── destaque do 1º clique (mantém o comportamento de 2 cliques do TV) ─────
    private void applyPressHighlights(View root) {
        if (root instanceof Button || root instanceof CheckBox) highlightable(root);
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) applyPressHighlights(vg.getChildAt(i));
        }
    }

    private void highlightable(View v) {
        Drawable base = v.getBackground();
        if (base == null) base = new ColorDrawable(Color.TRANSPARENT);
        StateListDrawable hl = new StateListDrawable();
        hl.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(0x8AFFC107));
        hl.addState(new int[]{android.R.attr.state_focused}, new ColorDrawable(0x66FFC107));
        hl.addState(new int[]{}, new ColorDrawable(0x00000000));
        v.setBackground(new LayerDrawable(new Drawable[]{base, hl}));
    }

    // ── pesos por preset (persistidos como no protótipo desktop) ──────────────
    private final GridLayoutEngine.WeightsStore weightsStore = new GridLayoutEngine.WeightsStore() {
        @Override
        public float[] get(String presetId, String path, int count) {
            try {
                JSONObject all = new JSONObject(preferences.getString(KEY_WEIGHTS, "{}"));
                JSONObject perPreset = all.optJSONObject(presetId);
                if (perPreset == null) return null;
                JSONArray arr = perPreset.optJSONArray(path);
                if (arr == null || arr.length() != count) return null;
                float[] w = new float[count];
                for (int i = 0; i < count; i++) w[i] = (float) arr.getDouble(i);
                return w;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public void save(String presetId, String path, float[] weights) {
            try {
                JSONObject all = new JSONObject(preferences.getString(KEY_WEIGHTS, "{}"));
                JSONObject perPreset = all.optJSONObject(presetId);
                if (perPreset == null) { perPreset = new JSONObject(); all.put(presetId, perPreset); }
                JSONArray arr = new JSONArray();
                for (float w : weights) arr.put(w);
                perPreset.put(path, arr);
                preferences.edit().putString(KEY_WEIGHTS, all.toString()).apply();
            } catch (Exception ignored) {
            }
        }
    };

    private int enabledCount() {
        int n = 0;
        for (int i = 0; i < MAX; i++) if (boxEnabled[i]) n++;
        return n;
    }

    private boolean isPortraitNow() {
        return currentOrientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private GridLayoutEngine.Preset currentPreset(int n) {
        String id = preferences.getString(KEY_PRESET_PREFIX + n, null);
        GridLayoutEngine.Preset p = GridLayoutEngine.byId(id);
        if (p == null || p.n != n) p = GridLayoutEngine.byId(GridLayoutEngine.defaultPresetId(n, isPortraitNow()));
        return p;
    }

    private void cycleLayout() {
        int n = enabledCount();
        List<GridLayoutEngine.Preset> list = GridLayoutEngine.presetsFor(n);
        if (list.isEmpty()) return;
        GridLayoutEngine.Preset cur = currentPreset(n);
        int idx = 0;
        for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(cur.id)) { idx = i; break; }
        GridLayoutEngine.Preset next = list.get((idx + 1) % list.size());
        preferences.edit().putString(KEY_PRESET_PREFIX + n, next.id).apply();
        updateLayout();
        Toast.makeText(this, "🗂 " + next.label, Toast.LENGTH_SHORT).show();
    }

    private void updateLayoutNameLabel() {
        if (tvLayoutName == null) return;
        GridLayoutEngine.Preset p = currentPreset(enabledCount());
        tvLayoutName.setText(p == null ? " " : p.label);
    }

    // ── applyGridSize: dimensões reais de mainLayout (exclui system bars) ─────
    private boolean applyGridSizeRunning = false;
    private void applyGridSize() {
        if (gridLayout == null || applyGridSizeRunning) return;
        int rootW = mainLayout.getWidth();
        int rootH = mainLayout.getHeight();
        if (rootW <= 0 || rootH <= 0) {
            mainLayout.post(this::applyGridSize);
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        int barH  = isBottomControlsVisible ? (int)(40 * density) : 0;
        int sideW = isSidebarVisible        ? (int)(200 * density) : 0;
        int W     = rootW - sideW;
        int H     = rootH - barH;

        applyGridSizeRunning = true;
        RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) gridLayout.getLayoutParams();
        p.width = W; p.height = H;
        p.leftMargin = 0; p.topMargin = 0; p.rightMargin = 0; p.bottomMargin = 0;
        p.removeRule(RelativeLayout.ABOVE);
        p.removeRule(RelativeLayout.ALIGN_PARENT_END);
        gridLayout.setLayoutParams(p);
        Log.d(TAG, "applyGridSize: " + W + "x" + H + " barH=" + barH + " sideW=" + sideW);

        rebuildGrid();
        applyGridSizeRunning = false;
    }

    /** Fire TV / Stick: sem touchscreen nem ponteiro fiável, e com pouca RAM. */
    private boolean isFireTv() {
        try {
            if (getPackageManager().hasSystemFeature("amazon.hardware.fire_tv")) return true;
        } catch (Exception ignored) {
        }
        String model = Build.MODEL != null ? Build.MODEL.toUpperCase() : "";
        String device = Build.DEVICE != null ? Build.DEVICE.toUpperCase() : "";
        return model.startsWith("AFT") || device.contains("MONTOYA");
    }

    private int configuredBoxCap() {
        return preferences.getInt(KEY_BOX_CAP, isFireTv() ? 4 : MAX);
    }

    /** Esconde as boxes acima do teto e desliga-as (com aviso de quantas caíram). */
    private void applyBoxCap() {
        maxBoxes = configuredBoxCap();
        int dropped = 0;
        for (int i = 0; i < MAX; i++) {
            boolean overCap = i >= maxBoxes;
            if (boxGroups[i] != null) boxGroups[i].setVisibility(overCap ? View.GONE : View.VISIBLE);
            if (!overCap || (!boxEnabled[i] && !boxKeepActive[i])) continue;
            boxEnabled[i] = false;
            boxKeepActive[i] = false;
            if (checkBoxes[i] != null) checkBoxes[i].setChecked(false);
            if (checkBoxesKeepActive[i] != null) checkBoxesKeepActive[i].setChecked(false);
            if (webViews[i] != null) webViews[i].loadUrl("about:blank");
            dropped++;
        }
        if (dropped > 0)
            Toast.makeText(this, "⚠️ " + dropped + " box(es) desligadas: limite de " + maxBoxes,
                    Toast.LENGTH_LONG).show();
    }

    private boolean httpAllowed() {
        return cbAllowHttp == null || cbAllowHttp.isChecked();
    }

    private boolean isPlainHttp(String url) {
        return url != null && url.toLowerCase().startsWith("http://");
    }

    private void applyMixedContent() {
        int mode = httpAllowed() ? WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                : WebSettings.MIXED_CONTENT_NEVER_ALLOW;
        for (WebView wv : webViews) if (wv != null) wv.getSettings().setMixedContentMode(mode);
    }

    /** Resposta vazia: o pedido é recusado sem partir o resto da página. */
    private static WebResourceResponse emptyResource() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
    }

    private boolean isFireTVorTablet() {
        if (isFireTv()) return true;
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

    private void setOrientation(int requestedOrientation) {
        String orientationName = (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ? "Portrait" : "Landscape";
        setRequestedOrientation(requestedOrientation);
        Toast.makeText(this, "📐 " + orientationName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentOrientation = newConfig.orientation;
        if (gridLayout != null) {
            gridLayout.post(this::updateLayout);
        }
    }

    private void initViews() {
        gridLayout = findViewById(R.id.gridLayout);
        bottomControls = findViewById(R.id.bottomControls);
        sidebarContainer = findViewById(R.id.sidebarContainer);
        mainLayout = findViewById(R.id.main_layout);
        tvFocusedBox = findViewById(R.id.tvFocusedBox);
        tvLayoutName = findViewById(R.id.tvLayoutName);

        btnToggleBottomBar = findViewById(R.id.btnToggleBottomBar);
        btnToggleSidebar = findViewById(R.id.btnToggleSidebar);
        btnSetPortrait = findViewById(R.id.btnSetPortrait);
        btnSetLandscape = findViewById(R.id.btnSetLandscape);
        btnCloseSidebar = findViewById(R.id.btnCloseSidebar);
        btnCycleLayout = findViewById(R.id.btnCycleLayout);

        btnSaveStateSidebar = findViewById(R.id.btnSaveStateSidebar);
        btnLoadStateSidebar = findViewById(R.id.btnLoadStateSidebar);
        btnSaveFavoritesSidebar = findViewById(R.id.btnSaveFavoritesSidebar);
        btnLoadFavoritesSidebar = findViewById(R.id.btnLoadFavoritesSidebar);
        btnLoadAllSidebar = findViewById(R.id.btnLoadAllSidebar);
        btnReloadAllSidebar = findViewById(R.id.btnReloadAllSidebar);
        btnClearAllSidebar = findViewById(R.id.btnClearAllSidebar);
        btnExportSidebar = findViewById(R.id.btnExportSidebar);
        btnImportSidebar = findViewById(R.id.btnImportSidebar);
        btnClearCacheSidebar = findViewById(R.id.btnClearCacheSidebar);

        cbAllowScripts = findViewById(R.id.cbAllowScripts);
        cbAllowForms = findViewById(R.id.cbAllowForms);
        cbAllowPopups = findViewById(R.id.cbAllowPopups);
        cbBlockRedirects = findViewById(R.id.cbBlockRedirects);
        cbBlockAds = findViewById(R.id.cbBlockAds);
        cbKeepScreenOn = findViewById(R.id.cbKeepScreenOn);
        cbAllowHttp = findViewById(R.id.cbAllowHttp);
        cbBoxCap = findViewById(R.id.cbBoxCap);

        int[] grpIds = {R.id.boxGroup1, R.id.boxGroup2, R.id.boxGroup3,
                R.id.boxGroup4, R.id.boxGroup5, R.id.boxGroup6};
        for (int i = 0; i < MAX; i++) boxGroups[i] = findViewById(grpIds[i]);

        int[] cbIds = {R.id.checkBox1, R.id.checkBox2, R.id.checkBox3, R.id.checkBox4, R.id.checkBox5, R.id.checkBox6};
        int[] kaIds = {R.id.checkBoxKeepActive1, R.id.checkBoxKeepActive2, R.id.checkBoxKeepActive3,
                R.id.checkBoxKeepActive4, R.id.checkBoxKeepActive5, R.id.checkBoxKeepActive6};
        int[] fullIds = {R.id.checkBoxFullVideo1, R.id.checkBoxFullVideo2, R.id.checkBoxFullVideo3,
                R.id.checkBoxFullVideo4, R.id.checkBoxFullVideo5, R.id.checkBoxFullVideo6};
        int[] rfIds = {R.id.btnRefresh1, R.id.btnRefresh2, R.id.btnRefresh3,
                R.id.btnRefresh4, R.id.btnRefresh5, R.id.btnRefresh6};
        int[] ziIds = {R.id.btnZoomIn1, R.id.btnZoomIn2, R.id.btnZoomIn3,
                R.id.btnZoomIn4, R.id.btnZoomIn5, R.id.btnZoomIn6};
        int[] zoIds = {R.id.btnZoomOut1, R.id.btnZoomOut2, R.id.btnZoomOut3,
                R.id.btnZoomOut4, R.id.btnZoomOut5, R.id.btnZoomOut6};
        int[] pvIds = {R.id.btnPrevious1, R.id.btnPrevious2, R.id.btnPrevious3,
                R.id.btnPrevious4, R.id.btnPrevious5, R.id.btnPrevious6};
        int[] nxIds = {R.id.btnNext1, R.id.btnNext2, R.id.btnNext3,
                R.id.btnNext4, R.id.btnNext5, R.id.btnNext6};
        int[] usbIds = {R.id.urlInputSidebar1, R.id.urlInputSidebar2, R.id.urlInputSidebar3,
                R.id.urlInputSidebar4, R.id.urlInputSidebar5, R.id.urlInputSidebar6};
        int[] gsbIds = {R.id.btnLoadUrlSidebar1, R.id.btnLoadUrlSidebar2, R.id.btnLoadUrlSidebar3,
                R.id.btnLoadUrlSidebar4, R.id.btnLoadUrlSidebar5, R.id.btnLoadUrlSidebar6};

        for (int i = 0; i < MAX; i++) {
            checkBoxes[i] = findViewById(cbIds[i]);
            checkBoxesKeepActive[i] = findViewById(kaIds[i]);
            checkBoxFullVideo[i] = findViewById(fullIds[i]);

            btnRefresh[i] = findViewById(rfIds[i]);
            btnZoomIn[i] = findViewById(ziIds[i]);
            btnZoomOut[i] = findViewById(zoIds[i]);
            btnPrevious[i] = findViewById(pvIds[i]);
            btnNext[i] = findViewById(nxIds[i]);

            urlInputsSidebar[i] = findViewById(usbIds[i]);
            btnLoadUrlSidebar[i] = findViewById(gsbIds[i]);
            setupUrlInputSidebar(i);
        }
    }

    private void setupUrlInputSidebar(final int idx) {
        EditText et = urlInputsSidebar[idx];
        if (et == null) return;
        et.setCursorVisible(true);
        et.setSelectAllOnFocus(true);
        et.setOnFocusChangeListener((v, focus) -> {
            if (focus) {
                et.selectAll();
                showKeyboard(et);
            }
        });
        et.setOnEditorActionListener((v, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String url = et.getText().toString().trim();
                if (!url.isEmpty()) {
                    loadURL(idx, url);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewsOnce() {
        for (int i = 0; i < MAX; i++) {
            final int idx = i;

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

            webViews[i] = new WebView(this);
            webViews[i].setId(View.generateViewId());
            setupWebView(webViews[i], i);
            boxContainers[i].addView(webViews[i], new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
        }

        if (btnToggleSidebar != null) btnToggleSidebar.requestFocus();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView wv, final int idx) {
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        Log.w(TAG, "WebView JavaScript enabled (ensure loaded content is trusted)");
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setJavaScriptCanOpenWindowsAutomatically(cbAllowPopups != null && cbAllowPopups.isChecked());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            s.setMixedContentMode(httpAllowed() ? WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    : WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(buildUserAgent());
        s.setTextZoom((int) (zoomLevels[idx] * 100));
        wv.setInitialScale(0);
        wv.setBackgroundColor(Color.BLACK);
        wv.setVerticalScrollBarEnabled(true);
        wv.setHorizontalScrollBarEnabled(false);
        wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return handleUrl(v, r.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return handleUrl(v, url);
            }

            private boolean handleUrl(WebView v, String url) {
                if (!httpAllowed() && isPlainHttp(url)) {
                    Toast.makeText(MainActivity.this, "🚫 http:// bloqueado (sidebar → Permitir http)",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (cbBlockRedirects != null && cbBlockRedirects.isChecked()) {
                    String cur = v.getUrl();
                    if (cur != null && !isSameDomain(cur, url)) return true;
                }
                if (cbBlockAds != null && cbBlockAds.isChecked() && isAdUrl(url)) return true;
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null;
                String url = request.getUrl().toString();
                if (!httpAllowed() && isPlainHttp(url)) return emptyResource();
                if (cbBlockAds != null && cbBlockAds.isChecked() && isAdUrl(url)) return emptyResource();
                return null;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                applyZoom(idx);
                if (cbBlockAds != null && cbBlockAds.isChecked()) injectAdBlocker(view);
                if (checkBoxFullVideo[idx] != null && checkBoxFullVideo[idx].isChecked()) {
                    enableFullBox(view);
                }
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                Log.d(TAG, "onShowCustomView: " + idx);
                fullscreenActive[idx] = true;
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
                Log.d(TAG, "onHideCustomView: " + idx);
                fullscreenActive[idx] = false;
                if (customViews[idx] != null) {
                    boxContainers[idx].removeView(customViews[idx]);
                    customViews[idx] = null;
                }
                webViews[idx].setVisibility(View.VISIBLE);
            }
        });
    }

    // Fullbox methods
    private void enableFullBox(WebView webView) {
        if (webView == null) return;
        String script =
            "javascript:(function() {" +
                "   var style = document.getElementById('msv_fullbox_style');" +
                "   if(!style){ style = document.createElement('style'); style.id='msv_fullbox_style'; }" +
                "   style.type = 'text/css';" +
                "   style.innerHTML = '" +
                "       video {" +
                "           position: fixed !important;" +
                "           top: 0 !important;" +
                "           left: 0 !important;" +
                "           width: 100% !important;" +
                "           height: 100% !important;" +
                "           object-fit: contain !important;" +
                "           z-index: 9999 !important;" +
                "           background: black;" +
                "       }" +
                "       body { overflow: hidden !important; }" +
                "   ';" +
                "   document.head.appendChild(style);" +
                "   var elements = document.querySelectorAll('header, footer, nav, aside');" +
                "   for (var i = 0; i < elements.length; i++) {" +
                "       elements[i].style.display = 'none';" +
                "   }" +
                "})();";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(script, null);
        } else {
            webView.loadUrl(script);
        }
    }

    private void disableFullBox(WebView webView) {
        if (webView == null) return;
        String script = "javascript:(function(){\n" +
                "  var s = document.getElementById('msv_fullbox_style');\n" +
                "  if(s) s.parentNode.removeChild(s);\n" +
                "  var elems = document.querySelectorAll('header, footer, nav, aside');\n" +
                "  for(var i=0;i<elems.length;i++){ elems[i].style.display=''; }\n" +
                "  document.body.style.overflow='';\n" +
                "})();";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(script, null);
        } else {
            webView.loadUrl(script);
        }
    }

    private String buildUserAgent() {
        return isFireTVorTablet()
                ? "Mozilla/5.0 (Linux; Android 9; AFTMM Build/PS7233; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.120 Mobile Safari/537.36"
                : "Mozilla/5.0 (Linux; Android 11; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36";
    }

    // ================== UPDATE LAYOUT ==================
    private final Runnable applyGridRunnable = this::applyGridSize;

    private void updateLayout() {
        // Coalesce: múltiplos toggles → uma única reconstrução
        if (mainLayout == null) return;
        mainLayout.removeCallbacks(applyGridRunnable);
        mainLayout.postDelayed(applyGridRunnable, 120);
    }

    private void rebuildGrid() {
        List<Integer> enabledIdx = new ArrayList<>();
        for (int i = 0; i < MAX; i++) if (boxEnabled[i]) enabledIdx.add(i);

        if (enabledIdx.isEmpty()) {
            boxEnabled[0] = true;
            if (checkBoxes[0] != null) checkBoxes[0].setChecked(true);
            enabledIdx.add(0);
        }

        for (int i = 0; i < MAX; i++) {
            if (boxContainers[i] == null) continue;
            if      (boxEnabled[i])    boxContainers[i].setVisibility(View.VISIBLE);
            else if (boxKeepActive[i]) boxContainers[i].setVisibility(View.INVISIBLE);
            else                       boxContainers[i].setVisibility(View.GONE);
        }

        final List<Integer> idx = new ArrayList<>(enabledIdx);
        gridLayout.post(() -> {
            for (int i = 0; i < MAX; i++) {
                if (boxContainers[i] != null && boxContainers[i].getParent() != null
                        && boxContainers[i].getParent() != gridLayout) {
                    ((ViewGroup) boxContainers[i].getParent()).removeView(boxContainers[i]);
                }
            }
            gridLayout.removeAllViews();

            List<View> visible = new ArrayList<>();
            for (int i : idx) visible.add(boxContainers[i]);
            GridLayoutEngine.Preset preset = currentPreset(idx.size());
            View root = engine.build(preset, visible, weightsStore);
            if (root != null) {
                gridLayout.addView(root, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
            }
            updateLayoutNameLabel();
            Log.d(TAG, "rebuildGrid " + idx.size() + "boxes preset=" + (preset == null ? "-" : preset.id));
        });
    }
    // ================== FIM DO LAYOUT ==================

    private void initEventListeners() {
        if (btnToggleBottomBar != null) {
            btnToggleBottomBar.setOnClickListener(v -> {
                if (bottomControls.getVisibility() == View.VISIBLE) {
                    bottomControls.setVisibility(View.GONE);
                    isBottomControlsVisible = false;
                } else {
                    bottomControls.setVisibility(View.VISIBLE);
                    isBottomControlsVisible = true;
                }
                Log.d(TAG, "bottomControls visibility changed to: " + (isBottomControlsVisible ? "VISIBLE" : "GONE"));
                applyGridSize();
            });
        }
        if (btnToggleSidebar != null)
            btnToggleSidebar.setOnClickListener(v -> {
                if (isSidebarVisible) closeSidebar();
                else openSidebar();
            });
        if (btnSetPortrait != null) btnSetPortrait.setOnClickListener(v -> setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        if (btnSetLandscape != null) btnSetLandscape.setOnClickListener(v -> setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        if (btnCloseSidebar != null) btnCloseSidebar.setOnClickListener(v -> closeSidebar());
        if (btnCycleLayout != null) btnCycleLayout.setOnClickListener(v -> cycleLayout());

        // Sidebar global buttons
        if (btnLoadAllSidebar != null) btnLoadAllSidebar.setOnClickListener(v -> loadAllURLs());
        if (btnReloadAllSidebar != null) btnReloadAllSidebar.setOnClickListener(v -> reloadAll());
        if (btnClearAllSidebar != null) btnClearAllSidebar.setOnClickListener(v -> clearAll());
        if (btnSaveStateSidebar != null) btnSaveStateSidebar.setOnClickListener(v -> saveCurrentState(true));
        if (btnLoadStateSidebar != null) btnLoadStateSidebar.setOnClickListener(v -> loadSavedState(false));
        if (btnSaveFavoritesSidebar != null) btnSaveFavoritesSidebar.setOnClickListener(v -> showSaveFavoriteDialog());
        if (btnLoadFavoritesSidebar != null) btnLoadFavoritesSidebar.setOnClickListener(v -> showLoadFavoritesDialog());
        if (btnExportSidebar != null) btnExportSidebar.setOnClickListener(v -> exportLauncher.launch(exportFileName()));
        if (btnImportSidebar != null) btnImportSidebar.setOnClickListener(v -> importLauncher.launch(new String[]{"*/*"}));
        if (btnClearCacheSidebar != null) btnClearCacheSidebar.setOnClickListener(v -> {
            clearAppCache();
            Toast.makeText(this, "🧹 Cache limpo", Toast.LENGTH_SHORT).show();
        });

        // Per-box controls
        for (int i = 0; i < MAX; i++) {
            final int idx = i;
            if (btnLoadUrlSidebar[i] != null)
                btnLoadUrlSidebar[i].setOnClickListener(v -> {
                    if (urlInputsSidebar[idx] != null) {
                        urlInputsSidebar[idx].clearFocus();
                        hideKeyboard();
                    }
                    String u = urlInputsSidebar[idx].getText().toString().trim();
                    if (!u.isEmpty()) {
                        loadURL(idx, u);
                    }
                });
            if (btnRefresh[i] != null)
                btnRefresh[i].setOnClickListener(v -> {
                    if (webViews[idx] != null) webViews[idx].reload();
                });
            if (btnZoomIn[i] != null) btnZoomIn[i].setOnClickListener(v -> zoomIn(idx));
            if (btnZoomOut[i] != null) btnZoomOut[i].setOnClickListener(v -> zoomOut(idx));
            if (btnPrevious[i] != null)
                btnPrevious[i].setOnClickListener(v -> {
                    if (webViews[idx] != null && webViews[idx].canGoBack()) webViews[idx].goBack();
                });
            if (btnNext[i] != null)
                btnNext[i].setOnClickListener(v -> {
                    if (webViews[idx] != null && webViews[idx].canGoForward()) webViews[idx].goForward();
                });

            if (checkBoxes[i] != null) {
                checkBoxes[i].setOnCheckedChangeListener((b, checked) -> {
                    if (isSyncingUI) return;
                    boxEnabled[idx] = checked;
                    updateLayout();
                });
            }

            if (checkBoxesKeepActive[i] != null) {
                checkBoxesKeepActive[i].setOnCheckedChangeListener((b, checked) -> {
                    if (isSyncingUI) return;
                    boxKeepActive[idx] = checked;
                    updateLayout();
                });
            }

            if (checkBoxFullVideo[i] != null) {
                checkBoxFullVideo[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (webViews[idx] != null) {
                        if (isChecked) enableFullBox(webViews[idx]);
                        else disableFullBox(webViews[idx]);
                    }
                });
            }
        }

        if (cbAllowScripts != null)
            cbAllowScripts.setOnCheckedChangeListener((b, c) -> {
                for (WebView wv : webViews) if (wv != null) wv.getSettings().setJavaScriptEnabled(c);
                Log.w(TAG, "User toggled WebView JavaScript: " + c);
            });
        if (cbAllowPopups != null)
            cbAllowPopups.setOnCheckedChangeListener((b, c) -> {
                for (WebView wv : webViews) if (wv != null) wv.getSettings().setJavaScriptCanOpenWindowsAutomatically(c);
            });
        if (cbBlockAds != null)
            cbBlockAds.setOnCheckedChangeListener((b, c) -> {
                // O bloqueio é feito por shouldInterceptRequest (apenas domínios de
                // anúncios); setBlockNetworkLoads está removido — bloqueava o vídeo todo.
                for (WebView wv : webViews) if (wv != null && c) injectAdBlocker(wv);
            });
        if (cbKeepScreenOn != null)
            cbKeepScreenOn.setOnCheckedChangeListener((b, c) -> applyKeepScreenOn(c));

        if (cbAllowHttp != null)
            cbAllowHttp.setOnCheckedChangeListener((b, c) -> {
                if (isSyncingUI) return;
                preferences.edit().putBoolean("allow_http", c).apply();
                applyMixedContent();
                reloadActiveBoxes();
                Toast.makeText(this, c ? "🌐 http:// permitido" : "🚫 http:// bloqueado",
                        Toast.LENGTH_SHORT).show();
            });

        if (cbBoxCap != null)
            cbBoxCap.setOnCheckedChangeListener((b, c) -> {
                if (isSyncingUI) return;
                preferences.edit().putInt(KEY_BOX_CAP, c ? MAX : 4).apply();
                applyBoxCap();
                updateLayout();
            });
    }

    /** Recarrega só as boxes visíveis — as desligadas não têm página para recarregar. */
    private void reloadActiveBoxes() {
        for (int i = 0; i < MAX; i++)
            if (webViews[i] != null && (boxEnabled[i] || boxKeepActive[i]) && webViews[i].getUrl() != null)
                webViews[i].reload();
    }

    private void loadURL(int idx, String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://"))
                url = "https://" + url;
            if (!httpAllowed() && isPlainHttp(url)) {
                // Sem toast durante a reposição de estado: 6 caixas = 6 toasts em fila.
                if (isSyncingUI) Log.w(TAG, "http:// ignorado no arranque: " + url);
                else Toast.makeText(this, "🚫 http:// bloqueado (sidebar → Permitir http)",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (webViews[idx] != null) webViews[idx].loadUrl(url);
        } catch (Exception e) {
            Log.e(TAG, "loadURL " + idx, e);
        }
    }

    private void loadAllURLs() {
        for (int i = 0; i < MAX; i++) {
            if (!boxEnabled[i] && !boxKeepActive[i]) continue;
            String url = urlInputsSidebar[i] != null ? urlInputsSidebar[i].getText().toString().trim() : "";
            if (url.isEmpty()) {
                url = defaultUrl();
                if (urlInputsSidebar[i] != null) urlInputsSidebar[i].setText(url);
            }
            loadURL(i, url);
        }
        Toast.makeText(this, "Carregando todas as URLs", Toast.LENGTH_SHORT).show();
    }

    private void loadInitialURLs() {
        for (int i = 0; i < MAX; i++) {
            if (!boxEnabled[i] && !boxKeepActive[i]) continue;
            String url = urlInputsSidebar[i] != null ? urlInputsSidebar[i].getText().toString().trim() : "";
            if (url.isEmpty()) {
                url = defaultUrl();
                if (urlInputsSidebar[i] != null) urlInputsSidebar[i].setText(url);
            }
            loadURL(i, url);
        }
    }

    private void reloadAll() {
        for (int i = 0; i < MAX; i++) if (webViews[i] != null) webViews[i].reload();
        Toast.makeText(this, "Recarregando todas", Toast.LENGTH_SHORT).show();
    }

    private void clearAll() {
        for (int i = 0; i < MAX; i++) if (webViews[i] != null) webViews[i].loadUrl("about:blank");
        Toast.makeText(this, "Limpando todas", Toast.LENGTH_SHORT).show();
    }

    private String defaultUrl() {
        return "https://dzritv.com/sport/football/";
    }

    private void applyZoom(int i) {
        if (webViews[i] != null) webViews[i].getSettings().setTextZoom((int) (zoomLevels[i] * 100));
    }

    private void zoomIn(int i) {
        if (zoomLevels[i] < 2.0f) {
            zoomLevels[i] += 0.1f;
            applyZoom(i);
            Toast.makeText(this, "Box " + (i + 1) + " Zoom: " + Math.round(zoomLevels[i] * 100) + "%", Toast.LENGTH_SHORT).show();
        }
    }

    private void zoomOut(int i) {
        if (zoomLevels[i] > 0.5f) {
            zoomLevels[i] -= 0.1f;
            applyZoom(i);
            Toast.makeText(this, "Box " + (i + 1) + " Zoom: " + Math.round(zoomLevels[i] * 100) + "%", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasSavedState() {
        return preferences.contains("url_0") || preferences.contains("box_enabled_0");
    }

    private void saveCurrentState(boolean withToast) {
        try {
            SharedPreferences.Editor ed = preferences.edit();
            for (int i = 0; i < MAX; i++) {
                String url = urlInputsSidebar[i] != null ? urlInputsSidebar[i].getText().toString().trim() : "";
                ed.putString("url_" + i, url.isEmpty() ? defaultUrl() : url);
                ed.putBoolean("box_enabled_" + i, boxEnabled[i]);
                ed.putBoolean("box_keep_active_" + i, boxKeepActive[i]);
                ed.putFloat("zoom_level_" + i, zoomLevels[i]);
                ed.putBoolean("fullbox_" + i, checkBoxFullVideo[i] != null && checkBoxFullVideo[i].isChecked());
            }
            if (cbAllowScripts != null) ed.putBoolean("allow_scripts", cbAllowScripts.isChecked());
            if (cbAllowForms != null) ed.putBoolean("allow_forms", cbAllowForms.isChecked());
            if (cbAllowPopups != null) ed.putBoolean("allow_popups", cbAllowPopups.isChecked());
            if (cbBlockRedirects != null) ed.putBoolean("block_redirects", cbBlockRedirects.isChecked());
            if (cbBlockAds != null) ed.putBoolean("block_ads", cbBlockAds.isChecked());
            if (cbKeepScreenOn != null) ed.putBoolean("keep_screen_on", cbKeepScreenOn.isChecked());
            if (cbAllowHttp != null) ed.putBoolean("allow_http", cbAllowHttp.isChecked());
            ed.putInt(KEY_BOX_CAP, maxBoxes);
            ed.apply();
            if (withToast) Toast.makeText(this, "✅ Estado guardado!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            if (withToast) Toast.makeText(this, "❌ Erro ao guardar estado", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedState(boolean silent) {
        try {
            isSyncingUI = true;
            for (int i = 0; i < MAX; i++) {
                boxEnabled[i] = preferences.getBoolean("box_enabled_" + i, i < 4);
                boxKeepActive[i] = preferences.getBoolean("box_keep_active_" + i, false);
                zoomLevels[i] = preferences.getFloat("zoom_level_" + i, 1.0f);
                boolean fullboxChecked = preferences.getBoolean("fullbox_" + i, false);

                if (checkBoxes[i] != null) checkBoxes[i].setChecked(boxEnabled[i]);
                if (checkBoxesKeepActive[i] != null) checkBoxesKeepActive[i].setChecked(boxKeepActive[i]);
                if (checkBoxFullVideo[i] != null) checkBoxFullVideo[i].setChecked(fullboxChecked);
                applyZoom(i);
            }
            // "Permitir http" e o teto de boxes repõem-se antes de carregar URLs:
            // assim as boxes fora do limite e os URLs http:// nem chegam a navegar.
            if (cbAllowHttp != null) cbAllowHttp.setChecked(preferences.getBoolean("allow_http", true));
            applyMixedContent();
            if (cbBoxCap != null) cbBoxCap.setChecked(configuredBoxCap() >= MAX);
            applyBoxCap();
            boolean hasUrls = false;
            for (int i = 0; i < MAX; i++) {
                String url = preferences.getString("url_" + i, "");
                if (!url.isEmpty()) {
                    hasUrls = true;
                    if (urlInputsSidebar[i] != null) urlInputsSidebar[i].setText(url);
                    if ((boxEnabled[i] || boxKeepActive[i]) && webViews[i] != null) {
                        loadURL(i, url);
                        if (checkBoxFullVideo[i] != null && checkBoxFullVideo[i].isChecked()) {
                            int finalI = i;
                            webViews[i].postDelayed(() -> enableFullBox(webViews[finalI]), 500);
                        }
                    }
                }
            }
            if (!hasUrls) {
                String d = defaultUrl();
                for (int i = 0; i < MAX; i++) {
                    if (urlInputsSidebar[i] != null) urlInputsSidebar[i].setText(d);
                }
            }
            if (cbAllowScripts != null) cbAllowScripts.setChecked(preferences.getBoolean("allow_scripts", true));
            if (cbAllowForms != null) cbAllowForms.setChecked(preferences.getBoolean("allow_forms", true));
            if (cbAllowPopups != null) cbAllowPopups.setChecked(preferences.getBoolean("allow_popups", true));
            if (cbBlockRedirects != null) cbBlockRedirects.setChecked(preferences.getBoolean("block_redirects", false));
            if (cbBlockAds != null) cbBlockAds.setChecked(preferences.getBoolean("block_ads", false));
            if (cbKeepScreenOn != null) cbKeepScreenOn.setChecked(preferences.getBoolean("keep_screen_on", true));
            if (!silent) Toast.makeText(this, "✅ Estado carregado!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            if (!silent) Toast.makeText(this, "❌ Erro ao carregar", Toast.LENGTH_SHORT).show();
        } finally {
            isSyncingUI = false;
            updateLayout();
        }
    }

    // ── favoritos ─────────────────────────────────────────────────────────────
    private void loadFavoritesList() {
        try {
            JSONArray a = new JSONArray(preferences.getString("favorites_list", "[]"));
            favoritesList.clear();
            for (int i = 0; i < a.length(); i++) favoritesList.add(a.getString(i));
        } catch (Exception e) {
            favoritesList.clear();
        }
    }

    private void saveFavoritesList() {
        try {
            preferences.edit().putString("favorites_list", new JSONArray(favoritesList).toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void saveFavorite(String name) {
        try {
            if (favoritesList.contains(name)) {
                Toast.makeText(this, "❌ Nome já existe!", Toast.LENGTH_SHORT).show();
                return;
            }
            JSONArray urls = new JSONArray();
            for (int i = 0; i < MAX; i++) {
                String u = urlInputsSidebar[i] != null ? urlInputsSidebar[i].getText().toString().trim() : "";
                urls.put(u.isEmpty() ? defaultUrl() : u);
            }
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("urls", urls);
            favoritesList.add(name);
            saveFavoritesList();
            preferences.edit().putString("favorite_" + name, obj.toString()).apply();
            Toast.makeText(this, "✅ Favorito guardado!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "❌ Erro ao guardar favorito", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFavorite(String name, int target) {
        try {
            String json = preferences.getString("favorite_" + name, "");
            if (json.isEmpty()) {
                Toast.makeText(this, "❌ Não encontrado!", Toast.LENGTH_SHORT).show();
                return;
            }
            JSONArray urls = new JSONObject(json).getJSONArray("urls");
            Log.d(TAG, "Carregando favorito: " + name + ", target=" + target + ", urls=" + urls);

            if (target == -1) {
                for (int i = 0; i < MAX && i < urls.length(); i++) {
                    String u = urls.getString(i);
                    if (urlInputsSidebar[i] != null) urlInputsSidebar[i].setText(u);
                    if ((boxEnabled[i] || boxKeepActive[i]) && webViews[i] != null) {
                        loadURL(i, u);
                    }
                }
                Toast.makeText(this, "✅ Carregado em todas!", Toast.LENGTH_SHORT).show();
            } else if (target >= 0 && target < urls.length() && target < MAX) {
                String u = urls.getString(target);
                if (urlInputsSidebar[target] != null) urlInputsSidebar[target].setText(u);
                if (webViews[target] != null) loadURL(target, u);
                Toast.makeText(this, "✅ Box " + (target + 1), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar favorito", e);
            Toast.makeText(this, "❌ Erro ao carregar favorito", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFavorite(String name) {
        favoritesList.remove(name);
        saveFavoritesList();
        preferences.edit().remove("favorite_" + name).apply();
        Toast.makeText(this, "✅ Removido!", Toast.LENGTH_SHORT).show();
    }

    private void showSaveFavoriteDialog() {
        EditText input = new EditText(this);
        input.setHint("Nome do favorito");
        input.setTextColor(Color.BLACK);
        input.setBackgroundResource(android.R.drawable.edit_text);
        new AlertDialog.Builder(this)
                .setTitle("Guardar Favorito")
                .setView(input)
                .setPositiveButton("GUARDAR", (d, w) -> {
                    String n = input.getText().toString().trim();
                    if (!n.isEmpty()) saveFavorite(n);
                })
                .setNegativeButton("CANCELAR", null)
                .show();
        input.requestFocus();
    }

    private void showLoadFavoritesDialog() {
        if (favoritesList.isEmpty()) {
            Toast.makeText(this, "🔭 Sem favoritos!", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = favoritesList.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Carregar Favorito")
                .setItems(names, (d, w) -> showFavoriteOptionsDialog(names[w]))
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    private void showFavoriteOptionsDialog(String name) {
        String[] opts = new String[MAX + 3];
        opts[0] = "Carregar em Todas";
        for (int i = 0; i < MAX; i++) opts[i + 1] = "Box " + (i + 1);
        opts[MAX + 1] = "Eliminar";
        opts[MAX + 2] = "Cancelar";
        new AlertDialog.Builder(this)
                .setTitle("Favorito: " + name)
                .setItems(opts, (d, w) -> {
                    if (w == 0) loadFavorite(name, -1);
                    else if (w <= MAX) loadFavorite(name, w - 1);
                    else if (w == MAX + 1) showDeleteConfirmDialog(name);
                })
                .show();
    }

    private void showDeleteConfirmDialog(String name) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar '" + name + "'?")
                .setPositiveButton("ELIMINAR", (d, w) -> deleteFavorite(name))
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    // ── export / import JSON ──────────────────────────────────────────────────
    private String exportFileName() {
        return "multistream_viewer_config_" + System.currentTimeMillis() + ".json";
    }

    private JSONObject buildConfigJson() throws Exception {
        JSONObject cfg = new JSONObject();
        cfg.put("version", 1);

        JSONObject settings = new JSONObject();
        settings.put("allowScripts", cbAllowScripts != null && cbAllowScripts.isChecked());
        settings.put("allowForms", cbAllowForms != null && cbAllowForms.isChecked());
        settings.put("allowPopups", cbAllowPopups != null && cbAllowPopups.isChecked());
        settings.put("blockRedirects", cbBlockRedirects != null && cbBlockRedirects.isChecked());
        settings.put("blockAds", cbBlockAds != null && cbBlockAds.isChecked());
        settings.put("keepScreenOn", cbKeepScreenOn == null || cbKeepScreenOn.isChecked());
        settings.put("allowHttp", cbAllowHttp == null || cbAllowHttp.isChecked());
        settings.put("boxCap", configuredBoxCap());
        settings.put("orientation", currentOrientation);
        cfg.put("settings", settings);

        JSONObject presets = new JSONObject();
        for (Map.Entry<String, ?> e : preferences.getAll().entrySet()) {
            if (e.getKey().startsWith(KEY_PRESET_PREFIX)) presets.put(e.getKey(), String.valueOf(e.getValue()));
        }
        cfg.put("layoutPresets", presets);
        cfg.put("layoutWeights", new JSONObject(preferences.getString(KEY_WEIGHTS, "{}")));

        JSONArray boxes = new JSONArray();
        for (int i = 0; i < MAX; i++) {
            JSONObject b = new JSONObject();
            b.put("url", urlInputsSidebar[i] != null ? urlInputsSidebar[i].getText().toString().trim() : "");
            b.put("enabled", boxEnabled[i]);
            b.put("keepActive", boxKeepActive[i]);
            b.put("zoom", zoomLevels[i]);
            b.put("fullbox", checkBoxFullVideo[i] != null && checkBoxFullVideo[i].isChecked());
            boxes.put(b);
        }
        cfg.put("boxes", boxes);

        JSONArray favs = new JSONArray();
        for (String name : favoritesList) {
            String raw = preferences.getString("favorite_" + name, null);
            if (raw != null) favs.put(new JSONObject(raw));
        }
        cfg.put("favorites", favs);
        return cfg;
    }

    private void writeConfigTo(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
            if (os == null) throw new IllegalStateException("sem stream");
            os.write(buildConfigJson().toString(2).getBytes("UTF-8"));
            Toast.makeText(this, "📤 Config exportada", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "export", e);
            Toast.makeText(this, "❌ Erro ao exportar", Toast.LENGTH_SHORT).show();
        }
    }

    private void readConfigFrom(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new IllegalStateException("sem stream");
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) > 0) bos.write(buf, 0, r);
            applyConfigJson(new JSONObject(bos.toString("UTF-8")));
        } catch (Exception e) {
            Log.e(TAG, "import", e);
            Toast.makeText(this, "❌ Ficheiro inválido", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyConfigJson(JSONObject cfg) throws Exception {
        SharedPreferences.Editor ed = preferences.edit();

        JSONObject settings = cfg.optJSONObject("settings");
        if (settings != null) {
            ed.putBoolean("allow_scripts", settings.optBoolean("allowScripts", true));
            ed.putBoolean("allow_forms", settings.optBoolean("allowForms", true));
            ed.putBoolean("allow_popups", settings.optBoolean("allowPopups", true));
            ed.putBoolean("block_redirects", settings.optBoolean("blockRedirects", false));
            ed.putBoolean("block_ads", settings.optBoolean("blockAds", false));
            ed.putBoolean("keep_screen_on", settings.optBoolean("keepScreenOn", true));
            ed.putBoolean("allow_http", settings.optBoolean("allowHttp", true));
            // Um Fire TV tem RAM para ~4 WebViews: um teto 6 vindo de um telemóvel
            // é travado aqui; o utilizador levanta-o na checkbox da sidebar.
            int cap = settings.optInt("boxCap", isFireTv() ? 4 : MAX);
            ed.putInt(KEY_BOX_CAP, isFireTv() ? Math.min(cap, 4) : cap);
        }

        JSONObject presets = cfg.optJSONObject("layoutPresets");
        if (presets != null) {
            for (int n = 1; n <= MAX; n++) {
                String v = presets.optString(KEY_PRESET_PREFIX + n, null);
                if (v != null) ed.putString(KEY_PRESET_PREFIX + n, v);
            }
        }
        JSONObject weights = cfg.optJSONObject("layoutWeights");
        ed.putString(KEY_WEIGHTS, weights != null ? weights.toString() : "{}");

        JSONArray boxes = cfg.optJSONArray("boxes");
        if (boxes != null) {
            for (int i = 0; i < MAX && i < boxes.length(); i++) {
                JSONObject b = boxes.getJSONObject(i);
                ed.putString("url_" + i, b.optString("url", defaultUrl()));
                ed.putBoolean("box_enabled_" + i, b.optBoolean("enabled", i < 4));
                ed.putBoolean("box_keep_active_" + i, b.optBoolean("keepActive", false));
                ed.putFloat("zoom_level_" + i, (float) b.optDouble("zoom", 1.0));
                ed.putBoolean("fullbox_" + i, b.optBoolean("fullbox", false));
            }
        }

        // favoritos: substituir todos os atuais
        for (String key : new ArrayList<>(preferences.getAll().keySet())) {
            if (key.startsWith("favorite_")) ed.remove(key);
        }
        JSONArray favs = cfg.optJSONArray("favorites");
        JSONArray names = new JSONArray();
        if (favs != null) {
            for (int i = 0; i < favs.length(); i++) {
                JSONObject f = favs.getJSONObject(i);
                String name = f.optString("name", "");
                if (name.isEmpty() || !f.has("urls")) continue;
                names.put(name);
                ed.putString("favorite_" + name, f.toString());
            }
        }
        ed.putString("favorites_list", names.toString());
        ed.apply();

        loadFavoritesList();
        loadSavedState(false);
        if (cbKeepScreenOn != null) applyKeepScreenOn(cbKeepScreenOn.isChecked());
        Toast.makeText(this, "📥 Config importada", Toast.LENGTH_SHORT).show();
    }

    // ── sidebar ───────────────────────────────────────────────────────────────
    public void closeSidebarFromOverlay(View v) {
        closeSidebar();
    }

    private void closeSidebar() {
        android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofFloat(sidebarContainer, "alpha", 1f, 0f);
        a.setDuration(250);
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator an) {
                sidebarContainer.setVisibility(View.GONE);
                isSidebarVisible = false;
                applyGridSize();
                hideKeyboard();
                if (btnToggleSidebar != null) btnToggleSidebar.requestFocus();
            }
        });
        a.start();
    }

    private void openSidebar() {
        sidebarContainer.setVisibility(View.VISIBLE);
        sidebarContainer.setAlpha(0f);
        isSidebarVisible = true;
        applyGridSize();
        android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofFloat(sidebarContainer, "alpha", 0f, 1f);
        a.setDuration(250);
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator an) {
                for (int i = 0; i < MAX; i++)
                    if (urlInputsSidebar[i] != null)
                        urlInputsSidebar[i].setText(urlInputsSidebar[i].getText());
                if (btnCloseSidebar != null) btnCloseSidebar.requestFocus();
            }
        });
        a.start();
    }

    private void updateFocusedBoxIndicator() {
        if (tvFocusedBox != null) tvFocusedBox.setText("Foco: " + (focusedBoxIndex + 1));
    }

    private void setFocusBorder(int idx, boolean focused) {
        if (idx < 0 || idx >= MAX || boxContainers[idx] == null) return;
        if (focused) {
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(Color.BLACK);
            gd.setStroke(4, Color.YELLOW);
            boxContainers[idx].setBackground(gd);
        } else {
            boxContainers[idx].setBackgroundColor(Color.BLACK);
        }
    }

    private void scrollWebView(int i, int dy) {
        if (webViews[i] != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            webViews[i].evaluateJavascript("window.scrollBy(0," + dy + ");", null);
    }

    private void showKeyboard(View v) {
        v.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        }, 100);
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void injectAdBlocker(WebView v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            v.evaluateJavascript("try{['div[class*=\"ad\"]','ins.adsbygoogle'].forEach(s=>document.querySelectorAll(s).forEach(e=>e.style.display='none'));}catch(e){}", null);
    }

    private boolean isAdUrl(String url) {
        String u = url.toLowerCase();
        for (String d : adDomains) if (u.contains(d)) return true;
        return false;
    }

    private String getDomain(String url) {
        try {
            String h = new java.net.URI(url).getHost();
            return h != null ? h.replace("www.", "") : url;
        } catch (Exception e) {
            return url;
        }
    }

    private boolean isSameDomain(String a, String b) {
        try {
            return getDomain(a).equals(getDomain(b));
        } catch (Exception e) {
            return false;
        }
    }

    private void clearAppCache() {
        try {
            for (WebView wv : webViews) if (wv != null) {
                wv.clearCache(true);
                wv.clearHistory();
            }
            if (getCacheDir() != null) deleteDir(getCacheDir());
        } catch (Exception ignored) {
        }
    }

    private boolean deleteDir(java.io.File dir) {
        if (dir.isDirectory()) {
            String[] c = dir.list();
            if (c != null) for (String s : c) if (!deleteDir(new java.io.File(dir, s))) return false;
        }
        return dir.delete();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentState(false);
        for (WebView wv : webViews) if (wv != null) wv.onPause();
        WebView.pauseTimers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebView.resumeTimers();
        for (WebView wv : webViews) if (wv != null) wv.onResume();
        loadFavoritesList();
        if (btnToggleSidebar != null) btnToggleSidebar.requestFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // O clearAppCache() foi removido daqui: apagar o cache em cada fecho
        // obrigava a recarregar tudo do zero no arranque seguinte.
        for (int i = 0; i < MAX; i++) {
            WebView wv = webViews[i];
            if (wv == null) continue;
            if (wv.getParent() != null) ((ViewGroup) wv.getParent()).removeView(wv);
            wv.stopLoading();
            wv.setWebViewClient(null);
            wv.setWebChromeClient(null);
            wv.destroy();
            webViews[i] = null;
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
                    if (focusedBoxIndex >= 0 && focusedBoxIndex < MAX
                            && webViews[focusedBoxIndex] != null && webViews[focusedBoxIndex].canGoBack()) {
                        webViews[focusedBoxIndex].goBack();
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_MENU:
                    if (isSidebarVisible) closeSidebar();
                    else openSidebar();
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (isSidebarVisible) {
                        View focused = getCurrentFocus();
                        if (focused != null) {
                            focused.performClick();
                            return true;
                        }
                    }
                    break;
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

    @Override
    public void onBackPressed() {
        if (isSidebarVisible) {
            closeSidebar();
            return;
        }
        if (focusedBoxIndex >= 0 && focusedBoxIndex < MAX
                && webViews[focusedBoxIndex] != null && webViews[focusedBoxIndex].canGoBack()) {
            webViews[focusedBoxIndex].goBack();
            return;
        }
        for (WebView wv : webViews) if (wv != null && wv.canGoBack()) {
            wv.goBack();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Sair?")
                .setMessage("Deseja sair do app?")
                .setPositiveButton("SIM", (d, w) -> finish())
                .setNegativeButton("NÃO", null)
                .show();
    }
}
