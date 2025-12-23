#!/bin/bash
echo "🧪 TESTANDO MULTISTREAMVIEWER"
echo "============================"

# Verificar ficheiros criados
echo "📁 Ficheiros da app:"
find app/src/main -type f -name "*.kt" -o -name "*.xml" | sort

echo ""
echo "🔧 Configurando ambiente..."
echo "sdk.dir=/usr/local/lib/android/sdk" > local.properties
chmod +x gradlew 2>/dev/null || true

echo ""
echo "🚀 Executando build..."
if ./gradlew :app:assembleDebug; then
    echo ""
    echo "✅ SUCESSO! App MultiStreamViewer criada."
    echo ""
    echo "📊 COMPATIBILIDADE:"
    echo "   • Gradle: 8.2 (do seu wrapper)"
    echo "   • AGP: 8.1.2 (do seu build.gradle)"
    echo "   • Java: 17 (do seu GitHub Actions)"
    echo "   • Estrutura: 100% Groovy DSL"
    echo ""
    echo "📱 APK gerado em: app/build/outputs/apk/debug/app-debug.apk"
else
    echo ""
    echo "❌ Erro no build."
    echo "Última saída:"
    ./gradlew :app:assembleDebug 2>&1 | tail -20
fi
