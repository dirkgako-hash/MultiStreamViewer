#!/bin/bash

echo "====================================="
echo " MultiBox DIROSKY Auto Version Commit"
echo "====================================="

# Procurar último commit com padrão MultiBox_DIROSKY_v
last_version=$(git log --pretty=format:"%s" | grep -o 'MultiBox_DIROSKY_v[0-9]*' | head -n 1 | grep -o '[0-9]*')

if [ -z "$last_version" ]; then
    new_version=1
else
    new_version=$((last_version + 1))
fi

# Formatar com zero à esquerda (v01, v02, etc.)
printf -v version_formatted "%02d" $new_version

echo "Nova versão: v$version_formatted"

git add .
git config commit.gpgsign false
git commit -m "MultiBox_DIROSKY_v$version_formatted"
git push origin main

echo ""
echo "✔ Commit enviado: MultiBox_DIROSKY_v$version_formatted"
echo "====================================="