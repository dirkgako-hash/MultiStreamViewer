#!/bin/bash
echo "=== Aplicando Correções no MultiStreamViewer ==="

# 1. Verificar se estamos no diretório correto
if [ ! -d "/workspaces/MultiStreamViewer/app" ]; then
    echo "Erro: Diretório /workspaces/MultiStreamViewer/app não encontrado!"
    exit 1
fi

echo "✓ Diretório correto encontrado"

# 2. Backup dos arquivos originais
echo "Criando backups..."
BACKUP_DIR="/workspaces/MultiStreamViewer/backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp "/workspaces/MultiStreamViewer/app/src/main/res/layout/activity_main.xml" "$BACKUP_DIR/"
cp "/workspaces/MultiStreamViewer/app/src/main/java/com/example/multistreamviewer/MainActivity.java" "$BACKUP_DIR/"
echo "✓ Backups criados em: $BACKUP_DIR"

# 3. Corrigir o arquivo de layout
echo "Corrigindo activity_main.xml..."
# Usar sed para adicionar android:background aos WebViews
sed -i 's|<WebView[^>]*id="@+id/webView1"[^>]*>|<WebView android:id="@+id/webView1" android:layout_width="match_parent" android:layout_height="match_parent" android:background="#000000">|g' /workspaces/MultiStreamViewer/app/src/main/res/layout/activity_main.xml
sed -i 's|<WebView[^>]*id="@+id/webView2"[^>]*>|<WebView android:id="@+id/webView2" android:layout_width="match_parent" android:layout_height="match_parent" android:background="#000000">|g' /workspaces/MultiStreamViewer/app/src/main/res/layout/activity_main.xml
sed -i 's|<WebView[^>]*id="@+id/webView3"[^>]*>|<WebView android:id="@+id/webView3" android:layout_width="match_parent" android:layout_height="match_parent" android:background="#000000">|g' /workspaces/MultiStreamViewer/app/src/main/res/layout/activity_main.xml
sed -i 's|<WebView[^>]*id="@+id/webView4"[^>]*>|<WebView android:id="@+id/webView4" android:layout_width="match_parent" android:layout_height="match_parent" android:background="#000000">|g' /workspaces/MultiStreamViewer/app/src/main/res/layout/activity_main.xml

# Melhorar visibilidade dos botões
sed -i 's|android:background="#333333"|android:background="#555555"|g' /workspaces/MultiStreamViewer/app/src/main/res/layout/activity_main.xml
echo "✓ Layout XML corrigido"

# 4. Adicionar import Color ao MainActivity.java se necessário
if ! grep -q "import android.graphics.Color" /workspaces/MultiStreamViewer/app/src/main/java/com/example/multistreamviewer/MainActivity.java; then
    sed -i '1s/^/import android.graphics.Color;\n/' /workspaces/MultiStreamViewer/app/src/main/java/com/example/multistreamviewer/MainActivity.java
fi

# 5. Adicionar setBackgroundColor no método setupWebView
sed -i '/webSettings.setDisplayZoomControls(false);/a\        \/\/ CORREÇÃO: Definir fundo preto para o WebView\n        webView.setBackgroundColor(Color.BLACK);' /workspaces/MultiStreamViewer/app/src/main/java/com/example/multistreamviewer/MainActivity.java

# 6. Adicionar injeção de CSS no onPageStarted
sed -i '/if (history.isEmpty() || !history.get(history.size() - 1).equals(url)) {/a\                    \/\/ INJEÇÃO DE CSS para forçar fundo preto no conteúdo web\n                    if (url != null \&\& !url.startsWith("about:")) {\n                        view.loadUrl("javascript:(function() {" +\n                            "document.body.style.backgroundColor = \\"#000000\\";" +\n                            "document.body.style.color = \\"#ffffff\\";" +\n                            "})()");\n                    }' /workspaces/MultiStreamViewer/app/src/main/java/com/example/multistreamviewer/MainActivity.java

echo "✓ MainActivity.java corrigido"

# 7. Limpar e recompilar o projeto
echo "Recompilando o projeto..."
cd /workspaces/MultiStreamViewer
./gradlew clean assembleDebug

echo ""
echo "=== CORREÇÕES APLICADAS COM SUCESSO ==="
echo "1. Fundo dos WebViews alterado para preto (#000000)"
echo "2. Botões com cor mais visível (#555555)"
echo "3. CSS injection para forçar tema escuro em páginas web"
echo "4. Projeto recompilado"
echo ""
echo "Próximos passos:"
echo "1. Execute o app no emulador: ./gradlew installDebug"
echo "2. Teste carregando uma URL no player"
echo "3. Toque em um player para ver os controles (agora mais visíveis)"
