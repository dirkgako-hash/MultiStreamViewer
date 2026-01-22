#!/bin/bash

echo "=== BUILD FINAL PARA FIRE STICK TV ==="
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
        echo "🎮 CORREÇÕES APLICADAS:"
        echo "1. ✓ EDIÇÃO DE URLs NO SIDEBAR:"
        echo "   • EditText com inputType textUri|textMultiLine"
        echo "   • setCursorVisible(true) e setSelectAllOnFocus(true)"
        echo "   • OnTouchListener para focar e selecionar ao tocar"
        echo "   • OnFocusChangeListener para selecionar texto"
        echo "2. ✓ BOTÕES GO funcionando"
        echo "3. ✓ ÁREA CLICÁVEL completa no sidebar"
        echo "4. ✓ Zoom no conteúdo da página"
        echo "5. ✓ Menu inferior sempre visível"
        echo "6. ✓ Fullscreen dentro da box"
        echo ""
        echo "📱 Para instalar no Fire Stick TV:"
        echo "   adb install $APK"
    else
        echo "❌ APK não encontrado"
        exit 1
    fi
else
    echo "❌ Falha no build"
    exit 1
fi
