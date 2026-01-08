#!/bin/bash

# Cores
BLUE="#2196F3"
WHITE="#FFFFFF"

# Tamanhos para diferentes densidades
declare -A sizes=(
  ["mdpi"]="48x48"
  ["hdpi"]="72x72"
  ["xhdpi"]="96x96"
  ["xxhdpi"]="144x144"
  ["xxxhdpi"]="192x192"
)

# Verificar se imagemagick está instalado
if ! command -v convert &> /dev/null; then
    echo "Instalando ImageMagick..."
    sudo apt-get update && sudo apt-get install -y imagemagick
fi

# Criar ícone quadrado (ic_launcher.png)
for density in "${!sizes[@]}"; do
    size=${sizes[$density]}
    echo "Criando ícone $dpi ($size)..."
    
    # Ícone quadrado
    convert -size $size xc:"$BLUE" \
      -fill "$WHITE" \
      -gravity Center \
      -pointsize $(echo $size | cut -dx -f1 | awk '{print int($1*0.4)}') \
      -annotate 0 "MSV" \
      "app/src/main/res/mipmap-$density/ic_launcher.png"
    
    # Ícone redondo (copiar o mesmo por enquanto)
    convert -size $size xc:"$BLUE" \
      -fill "$WHITE" \
      -gravity Center \
      -pointsize $(echo $size | cut -dx -f1 | awk '{print int($1*0.4)}') \
      -annotate 0 "MSV" \
      "app/src/main/res/mipmap-$density/ic_launcher_round.png"
done

echo "Ícones gerados com sucesso!"
