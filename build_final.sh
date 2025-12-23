#!/bin/bash
echo "🔨 BUILD FINAL - CORRIGIDO"
echo "=========================="

# 1. Garantir que não há recursos privados
echo "🔍 Verificando recursos..."
if grep -r "android:drawable/ic_menu" app/src/main/res/ 2>/dev/null; then
    echo "⚠️  Encontrados recursos privados. Corrigindo..."
    # Remover referências a ic_menu_refresh
    find app/src/main/res/ -name "*.xml" -exec sed -i 's|android:drawable/ic_menu_refresh|@drawable/ic_refresh|g' {} \;
fi

# 2. Build
echo ""
echo "🚀 Executando build..."
if ./gradlew clean assembleDebug; then
    echo ""
    echo "✅ BUILD SUCESSO!"
    
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        echo "📦 APK: $APK_PATH"
        echo "📏 Tamanho: $(du -h "$APK_PATH" | cut -f1)"
    else
        echo "⚠️  APK não encontrado no local padrão"
        find . -name "*.apk" -type f 2>/dev/null
    fi
else
    echo ""
    echo "❌ BUILD FALHOU"
    echo "Últimos erros:"
    ./gradlew assembleDebug 2>&1 | tail -20
fi
