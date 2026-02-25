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
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
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
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║  IMPORTANT – AndroidManifest.xml must have this on the Activity: ║
 * ║  android:configChanges="orientation|screenSize|screenLayout      ║
 * ║      |keyboardHidden|smallestScreenSize"                         ║
 * ║  Without it the Activity restarts on rotation and fullscreen     ║
 * ║  state is lost.                                                  ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class MainActivity extends AppCompatActivity {

    // ─── Device type ──────────────────────────────────────────────────────────
    public enum DeviceType { FIRE_TV, TABLET, PHONE }
    private DeviceType deviceType = DeviceType.PHONE;

    // ─── Views ────────────────────────────────────────────────────────────────
    private GridLayout    gridLayout;
    private FrameLayout[] boxContainers = new FrameLayout[4];
    private WebView[]     webViews      = new WebView[4];

    private LinearLayout  bottomControls, expandedBottomBar;
    private FrameLayout   sidebarContainer;
    private RelativeLayout mainLayout;
    private TextView      tvFocusedBox;

    private Button btnToggleBottomBar, btnToggleSidebar, btnToggleOrientation;
    private Button btnCloseMenu, btnCloseSidebar;
    private Button btnLoadAll, btnReloadAll, btnClearAll;
    private Button btnSaveState, btnLoadState, btnSaveFavorites, btnLoadFavorites;

    // Per-box bottom-bar controls
    private Button[]   btnRefresh  = new Button[4];
    private Button[]   btnZoomIn   = new Button[4];
    private Button[]   btnZoomOut  = new Button[4];
    private Button[]   btnPrevious = new Button[4];
    private Button[]   btnNext     = new Button[4];
    private Button[]   btnLoadUrl  = new Button[4];
    private EditText[] urlInputs   = new EditText[4];
    private CheckBox[] checkBoxes           = new CheckBox[4];
    private CheckBox[] checkBoxesKeepActive = new CheckBox[4];

    // Sidebar controls (unique IDs to avoid duplicate-ID build errors)
    private EditText[] urlInputsSidebar  = new EditText[4];
    private Button[]   btnLoadUrlSidebar = new Button[4];
    private Button btnLoadAllSidebar, btnReloadAllSidebar, btnClearAllSidebar;
    private Button btnSaveStateSidebar, btnLoadStateSidebar;
    private Button btnSaveFavoritesSidebar, btnLoadFavoritesSidebar;

    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups, cbBlockRedirects, cbBlockAds;

    // ─── State ────────────────────────────────────────────────────────────────
    private boolean[] boxEnabled    = {true, true, true, true};
    private boolean[] boxKeepActive = {false, false, false, false};
    private boolean   isSidebarVisible    = false;
    private boolean   isBottomBarExpanded = false;
    private int       focusedBoxIndex     = 0;
    private float[]   zoomLevels          = {1.0f, 1.0f, 1.0f, 1.0f};

    /**
     * Tracks the LOGICAL orientation chosen by the user (may differ from
     * getResources().getConfiguration().orientation while the system is
     * still animating the rotation).
     * PORTRAIT = 1, LANDSCAPE = 2  (same values as Configuration constants)
     */
    private int currentOrientation = Configuration.ORIENTATION_LANDSCAPE;

    // Fullscreen support
    private boolean[]                            boxInFullscreen    = new boolean[4];
    private View[]                               fullscreenViews    = new View[4];
    private WebChromeClient.CustomViewCallback[] fullscreenCallbacks = new WebChromeClient.CustomViewCallback[4];

    private ArrayList<String> favoritesList = new ArrayList<>();
    private SharedPreferences preferences;

    private final List<String> adDomains = Arrays.asList(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com");

    private static final String TAG = "MultiStreamViewer";

    // =========================================================================
    //  LIFECYCLE
    // =========================================================================

    @SuppressLint({"SetJavaScriptEnabled","ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceType = detectDeviceType();
        Log.d(TAG, "DeviceType = " + deviceType);

        applyOrientationForDevice();
        setContentView(R.layout.activity_main);

        // Read the REAL current orientation AFTER setContentView
        currentOrientation = getResources().getConfiguration().orientation;

        preferences = getSharedPreferences("MultiStreamViewer", MODE_PRIVATE);

        initViews();
        initWebViews();
        initEventListeners();

        loadSavedState(true);
        loadFavoritesList();

        new Handler().postDelayed(() -> {
            if (!favoritesList.isEmpty())
                Toast.makeText(this, "✅ " + favoritesList.size() + " Favoritos carregados!", Toast.LENGTH_SHORT).show();
        }, 1500);

        // Wait for the GridLayout to be measured before the first layout pass
        gridLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                gridLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                updateLayout();
                updateFocusedBoxIndicator();
                if (!hasSavedState()) {
                    new Handler().postDelayed(MainActivity.this::loadInitialURLs, 500);
                }
            }
        });
    }

    // =========================================================================
    //  DEVICE DETECTION
    // =========================================================================

    private DeviceType detectDeviceType() {
        if (isFireTV()) return DeviceType.FIRE_TV;
        if (isTablet()) return DeviceType.TABLET;
        return DeviceType.PHONE;
    }

    private boolean isFireTV() {
        if (getPackageManager().hasSystemFeature("amazon.hardware.fire_tv")) return true;
        String model  = Build.MODEL   != null ? Build.MODEL.toUpperCase()   : "";
        String device = Build.DEVICE  != null ? Build.DEVICE.toUpperCase()  : "";
        String prod   = Build.PRODUCT != null ? Build.PRODUCT.toUpperCase() : "";
        String brand  = Build.BRAND   != null ? Build.BRAND.toUpperCase()   : "";
        if (model.startsWith("AFT"))                              return true;
        if (device.contains("MONTOYA") || prod.contains("MONTOYA")) return true;
        if (brand.equals("AMAZON") && (model.contains("FIRE") || device.contains("FIRE"))) return true;
        return false;
    }

    private boolean isTablet() {
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        float w = dm.widthPixels / dm.xdpi;
        float h = dm.heightPixels / dm.ydpi;
        return Math.sqrt(w * w + h * h) >= 6.0;
    }

    private void applyOrientationForDevice() {
        switch (deviceType) {
            case FIRE_TV:
            case TABLET:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
                break;
            case PHONE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                currentOrientation = Configuration.ORIENTATION_PORTRAIT;
                break;
        }
    }

    // =========================================================================
    //  MOUSE / HOVER EVENTS
    // =========================================================================

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        int src = event.getSource();
        if ((src & InputDevice.SOURCE_CLASS_POINTER) != 0) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_HOVER_MOVE:
                    if (webViews[focusedBoxIndex] != null)
                        webViews[focusedBoxIndex].onGenericMotionEvent(event);
                    return true;
                case MotionEvent.ACTION_SCROLL:
                    float sy = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                    float sx = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
                    scrollWebViewXY(focusedBoxIndex, (int)(-sx * 80), (int)(-sy * 80));
                    return true;
                case MotionEvent.ACTION_BUTTON_PRESS:
                    if (event.getActionButton() == MotionEvent.BUTTON_TERTIARY) {
                        if (webViews[focusedBoxIndex] != null) webViews[focusedBoxIndex].reload();
                        return true;
                    }
            }
        }
        if ((src & InputDevice.SOURCE_CLASS_TRACKBALL) != 0) {
            if (webViews[focusedBoxIndex] != null)
                webViews[focusedBoxIndex].onTrackballEvent(event);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int tx = (int) event.getX(), ty = (int) event.getY();
            for (int i = 0; i < 4; i++) {
                if (boxContainers[i] == null || boxContainers[i].getVisibility() != View.VISIBLE) continue;
                int[] loc = new int[2];
                boxContainers[i].getLocationOnScreen(loc);
                if (tx >= loc[0] && tx <= loc[0] + boxContainers[i].getWidth()
                 && ty >= loc[1] && ty <= loc[1] + boxContainers[i].getHeight()) {
                    focusedBoxIndex = i;
                    updateFocusedBoxIndicator();
                    highlightFocusedBox(i);
                    break;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    // =========================================================================
    //  INIT VIEWS
    // =========================================================================

    private void initViews() {
        gridLayout          = findViewById(R.id.gridLayout);
        bottomControls      = findViewById(R.id.bottomControls);
        expandedBottomBar   = findViewById(R.id.expandedBottomBar);
        sidebarContainer    = findViewById(R.id.sidebarContainer);
        mainLayout          = findViewById(R.id.main_layout);
        tvFocusedBox        = findViewById(R.id.tvFocusedBox);

        btnToggleBottomBar   = findViewById(R.id.btnToggleBottomBar);
        btnToggleSidebar     = findViewById(R.id.btnToggleSidebar);
        btnToggleOrientation = findViewById(R.id.btnToggleOrientation);
        btnCloseMenu         = findViewById(R.id.btnCloseMenu);
        btnCloseSidebar      = findViewById(R.id.btnCloseSidebar);

        btnLoadAll     = findViewById(R.id.btnLoadAll);
        btnReloadAll   = findViewById(R.id.btnReloadAll);
        btnClearAll    = findViewById(R.id.btnClearAll);
        btnSaveState   = findViewById(R.id.btnSaveState);
        btnLoadState   = findViewById(R.id.btnLoadState);
        btnSaveFavorites = findViewById(R.id.btnSaveFavorites);
        btnLoadFavorites = findViewById(R.id.btnLoadFavorites);

        btnSaveStateSidebar     = findViewById(R.id.btnSaveStateSidebar);
        btnLoadStateSidebar     = findViewById(R.id.btnLoadStateSidebar);
        btnSaveFavoritesSidebar = findViewById(R.id.btnSaveFavoritesSidebar);
        btnLoadFavoritesSidebar = findViewById(R.id.btnLoadFavoritesSidebar);
        btnLoadAllSidebar       = findViewById(R.id.btnLoadAllSidebar);
        btnReloadAllSidebar     = findViewById(R.id.btnReloadAllSidebar);
        btnClearAllSidebar      = findViewById(R.id.btnClearAllSidebar);

        cbAllowScripts  = findViewById(R.id.cbAllowScripts);
        cbAllowForms    = findViewById(R.id.cbAllowForms);
        cbAllowPopups   = findViewById(R.id.cbAllowPopups);
        cbBlockRedirects= findViewById(R.id.cbBlockRedirects);
        cbBlockAds      = findViewById(R.id.cbBlockAds);

        int[] cbId  = {R.id.checkBox1,          R.id.checkBox2,          R.id.checkBox3,          R.id.checkBox4};
        int[] kaId  = {R.id.checkBoxKeepActive1, R.id.checkBoxKeepActive2, R.id.checkBoxKeepActive3, R.id.checkBoxKeepActive4};
        int[] rfId  = {R.id.btnRefresh1,  R.id.btnRefresh2,  R.id.btnRefresh3,  R.id.btnRefresh4};
        int[] ziId  = {R.id.btnZoomIn1,   R.id.btnZoomIn2,   R.id.btnZoomIn3,   R.id.btnZoomIn4};
        int[] zoId  = {R.id.btnZoomOut1,  R.id.btnZoomOut2,  R.id.btnZoomOut3,  R.id.btnZoomOut4};
        int[] pvId  = {R.id.btnPrevious1, R.id.btnPrevious2, R.id.btnPrevious3, R.id.btnPrevious4};
        int[] nxId  = {R.id.btnNext1,     R.id.btnNext2,     R.id.btnNext3,     R.id.btnNext4};
        int[] goId  = {R.id.btnLoadUrl1,  R.id.btnLoadUrl2,  R.id.btnLoadUrl3,  R.id.btnLoadUrl4};
        int[] ulId  = {R.id.urlInput1,    R.id.urlInput2,    R.id.urlInput3,    R.id.urlInput4};
        int[] usbId = {R.id.urlInputSidebar1,  R.id.urlInputSidebar2,  R.id.urlInputSidebar3,  R.id.urlInputSidebar4};
        int[] gsbId = {R.id.btnLoadUrlSidebar1, R.id.btnLoadUrlSidebar2, R.id.btnLoadUrlSidebar3, R.id.btnLoadUrlSidebar4};

        String def = "https://dzritv.com/sport/football/";
        for (int i = 0; i < 4; i++) {
            checkBoxes[i]           = findViewById(cbId[i]);
            checkBoxesKeepActive[i] = findViewById(kaId[i]);
            btnRefresh[i]           = findViewById(rfId[i]);
            btnZoomIn[i]            = findViewById(ziId[i]);
            btnZoomOut[i]           = findViewById(zoId[i]);
            btnPrevious[i]          = findViewById(pvId[i]);
            btnNext[i]              = findViewById(nxId[i]);
            btnLoadUrl[i]           = findViewById(goId[i]);
            urlInputs[i]            = findViewById(ulId[i]);
            urlInputsSidebar[i]     = findViewById(usbId[i]);
            btnLoadUrlSidebar[i]    = findViewById(gsbId[i]);

            setupUrlInput(urlInputs[i],        def, i, false);
            setupUrlInput(urlInputsSidebar[i], def, i, true);
        }
    }

    private void setupUrlInput(EditText et, String def, int idx, boolean sidebar) {
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
    private void syncToBottomBar(int i, String url) { if (urlInputs[i] != null)        urlInputs[i].setText(url); }

    // =========================================================================
    //  INIT WEB VIEWS
    // =========================================================================

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViews() {
        for (int i = 0; i < 4; i++) {
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
                    highlightFocusedBox(idx);
                } else if (!hasFocus) {
                    boxContainers[idx].setBackgroundColor(Color.BLACK);
                }
            });
            boxContainers[i].setOnClickListener(v -> {
                if (!isSidebarVisible) { focusedBoxIndex = idx; updateFocusedBoxIndicator(); boxContainers[idx].requestFocus(); }
            });
            boxContainers[i].setOnHoverListener((v, ev) -> {
                if (ev.getAction() == MotionEvent.ACTION_HOVER_ENTER && !isSidebarVisible) {
                    focusedBoxIndex = idx;
                    updateFocusedBoxIndicator();
                    highlightFocusedBox(idx);
                } else if (ev.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                    boxContainers[idx].setBackgroundColor(Color.BLACK);
                }
                if (webViews[idx] != null) webViews[idx].onGenericMotionEvent(ev);
                return true;
            });

            webViews[i] = new WebView(this);
            webViews[i].setId(View.generateViewId());
            setupWebView(webViews[i], i);
            boxContainers[i].addView(webViews[i], new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            gridLayout.addView(boxContainers[i]);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView wv, int idx) {
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
        s.setNeedInitialFocus(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(buildUserAgent());
        s.setTextZoom((int)(zoomLevels[idx] * 100));
        wv.setInitialScale((int)(zoomLevels[idx] * 100));
        wv.setBackgroundColor(Color.BLACK);
        wv.setVerticalScrollBarEnabled(true);
        wv.setHorizontalScrollBarEnabled(false);
        wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        wv.setFocusable(true);
        wv.setFocusableInTouchMode(true);

        wv.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return handleUrl(v, r.getUrl().toString()); }
            @Override public boolean shouldOverrideUrlLoading(WebView v, String url)           { return handleUrl(v, url); }
            private boolean handleUrl(WebView v, String url) {
                if (cbBlockRedirects != null && cbBlockRedirects.isChecked()) {
                    String cur = v.getUrl();
                    if (cur != null && !isSameDomain(cur, url)) return true;
                }
                if (cbBlockAds != null && cbBlockAds.isChecked() && isAdUrl(url)) return true;
                if (url.contains("youtube.com") || url.contains("twitch.tv") || url.contains("stream")) {
                    v.loadUrl(url); return true;
                }
                return false;
            }
            @Override public void onPageFinished(WebView v, String url) {
                applyZoom(idx);
                if (cbBlockAds != null && cbBlockAds.isChecked()) injectAdBlocker(v);
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback cb) {
                boxInFullscreen[idx] = true;
                fullscreenViews[idx] = view;
                fullscreenCallbacks[idx] = cb;
                if (boxContainers[idx].getChildCount() > 1) boxContainers[idx].removeViewAt(1);
                boxContainers[idx].addView(view, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                wv.setVisibility(View.GONE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                Log.d(TAG, "Box " + (idx+1) + " entered fullscreen");
            }
            @Override public void onHideCustomView() {
                if (fullscreenViews[idx] == null) return;
                if (boxContainers[idx].indexOfChild(fullscreenViews[idx]) != -1)
                    boxContainers[idx].removeView(fullscreenViews[idx]);
                wv.setVisibility(View.VISIBLE);
                if (fullscreenCallbacks[idx] != null) fullscreenCallbacks[idx].onCustomViewHidden();
                fullscreenViews[idx] = null; fullscreenCallbacks[idx] = null; boxInFullscreen[idx] = false;
                if (!isAnyBoxInFullscreen()) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    new Handler(getMainLooper()).post(MainActivity.this::updateLayout);
                }
            }
        });
    }

    private String buildUserAgent() {
        switch (deviceType) {
            case FIRE_TV: return "Mozilla/5.0 (Linux; Android 9; AFTMM Build/PS7233; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.120 Mobile Safari/537.36";
            case TABLET:  return "Mozilla/5.0 (Linux; Android 11; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Safari/537.36";
            default:      return "Mozilla/5.0 (Linux; Android 11; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36";
        }
    }



    // =========================================================================
    //  LAYOUT UPDATE  (fixed for portrait box toggle + orientation)
    // =========================================================================

    /**
     * Rebuilds the GridLayout contents based on the current orientation and
     * which boxes are enabled.
     *
     * FIX 1: Portrait checkbox toggle was broken because GridLayout weight-based
     *         sizing requires an explicit size.  We use a post() to read the
     *         actual measured size and compute explicit item heights/widths.
     *
     * FIX 2: If any box is in fullscreen, we leave the layout completely alone
     *         so the custom video view is not disturbed.
     */
    private void updateLayout() {
        // Never touch the layout while a video is in fullscreen mode
        if (isAnyBoxInFullscreen()) {
            Log.d(TAG, "updateLayout: skipped – fullscreen active");
            return;
        }

        // Count enabled boxes
        int active = 0;
        for (boolean e : boxEnabled) if (e) active++;

        // At least one box must be visible
        if (active == 0) {
            for (int i = 0; i < 4; i++) {
                boxEnabled[i] = true;
                if (checkBoxes[i] != null) checkBoxes[i].setChecked(true);
            }
            active = 4;
        }

        final int finalActive = active;
        final boolean portrait = (currentOrientation == Configuration.ORIENTATION_PORTRAIT);

        int rows, cols;
        if (portrait) {
            // Portrait: stack vertically
            switch (active) { case 1: rows=1;cols=1;break; case 2: rows=2;cols=1;break;
                              case 3: rows=3;cols=1;break; default: rows=2;cols=2;break; }
        } else {
            // Landscape: spread horizontally
            switch (active) { case 1: rows=1;cols=1;break; case 2: rows=1;cols=2;break;
                              case 3: rows=1;cols=3;break; default: rows=2;cols=2;break; }
        }

        final int finalRows = rows;
        final int finalCols = cols;

        gridLayout.removeAllViews();
        gridLayout.setRowCount(rows);
        gridLayout.setColumnCount(cols);

        // ── Hide / show containers BEFORE measuring ────────────────────────
        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i]) {
                if (boxContainers[i] != null) boxContainers[i].setVisibility(View.VISIBLE);
            } else if (boxKeepActive[i]) {
                if (boxContainers[i] != null) boxContainers[i].setVisibility(View.INVISIBLE);
            } else {
                if (boxContainers[i] != null) boxContainers[i].setVisibility(View.GONE);
            }
        }

        // ── Apply layout params with explicit sizes (fixes portrait weight bug) ──
        // We post to the next frame so gridLayout has been measured first
        gridLayout.post(() -> {
            if (isAnyBoxInFullscreen()) return;  // guard again after post

            int gridW = gridLayout.getWidth();
            int gridH = gridLayout.getHeight();

            // Fallback if not yet measured
            if (gridW <= 0 || gridH <= 0) {
                gridW = getResources().getDisplayMetrics().widthPixels;
                gridH = getResources().getDisplayMetrics().heightPixels / 2;
            }

            int margin = finalActive <= 1 ? 0 : finalActive == 2 ? 2 : 1;
            int itemW  = (gridW  - margin * 2 * finalCols) / finalCols;
            int itemH  = (gridH  - margin * 2 * finalRows) / finalRows;

            int pos = 0;
            for (int i = 0; i < 4; i++) {
                if (!boxEnabled[i]) continue;  // skip disabled (they're GONE or INVISIBLE)
                if (boxContainers[i] == null) { pos++; continue; }

                int r = pos / finalCols;
                int c = pos % finalCols;

                GridLayout.Spec rowSpec = GridLayout.spec(r, 1, 1f);
                GridLayout.Spec colSpec = GridLayout.spec(c, 1, 1f);
                GridLayout.LayoutParams p = new GridLayout.LayoutParams(rowSpec, colSpec);

                // Explicit pixel sizes so portrait weights work correctly
                p.width  = itemW > 0 ? itemW : 0;
                p.height = itemH > 0 ? itemH : 0;
                p.setMargins(margin, margin, margin, margin);

                gridLayout.addView(boxContainers[i], p);

                if (webViews[i] != null) webViews[i].setVisibility(View.VISIBLE);
                pos++;
            }

            gridLayout.requestLayout();
            Log.d(TAG, "updateLayout done – " + finalActive + " boxes, " +
                (portrait ? "portrait" : "landscape") + " [" + finalCols + "×" + finalRows + "] " +
                gridW + "×" + gridH + "px");
        });
    }

    // =========================================================================
    //  ORIENTATION TOGGLE
    // =========================================================================

    /**
     * FIX: When switching orientation, we must:
     *   1. Update currentOrientation immediately (before onConfigurationChanged fires)
     *   2. Call updateLayout() so the grid reconfigures in the new orientation
     *   3. If a box was in fullscreen, restore fullscreen UI flags after rotation
     */
    private void toggleOrientation() {
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            currentOrientation = Configuration.ORIENTATION_PORTRAIT;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            Toast.makeText(this, "📱 Portrait", Toast.LENGTH_SHORT).show();
        } else {
            currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            Toast.makeText(this, "📺 Landscape", Toast.LENGTH_SHORT).show();
        }
        // Layout will be triggered by onConfigurationChanged → updateLayout()
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Always sync currentOrientation to the actual system orientation
        currentOrientation = newConfig.orientation;
        Log.d(TAG, "onConfigurationChanged → " +
            (currentOrientation == Configuration.ORIENTATION_PORTRAIT ? "PORTRAIT" : "LANDSCAPE"));

        if (isAnyBoxInFullscreen()) {
            // Fullscreen must survive rotation – just restore the UI flags
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            Log.d(TAG, "onConfigurationChanged: fullscreen preserved");
        } else {
            // Normal rotation → rebuild layout for new orientation
            // Use post() so the new layout dimensions are available
            gridLayout.post(this::updateLayout);
        }
    }

    // =========================================================================
    //  EVENT LISTENERS
    // =========================================================================

    private void initEventListeners() {
        if (btnToggleBottomBar  != null) btnToggleBottomBar.setOnClickListener(v -> toggleBottomBar());
        if (btnToggleSidebar    != null) btnToggleSidebar.setOnClickListener(v -> { if (isSidebarVisible) closeSidebar(); else openSidebar(); });
        if (btnToggleOrientation!= null) btnToggleOrientation.setOnClickListener(v -> toggleOrientation());
        if (btnCloseSidebar     != null) btnCloseSidebar.setOnClickListener(v -> closeSidebar());
        if (btnCloseMenu        != null) btnCloseMenu.setOnClickListener(v -> { if (isBottomBarExpanded) closeBottomBar(); });

        if (btnLoadAll     != null) btnLoadAll.setOnClickListener(v -> loadAllURLs());
        if (btnReloadAll   != null) btnReloadAll.setOnClickListener(v -> reloadAllWebViews());
        if (btnClearAll    != null) btnClearAll.setOnClickListener(v -> clearAllWebViews());
        if (btnSaveState   != null) btnSaveState.setOnClickListener(v -> saveCurrentState());
        if (btnLoadState   != null) btnLoadState.setOnClickListener(v -> loadSavedState(false));
        if (btnSaveFavorites != null) btnSaveFavorites.setOnClickListener(v -> showSaveFavoriteDialog());
        if (btnLoadFavorites != null) btnLoadFavorites.setOnClickListener(v -> showLoadFavoritesDialog());

        if (btnSaveStateSidebar     != null) btnSaveStateSidebar.setOnClickListener(v -> saveCurrentState());
        if (btnLoadStateSidebar     != null) btnLoadStateSidebar.setOnClickListener(v -> loadSavedState(false));
        if (btnSaveFavoritesSidebar != null) btnSaveFavoritesSidebar.setOnClickListener(v -> showSaveFavoriteDialog());
        if (btnLoadFavoritesSidebar != null) btnLoadFavoritesSidebar.setOnClickListener(v -> showLoadFavoritesDialog());
        if (btnLoadAllSidebar       != null) btnLoadAllSidebar.setOnClickListener(v -> loadAllURLs());
        if (btnReloadAllSidebar     != null) btnReloadAllSidebar.setOnClickListener(v -> reloadAllWebViews());
        if (btnClearAllSidebar      != null) btnClearAllSidebar.setOnClickListener(v -> clearAllWebViews());

        for (int i = 0; i < 4; i++) {
            final int idx = i;

            if (btnLoadUrl[i]    != null) btnLoadUrl[i].setOnClickListener(v -> {
                String url = urlInputs[idx].getText().toString().trim();
                if (!url.isEmpty()) { syncToSidebar(idx, url); loadURL(idx, url); }
            });
            if (btnLoadUrlSidebar[i] != null) btnLoadUrlSidebar[i].setOnClickListener(v -> {
                String url = urlInputsSidebar[idx].getText().toString().trim();
                if (!url.isEmpty()) { syncToBottomBar(idx, url); loadURL(idx, url); }
            });
            if (btnRefresh[i]  != null) btnRefresh[i].setOnClickListener(v -> { if (webViews[idx] != null) webViews[idx].reload(); });
            if (btnZoomIn[i]   != null) btnZoomIn[i].setOnClickListener(v -> zoomIn(idx));
            if (btnZoomOut[i]  != null) btnZoomOut[i].setOnClickListener(v -> zoomOut(idx));
            if (btnPrevious[i] != null) btnPrevious[i].setOnClickListener(v -> { if (webViews[idx] != null && webViews[idx].canGoBack())    webViews[idx].goBack(); });
            if (btnNext[i]     != null) btnNext[i].setOnClickListener(v -> { if (webViews[idx] != null && webViews[idx].canGoForward()) webViews[idx].goForward(); });

            if (checkBoxes[i] != null) checkBoxes[i].setOnCheckedChangeListener((b, checked) -> {
                boxEnabled[idx] = checked;
                // Force a full layout pass immediately
                gridLayout.removeAllViews();
                updateLayout();
            });
            if (checkBoxesKeepActive[i] != null) checkBoxesKeepActive[i].setOnCheckedChangeListener((b, checked) -> {
                boxKeepActive[idx] = checked;
                updateLayout();
            });
        }

        if (cbAllowScripts != null) cbAllowScripts.setOnCheckedChangeListener((b, checked) -> {
            for (WebView wv : webViews) if (wv != null) wv.getSettings().setJavaScriptEnabled(checked);
        });
        if (cbBlockAds != null) cbBlockAds.setOnCheckedChangeListener((b, checked) -> {
            for (WebView wv : webViews) {
                if (wv != null) { wv.getSettings().setBlockNetworkLoads(checked); wv.getSettings().setBlockNetworkImage(checked); }
            }
        });
    }

    // =========================================================================
    //  URL LOADING
    // =========================================================================

    private void loadAllURLs() {
        for (int i = 0; i < 4; i++) {
            if (!boxEnabled[i] && !boxKeepActive[i]) continue;
            String url = urlInputs[i] != null ? urlInputs[i].getText().toString().trim() : "";
            if (url.isEmpty()) { url = getDefaultUrl(i); if (urlInputs[i] != null) urlInputs[i].setText(url); }
            syncToSidebar(i, url);
            loadURL(i, url);
        }
        Toast.makeText(this, "Carregando todas as URLs", Toast.LENGTH_SHORT).show();
    }

    private void loadInitialURLs() {
        for (int i = 0; i < 4; i++) {
            if (!boxEnabled[i] && !boxKeepActive[i]) continue;
            String url = urlInputs[i] != null ? urlInputs[i].getText().toString().trim() : "";
            if (url.isEmpty()) { url = getDefaultUrl(i); if (urlInputs[i] != null) urlInputs[i].setText(url); }
            syncToSidebar(i, url);
            loadURL(i, url);
        }
    }

    private void loadURL(int idx, String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://"))
                url = "https://" + url;
            if (webViews[idx] != null) webViews[idx].loadUrl(url);
        } catch (Exception e) { Log.e(TAG, "loadURL box " + idx, e); }
    }

    private void reloadAllWebViews() {
        for (int i = 0; i < 4; i++) if (boxEnabled[i] && webViews[i] != null) webViews[i].reload();
        Toast.makeText(this, "Recarregando todas", Toast.LENGTH_SHORT).show();
    }

    private void clearAllWebViews() {
        for (int i = 0; i < 4; i++) if (webViews[i] != null) webViews[i].loadUrl("about:blank");
        Toast.makeText(this, "Limpando todas", Toast.LENGTH_SHORT).show();
    }

    private String getDefaultUrl(int i) { return "https://dzritv.com/sport/football/"; }

    // =========================================================================
    //  ZOOM
    // =========================================================================

    private void applyZoom(int i) { if (webViews[i] != null) webViews[i].getSettings().setTextZoom((int)(zoomLevels[i] * 100)); }
    private void zoomIn(int i)  { if (zoomLevels[i] < 2.0f) { zoomLevels[i] += 0.1f; applyZoom(i); showZoomToast(i); } }
    private void zoomOut(int i) { if (zoomLevels[i] > 0.5f) { zoomLevels[i] -= 0.1f; applyZoom(i); showZoomToast(i); } }
    private void showZoomToast(int i) { Toast.makeText(this, "Box "+(i+1)+" Zoom: "+String.format("%.0f",zoomLevels[i]*100)+"%", Toast.LENGTH_SHORT).show(); }

    // =========================================================================
    //  AD BLOCKER
    // =========================================================================

    private void injectAdBlocker(WebView v) {
        String js = "try{['div[class*=\"ad\"]','div[id*=\"ad\"]','iframe[src*=\"ad\"]','ins.adsbygoogle','div.ad-container','div.advertisement'].forEach(function(s){document.querySelectorAll(s).forEach(function(e){e.style.display='none';if(e.parentNode)e.parentNode.removeChild(e);})});}catch(e){}";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) v.evaluateJavascript(js, null);
    }

    private boolean isAdUrl(String url) { for (String d : adDomains) if (url.toLowerCase().contains(d)) return true; return false; }
    private String  getDomain(String u) { try { String h = new java.net.URI(u).getHost(); return h != null ? h.replace("www.","") : u; } catch (Exception e) { return u; } }
    private boolean isSameDomain(String a, String b) { try { return getDomain(a).equals(getDomain(b)); } catch (Exception e) { return false; } }

    // =========================================================================
    //  STATE SAVE / LOAD
    // =========================================================================

    private boolean hasSavedState() { return preferences.contains("url_0") || preferences.contains("box_enabled_0"); }

    private void saveCurrentState() {
        try {
            SharedPreferences.Editor ed = preferences.edit();
            for (int i = 0; i < 4; i++) {
                String u = urlInputs[i] != null ? urlInputs[i].getText().toString().trim() : "";
                ed.putString("url_"+i, u.isEmpty() ? getDefaultUrl(i) : u);
                ed.putBoolean("box_enabled_"+i, boxEnabled[i]);
                ed.putBoolean("box_keep_active_"+i, boxKeepActive[i]);
                ed.putFloat("zoom_level_"+i, zoomLevels[i]);
            }
            if (cbAllowScripts   != null) ed.putBoolean("allow_scripts",   cbAllowScripts.isChecked());
            if (cbAllowForms     != null) ed.putBoolean("allow_forms",     cbAllowForms.isChecked());
            if (cbAllowPopups    != null) ed.putBoolean("allow_popups",    cbAllowPopups.isChecked());
            if (cbBlockRedirects != null) ed.putBoolean("block_redirects", cbBlockRedirects.isChecked());
            if (cbBlockAds       != null) ed.putBoolean("block_ads",       cbBlockAds.isChecked());
            ed.apply();
            Toast.makeText(this, "✅ Estado guardado!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "❌ Erro ao guardar estado", Toast.LENGTH_SHORT).show(); }
    }

    private void loadSavedState(boolean silent) {
        try {
            for (int i = 0; i < 4; i++) {
                boxEnabled[i]    = preferences.getBoolean("box_enabled_"+i, true);
                boxKeepActive[i] = preferences.getBoolean("box_keep_active_"+i, false);
                if (checkBoxes[i]           != null) checkBoxes[i].setChecked(boxEnabled[i]);
                if (checkBoxesKeepActive[i] != null) checkBoxesKeepActive[i].setChecked(boxKeepActive[i]);
            }
            boolean hasUrls = false;
            for (int i = 0; i < 4; i++) {
                String url = preferences.getString("url_"+i, "");
                if (!url.isEmpty()) {
                    hasUrls = true;
                    if (urlInputs[i] != null) urlInputs[i].setText(url);
                    syncToSidebar(i, url);
                    if ((boxEnabled[i] || boxKeepActive[i]) && webViews[i] != null) loadURL(i, url);
                }
            }
            if (!hasUrls) {
                for (int i = 0; i < 4; i++) { String d = getDefaultUrl(i); if (urlInputs[i] != null) urlInputs[i].setText(d); syncToSidebar(i, d); }
            }
            for (int i = 0; i < 4; i++) { zoomLevels[i] = preferences.getFloat("zoom_level_"+i, 1.0f); applyZoom(i); }
            if (cbAllowScripts   != null) cbAllowScripts.setChecked(preferences.getBoolean("allow_scripts", true));
            if (cbAllowForms     != null) cbAllowForms.setChecked(preferences.getBoolean("allow_forms", true));
            if (cbAllowPopups    != null) cbAllowPopups.setChecked(preferences.getBoolean("allow_popups", true));
            if (cbBlockRedirects != null) cbBlockRedirects.setChecked(preferences.getBoolean("block_redirects", false));
            if (cbBlockAds       != null) cbBlockAds.setChecked(preferences.getBoolean("block_ads", false));
            if (!silent) Toast.makeText(this, "✅ Estado carregado!", Toast.LENGTH_SHORT).show();
            updateLayout();
            updateFocusedBoxIndicator();
        } catch (Exception e) { if (!silent) Toast.makeText(this, "❌ Erro ao carregar estado", Toast.LENGTH_SHORT).show(); }
    }

    // =========================================================================
    //  FAVORITES
    // =========================================================================

    private void loadFavoritesList() {
        try { JSONArray a = new JSONArray(preferences.getString("favorites_list","[]")); favoritesList.clear(); for (int i=0;i<a.length();i++) favoritesList.add(a.getString(i)); }
        catch (Exception e) { favoritesList.clear(); }
    }
    private void saveFavoritesList() {
        try { preferences.edit().putString("favorites_list", new JSONArray(favoritesList).toString()).apply(); } catch (Exception ignored) {}
    }
    private void saveFavorite(String name) {
        try {
            if (favoritesList.contains(name)) { Toast.makeText(this,"❌ Nome já existe!",Toast.LENGTH_SHORT).show(); return; }
            JSONArray urls = new JSONArray();
            for (int i=0;i<4;i++) { String u = urlInputs[i]!=null?urlInputs[i].getText().toString().trim():""; urls.put(u.isEmpty()?getDefaultUrl(i):u); }
            JSONObject obj = new JSONObject(); obj.put("name",name); obj.put("urls",urls);
            favoritesList.add(name); saveFavoritesList();
            preferences.edit().putString("favorite_"+name, obj.toString()).apply();
            Toast.makeText(this,"✅ Favorito guardado!",Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this,"❌ Erro ao guardar favorito",Toast.LENGTH_SHORT).show(); }
    }
    private void loadFavorite(String name, int target) {
        try {
            String json = preferences.getString("favorite_"+name,"");
            if (json.isEmpty()) { Toast.makeText(this,"❌ Favorito não encontrado!",Toast.LENGTH_SHORT).show(); return; }
            JSONArray urls = new JSONObject(json).getJSONArray("urls");
            if (target==-1) { for (int i=0;i<4&&i<urls.length();i++) { String u=urls.getString(i); if (urlInputs[i]!=null) urlInputs[i].setText(u); syncToSidebar(i,u); if (boxEnabled[i]&&webViews[i]!=null) loadURL(i,u); } Toast.makeText(this,"✅ Favorito em todas!",Toast.LENGTH_SHORT).show(); }
            else if (target<urls.length()) { String u=urls.getString(target); if (urlInputs[target]!=null) urlInputs[target].setText(u); syncToSidebar(target,u); if (boxEnabled[target]&&webViews[target]!=null) loadURL(target,u); Toast.makeText(this,"✅ Box "+(target+1),Toast.LENGTH_SHORT).show(); }
        } catch (Exception e) { Toast.makeText(this,"❌ Erro ao carregar favorito!",Toast.LENGTH_SHORT).show(); }
    }
    private void deleteFavorite(String name) {
        favoritesList.remove(name); saveFavoritesList(); preferences.edit().remove("favorite_"+name).apply();
        Toast.makeText(this,"✅ Favorito removido!",Toast.LENGTH_SHORT).show();
    }
    private void showSaveFavoriteDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Nome do favorito"); input.setTextColor(Color.BLACK); input.setBackgroundResource(android.R.drawable.edit_text);
        new AlertDialog.Builder(this).setTitle("Guardar Favorito").setView(input)
            .setPositiveButton("GUARDAR",(d,w)->{ String n=input.getText().toString().trim(); if (!n.isEmpty()) saveFavorite(n); })
            .setNegativeButton("CANCELAR",null).show();
        input.requestFocus();
    }
    private void showLoadFavoritesDialog() {
        if (favoritesList.isEmpty()) { Toast.makeText(this,"🔭 Sem favoritos!",Toast.LENGTH_SHORT).show(); return; }
        String[] names = favoritesList.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Carregar Favorito").setItems(names,(d,w)->showFavoriteOptionsDialog(names[w])).setNegativeButton("CANCELAR",null).show();
    }
    private void showFavoriteOptionsDialog(String name) {
        String[] opts = {"Carregar em Todas","Box 1","Box 2","Box 3","Box 4","Eliminar","Cancelar"};
        new AlertDialog.Builder(this).setTitle("Favorito: "+name)
            .setItems(opts,(d,w)->{ if (w<5) loadFavorite(name,w==0?-1:w-1); else if (w==5) showDeleteConfirmDialog(name); }).show();
    }
    private void showDeleteConfirmDialog(String name) {
        new AlertDialog.Builder(this).setTitle("Confirmar Eliminação").setMessage("Eliminar '"+name+"'?")
            .setPositiveButton("ELIMINAR",(d,w)->deleteFavorite(name)).setNegativeButton("CANCELAR",null).show();
    }

    // =========================================================================
    //  SIDEBAR / BOTTOM BAR ANIMATIONS
    // =========================================================================

    public void closeSidebarFromOverlay(View v) { closeSidebar(); }

    private void closeSidebar() {
        android.animation.ObjectAnimator anim = android.animation.ObjectAnimator.ofFloat(sidebarContainer,"alpha",1f,0f);
        anim.setDuration(300);
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                sidebarContainer.setVisibility(View.GONE); isSidebarVisible = false;
                RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) gridLayout.getLayoutParams();
                p.removeRule(RelativeLayout.LEFT_OF); gridLayout.setLayoutParams(p);
                hideKeyboard(); if (btnToggleSidebar!=null) btnToggleSidebar.requestFocus();
            }
        }); anim.start();
    }

    private void openSidebar() {
        sidebarContainer.setVisibility(View.VISIBLE); sidebarContainer.setAlpha(0f); isSidebarVisible = true;
        RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) gridLayout.getLayoutParams();
        p.addRule(RelativeLayout.LEFT_OF, R.id.sidebarContainer); gridLayout.setLayoutParams(p);
        android.animation.ObjectAnimator anim = android.animation.ObjectAnimator.ofFloat(sidebarContainer,"alpha",0f,1f);
        anim.setDuration(300);
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                for (int i=0;i<4;i++) if (urlInputs[i]!=null&&urlInputsSidebar[i]!=null) urlInputsSidebar[i].setText(urlInputs[i].getText());
                if (btnCloseSidebar!=null) btnCloseSidebar.requestFocus();
            }
        }); anim.start();
    }

    private void toggleBottomBar() { if (isBottomBarExpanded) closeBottomBar(); else openBottomBar(); }

    private void closeBottomBar() {
        if (expandedBottomBar==null) return;
        android.animation.ObjectAnimator anim = android.animation.ObjectAnimator.ofFloat(expandedBottomBar,"translationY",0f,expandedBottomBar.getHeight());
        anim.setDuration(300); anim.setInterpolator(new android.view.animation.AccelerateInterpolator());
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                expandedBottomBar.setVisibility(View.GONE); isBottomBarExpanded=false; updateLayoutForBottomBar(); hideKeyboard();
                if (btnToggleBottomBar!=null) btnToggleBottomBar.requestFocus();
            }
        }); anim.start();
    }

    private void openBottomBar() {
        if (expandedBottomBar==null) return;
        expandedBottomBar.setVisibility(View.VISIBLE); expandedBottomBar.setTranslationY(expandedBottomBar.getHeight());
        isBottomBarExpanded=true; updateLayoutForBottomBar();
        android.animation.ObjectAnimator anim = android.animation.ObjectAnimator.ofFloat(expandedBottomBar,"translationY",expandedBottomBar.getHeight(),0f);
        anim.setDuration(300); anim.setInterpolator(new android.view.animation.DecelerateInterpolator());
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) { if (btnCloseMenu!=null) btnCloseMenu.requestFocus(); }
        }); anim.start();
    }

    private void updateLayoutForBottomBar() {
        if (gridLayout==null) return;
        RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) gridLayout.getLayoutParams();
        p.removeRule(RelativeLayout.ABOVE);
        p.addRule(RelativeLayout.ABOVE, isBottomBarExpanded ? R.id.expandedBottomBar : R.id.bottomControls);
        gridLayout.setLayoutParams(p);
    }

    // =========================================================================
    //  FULLSCREEN HELPERS
    // =========================================================================

    private boolean isAnyBoxInFullscreen() { for (boolean b : boxInFullscreen) if (b) return true; return false; }

    private void exitFullscreen(int i) {
        if (!boxInFullscreen[i]||fullscreenViews[i]==null) return;
        if (boxContainers[i].indexOfChild(fullscreenViews[i])!=-1) boxContainers[i].removeView(fullscreenViews[i]);
        if (webViews[i]!=null) webViews[i].setVisibility(View.VISIBLE);
        if (fullscreenCallbacks[i]!=null) fullscreenCallbacks[i].onCustomViewHidden();
        fullscreenViews[i]=null; fullscreenCallbacks[i]=null; boxInFullscreen[i]=false;
        if (!isAnyBoxInFullscreen()) { getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); new Handler(getMainLooper()).post(this::updateLayout); }
    }

    // =========================================================================
    //  MISC HELPERS
    // =========================================================================

    private void highlightFocusedBox(int focused) {
        for (int i=0;i<4;i++) {
            if (boxContainers[i]==null) continue;
            boxContainers[i].setBackgroundColor(i==focused ? Color.TRANSPARENT : Color.BLACK);
            if (i==focused) boxContainers[i].setBackgroundResource(R.drawable.box_focused_border);
        }
    }

    private void scrollWebView(int i, int dy) { scrollWebViewXY(i,0,dy); }
    private void scrollWebViewXY(int i, int dx, int dy) {
        if (webViews[i]!=null&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT)
            webViews[i].evaluateJavascript("window.scrollBy("+dx+","+dy+");",null);
    }

    private void updateFocusedBoxIndicator() { if (tvFocusedBox!=null) tvFocusedBox.setText("Foco: "+(focusedBoxIndex+1)); }

    private void showKeyboard(View v) { v.postDelayed(()->{android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(imm!=null)imm.showSoftInput(v,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);},100); }
    private void hideKeyboard() { View v=getCurrentFocus(); if(v!=null){android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(imm!=null)imm.hideSoftInputFromWindow(v.getWindowToken(),0);} }

    private void clearAppCache() {
        try { for (WebView wv:webViews) if(wv!=null){wv.clearCache(true);wv.clearHistory();} if(getCacheDir()!=null) deleteDir(getCacheDir()); } catch (Exception ignored) {}
    }
    private boolean deleteDir(java.io.File dir) {
        if(dir.isDirectory()){String[] c=dir.list();if(c!=null)for(String s:c)if(!deleteDir(new java.io.File(dir,s)))return false;} return dir.delete();
    }

    // =========================================================================
    //  ANDROID LIFECYCLE
    // =========================================================================

    @Override protected void onPause()   { super.onPause();   saveCurrentState(); }
    @Override protected void onResume()  { super.onResume();  loadFavoritesList(); if (btnToggleSidebar!=null) btnToggleSidebar.requestFocus(); }
    @Override protected void onDestroy() {
        super.onDestroy(); clearAppCache();
        for (WebView wv:webViews) if(wv!=null){wv.stopLoading();wv.setWebViewClient(null);wv.setWebChromeClient(null);wv.destroy();}
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction()==KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (isSidebarVisible) { closeSidebar(); return true; }
                    for (int i=0;i<4;i++) if(boxInFullscreen[i]){exitFullscreen(i);return true;}
                    if (webViews[focusedBoxIndex]!=null&&webViews[focusedBoxIndex].canGoBack()){webViews[focusedBoxIndex].goBack();return true;}
                    break;
                case KeyEvent.KEYCODE_MENU:
                    if (isSidebarVisible) closeSidebar(); else openSidebar(); return true;
                case KeyEvent.KEYCODE_DPAD_UP:    if (!isSidebarVisible){scrollWebView(focusedBoxIndex,-100);return true;} break;
                case KeyEvent.KEYCODE_DPAD_DOWN:  if (!isSidebarVisible){scrollWebView(focusedBoxIndex, 100);return true;} break;
                case KeyEvent.KEYCODE_PAGE_UP:    if (!isSidebarVisible){scrollWebView(focusedBoxIndex,-500);return true;} break;
                case KeyEvent.KEYCODE_PAGE_DOWN:  if (!isSidebarVisible){scrollWebView(focusedBoxIndex, 500);return true;} break;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (isSidebarVisible){closeSidebar();return;}
        for (int i=0;i<4;i++) if(boxInFullscreen[i]){exitFullscreen(i);return;}
        if (webViews[focusedBoxIndex]!=null&&webViews[focusedBoxIndex].canGoBack()){webViews[focusedBoxIndex].goBack();return;}
        for (WebView wv:webViews) if(wv!=null&&wv.canGoBack()){wv.goBack();return;}
        new AlertDialog.Builder(this).setTitle("Sair do App").setMessage("Deseja sair?")
            .setPositiveButton("SIM",(d,w)->finish()).setNegativeButton("NÃO",null).show();
    }
}
