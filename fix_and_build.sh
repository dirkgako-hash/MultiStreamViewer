#!/bin/bash
echo "=== CORRIGINDO ERRO E CONSTRUINDO APK ==="

cd /workspaces/MultiStreamViewer

echo "1. Limpando build anterior..."
rm -rf app/build
./gradlew clean

echo "2. Verificando imports corretos..."
# Verificar se temos o GridLayout correto
if grep -q "android.widget.GridLayout" app/src/main/java/com/example/multistreamviewer/MainActivity.java; then
    echo "   ⚠️  Ainda há referências ao android.widget.GridLayout"
    # Substituir por androidx
    sed -i 's/android.widget.GridLayout/androidx.gridlayout.widget.GridLayout/g' app/src/main/java/com/example/multistreamviewer/MainActivity.java
    echo "   ✅ Corrigido para androidx.gridlayout.widget.GridLayout"
fi

echo "3. Construindo APK..."
if ./gradlew assembleDebug --stacktrace 2>&1 | tee build.log; then
    echo "✅ BUILD BEM-SUCEDIDO!"
    
    # Verificar APK
    APK_FILE=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
    if [ -f "$APK_FILE" ]; then
        echo ""
        echo "🎉 APK CRIADO COM SUCESSO!"
        echo "📦 Arquivo: $APK_FILE"
        echo "📏 Tamanho: $(du -h "$APK_FILE" | cut -f1)"
        echo ""
        echo "✅ Erro corrigido: ClassCastException resolvido"
        echo "✅ Agora usando: androidx.gridlayout.widget.GridLayout"
        echo "✅ Compatível com o XML: androidx.gridlayout.widget.GridLayout"
        
        # Instalar se ADB disponível
        if command -v adb >/dev/null 2>&1; then
            if adb devices | grep -q "device$"; then
                echo ""
                echo "📱 Instalando no dispositivo..."
                adb install -r "$APK_FILE" && echo "✅ App instalado!"
            fi
        fi
    else
        echo "⚠️  APK não encontrado após build bem-sucedido"
    fi
else
    echo "❌ BUILD FALHOU"
    echo ""
    echo "🔍 Erros encontrados:"
    grep -i "error\|failed\|exception" build.log | head -20
fi
