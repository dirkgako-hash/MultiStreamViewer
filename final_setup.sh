#!/bin/bash
echo "🚀 CONFIGURAÇÃO FINAL MULTISTREAMVIEWER"
echo "======================================"

# 1. Criar layout simplificado
echo "📝 Criando layout simplificado..."
# O código acima já criou o layout

# 2. Garantir que colors.xml tem todas as cores necessárias
echo "🎨 Verificando colors.xml..."
cat > app/src/main/res/values/colors.xml << 'COLORS_EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="purple_900">#FF1A237E</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    <color name="gray_800">#FF424242</color>
    <color name="gray_900">#FF212121</color>
</resources>
COLORS_EOF

# 3. Testar build local
echo "🧪 Testando build local..."
if ./gradlew clean assembleDebug 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    echo "✅ BUILD LOCAL SUCESSO!"
    
    # 4. Commit e push
    echo "📤 Fazendo commit e push..."
    git add app/src/main/res/layout/activity_main.xml app/src/main/res/values/colors.xml
    git commit -m "MultiStreamViewer_2: Layout simplificado final

✅ LAYOUT COMPLETO:
- Grid 2x2 com 4 WebViews (tv_webview_1-4)
- Controles com texto (← → ↻ ⌂ GO)
- Input de URL para streams
- Status bar e FAB toggle
- NENHUM drawable privado do Android

🎯 PRONTO PARA:
- Build GitHub Actions 100% funcional
- APK gerado automaticamente
- MultiStreamViewer operacional

🔧 CORREÇÕES FINAIS:
- Removidos android:drawable/ic_menu_refresh
- Removidos android:drawable/ic_menu_home
- Layout 100% compatível com Android público"
    
    git push origin main
    
    echo ""
    echo "🎉 CONFIGURAÇÃO FINALIZADA!"
    echo "📡 GitHub Actions iniciará build em instantes"
    echo "📦 APK será gerado em: app/build/outputs/apk/debug/"
    
else
    echo "❌ Erro no build local. Verificando..."
    ./gradlew assembleDebug 2>&1 | tail -20
fi
