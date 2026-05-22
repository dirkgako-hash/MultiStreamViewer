#!/bin/bash

# Script para testar criação de keystore localmente
# Útil para verificar se as credenciais funcionam antes de fazer push

set -e

echo "=================================================="
echo "  Teste Local de Keystore"
echo "=================================================="
echo ""

# Verificar se keytool está disponível
if ! command -v keytool &> /dev/null; then
    echo "❌ keytool não encontrado! Certifique-se de ter Java instalado."
    exit 1
fi

# Usar argumentos ou pedir input
if [ $# -eq 3 ]; then
    STORE_PASS=$1
    KEY_PASS=$2
    KEY_ALIAS=$3
else
    echo "📝 Digite as credenciais para testar o keystore:"
    echo ""
    
    read -sp "STORE_PASS (mínimo 6 caracteres): " STORE_PASS
    echo ""
    
    read -sp "KEY_PASS (mínimo 6 caracteres): " KEY_PASS
    echo ""
    
    read -p "KEY_ALIAS (nome da chave): " KEY_ALIAS
    echo ""
fi

# Validar comprimentos
if [ ${#STORE_PASS} -lt 6 ]; then
    echo "❌ STORE_PASS deve ter pelo menos 6 caracteres"
    exit 1
fi

if [ ${#KEY_PASS} -lt 6 ]; then
    echo "❌ KEY_PASS deve ter pelo menos 6 caracteres"
    exit 1
fi

if [ -z "$KEY_ALIAS" ]; then
    echo "❌ KEY_ALIAS não pode estar vazio"
    exit 1
fi

# Criar pasta temporária para teste
TEST_DIR=$(mktemp -d)
trap "rm -rf $TEST_DIR" EXIT

echo "🔨 Criando keystore de teste em: $TEST_DIR"
echo ""

# Criar o keystore
keytool -genkeypair \
    -keystore "$TEST_DIR/test.jks" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$STORE_PASS" \
    -keypass "$KEY_PASS" \
    -dname "CN=Android, OU=Android, O=Android, L=City, ST=State, C=US" \
    -noprompt

# Verificar o keystore
echo ""
echo "📋 Informações do keystore criado:"
keytool -list -v -keystore "$TEST_DIR/test.jks" -storepass "$STORE_PASS" 2>/dev/null | head -20

echo ""
echo "=================================================="
echo "✅ Keystore criado com sucesso!"
echo "=================================================="
echo ""
echo "✓ Senhas estão corretas"
echo "✓ Alias '$KEY_ALIAS' é válido"
echo "✓ Pronto para usar no GitHub Actions"
echo ""
