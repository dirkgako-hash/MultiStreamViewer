package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private GridLayout gridLayout;
    private WebView[] webViews = new WebView[4];
    private FrameLayout[] boxContainers = new FrameLayout[4];
    private View sidebarContainer, sidebarOverlay;
    private Button btnMenu, btnBack, btnCloseMenu;
    private Button btnLoadAll, btnReloadAll, btnClearAll;
    private CheckBox[] checkBoxes = new CheckBox[4];
    private EditText[] urlInputs = new EditText[4];
    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups, cbBlockRedirects, cbBlockAds;
    private TextView tvFocusedBox;

    private boolean[] boxEnabled = {true, true, true, true};
    private int focusedBoxIndex = 0;
    private SharedPreferences preferences;
    private String defaultUrl = "https://dzritv.com/sport/football/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("MultiStreamViewer", MODE_PRIVATE);
        
        initViews();
        initWebViews();
        updateLayout();
        
        loadSavedState(true);
    }

    private void initViews() {
        gridLayout = findViewById(R.id.gridLayout);
        sidebarContainer = findViewById(R.id.sidebarContainer);
        sidebarOverlay = findViewById(R.id.sidebarOverlay);
        tvFocusedBox = findViewById(R.id.tvFocusedBox);
        btnMenu = findViewById(R.id.btnMenu);
        btnBack = findViewById(R.id.btnBack);
        btnCloseMenu = findViewById(R.id.btnCloseMenu);

        urlInputs[0] = findViewById(R.id.urlInput1);
        urlInputs[1] = findViewById(R.id.urlInput2);
        urlInputs[2] = findViewById(R.id.urlInput3);
        urlInputs[3] = findViewById(R.id.urlInput4);

        checkBoxes[0] = findViewById(R.id.checkBox1);
        checkBoxes[1] = findViewById(R.id.checkBox2);
        checkBoxes[2] = findViewById(R.id.checkBox3);
        checkBoxes[3] = findViewById(R.id.checkBox4);

        cbAllowScripts = findViewById(R.id.cbAllowScripts);
        cbAllowForms = findViewById(R.id.cbAllowForms);
        cbAllowPopups = findViewById(R.id.cbAllowPopups);
        cbBlockRedirects = findViewById(R.id.cbBlockRedirects);
        cbBlockAds = findViewById(R.id.cbBlockAds);

        btnLoadAll = findViewById(R.id.btnLoadAll);
        btnReloadAll = findViewById(R.id.btnReloadAll);
        btnClearAll = findViewById(R.id.btnClearAll);

        btnMenu.setOnClickListener(v -> openSidebar());
        btnCloseMenu.setOnClickListener(v -> closeSidebar());
        sidebarOverlay.setOnClickListener(v -> closeSidebar());

        sidebarContainer.setOnClickListener(v -> {});

        for (int i = 0; i < 4; i++) {
            final int index = i;
            urlInputs[i].setOnEditorActionListener((v, actionId, event) -> {
                String url = urlInputs[index].getText().toString().trim();
                if (!url.isEmpty()) loadURL(index, url);
                return true;
            });
            checkBoxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                boxEnabled[index] = isChecked;
                updateLayout();
            });
        }

        btnLoadAll.setOnClickListener(v -> {
            for(int i=0; i<4; i++) {
                String url = urlInputs[i].getText().toString().trim();
                if(!url.isEmpty() && boxEnabled[i]) loadURL(i, url);
            }
            closeSidebar();
        });

        btnReloadAll.setOnClickListener(v -> {
            for(int i=0; i<4; i++) if(boxEnabled[i]) webViews[i].reload();
            closeSidebar();
        });

        btnClearAll.setOnClickListener(v -> {
            for(int i=0; i<4; i++) {
                webViews[i].loadUrl("about:blank");
                urlInputs[i].setText("");
            }
            closeSidebar();
        });

        findViewById(R.id.btnSaveState).setOnClickListener(v -> saveCurrentState());
        findViewById(R.id.btnLoadState).setOnClickListener(v -> loadSavedState(false));
        findViewById(R.id.btnSaveFavorites).setOnClickListener(v -> showSaveFavoriteDialog());
        findViewById(R.id.btnLoadFavorites).setOnClickListener(v -> showLoadFavoritesDialog());
    }

    private void initWebViews() {
        for (int i = 0; i < 4; i++) {
            final int index = i;
            boxContainers[i] = new FrameLayout(this);
            boxContainers[i].setBackgroundColor(Color.BLACK);
            
            webViews[i] = new WebView(this);
            setupWebView(webViews[i]);
            
            boxContainers[i].addView(webViews[i], new FrameLayout.LayoutParams(-1, -1));
            boxContainers[i].setOnClickListener(v -> {
                focusedBoxIndex = index;
                updateFocusedBoxIndicator();
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView) {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                String js = "var videos = document.getElementsByTagName('video'); " +
                           "for(var i=0; i<videos.length; i++) { " +
                           "  videos[i].muted = true; " +
                           "  videos[i].play(); " +
                           "}";
                view.evaluateJavascript(js, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                webView.evaluateJavascript(
                    "var v = document.querySelector('video'); " +
                    "if(v) { v.style.position='fixed'; v.style.top='0'; v.style.left='0'; v.style.width='100%'; v.style.height='100%'; v.style.zIndex='9999'; }", 
                    null
                );
            }
        });
    }

    private void updateLayout() {
        gridLayout.removeAllViews();
        int activeCount = 0;
        for (boolean b : boxEnabled) if (b) activeCount++;
        if (activeCount == 0) return;

        int rows = 1, cols = 1;
        if (activeCount == 2) { rows = 1; cols = 2; }
        else if (activeCount == 3) { rows = 1; cols = 3; }
        else if (activeCount == 4) { rows = 2; cols = 2; }

        gridLayout.setRowCount(rows);
        gridLayout.setColumnCount(cols);

        for (int i = 0; i < 4; i++) {
            if (boxEnabled[i]) {
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                params.width = 0;
                params.height = 0;
                gridLayout.addView(boxContainers[i], params);
            }
        }
    }

    private void loadURL(int index, String url) {
        if (!url.startsWith("http")) url = "https://" + url;
        webViews[index].loadUrl(url);
        urlInputs[index].setText(url);
    }

    private void openSidebar() {
        sidebarContainer.setVisibility(View.VISIBLE);
        sidebarOverlay.setVisibility(View.VISIBLE);
    }

    private void closeSidebar() {
        sidebarContainer.setVisibility(View.GONE);
        sidebarOverlay.setVisibility(View.GONE);
    }

    private void updateFocusedBoxIndicator() {
        tvFocusedBox.setText("Foco: " + (focusedBoxIndex + 1));
    }

    private void saveCurrentState() {
        SharedPreferences.Editor ed = preferences.edit();
        for (int i = 0; i < 4; i++) {
            ed.putString("url_" + i, urlInputs[i].getText().toString());
            ed.putBoolean("enabled_" + i, boxEnabled[i]);
        }
        ed.apply();
        Toast.makeText(this, "Estado Guardado", Toast.LENGTH_SHORT).show();
    }

    private void loadSavedState(boolean silent) {
        for (int i = 0; i < 4; i++) {
            String url = preferences.getString("url_" + i, defaultUrl);
            boolean en = preferences.getBoolean("enabled_" + i, true);
            boxEnabled[i] = en;
            checkBoxes[i].setChecked(en);
            urlInputs[i].setText(url);
            if (en) loadURL(i, url);
        }
        updateLayout();
        if (!silent) Toast.makeText(this, "Estado Carregado", Toast.LENGTH_SHORT).show();
    }

    private void showSaveFavoriteDialog() {
        EditText input = new EditText(this);
        new AlertDialog.Builder(this).setTitle("Nome do Favorito").setView(input)
            .setPositiveButton("OK", (d, w) -> {
                String name = input.getText().toString();
                try {
                    JSONObject fav = new JSONObject();
                    JSONArray urls = new JSONArray();
                    for (int i = 0; i < 4; i++) urls.put(urlInputs[i].getText().toString());
                    fav.put("urls", urls);
                    preferences.edit().putString("fav_" + name, fav.toString()).apply();
                    Toast.makeText(this, "Favorito Salvo", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {}
            }).show();
    }

    private void showLoadFavoritesDialog() {
        ArrayList<String> favs = new ArrayList<>();
        for (String key : preferences.getAll().keySet()) if (key.startsWith("fav_")) favs.add(key.replace("fav_", ""));
        
        new AlertDialog.Builder(this).setTitle("Carregar Favorito")
            .setItems(favs.toArray(new String[0]), (d, which) -> {
                String name = favs.get(which);
                try {
                    JSONObject fav = new JSONObject(preferences.getString("fav_" + name, ""));
                    JSONArray urls = fav.getJSONArray("urls");
                    for (int i = 0; i < 4; i++) {
                        String url = urls.getString(i);
                        urlInputs[i].setText(url);
                        if (boxEnabled[i]) loadURL(i, url);
                    }
                } catch (Exception e) {}
            }).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (sidebarContainer.getVisibility() == View.VISIBLE) {
                closeSidebar();
                return true;
            }
            if (webViews[focusedBoxIndex].canGoBack()) {
                webViews[focusedBoxIndex].goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
