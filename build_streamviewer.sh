#!/bin/bash
echo "🔨 BUILD MULTISTREAMVIEWER - LAYOUT COMPATÍVEL"
echo "=============================================="

# Clean everything
echo "🧹 Cleaning..."
./gradlew clean

# Remove problematic backup files
find app/src/main/res -name "*.backup" -o -name "*.bak" -delete 2>/dev/null

echo ""
echo "🚀 Building with compatible layout..."
if ./gradlew assembleDebug --stacktrace; then
    echo ""
    echo "✅ BUILD SUCESSO!"
    
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        echo "📦 APK: $APK_PATH"
        echo "📏 Tamanho: $(du -h "$APK_PATH" | cut -f1)"
        echo ""
        echo "🎯 LAYOUT INCLUI:"
        echo "   • Grid 2x2 com 4 WebViews (tv_webview_1-4)"
        echo "   • Controles de navegação"
        echo "   • Barra de status"
        echo "   • FAB toggle"
        echo "   • Compatível com seu MainActivity.kt"
    fi
else
    echo ""
    echo "❌ BUILD FALHOU"
    echo "Últimos erros:"
    ./gradlew assembleDebug 2>&1 | tail -30
fi
