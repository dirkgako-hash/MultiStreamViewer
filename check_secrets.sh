#!/bin/bash

echo "=== Verificando configuração para GitHub Actions ==="
echo ""

# Verificar se temos o keystore localmente
if [ ! -f "multistreamviewer.jks" ]; then
    echo "❌ Erro: multistreamviewer.jks não encontrado!"
    echo ""
    echo "Para criar o keystore, execute:"
    echo "keytool -genkeypair -v \\"
    echo "  -keystore multistreamviewer.jks \\"
    echo "  -keyalg RSA \\"
    echo "  -keysize 2048 \\"
    echo "  -validity 10000 \\"
    echo "  -alias key0 \\"
    echo "  -dname \"CN=MultiStreamViewer, OU=Android, O=MultiStreamViewer, L=City, S=State, C=BR\" \\"
    echo "  -storepass 123456 \\"
    echo "  -keypass 123456"
    echo ""
    exit 1
fi

if [ ! -f "keystore.properties" ]; then
    echo "❌ Erro: keystore.properties não encontrado!"
    echo ""
    echo "Crie o arquivo keystore.properties:"
    echo "echo 'storePassword=123456' > keystore.properties"
    echo "echo 'keyPassword=123456' >> keystore.properties"
    echo "echo 'keyAlias=key0' >> keystore.properties"
    echo "echo 'storeFile=multistreamviewer.jks' >> keystore.properties"
    echo ""
    exit 1
fi

# Converter keystore para base64
echo "Convertendo keystore para base64..."
KEYSTORE_BASE64=$(base64 -w 0 multistreamviewer.jks)

# Ler senhas do keystore.properties
STORE_PASSWORD=$(grep storePassword keystore.properties | cut -d'=' -f2)
KEY_PASSWORD=$(grep keyPassword keystore.properties | cut -d'=' -f2)
KEY_ALIAS=$(grep keyAlias keystore.properties | cut -d'=' -f2)

echo ""
echo "✅ Arquivos locais verificados!"
echo ""
echo "=== CONFIGURAÇÃO DOS SECRETS NO GITHUB ==="
echo ""
echo "1. Acesse: https://github.com/[SEU-USUARIO]/MultiStreamViewer/settings/secrets/actions"
echo "2. Clique em 'New repository secret'"
echo ""
echo "Adicione os seguintes secrets:"
echo ""
echo "📁 Secret 1: ANDROID_KEYSTORE_BASE64"
echo "   Value (cole TODO o conteúdo abaixo):"
echo "   $KEYSTORE_BASE64"
echo ""
echo "🔑 Secret 2: ANDROID_KEYSTORE_PASSWORD"
echo "   Value: $STORE_PASSWORD"
echo ""
echo "🔑 Secret 3: ANDROID_KEY_PASSWORD"
echo "   Value: $KEY_PASSWORD"
echo ""
echo "🏷️  Secret 4: ANDROID_KEY_ALIAS"
echo "   Value: $KEY_ALIAS"
echo ""
echo "=== INSTRUÇÕES ==="
echo "Depois de configurar os 4 secrets:"
echo "1. Faça commit e push das mudanças"
echo "2. Vá para Actions → Android Build and Release"
echo "3. Clique em 'Run workflow'"
echo ""
