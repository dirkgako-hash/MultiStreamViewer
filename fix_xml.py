#!/usr/bin/env python3
import re
import sys

# Ler o arquivo
with open('/workspaces/MultiStreamViewer/app/src/main/res/layout/activity_main.xml', 'r') as f:
    content = f.read()

# Problema comum: tags Button não fechadas corretamente
# Corrigir tags Button que podem estar mal formadas
content = re.sub(r'<Button([^>]+)>\s*', r'<Button\1 />', content)

# Outro problema comum: atributos sem aspas
content = re.sub(r'=\s*([^"\s][^>\s]*)', r'="\1"', content)

# Garantir que todos os elementos Button tenham />
content = re.sub(r'<Button([^>/]+)>\s*(.*?)\s*</Button>', r'<Button\1 />', content, flags=re.DOTALL)

# Salvar o arquivo corrigido
with open('/workspaces/MultiStreamViewer/app/src/main/res/layout/activity_main.xml', 'w') as f:
    f.write(content)

print("XML corrigido")
