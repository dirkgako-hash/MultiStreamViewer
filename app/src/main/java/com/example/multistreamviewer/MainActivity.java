package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // Componentes principais
    private GridLayout gridLayout;
    private WebView[] webViews = new WebView[4];
    private FrameLayout[] boxContainers = new FrameLayout[4];
    private LinearLayout bottomControls;
    private ScrollView sidebarMenu;
    
    // Controles
    private Button btnMenu, btnOrientation;
    private Button btnCloseMenu, btnLoadAll, btnReloadAll, btnClearAll;
    private Button btnSaveState, btnLoadState, btnSaveFavorites, btnLoadFavorites;
    private CheckBox[] checkBoxes = new CheckBox[4];
    private CheckBox cbAllowScripts, cbAllowForms, cbAllowPopups, cbBlockRedirects;
    private EditText[] urlInputs = new EditText[4];
    
    // Estado
    private boolean[] boxEnabled = {true, true, true, true};
    private boolean isSidebarVisible = false;
    private int currentOrientation = Configuration.ORIENTATION_PORTRAIT;
    
    // Favoritos
    private ArrayList<String> favoritesList = new ArrayList<>();
    private SharedPreferences preferences;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
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
        
        // Carregar estado salvo
        loadSavedState();
        
        // Carregar lista de favoritos
        loadFavoritesList();
        
        // Configurar layout inicial
        updateLayout();
        
        // Carregar URLs iniciais
        new Handler().postDelayed(this::loadInitialURLs, 1000);
    }
    
    private void initViews() {
        gridLayout = findViewById(R.id.gridLayout);
        bottomControls = findViewById(R.id.bottomControls);
        sidebarMenu = findViewById(R.id.sidebarMenu);
        
        // Botões na barra inferior
        btnMenu = findViewById(R.id.btnMenu);
        btnOrientation = findViewById(R.id.btnOrientation);
        
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
            
            // Configurar clique duplo para fullscreen
            boxContainers[i].setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = 0;
                
                @Override
                public void onClick(View v) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < 300) {
                        // Clique duplo detectado - ativar fullscreen
                        activateFullscreen(boxIndex);
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
        
        // User agent
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        
        // Cor de fundo
        webView.setBackgroundColor(Color.BLACK);
        
        // WebViewClient simplificado
        webView.setWebViewClient(new WebViewClient());
        
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
        // Botão menu
        btnMenu.setOnClickListener(v -> {
            if (sidebarMenu.getVisibility() == View.VISIBLE) {
                sidebarMenu.setVisibility(View.GONE);
                isSidebarVisible = false;
            } else {
                sidebarMenu.setVisibility(View.VISIBLE);
                isSidebarVisible = true;
            }
        });
        
        // Botão orientação
        btnOrientation.setOnClickListener(v -> {
            changeOrientation();
        });
        
        // Botão fechar menu lateral
        btnCloseMenu.setOnClickListener(v -> {
            sidebarMenu.setVisibility(View.GONE);
            isSidebarVisible = false;
        });
        
        // Botões de ação no menu lateral
        btnLoadAll.setOnClickListener(v -> {
            loadAllURLs();
        });
        
        btnReloadAll.setOnClickListener(v -> {
            reloadAllWebViews();
        });
        
        btnClearAll.setOnClickListener(v -> {
            clearAllWebViews();
        });
        
        // Botões de estado e favoritos
        btnSaveState.setOnClickListener(v -> {
            saveCurrentState();
        });
        
        btnLoadState.setOnClickListener(v -> {
            loadSavedState();
        });
        
        btnSaveFavorites.setOnClickListener(v -> {
            showSaveFavoriteDialog();
        });
        
        btnLoadFavorites.setOnClickListener(v -> {
            showLoadFavoritesDialog();
        });
        
        // Checkboxes de boxes
        for (int i = 0; i < 4; i++) {
            final int index = i;
            checkBoxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                boxEnabled[index] = isChecked;
                updateLayout();
            });
        }
        
        // Configurações de segurança
        cbAllowScripts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (WebView webView : webViews) {
                if (webView != null) {
                    webView.getSettings().setJavaScriptEnabled(isChecked);
                }
            }
            Toast.makeText(this, "JavaScript: " + (isChecked ? "Ativado" : "Desativado"), 
                          Toast.LENGTH_SHORT).show();
        });
    }
    
    // ==================== ESTADO ====================
    
    private void saveCurrentState() {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            
            // Salvar URLs
            for (int i = 0; i < 4; i++) {
                editor.putString("url_" + i, urlInputs[i].getText().toString());
            }
            
            // Salvar estado das checkboxes
            for (int i = 0; i < 4; i++) {
                editor.putBoolean("box_enabled_" + i, boxEnabled[i]);
            }
            
            // Salvar orientação
            editor.putInt("orientation", currentOrientation);
            
            editor.apply();
            
            Toast.makeText(this, "Estado guardado com sucesso!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao guardar estado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadSavedState() {
        try {
            // Carregar URLs
            for (int i = 0; i < 4; i++) {
                String savedUrl = preferences.getString("url_" + i, "");
                if (!savedUrl.isEmpty()) {
                    urlInputs[i].setText(savedUrl);
                }
            }
            
            // Carregar estado das checkboxes
            for (int i = 0; i < 4; i++) {
                boolean savedState = preferences.getBoolean("box_enabled_" + i, true);
                boxEnabled[i] = savedState;
                checkBoxes[i].setChecked(savedState);
            }
            
            // Carregar orientação
            int savedOrientation = preferences.getInt("orientation", Configuration.ORIENTATION_PORTRAIT);
            currentOrientation = savedOrientation;
            btnOrientation.setText(currentOrientation == Configuration.ORIENTATION_PORTRAIT ? "📱" : "↻");
            
            Toast.makeText(this, "Estado carregado com sucesso!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao carregar estado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // ==================== FAVORITOS ====================
    
    private void loadFavoritesList() {
        try {
            String favoritesJson = preferences.getString("favorites_list", "[]");
            JSONArray jsonArray = new JSONArray(favoritesJson);
            
            favoritesList.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject favorite = jsonArray.getJSONObject(i);
                String name = favorite.getString("name");
                favoritesList.add(name);
            }
            
        } catch (Exception e) {
            favoritesList.clear();
        }
    }
    
    private void saveFavoritesList() {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (int i = 0; i < favoritesList.size(); i++) {
                JSONObject favorite = new JSONObject();
                favorite.put("name", favoritesList.get(i));
                jsonArray.put(favorite);
            }
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("favorites_list", jsonArray.toString());
            editor.apply();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void saveFavorite(String favoriteName) {
        try {
            // Verificar se já existe
            if (favoritesList.contains(favoriteName)) {
                Toast.makeText(this, "Já existe um favorito com este nome!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Criar JSON com as URLs atuais
            JSONObject favoriteData = new JSONObject();
            favoriteData.put("name", favoriteName);
            
            JSONArray urlsArray = new JSONArray();
            for (int i = 0; i < 4; i++) {
                urlsArray.put(urlInputs[i].getText().toString());
            }
            favoriteData.put("urls", urlsArray);
            
            // Adicionar à lista e salvar
            favoritesList.add(favoriteName);
            saveFavoritesList();
            
            // Salvar os dados do favorito
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("favorite_" + favoriteName, favoriteData.toString());
            editor.apply();
            
            Toast.makeText(this, "Favorito '" + favoriteName + "' guardado!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao guardar favorito: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadFavorite(String favoriteName) {
        try {
            String favoriteJson = preferences.getString("favorite_" + favoriteName, "");
            if (favoriteJson.isEmpty()) {
                Toast.makeText(this, "Favorito não encontrado!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            JSONObject favoriteData = new JSONObject(favoriteJson);
            JSONArray urlsArray = favoriteData.getJSONArray("urls");
            
            // Carregar URLs
            for (int i = 0; i < 4 && i < urlsArray.length(); i++) {
                String url = urlsArray.getString(i);
                urlInputs[i].setText(url);
            }
            
            // Carregar todas as URLs
            loadAllURLs();
            
            Toast.makeText(this, "Favorito '" + favoriteName + "' carregado!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao carregar favorito: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteFavorite(String favoriteName) {
        try {
            // Remover da lista
            favoritesList.remove(favoriteName);
            saveFavoritesList();
            
            // Remover dados
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("favorite_" + favoriteName);
            editor.apply();
            
            Toast.makeText(this, "Favorito '" + favoriteName + "' removido!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao remover favorito: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // ==================== DIALOGS ====================
    
    private void showSaveFavoriteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guardar Favorito");
        
        // Configurar a view do dialog
        final EditText input = new EditText(this);
        input.setHint("Nome do favorito");
        builder.setView(input);
        
        builder.setPositiveButton("GUARDAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String favoriteName = input.getText().toString().trim();
                if (!favoriteName.isEmpty()) {
                    saveFavorite(favoriteName);
                } else {
                    Toast.makeText(MainActivity.this, "Digite um nome para o favorito!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }
    
    private void showLoadFavoritesDialog() {
        if (favoritesList.isEmpty()) {
            Toast.makeText(this, "Não há favoritos guardados!", Toast.LENGTH_SHORT).show();
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
        
        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }
    
    private void showFavoriteOptionsDialog(final String favoriteName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Favorito: " + favoriteName);
        
        String[] options = {"Carregar", "Eliminar", "Cancelar"};
        
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Carregar
                        loadFavorite(favoriteName);
                        break;
                    case 1: // Eliminar
                        showDeleteConfirmDialog(favoriteName);
                        break;
                    case 2: // Cancelar
                        dialog.dismiss();
                        break;
                }
            }
        });
        
        builder.show();
    }
    
    private void showDeleteConfirmDialog(final String favoriteName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Eliminação");
        builder.setMessage("Tem certeza que deseja eliminar o favorito '" + favoriteName + "'?");
        
        builder.setPositiveButton("ELIMINAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteFavorite(favoriteName);
            }
        });
        
        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        
        builder.show();
    }
    
    // ==================== OUTROS MÉTODOS ====================
    
    private void activateFullscreen(int boxIndex) {
        // Ativar fullscreen via JavaScript
        WebView webView = webViews[boxIndex];
        String fullscreenJS = 
            "var videos = document.getElementsByTagName('video');" +
            "if (videos.length > 0) {" +
            "   videos[0].requestFullscreen();" +
            "} else {" +
            "   document.documentElement.requestFullscreen();" +
            "}";
        
        webView.loadUrl("javascript:" + fullscreenJS);
    }
    
    private void changeOrientation() {
        // Alternar entre portrait e landscape
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            btnOrientation.setText("↻");
        } else {
            currentOrientation = Configuration.ORIENTATION_PORTRAIT;
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            btnOrientation.setText("📱");
        }
        updateLayout();
    }
    
    private void updateLayout() {
        int activeBoxes = 0;
        for (boolean enabled : boxEnabled) {
            if (enabled) activeBoxes++;
        }
        
        if (activeBoxes == 0) {
            // Nenhuma box ativa, mostrar todas
            for (int i = 0; i < 4; i++) {
                boxEnabled[i] = true;
                checkBoxes[i].setChecked(true);
            }
            activeBoxes = 4;
            Toast.makeText(this, "Todas as boxes ativadas", Toast.LENGTH_SHORT).show();
        }
        
        int rows, cols;
        
        // Layout automático baseado em boxes ativas e orientação
        if (activeBoxes == 1) {
            rows = 1; cols = 1;
        } else if (activeBoxes == 2) {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                rows = 1; cols = 2;
            } else {
                rows = 2; cols = 1;
            }
        } else if (activeBoxes == 3) {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                rows = 1; cols = 3;
            } else {
                rows = 3; cols = 1;
            }
        } else { // 4 boxes
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                rows = 2; cols = 2;
            } else {
                rows = 4; cols = 1;
            }
        }
        
        // Aplicar layout
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
                params.setMargins(2, 2, 2, 2);
                
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
                if (!url.isEmpty()) {
                    loadURL(i, url);
                }
            }
        }
        Toast.makeText(this, "Carregando todas as URLs", Toast.LENGTH_SHORT).show();
    }
    
    private void loadInitialURLs() {
        for (int i = 0; i < 4; i++) {
            String url = urlInputs[i].getText().toString().trim();
            if (!url.isEmpty()) {
                loadURL(i, url);
            }
        }
    }
    
    private void loadURL(int boxIndex, String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        webViews[boxIndex].loadUrl(url);
        Toast.makeText(this, "Carregando Box " + (boxIndex + 1), Toast.LENGTH_SHORT).show();
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
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentOrientation = newConfig.orientation;
        updateLayout();
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
        if (isSidebarVisible) {
            sidebarMenu.setVisibility(View.GONE);
            isSidebarVisible = false;
            return;
        }
        
        // Tentar sair do fullscreen
        for (WebView webView : webViews) {
            if (webView != null && webView.getVisibility() == View.GONE) {
                webView.setVisibility(View.VISIBLE);
                bottomControls.setVisibility(View.VISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                return;
            }
        }
        
        super.onBackPressed();
    }
}