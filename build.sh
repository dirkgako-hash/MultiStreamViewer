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
        echo "🔧 CORREÇÕES APLICADAS:"
        echo "• ✓ Sidebar com overlay para capturar cliques"
        echo "• ✓ Botões CARREGAR com área clicável ampla (60dp)"
        echo "• ✓ Sidebar com 200dp de largura"
        echo "• ✓ Elevation para sidebar ficar sobre gridLayout"
        echo "• ✓ Todos os elementos clicáveis com clickable=true"
        echo "• ✓ Overlay para fechar sidebar ao clicar fora"
    else
        echo "❌ APK não encontrado"
        exit 1
    fi
else
    echo "❌ Falha no build"
    exit 1
fi
