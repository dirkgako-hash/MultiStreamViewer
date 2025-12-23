#!/bin/bash
set -e

echo "=== START: Fix environment and build (Gradle 8.7, Java 17, Android SDK, deps) ==="

# 1) Instalar pacotes essenciais, ignorando repositórios problemáticos
echo "Installing essential system packages..."
sudo apt-get update || true
sudo apt-get install -y unzip curl wget openjdk-17-jdk || true

# 2) Configurar JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
java -version

# 3) Instalar Gradle 8.7
GRADLE_VERSION=8.7
if [ ! -d "$HOME/gradle-$GRADLE_VERSION" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    wget -q https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip -O /tmp/gradle.zip
    unzip -q /tmp/gradle.zip -d $HOME
    rm /tmp/gradle.zip
fi
export PATH=$HOME/gradle-$GRADLE_VERSION/bin:$PATH
gradle -v

# 4) Instalar Android SDK command-line tools
SDK_DIR=$HOME/Android/Sdk
mkdir -p $SDK_DIR/cmdline-tools
if [ ! -d "$SDK_DIR/cmdline-tools/latest" ]; then
    echo "Downloading Android SDK command-line tools..."
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip
    unzip -q /tmp/cmdline-tools.zip -d $SDK_DIR/cmdline-tools
    mv $SDK_DIR/cmdline-tools/cmdline-tools $SDK_DIR/cmdline-tools/latest
    rm /tmp/cmdline-tools.zip
fi
export ANDROID_HOME=$SDK_DIR
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

# 5) Instalar plataformas e build-tools e aceitar licenças
yes | sdkmanager --licenses || true
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 6) Corrigir dependências no build.gradle (ConstraintLayout 2.1.4 compatível)
echo "Ensuring ConstraintLayout dependency is compatible..."
sed -i 's/androidx.constraintlayout:constraintlayout:2\.1\.5/androidx.constraintlayout:constraintlayout:2.1.4/' app/build.gradle || true

# 7) Limpar projeto e compilar APK debug
echo "Cleaning project..."
./gradlew clean
echo "Assembling debug APK..."
./gradlew app:assembleDebug --info

APK_PATH=$(find app/build/outputs/apk/debug -name "*.apk" | head -n 1)
if [ -f "$APK_PATH" ]; then
    echo "✓ APK debug gerado em: $APK_PATH"
else
    echo "✗ Nenhum APK encontrado em app/build/outputs/apk/debug/"
fi

echo "=== END: Fix environment and build ==="
