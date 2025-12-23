#!/bin/bash
echo "🔨 BUILD CORRIGIDO - COM KOTLIN"
echo "================================"

# 1. Parar tudo e limpar
echo "🧹 Limpando completamente..."
./gradlew --stop 2>/dev/null || true
rm -rf app/build/ .gradle/ build/

# 2. Garantir local.properties
echo "🔧 Configurando SDK..."
echo "sdk.dir=/usr/local/lib/android/sdk" > local.properties

# 3. Sincronizar Gradle (CRÍTICO!)
echo "🔄 Sincronizando Gradle com Kotlin..."
./gradlew :app:preBuild --stacktrace

# 4. Build
echo "🚀 Construindo APK..."
if ./gradlew assembleDebug --stacktrace; then
    echo ""
    echo "✅ BUILD SUCESSO!"
    echo ""
    
    # Verificar APK
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        echo "📦 APK: $APK_PATH"
        echo "📏 Tamanho: $(du -h "$APK_PATH" | cut -f1)"
        
        # Verificar se MainActivity está no APK
        echo ""
        echo "🔍 Verificando conteúdo do APK..."
        if command -v unzip &> /dev/null; then
            echo "📄 Classes no APK:"
            unzip -l "$APK_PATH" | grep -i "multistreamviewer\|MainActivity" | head -5
        fi
    fi
    
    # Instruções
    echo ""
    echo "🎯 PRÓXIMOS PASSOS:"
    echo "1. DESINSTALE versões anteriores:"
    echo "   adb uninstall com.example.multistreamviewer"
    echo "2. INSTALE nova versão:"
    echo "   adb install $APK_PATH"
    echo "3. Se ainda falhar, limpe cache do Play Store no dispositivo"
    
else
    echo ""
    echo "❌ BUILD FALHOU"
    echo "Últimos erros:"
    ./gradlew assembleDebug 2>&1 | tail -30
fi
