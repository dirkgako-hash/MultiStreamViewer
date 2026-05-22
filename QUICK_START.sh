#!/bin/bash

# ========================================
# Configuração Rápida de Secrets
# ========================================
# Execute este script e siga as instruções

echo "🔐 Configurador de Secrets - Guia Rápido"
echo "=========================================="
echo ""
echo "Você pode usar este script de 3 formas:"
echo ""
echo "1. INTERATIVO (com prompts):"
echo "   ./setup_github_secrets.sh"
echo ""
echo "2. COM ARGUMENTOS:"
echo "   ./setup_github_secrets.sh 'minhaSenha123' 'outraSenha456' 'meu-alias'"
echo ""
echo "3. TESTAR LOCALMENTE PRIMEIRO:"
echo "   ./test_keystore_local.sh"
echo ""
echo "=========================================="
echo ""
echo "Valores de exemplo que pode usar:"
echo "  STORE_PASS: $(openssl rand -base64 12 2>/dev/null || echo 'SuaSenha123')"
echo "  KEY_PASS: $(openssl rand -base64 12 2>/dev/null || echo 'OutraSenha456')"
echo "  KEY_ALIAS: multistreamviewer-release"
echo ""
echo "Para começar, execute:"
echo "  ./setup_github_secrets.sh"
echo ""
