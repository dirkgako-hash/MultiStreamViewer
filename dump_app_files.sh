#!/bin/bash

BASE_DIR="/workspaces/MultiStreamViewer/app"
OUTPUT_FILE="app_dump.txt"

echo "📁 Dump de ficheiros" > "$OUTPUT_FILE"
echo "Base: $BASE_DIR" >> "$OUTPUT_FILE"
echo "Data: $(date)" >> "$OUTPUT_FILE"
echo "========================================" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

find "$BASE_DIR" -type f | sort | while read -r file; do
    echo "----------------------------------------" >> "$OUTPUT_FILE"
    echo "FILE: ${file#$BASE_DIR/}" >> "$OUTPUT_FILE"
    echo "----------------------------------------" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"

    # Conteúdo do ficheiro
    cat "$file" >> "$OUTPUT_FILE"

    echo "" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
done

echo "========================================" >> "$OUTPUT_FILE"
echo "✅ Dump concluído com sucesso." >> "$OUTPUT_FILE"
