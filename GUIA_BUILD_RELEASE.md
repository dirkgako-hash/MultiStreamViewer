# 🔧 Guia Completo - Configuração do Build Release

## 📋 Resumo do Problema

O workflow GitHub Actions não consegue criar a keystore porque os **secrets necessários não estão configurados** no repositório.

---

## ✅ Solução Passo a Passo

### **Opção 1: Via GitHub Web Interface (Mais Fácil)**

1. Abra seu repositório no GitHub
2. Clique em **Settings** (⚙️)
3. No menu esquerdo: **Secrets and variables** → **Actions**
4. Clique em **New repository secret**
5. Adicione cada secret (3 ao total):

| Nome | Valor | Notas |
|------|-------|-------|
| `STORE_PASS` | `SuaSenhaForte123!` | Mín. 6 caracteres |
| `KEY_PASS` | `OutraSenhaForte456!` | Mín. 6 caracteres |
| `KEY_ALIAS` | `multistreamviewer-key` | Identificador único |

---

### **Opção 2: Via GitHub CLI (Mais Rápido)**

Execute na raiz do projeto:

```bash
# Usando o script automático (interativo)
./setup_github_secrets.sh

# Ou direto via CLI
gh secret set STORE_PASS --body "SuaSenhaForte123!"
gh secret set KEY_PASS --body "OutraSenhaForte456!"
gh secret set KEY_ALIAS --body "multistreamviewer-key"
```

---

### **Opção 3: Testar Localmente Primeiro (Recomendado)**

Verifique se suas credenciais funcionam antes de fazer push:

```bash
./test_keystore_local.sh
```

O script vai pedir as mesmas credenciais e criar um keystore de teste.

---

## 🚀 Próximas Etapas

Depois de configurar os secrets:

```bash
# 1. Adicionar mudanças (se houver)
git add .

# 2. Fazer commit
git commit -m "feat: Configure GitHub Secrets for release build"

# 3. Fazer push
git push origin main
```

O workflow será disparado automaticamente! 🎉

---

## 📊 Status do Workflow

Após o push, verifique em:
- **GitHub.com** → seu repositório → **Actions** tab
- Procure pelo workflow mais recente
- Veja os logs em tempo real

---

## 🛠️ Arquivos de Ajuda

- **SETUP_GITHUB_SECRETS.md** - Documentação detalhada dos secrets
- **setup_github_secrets.sh** - Script para configurar secrets automaticamente
- **test_keystore_local.sh** - Script para testar localmente

---

## ⚠️ Troubleshooting

### Erro: "STORE_PASS secret must be at least 6 characters"
- ✓ Verifique se você digitou a senha corretamente
- ✓ Certifique-se de que tem pelo menos 6 caracteres
- ✓ Tente usar: `setup_github_secrets.sh` para evitar erros

### Erro: "gh: command not found"
- Instale GitHub CLI: https://cli.github.com/
- Autentique-se: `gh auth login`

### Erro: "keytool: command not found" (ao testar localmente)
- Certifique-se de ter Java instalado
- Configure: `export JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-amd64`

---

## 💡 Dicas de Segurança

✓ Use senhas **fortes** e **únicas**  
✓ Os secrets são **criptografados** no GitHub  
✓ Nunca aparecem nos logs (mascarados com `***`)  
✓ Regenere periodicamente por segurança  

---

## 📞 Precisa de Ajuda?

Se o workflow falhar, verifique:
1. ✓ Os secrets estão todos configurados?
2. ✓ As senhas têm pelo menos 6 caracteres?
3. ✓ O KEY_ALIAS não é vazio?
4. ✓ Você fez push para `main`?

Veja os logs completos em **Actions** → seu workflow → **Create Release Keystore** para mais detalhes.
