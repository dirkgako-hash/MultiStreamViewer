# ✅ Erro de Keystore Corrigido

## 🔴 Problema Anterior

O erro `BadPaddingException: Given final block not properly padded` ocorria porque:
1. O keystore era criado com senhas diferentes para store e key
2. O caminho do arquivo estava incorreto no keystore.properties
3. Faltava validação do keystore logo após criação

```
Failed to read key *** from store ".../multistreamviewer.jks": 
Get Key failed: Given final block not properly padded.
```

---

## ✅ Soluções Aplicadas

### 1. **Mesma Senha para Keystore e Key**
Antes: `storepass "$STORE_PASS"` + `keypass "$KEY_PASS"` (senhas diferentes)
Depois: `storepass "$STORE_PASS"` + `keypass "$STORE_PASS"` (mesma senha)

### 2. **Validação Imediata do Keystore**
Após criar o keystore, o workflow agora testa se consegue ler com a mesma senha:
```bash
keytool -list -v -keystore multistreamviewer.jks -storepass "$STORE_PASS"
```

### 3. **Caminho Correto do Arquivo**
- Antes: `storeFile=multistreamviewer.jks` (errado, fazia o Gradle procurar em `app/multistreamviewer.jks`)
- Depois: `storeFile=../multistreamviewer.jks` (correto, porque o arquivo é criado na raiz do projeto)

### 4. **Limpeza de Arquivos Antigos**
O workflow agora remove arquivos antigos antes de criar novos:
```bash
rm -f multistreamviewer.jks keystore.properties
```

---

## 🚀 Próximas Etapas

O workflow já foi atualizado e enviado para o GitHub. Agora:

### **Opção 1: Aguarde a Próxima Build**
Se você já tem os secrets configurados, o workflow será executado novamente automaticamente quando fizer push.

### **Opção 2: Dispare Manualmente**
1. Vá em: **Actions** → **Android Build Release**
2. Clique em **Re-run all jobs**

### **Opção 3: Force um Novo Build**
```bash
git commit --allow-empty -m "trigger: Force rebuild"
git push origin main
```

---

## 📊 O que Mudou no Workflow

### Antes (ERRADO)
```bash
keytool -genkeypair \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS"        # ❌ Senha diferente

echo "storePassword=${STORE_PASS}" > keystore.properties
echo "keyPassword=${KEY_PASS}" >> keystore.properties
echo "storeFile=multistreamviewer.jks" >> keystore.properties  # ❌ Caminho errado
```

### Depois (CORRETO)
```bash
keytool -genkeypair \
  -storepass "$STORE_PASS" \
  -keypass "$STORE_PASS"      # ✅ Mesma senha

# Validação imediata
keytool -list -v -keystore multistreamviewer.jks -storepass "$STORE_PASS"

echo "storeFile=../multistreamviewer.jks" > keystore.properties  # ✅ Caminho correto
echo "storePassword=$STORE_PASS" >> keystore.properties
echo "keyAlias=$KEY_ALIAS" >> keystore.properties
echo "keyPassword=$STORE_PASS" >> keystore.properties
```

---

## 🔍 Verificação

Você pode verificar o novo workflow em:
```
https://github.com/dirkgako-hash/MultiStreamViewer/actions
```

Procure pelo workflow mais recente e veja se passou na etapa **"Create Release Keystore"**.

---

## 📝 Resumo

| Problema | Causa | Solução |
|----------|-------|--------|
| BadPadding | Senhas diferentes | Usar mesma senha para store e key |
| Caminho errado | `multistreamviewer.jks` | Usar `../multistreamviewer.jks` |
| Sem validação | Não testava se a senha funcionava | Adicionar teste de leitura do keystore |

---

## ✨ Se Ainda Tiver Problemas

1. Verifique se os secrets estão configurados (no GitHub Settings)
2. Veja os logs do workflow em **Actions** → seu workflow → **Create Release Keystore**
3. Se necessário, reconfigure os secrets usando senhas **sem caracteres especiais**

Exemplo de senhas seguras:
```
STORE_PASS: SecurePass123456
KEY_PASS: AnotherPass789012  (não será usado, mas deixe configurado)
KEY_ALIAS: multistreamviewer-release
```
