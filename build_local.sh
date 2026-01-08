#!/bin/bash

# Script para build local

echo "Limpando projeto..."
./gradlew clean

echo "Construindo APK de debug..."
./gradlew assembleDebug

echo "Construindo APK de release..."
./gradlew assembleRelease

echo "APKs gerados:"
find app/build/outputs -name "*.apk" -type f | xargs ls -la

echo "Tamanho dos APKs:"
du -h app/build/outputs/apk/*/*.apk

echo ""
echo "Para instalar o debug no dispositivo conectado:"
echo "adb install -r app/build/outputs/apk/debug/*.apk"
