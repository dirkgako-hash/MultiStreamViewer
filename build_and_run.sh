#!/bin/bash
echo "🔨 CONSTRUINDO MULTISTREAMVIEWER FULLSCREEN"
echo "=========================================="

# Limpar e construir
./gradlew clean assembleDebug

# Verificar APK
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "✅ BUILD SUCESSO!"
    echo "📱 APK: $APK_PATH"
    echo "📏 Tamanho: $(du -h "$APK_PATH" | cut -f1)"
    echo ""
    echo "🎯 CARACTERÍSTICAS IMPLEMENTADAS:"
    echo "   • ✅ Fullscreen (acima da notification bar)"
    echo "   • ✅ 4 WebViews em grid 2x2"
    echo "   • ✅ Controles minimalistas (ocultáveis)"
    echo "   • ✅ Suporte a vídeo em páginas web"
    echo "   • ✅ Navegação individual por WebView"
    echo "   • ✅ URLs padrão para teste"
    echo ""
    echo "📲 INSTRUÇÕES DE USO:"
    echo "   1. Toque no FAB (botão flutuante) para mostrar controles"
    echo "   2. Toque em um WebView para selecioná-lo"
    echo "   3. Digite URL e clique LOAD para carregar"
    echo "   4. Use botões de navegação (← → ↻ ✕)"
    echo "   5. Toque fora dos controles para escondê-los"
else
    echo ""
    echo "❌ APK não gerado"
fi
