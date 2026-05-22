#!/bin/bash

# Script para configurar secrets do GitHub automaticamente
# Uso: ./setup_github_secrets.sh <store_pass> <key_pass> <key_alias>

set -e

echo "=================================================="
echo "  Configurador de Secrets do GitHub Actions"
echo "=================================================="
echo ""

# Verificar se gh CLI está instalado
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) não encontrado!"
    echo "Instale com: https://cli.github.com/"
    exit 1
fi

# Verificar autenticação
if ! gh auth status &> /dev/null; then
    echo "❌ Não autenticado no GitHub!"
    echo "Execute: gh auth login"
    exit 1
fi

# Usar argumentos ou pedir input
if [ $# -eq 3 ]; then
    STORE_PASS=$1
    KEY_PASS=$2
    KEY_ALIAS=$3
else
    echo "📝 Digite as credenciais para o keystore de release:"
    echo ""
    
    read -sp "STORE_PASS (mínimo 6 caracteres): " STORE_PASS
    echo ""
    
    read -sp "KEY_PASS (mínimo 6 caracteres): " KEY_PASS
    echo ""
    
    read -p "KEY_ALIAS (nome da chave, ex: multistreamviewer-release): " KEY_ALIAS
    echo ""
fi

# Validar comprimentos
if [ ${#STORE_PASS} -lt 6 ]; then
    echo "❌ STORE_PASS deve ter pelo menos 6 caracteres (tem ${#STORE_PASS})"
    exit 1
fi

if [ ${#KEY_PASS} -lt 6 ]; then
    echo "❌ KEY_PASS deve ter pelo menos 6 caracteres (tem ${#KEY_PASS})"
    exit 1
fi

if [ -z "$KEY_ALIAS" ]; then
    echo "❌ KEY_ALIAS não pode estar vazio"
    exit 1
fi

echo ""
echo "🔐 Configurando secrets no GitHub..."
echo ""

# Criar os secrets
gh secret set STORE_PASS --body "$STORE_PASS"
echo "✓ STORE_PASS configurado"

gh secret set KEY_PASS --body "$KEY_PASS"
echo "✓ KEY_PASS configurado"

gh secret set KEY_ALIAS --body "$KEY_ALIAS"
echo "✓ KEY_ALIAS configurado"

echo ""
echo "=================================================="
echo "✅ Todos os secrets foram configurados com sucesso!"
echo "=================================================="
echo ""
echo "📌 Próximas etapas:"
echo "  1. Faça commit e push para main"
echo "  2. O workflow será disparado automaticamente"
echo "  3. Verifique o progresso em: GitHub Actions"
echo ""
