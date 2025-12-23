#!/bin/bash
echo "🚀 DESENVOLVIMENTO MULTISTREAMVIEWER"
echo "==================================="

# Configurar ambiente
echo "sdk.dir=/usr/local/lib/android/sdk" > local.properties
chmod +x gradlew

echo ""
echo "1️⃣  LIMPANDO..."
./gradlew clean

echo ""
echo "2️⃣  CONSTRUINDO APK..."
if ./gradlew assembleDebug --stacktrace; then
    echo ""
    echo "✅ BUILD SUCESSO!"
    
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        echo "📱 APK: $APK_PATH"
        echo "📏 Tamanho: $(du -h "$APK_PATH" | cut -f1)"
        echo ""
        echo "🎯 PRÓXIMOS PASSOS:"
        echo "   • Edite os ficheiros em app/src/main/"
        echo "   • Execute este script novamente para testar"
        echo "   • Commit e push para acionar GitHub Actions"
    fi
else
    echo ""
    echo "❌ BUILD FALHOU"
    echo "Últimos erros:"
    ./gradlew assembleDebug 2>&1 | tail -20
fi
