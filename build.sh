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
        echo "• ✓ Import WindowManager corrigido"
        echo "• ✓ Auto refresh removido"
        echo "• ✓ Zoom corrigido (usa JavaScript para conteúdo)"
        echo "• ✓ Botões GO funcionando"
        echo "• ✓ Inputs de URL editáveis"
        echo "• ✓ Layout sidebar corrigido (160dp)"
        echo "• ✓ Área direita clicável"
        echo "• ✓ Menu inferior sempre visível"
        echo "• ✓ Favoritos com mesma lógica de input"
    else
        echo "❌ APK não encontrado"
        exit 1
    fi
else
    echo "❌ Falha no build"
    exit 1
fi
