#!/bin/bash
echo "=== BUILDING MULTISTREAM VIEWER ==="

cd /workspaces/MultiStreamViewer

# Limpar
echo "1. Cleaning..."
rm -rf app/build
./gradlew clean

# Build
echo "2. Building APK..."
if ./gradlew assembleDebug 2>&1 | tee build.log; then
    echo "✅ BUILD SUCCESSFUL!"
    
    # Verificar APK
    APK_FILE=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
    if [ -f "$APK_FILE" ]; then
        echo ""
        echo "🎉 APK CREATED!"
        echo "📦 File: $APK_FILE"
        echo "📏 Size: $(du -h "$APK_FILE" | cut -f1)"
        echo ""
        echo "📱 Features included:"
        echo "   • 4 WebView boxes with black background"
        echo "   • Dynamic layouts (1x1, 2x2, 1x3, 3x1, 1x4, 4x1)"
        echo "   • Compact ☰ menu button"
        echo "   • 📱 orientation toggle"
        echo "   • Foldable checkbox panel"
        echo "   • Per-box control panel (appears on click)"
        echo "   • Security settings (Scripts, Forms, Popups, Redirects)"
        echo "   • Fullscreen within box (YouTube support)"
        echo "   • Zoom in/out per box"
        echo "   • Auto-hide controls after 10 seconds"
        echo "   • Block redirects option"
        echo ""
        
        # Instalar se ADB disponível
        if command -v adb >/dev/null 2>&1; then
            if adb devices | grep -q "device$"; then
                echo "Installing on device..."
                adb install -r "$APK_FILE"
            fi
        fi
    else
        echo "❌ APK not found"
    fi
else
    echo "❌ BUILD FAILED"
    echo ""
    echo "Errors:"
    grep -i "error\|failed\|exception" build.log | head -20
fi
