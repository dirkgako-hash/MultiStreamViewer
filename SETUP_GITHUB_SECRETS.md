# Configuração de Secrets do GitHub para Build Release

## 🔐 Secrets Necessários

O workflow de build do GitHub Actions requer 3 secrets para criar a keystore de release:

### 1. **STORE_PASS**
- **O que é**: Senha do keystore (arquivo .jks)
- **Requisito**: Mínimo 6 caracteres
- **Exemplo**: `MySecureStore123!`

### 2. **KEY_PASS**
- **O que é**: Senha da chave privada
- **Requisito**: Mínimo 6 caracteres
- **Exemplo**: `MySecureKey456!`

### 3. **KEY_ALIAS**
- **O que é**: Nome/alias da chave (identificador)
- **Requisito**: Não vazio
- **Exemplo**: `multistreamviewer-release`

---

## 📝 Como Adicionar os Secrets no GitHub

### Via Web Interface:
1. Acesse seu repositório no GitHub
2. Clique em **Settings** (Configurações)
3. No menu esquerdo, vá para **Secrets and variables** > **Actions**
4. Clique em **New repository secret**
5. Para cada secret abaixo:
   - **Name**: Digite o nome exato (STORE_PASS, KEY_PASS, KEY_ALIAS)
   - **Secret**: Digite o valor
   - Clique em **Add secret**

### Via GitHub CLI (terminal):
```bash
gh secret set STORE_PASS --body "MySecureStore123!"
gh secret set KEY_PASS --body "MySecureKey456!"
gh secret set KEY_ALIAS --body "multistreamviewer-release"
```

---

## ✅ Verificação

Após adicionar os secrets:
1. Faça um novo commit e push para `main`
2. O workflow será disparado automaticamente
3. Verifique em **Actions** para ver o progresso
4. Se tudo correr bem, o APK será gerado

---

## 🛡️ Segurança

- Os secrets são **criptografados** no GitHub
- Nunca aparecem nos logs do workflow (mascarados com `***`)
- Apenas você e contribuidores autorizados podem vê-los
- Use senhas **fortes** e **únicas**

---

## 💡 Dica de Senhas Seguras

Se precisar gerar senhas aleatórias seguras:
```bash
# Linux/Mac
openssl rand -base64 12

# Ou usar Python
python3 -c "import secrets; print(secrets.token_urlsafe(16))"
```

Exemplos gerados:
- `STORE_PASS`: `K9mX2pL4vN7qR8sT`
- `KEY_PASS`: `W1jU3yQ5bC7dF9gH`
- `KEY_ALIAS`: `multistreamviewer-key`
