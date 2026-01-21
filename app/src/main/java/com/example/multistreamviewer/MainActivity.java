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
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private FrameLayout sidebarContainer;
    private View sidebarOverlay;
    
    // Controles
    private Button btnMenu, btnBack, btnCursorMode;
    private Button btnCloseMenu, btnLoadAll, btnReloadAll, btnClearAll;
    private Button[] btnLoadUrls = new Button[4];
    private CheckBox[] checkBoxes = new CheckBox[4];
    private EditText[] urlInputs = new EditText[4];
    private TextView tvFocusedBox;
    
    // Cursor Virtual
    private ImageView virtualCursor;
    private boolean cursorMode = false;
    private float cursorX = 0;
    private float cursorY = 0;
    private final int CURSOR_SPEED = 20;
    
    // Estado
    private boolean[] boxEnabled = {true, true, true, true};
    private boolean isSidebarVisible = false;
    private int focusedBoxIndex = 0;
    
    // Favoritos
    private ArrayList<String> favoritesList = new ArrayList<>();
    private SharedPreferences preferences;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
        
        preferences = getSharedPreferences("MultiStreamViewer", MODE_PRIVATE);
        
        // Configurar tela cheia
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        
        initViews();
        initWebViews();
        initEventListeners();
        initCursor();
        
        loadSavedState(true);
        loadFavoritesList();
        updateLayout();
        updateFocusedBoxIndicator();
        
        if (!hasSavedState()) {
            new Handler().postDelayed(this::loadInitialURLs, 1000);
        }
    }
    
    private void initCursor() {
        virtualCursor = findViewById(R.id.virtualCursor);
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        cursorX = metrics.widthPixels / 2;
        cursorY = metrics.heightPixels / 2;
        
        virtualCursor.setX(cursorX);
        virtualCursor.setY(cursorY);
    }
    
    private void toggleCursorMode() {
        cursorMode = !cursorMode;
        
        if (cursorMode) {
            virtualCursor.setVisibility(View.VISIBLE);
            btnCursorMode.setText("DPAD");
            btnCursorMode.setBackgroundColor(Color.parseColor("#FF9800"));
            
            // Desativar foco em todos os elementos
            for (View view : getAllFocusableViews()) {
                view.setFocusable(false);
                view.setFocusableInTouchMode(false);
            }
        } else {
            virtualCursor.setVisibility(View.GONE);
            btnCursorMode.setText("CURSOR");
            btnCursorMode.setBackgroundColor(Color.parseColor("#555555"));
            
            // Reativar foco
            for (View view : getAllFocusableViews()) {
                view.setFocusable(true);
                view.setFocusableInTouchMode(true);
            }
            boxContainers[focusedBoxIndex].requestFocus();
        }
    }
    
    private List<View> getAllFocusableViews() {
        List<View> views = new ArrayList<>();
        views.add(btnBack);
        views.add(btnMenu);
        views.add(btnCursorMode);
        views.add(btnCloseMenu);
        views.addAll(Arrays.asList(checkBoxes));
        views.addAll(Arrays.asList(btnLoadUrls));
        views.addAll(Arrays.asList(urlInputs));
        views.add(btnLoadAll);
        views.add(btnReloadAll);
        views.add(btnClearAll);
        views.addAll(Arrays.asList(boxContainers));
        return views;
    }
    
    private void moveCursor(int dx, int dy) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        cursorX += dx;
        cursorY += dy;
        
        // Limitar aos limites da tela
        cursorX = Math.max(0, Math.min(cursorX, metrics.widthPixels - virtualCursor.getWidth()));
        cursorY = Math.max(0, Math.min(cursorY, metrics.heightPixels - virtualCursor.getHeight()));
        
        virtualCursor.setX(cursorX);
        virtualCursor.setY(cursorY);
    }
    
    private void performClickAtCursor() {
        // Encontrar a view sob o cursor
        View targetView = findViewAtPosition((int) cursorX, (int) cursorY);
        
        if (targetView != null) {
            // Simular clique
            if (targetView.isClickable()) {
                targetView.performClick();
            } else {
                // Simular MotionEvent para WebViews
                simulateTouchEvent(targetView, cursorX, cursorY);
            }
        }
    }
    
    private View findViewAtPosition(int x, int y) {
        View rootView = getWindow().getDecorView();
        return findViewAt(rootView, x, y);
    }
    
    private View findViewAt(View view, int x, int y) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child.getVisibility() == View.VISIBLE) {
                    View found = findViewAt(child, x, y);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        
        if (x >= left && x <= right && y >= top && y <= bottom) {
            return view;
        }
        
        return null;
    }
    
    private void simulateTouchEvent(View view, float x, float y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        
        float viewX = x - location[0];
        float viewY = y - location[1];
        
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        
        MotionEvent downEvent = MotionEvent.obtain(
            downTime, eventTime,
            MotionEvent.ACTION_DOWN,
            viewX, viewY, 0
        );
        
        MotionEvent upEvent = MotionEvent.obtain(
            downTime, eventTime + 100,
            MotionEvent.ACTION_UP,
            viewX, viewY, 0
        );
        
        view.dispatchTouchEvent(downEvent);
        view.dispatchTouchEvent(upEvent);
        
        downEvent.recycle();
        upEvent.recycle();
    }
    
    private void initViews() {
        gridLayout = findViewById(R.id.gridLayout);
        bottomControls = findViewById(R.id.bottomControls);
        sidebarContainer = findViewById(R.id.sidebarContainer);
        sidebarOverlay = findViewById(R.id.sidebarOverlay);
        tvFocusedBox = findViewById(R.id.tvFocusedBox);
        
        btnMenu = findViewById(R.id.btnMenu);
        btnBack = findViewById(R.id.btnBack);
        btnCursorMode = findViewById(R.id.btnCursorMode);
        
        btnCloseMenu = findViewById(R.id.btnCloseMenu);
        btnLoadAll = findViewById(R.id.btnLoadAll);
        btnReloadAll = findViewById(R.id.btnReloadAll);
        btnClearAll = findViewById(R.id.btnClearAll);
        
        checkBoxes[0] = findViewById(R.id.checkBox1);
        checkBoxes[1] = findViewById(R.id.checkBox2);
        checkBoxes[2] = findViewById(R.id.checkBox3);
        checkBoxes[3] = findViewById(R.id.checkBox4);
        
        btnLoadUrls[0] = findViewById(R.id.btnLoadUrl1);
        btnLoadUrls[1] = findViewById(R.id.btnLoadUrl2);
        btnLoadUrls[2] = findViewById(R.id.btnLoadUrl3);
        btnLoadUrls[3] = findViewById(R.id.btnLoadUrl4);
        
        urlInputs[0] = findViewById(R.id.urlInput1);
        urlInputs[1] = findViewById(R.id.urlInput2);
        urlInputs[2] = findViewById(R.id.urlInput3);
        urlInputs[3] = findViewById(R.id.urlInput4);
        
        // URLs padrão
        String defaultUrl = "https://dzritv.com/sport/football/";
        for (EditText urlInput : urlInputs) {
            urlInput.setText(defaultUrl);
        }
        
        // Configurar ações de teclado
        for (int i = 0; i < 4; i++) {
            final int boxIndex = i;
            urlInputs[i].setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE || 
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
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
    
    private void initEventListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webViews[focusedBoxIndex].canGoBack()) {
                    webViews[focusedBoxIndex].goBack();
                }
            }
        });
        
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
        
        btnCursorMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCursorMode();
            }
        });
        
        btnCloseMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSidebar();
            }
        });
        
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
        
        // Botões de carregar URL individual
        for (int i = 0; i < 4; i++) {
            final int index = i;
            btnLoadUrls[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = urlInputs[index].getText().toString().trim();
                    if (!url.isEmpty()) {
                        loadURL(index, url);
                        Toast.makeText(MainActivity.this, 
                            "Box " + (index + 1) + " carregada", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        
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
        
        // Configurar overlay do sidebar
        sidebarOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSidebar();
            }
        });
    }
    
    private void closeSidebar() {
        sidebarContainer.setVisibility(View.GONE);
        sidebarOverlay.setVisibility(View.GONE);
        isSidebarVisible = false;
    }
    
    private void openSidebar() {
        sidebarContainer.setVisibility(View.VISIBLE);
        sidebarOverlay.setVisibility(View.VISIBLE);
        isSidebarVisible = true;
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
            
            // Listener de foco
            boxContainers[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        focusedBoxIndex = boxIndex;
                        updateFocusedBoxIndicator();
                        // Destacar box focada
                        boxContainers[boxIndex].setBackgroundResource(R.drawable.box_focused_border);
                    } else {
                        boxContainers[boxIndex].setBackgroundColor(Color.BLACK);
                    }
                }
            });
            
            // Clique
            boxContainers[i].setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = 0;
                
                @Override
                public void onClick(View v) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < 500) {
                        enterFullscreenMode(boxIndex);
                    } else {
                        focusedBoxIndex = boxIndex;
                        updateFocusedBoxIndicator();
                    }
                    lastClickTime = clickTime;
                }
            });
            
            webViews[i] = new WebView(this);
            webViews[i].setId(View.generateViewId());
            setupWebView(webViews[i]);
            
            boxContainers[i].addView(webViews[i], 
                new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            
            gridLayout.addView(boxContainers[i]);
        }
        
        // Focar na primeira box
        boxContainers[0].requestFocus();
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        
        // User agent para Fire Stick
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 9; AFTMM Build/PS7233) AppleWebKit/537.36");
        
        webView.setBackgroundColor(Color.BLACK);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                mCustomView = view;
                
                bottomControls.setVisibility(View.GONE);
                gridLayout.setVisibility(View.GONE);
                
                ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
                decorView.addView(view, 
                    new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                
                getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            
            @Override
            public void onHideCustomView() {
                if (mCustomView == null) return;
                
                ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
                decorView.removeView(mCustomView);
                
                mCustomView = null;
                
                bottomControls.setVisibility(View.VISIBLE);
                gridLayout.setVisibility(View.VISIBLE);
                
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        });
    }
    
    private void updateFocusedBoxIndicator() {
        tvFocusedBox.setText("Foco: " + (focusedBoxIndex + 1));
    }
    
    private void updateLayout() {
        int activeBoxes = 0;
        for (boolean enabled : boxEnabled) {
            if (enabled) activeBoxes++;
        }
        
        if (activeBoxes == 0) {
            for (int i = 0; i < 4; i++) {
                boxEnabled[i] = true;
                checkBoxes[i].setChecked(true);
            }
            activeBoxes = 4;
        }
        
        int rows, cols;
        
        switch (activeBoxes) {
            case 1: rows = 1; cols = 1; break;
            case 2: rows = 1; cols = 2; break;
            case 3: rows = 1; cols = 3; break;
            case 4: rows = 2; cols = 2; break;
            default: rows = 1; cols = 1; break;
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
                } else {
                    params.setMargins(2, 2, 2, 2);
                }
                
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
    
    private void enterFullscreenMode(int boxIndex) {
        WebView webView = webViews[boxIndex];
        String fullscreenJS = 
            "var videos = document.getElementsByTagName('video');" +
            "if (videos.length > 0) {" +
            "   var video = videos[0];" +
            "   if (video.requestFullscreen) {" +
            "       video.requestFullscreen();" +
            "   } else if (video.webkitRequestFullscreen) {" +
            "       video.webkitRequestFullscreen();" +
            "   }" +
            "}";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(fullscreenJS, null);
        } else {
            webView.loadUrl("javascript:" + fullscreenJS);
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (cursorMode) {
            // Modo cursor - controles do cursor
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    moveCursor(-CURSOR_SPEED, 0);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    moveCursor(CURSOR_SPEED, 0);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    moveCursor(0, -CURSOR_SPEED);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    moveCursor(0, CURSOR_SPEED);
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    performClickAtCursor();
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (isSidebarVisible) {
                        closeSidebar();
                        return true;
                    }
                    if (webViews[focusedBoxIndex].canGoBack()) {
                        webViews[focusedBoxIndex].goBack();
                        return true;
                    }
                    break;
            }
        } else {
            // Modo DPAD - navegação normal
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    // Focar no menu inferior
                    btnMenu.requestFocus();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    // Voltar para box
                    if (bottomControls.hasFocus()) {
                        boxContainers[focusedBoxIndex].requestFocus();
                        return true;
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (boxContainers[focusedBoxIndex].hasFocus()) {
                        if (focusedBoxIndex > 0) {
                            focusedBoxIndex--;
                            boxContainers[focusedBoxIndex].requestFocus();
                            return true;
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (boxContainers[focusedBoxIndex].hasFocus()) {
                        if (focusedBoxIndex < 3) {
                            focusedBoxIndex++;
                            boxContainers[focusedBoxIndex].requestFocus();
                            return true;
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    enterFullscreenMode(focusedBoxIndex);
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (isSidebarVisible) {
                        closeSidebar();
                        return true;
                    }
                    if (webViews[focusedBoxIndex].canGoBack()) {
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
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    // Métodos para persistência (simplificados)
    private boolean hasSavedState() {
        return preferences.contains("url_0");
    }
    
    private void saveCurrentState() {
        SharedPreferences.Editor editor = preferences.edit();
        for (int i = 0; i < 4; i++) {
            editor.putString("url_" + i, urlInputs[i].getText().toString());
        }
        editor.apply();
    }
    
    private void loadSavedState(boolean silent) {
        for (int i = 0; i < 4; i++) {
            String savedUrl = preferences.getString("url_" + i, "");
            if (!savedUrl.isEmpty()) {
                urlInputs[i].setText(savedUrl);
                loadURL(i, savedUrl);
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
                favoritesList.add(jsonArray.getString(i));
            }
        } catch (Exception e) {
            favoritesList.clear();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentState();
    }
    
    @Override
    public void onBackPressed() {
        if (isSidebarVisible) {
            closeSidebar();
            return;
        }
        
        if (webViews[focusedBoxIndex].canGoBack()) {
            webViews[focusedBoxIndex].goBack();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Deseja sair?")
            .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .setNegativeButton("Não", null)
            .show();
    }
}
