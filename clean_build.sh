#!/bin/bash
echo "=== LIMPEZA TOTAL E BUILD ==="

# 1. REMOVER TODOS OS ARQUIVOS PROBLEMÁTICOS
echo "1. Limpando diretório res/layout..."
cd /workspaces/MultiStreamViewer
rm -f app/src/main/res/layout/*.backup
rm -f app/src/main/res/layout/*.error
rm -f app/src/main/res/layout/*.bak
rm -f app/src/main/res/layout/*.tmp
rm -f app/src/main/res/layout/*.old

# Garantir que só tem arquivos .xml
find app/src/main/res/layout -type f ! -name "*.xml" -delete

# 2. VERIFICAR SE O ARQUIVO PRINCIPAL EXISTE
if [ ! -f "app/src/main/res/layout/activity_main.xml" ]; then
    echo "2. Criando activity_main.xml básico..."
    cat > app/src/main/res/layout/activity_main.xml << 'XML'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <WebView
        android:id="@+id/webView1"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#000000" />
</FrameLayout>
XML
fi

# 3. VALIDAR XML
echo "3. Validando XML..."
if xmllint --noout app/src/main/res/layout/activity_main.xml 2>/dev/null; then
    echo "   ✓ XML válido"
else
    echo "   ✗ XML inválido - criando novo"
    cat > app/src/main/res/layout/activity_main.xml << 'XML'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <WebView
        android:id="@+id/webView1"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</FrameLayout>
XML
fi

# 4. LIMPAR BUILD ANTERIOR COMPLETAMENTE
echo "4. Limpando build anterior..."
rm -rf app/build
rm -rf build
rm -rf .gradle
./gradlew clean

# 5. CONSTRUIR APK DEBUG
echo "5. Construindo APK Debug..."
if ./gradlew assembleDebug --stacktrace 2>&1 | tee build_output.txt; then
    echo "✅ BUILD BEM-SUCEDIDO!"
    
    # Verificar APK
    if find app/build/outputs/apk -name "*.apk" 2>/dev/null | grep -q .; then
        echo ""
        echo "📦 APKs ENCONTRADOS:"
        find app/build/outputs/apk -name "*.apk" -exec ls -lh {} \;
        
        DEBUG_APK=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
        if [ -f "$DEBUG_APK" ]; then
            echo ""
            echo "🎯 APK PRINCIPAL: $DEBUG_APK"
            echo "📏 Tamanho: $(du -h "$DEBUG_APK" | cut -f1)"
            
            # Instalar se ADB disponível
            if command -v adb >/dev/null 2>&1; then
                if adb devices | grep -q "device$"; then
                    echo "📱 Instalando no dispositivo..."
                    adb install -r "$DEBUG_APK"
                fi
            fi
        fi
    else
        echo "⚠️  Nenhum APK encontrado após build bem-sucedido"
    fi
else
    echo "❌ BUILD FALHOU"
    echo ""
    echo "📋 ÚLTIMOS ERROS:"
    tail -50 build_output.txt | grep -i "error\|failed\|exception"
    echo ""
    echo "📁 Verifique o arquivo completo: build_output.txt"
fi
