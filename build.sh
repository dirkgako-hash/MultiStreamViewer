#!/bin/bash

echo "=== BUILD PARA FIRE STICK TV ==="
echo ""

# Limpar
./gradlew clean

# Construir APK
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    APK=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
    if [ -n "$APK" ]; then
        echo ""
        echo "✅ BUILD COMPLETO!"
        echo ""
        echo "📦 APK gerado: $APK"
        echo ""
        echo "📺 Para instalar no Fire Stick:"
        echo "1. adb install -r \"$APK\""
        echo ""
        echo "🎮 Controles:"
        echo "• Botão CURSOR: Alterna entre modo cursor/DPAD"
        echo "• Modo CURSOR: D-Pad move cursor, Enter clica"
        echo "• Modo DPAD: Navegação tradicional entre boxes"
        echo "• Menu: Abre/fecha sidebar"
        echo "• Back: Voltar/fechar"
    else
        echo "❌ APK não encontrado"
        exit 1
    fi
else
    echo "❌ Falha no build"
    exit 1
fi
