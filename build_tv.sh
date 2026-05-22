#!/bin/bash

echo "=== BUILD PARA FIRE STICK TV ==="
echo ""

# Limpar
./gradlew clean

# Criar keystores se não existirem
if [ ! -f "debug.keystore" ]; then
    echo "Criando debug.keystore..."
    keytool -genkeypair \
      -keystore debug.keystore \
      -alias androiddebugkey \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -storepass android \
      -keypass android \
      -dname "CN=Android Debug, O=Android, C=US" \
      -noprompt
fi

if [ ! -f "multistreamviewer.jks" ]; then
    echo "Criando multistreamviewer.jks..."
    if [ -z "$STORE_PASS" ]; then
        while true; do
            read -s -p "Enter release keystore store password (min 6 chars): " STORE_PASS
            echo
            if [ ${#STORE_PASS} -ge 6 ]; then
                break
            else
                echo "❌ Password must be at least 6 characters long. Try again."
            fi
        done
    fi
    if [ -z "$KEY_PASS" ]; then
        while true; do
            read -s -p "Enter release keystore key password (min 6 chars): " KEY_PASS
            echo
            if [ ${#KEY_PASS} -ge 6 ]; then
                break
            else
                echo "❌ Password must be at least 6 characters long. Try again."
            fi
        done
    fi
    KEY_ALIAS=${KEY_ALIAS:-key0}
    keytool -genkeypair \
      -keystore multistreamviewer.jks \
      -alias "$KEY_ALIAS" \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -storepass "$STORE_PASS" \
      -keypass "$KEY_PASS" \
      -dname "CN=MultiStreamViewer TV, O=Android TV, C=US" \
      -noprompt
fi

if [ ! -f "keystore.properties" ]; then
    echo "Criando keystore.properties..."
    echo "storePassword=${STORE_PASS}" > keystore.properties
    echo "keyPassword=${KEY_PASS}" >> keystore.properties
    echo "keyAlias=${KEY_ALIAS}" >> keystore.properties
    echo "storeFile=../multistreamviewer.jks" >> keystore.properties
fi

echo ""
echo "Construindo APK para TV..."
./gradlew assembleRelease

if [ $? -eq 0 ]; then
    APK=$(find app/build/outputs/apk/release -name "*.apk" | head -1)
    if [ -n "$APK" ]; then
        echo ""
        echo "✅ BUILD COMPLETO PARA TV!"
        echo ""
        echo "📦 APK gerado: $APK"
        echo "📏 Tamanho: $(du -h "$APK" | cut -f1)"
        echo ""
        echo "📺 Para instalar no Fire Stick TV:"
        echo "1. Ative 'Apps de fontes desconhecidas' nas configurações"
        echo "2. Use ADB: adb install -r \"$APK\""
        echo "3. Ou copie via USB"
        echo ""
        echo "🎮 Controles Fire Stick:"
        echo "• D-Pad Left/Right: Navegar entre boxes"
        echo "• D-Pad Up/Down: Navegar no sidebar"
        echo "• Enter/OK: Selecionar/Fullscreen"
        echo "• Menu: Abrir/fechar sidebar"
        echo "• Back: Retroceder/fechar sidebar"
        echo ""
        echo "🖱️ Com MouseToggle:"
        echo "• Clique duplo: Fullscreen na box"
        echo "• Clique simples: Selecionar box"
        echo "• Use o touchpad para navegação precisa"
    else
        echo "❌ APK não encontrado"
        exit 1
    fi
else
    echo "❌ Falha no build"
    exit 1
fi