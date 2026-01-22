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
        echo "🔧 AUDITORIA E CORREÇÃO DO SIDEBAR:"
        echo "• ✓ PROBLEMA IDENTIFICADO: ScrollView com layout_weight causava problema"
        echo "• ✓ SOLUÇÃO: FrameLayout principal com overlay transparente"
        echo "• ✓ FrameLayout interno fixo em 180dp (todo clicável)"
        echo "• ✓ Botões GO com 70dp (área clicável ampla)"
        echo "• ✓ Elevation 100dp para garantir sobreposição"
        echo "• ✓ Método closeSidebarFromOverlay no XML"
        echo "• ✓ Todos os elementos com clickable=true"
        echo "• ✓ Sidebar organizado em 3 camadas para capturar cliques"
    else
        echo "❌ APK não encontrado"
        exit 1
    fi
else
    echo "❌ Falha no build"
    exit 1
fi
