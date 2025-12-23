#!/bin/bash

ROOT_DIR="/workspaces/MultiStreamViewer"
OUTPUT_FILE="build_files_dump_fullpath.txt"

echo "📦 ANDROID BUILD AUDIT" > "$OUTPUT_FILE"
echo "Root: $ROOT_DIR" >> "$OUTPUT_FILE"
echo "Date: $(date)" >> "$OUTPUT_FILE"
echo "==================================================" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

dump_file () {
    local file="$1"

    if [ -f "$file" ]; then
        echo "--------------------------------------------------" >> "$OUTPUT_FILE"
        echo "FILE (FULL PATH): $file" >> "$OUTPUT_FILE"
        echo "--------------------------------------------------" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        cat "$file" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    fi
}

# Gradle wrapper
dump_file "$ROOT_DIR/gradlew"
dump_file "$ROOT_DIR/gradlew.bat"

# Root gradle config
dump_file "$ROOT_DIR/build.gradle"
dump_file "$ROOT_DIR/build.gradle.kts"
dump_file "$ROOT_DIR/settings.gradle"
dump_file "$ROOT_DIR/settings.gradle.kts"
dump_file "$ROOT_DIR/gradle.properties"
dump_file "$ROOT_DIR/local.properties"

# Gradle directory
find "$ROOT_DIR/gradle" -type f 2>/dev/null | sort | while read -r f; do
    dump_file "$f"
done

# App module essentials
dump_file "$ROOT_DIR/app/build.gradle"
dump_file "$ROOT_DIR/app/build.gradle.kts"
dump_file "$ROOT_DIR/app/src/main/AndroidManifest.xml"

# GitHub Actions
find "$ROOT_DIR/.github/workflows" -type f 2>/dev/null | sort | while read -r f; do
    dump_file "$f"
done

# Build / fix scripts
find "$ROOT_DIR" -maxdepth 1 -name "*.sh" -type f | sort | while read -r f; do
    dump_file "$f"
done

echo "==================================================" >> "$OUTPUT_FILE"
echo "✅ Dump de ficheiros de compilação concluído." >> "$OUTPUT_FILE"
