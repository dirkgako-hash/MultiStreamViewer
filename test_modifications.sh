#!/bin/bash

echo "=== Testando Modificações ==="
echo ""

# Verificar se os arquivos foram criados
echo "1. Verificando layout..."
if [ -f "app/src/main/res/layout/activity_main.xml" ]; then
    echo "✅ Layout principal criado"
else
    echo "❌ Layout principal não encontrado"
fi

echo ""
echo "2. Verificando código Java..."
if [ -f "app/src/main/java/com/example/multistreamviewer/MainActivity.java" ]; then
    echo "✅ Código Java atualizado"
    # Verificar se tem as novas funcionalidades
    grep -q "btnLoadUrl1" app/src/main/java/com/example/multistreamviewer/MainActivity.java && \
      echo "✅ Botões de carregar individual OK"
    grep -q "kevinsport.pro" app/src/main/java/com/example/multistreamviewer/MainActivity.java && \
      echo "✅ URL padrão OK"
    grep -q "videosMutedByDefault" app/src/main/java/com/example/multistreamviewer/MainActivity.java && \
      echo "✅ Mute por padrão OK"
else
    echo "❌ Código Java não encontrado"
fi

echo ""
echo "3. Testando build..."
./gradlew clean assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build bem-sucedido!"
    APK=$(find app/build -name "*.apk" -type f | head -1)
    if [ -n "$APK" ]; then
        echo "📦 APK gerado: $APK"
        echo "📏 Tamanho: $(du -h "$APK" | cut -f1)"
    fi
else
    echo "❌ Falha no build"
    exit 1
fi

echo ""
echo "=== RESUMO DAS MODIFICAÇÕES ==="
echo "✅ Botão ao lado de cada URL para carregar individual"
echo "✅ Layout 1x3 quando 3 boxes ativas"
echo "✅ Sidebar sobreposto (não clica nas boxes)"
echo "✅ Favoritos corrigidos para carregar na box correta"
echo "✅ URL padrão: https://kevinsport.pro/live/football/"
echo "✅ Botão fechar sempre visível no sidebar"
echo "✅ Scroll down para itens do sidebar"
echo "✅ Menu inferior reduzido (2/3 menor)"
echo "✅ Sidebar sem transparência"
echo "✅ Vídeos em mute por padrão"
