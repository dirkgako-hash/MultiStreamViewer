#!/bin/bash
echo "🧹 LIMPEZA PARA BUILD LIMPO"
echo "==========================="

# 1. Remover arquivos de backup e temporários
echo "Removendo backups..."
find app/src/main/res -type f \( -name "*.backup" -o -name "*~" -o -name "*.bak" \) -delete 2>/dev/null

# 2. Remover builds antigos
echo "Limpando builds..."
rm -rf app/build/ build/

# 3. Verificar estrutura
echo ""
echo "📁 ESTRUTURA DE RECURSOS:"
find app/src/main/res -type f -name "*.xml" | sort

# 4. Testar build local (opcional)
echo ""
read -p "Deseja testar build localmente? (s/n): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Ss]$ ]]; then
    echo "🧪 Testando build local..."
    ./gradlew clean assembleDebug 2>&1 | tail -20
fi
