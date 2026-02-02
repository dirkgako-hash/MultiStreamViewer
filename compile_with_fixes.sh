#!/bin/bash
echo "=== COMPILANDO APLICATIVO COM OTIMIZAÇÕES ==="
cd /workspaces/MultiStreamViewer
./gradlew clean assembleDebug
if [ $? -eq 0 ]; then
    echo "✅ Compilação bem-sucedida!"
    echo "APK gerado em: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "❌ Erro na compilação!"
    exit 1
fi
