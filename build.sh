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
        echo "🎮 NOVAS MELHORIAS:"
        echo "• Menu inferior: 24dp altura (reduzido para metade)"
        echo "• Menu inferior: SEMPRE VISÍVEL, mesmo em fullscreen"
        echo "• Sidebar: 150dp largura (compactado)"
        echo "• Botões GO: Carregar URLs individuais"
        echo "• Zoom: Atua no conteúdo da página (50-200%)"
        echo "• Inputs de URL: Editáveis normalmente"
        echo "• Botão fechar sidebar: Funciona corretamente"
    else
        echo "❌ APK não encontrado"
        exit 1
    fi
else
    echo "❌ Falha no build"
    exit 1
fi
