#!/bin/bash
echo "🚀 WORKFLOW MULTISTREAMVIEWER"
echo "============================"

# 1. Desenvolver
echo "1️⃣  Editar os ficheiros em:"
echo "   • app/src/main/java/com/example/multistreamviewer/MainActivity.kt"
echo "   • app/src/main/res/layout/activity_main.xml"
echo "   • app/src/main/res/values/*.xml"

# 2. Testar build
echo ""
echo "2️⃣  Testar build local:"
./gradlew assembleDebug && echo "✅ Build local OK" || echo "❌ Erro no build"

# 3. Commit
echo ""
echo "3️⃣  Commit e push:"
echo "   git add ."
echo "   git commit -m 'Mensagem descritiva'"
echo "   git push origin main"

# 4. GitHub Actions faz deploy automático
echo ""
echo "4️⃣  GitHub Actions gera APK automaticamente"
echo "   • Verifique em: Actions → build"
echo "   • Download do APK na seção 'Artifacts'"
