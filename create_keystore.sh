#!/bin/bash

echo "Criando keystore..."

# Tente diferentes abordagens
echo "Tentando método 1..."
keytool -genkey \
  -keystore multistreamviewer.jks \
  -alias key0 \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass 123456 \
  -keypass 123456 \
  -dname "CN=Android Debug, O=Android, C=US" 2>/dev/null && echo "✅ Método 1 bem-sucedido!" && exit 0

echo "Tentando método 2..."
keytool -genkeypair \
  -keystore multistreamviewer.jks \
  -alias key0 \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass 123456 \
  -keypass 123456 2>/dev/null && echo "✅ Método 2 bem-sucedido!" && exit 0

echo "Tentando método 3 (interativo)..."
# Método interativo
expect << 'END_EXPECT'
spawn keytool -genkey -keystore multistreamviewer.jks -alias key0 -keyalg RSA -keysize 2048 -validity 10000
expect "Enter keystore password:"
send "123456\r"
expect "Re-enter new password:"
send "123456\r"
expect "What is your first and last name?"
send "Android Developer\r"
expect "What is the name of your organizational unit?"
send "Android\r"
expect "What is the name of your organization?"
send "MultiStreamViewer\r"
expect "What is the name of your City or Locality?"
send "City\r"
expect "What is the name of your State or Province?"
send "State\r"
expect "What is the two-letter country code for this unit?"
send "BR\r"
expect "Is CN=Android Developer, OU=Android, O=MultiStreamViewer, L=City, ST=State, C=BR correct?"
send "yes\r"
expect eof
END_EXPECT

if [ -f "multistreamviewer.jks" ]; then
  echo "✅ Keystore criado com sucesso!"
  exit 0
else
  echo "❌ Falha ao criar keystore"
  exit 1
fi
