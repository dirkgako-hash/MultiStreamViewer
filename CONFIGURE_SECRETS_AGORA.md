# ⚠️ CONFIGURAÇÃO DE SECRETS - INSTRUÇÕES URGENTES

## 🚨 Problema

Os secrets não foram configurados no repositório. O workflow falha porque:
```
❌ ERROR: STORE_PASS secret must be at least 6 characters long (currently 0 chars)
```

---

## ✅ Solução - Configurar via Web do GitHub

Como o token não tem permissões completas, configure diretamente no GitHub:

### **Acesse:**
```
https://github.com/dirkgako-hash/MultiStreamViewer/settings/secrets/actions
```

### **Clique em "New repository secret"**

Adicione **exatamente 3 secrets** (copie os valores):

#### 1️⃣ Primeiro Secret
- **Name:** `STORE_PASS`
- **Value:** `K9mX2pL4vN7qR8sT9Wub`
- Clique em **Add secret**

#### 2️⃣ Segundo Secret  
- **Name:** `KEY_PASS`
- **Value:** `W1jU3yQ5bC7dF9gH2Ijk`
- Clique em **Add secret**

#### 3️⃣ Terceiro Secret
- **Name:** `KEY_ALIAS`
- **Value:** `multistreamviewer-release`
- Clique em **Add secret**

---

## 📸 Passo a Passo Visual

1. Vá em **Settings** (aba no topo do seu repositório)
2. Clique em **Secrets and variables** (no menu esquerdo)
3. Clique em **Actions** (abaixo de "Secrets and variables")
4. Clique em **New repository secret** (botão verde)
5. Preencha conforme acima, clicando em **Add secret** após cada um

---

## ✔️ Depois de Adicionar os Secrets

1. Vá em **Actions** (aba no seu repositório)
2. Procure pelo workflow "Android Build Release"
3. Clique em **Re-run all jobs** (se houver failed)
4. Ou faça um novo push: `git push origin main`

O build deve passar agora! ✅

---

## 🔗 Link Rápido
Abra direto neste link (substitua o repo se necessário):
```
https://github.com/dirkgako-hash/MultiStreamViewer/settings/secrets/actions/new
```

---

## ❓ Precisa de Senhas Diferentes?

Pode usar qualquer valor com essas regras:
- `STORE_PASS`: Mínimo **6 caracteres**
- `KEY_PASS`: Mínimo **6 caracteres**
- `KEY_ALIAS`: Qualquer texto sem espaços

Exemplo com suas próprias senhas:
```
STORE_PASS: MinhaSenhaSegura@123
KEY_PASS: OutraSenha@456
KEY_ALIAS: myapp-release-key
```

---

## 🎯 Checklist Final

- [ ] Abri https://github.com/dirkgako-hash/MultiStreamViewer/settings/secrets/actions
- [ ] Adicionei STORE_PASS
- [ ] Adicionei KEY_PASS
- [ ] Adicionei KEY_ALIAS
- [ ] Todos os 3 secrets aparecem na lista

**Pronto?** Então o build deve funcionar! 🚀
