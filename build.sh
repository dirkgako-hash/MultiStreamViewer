#!/bin/bash

echo "=== BUILD PARA FIRE STICK TV ==="
echo ""

# Limpar
./gradlew clean

# Construir APK
echo "Construindo APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    APK=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
    if [ -n "$APK" ]; then
        echo ""
        echo "✅ BUILD COMPLETO!"
        echo ""
        echo "📦 APK gerado: $APK"
        echo "📏 Tamanho: $(du -h "$APK" | cut -f1)"
        echo ""
        echo "🎮 Controles:"
        echo "• D-Pad Up/Down: Scroll dentro das boxes"
        echo "• D-Pad Left/Right: Navegar entre boxes"
        echo "• Enter/Long Press: Fullscreen dentro da box"
        echo "• Menu: Abrir/fechar sidebar"
        echo "• Back: Sair do fullscreen/retroceder"
    else
        echo "❌ APK não encontrado"
        exit 1
    fi
else
    echo "❌ Falha no build"
    exit 1
fi
