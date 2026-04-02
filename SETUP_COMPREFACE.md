# 🔧 Setup CompreFace - Guia Passo a Passo

## ✅ Pré-requisitos

Antes de começar, certifique-se de ter instalado:
- [Docker](https://docs.docker.com/get-docker/) (versão 20.10+)
- [Docker Compose](https://docs.docker.com/compose/install/) (versão 1.29+)

### Verificar instalação:
```bash
docker --version
docker-compose --version
```

---

## 📦 PASSO 1: Instalar CompreFace

### 1.1 Iniciar CompreFace com Docker Compose

No diretório do projeto `/Users/igorguerreiro/Documents/Projects/taskly`, execute:

```bash
# Iniciar todos os serviços do CompreFace
docker-compose up -d
```

### 1.2 Verificar se os containers estão rodando

```bash
# Listar containers
docker-compose ps

# Deve mostrar 4 containers:
# - compreface-postgres (PostgreSQL)
# - compreface-api (API Core)
# - compreface-admin (Admin Backend)
# - compreface-ui (Frontend UI)
```

### 1.3 Acompanhar os logs (opcional)

```bash
# Ver logs de todos os serviços
docker-compose logs -f

# Ver logs apenas da API
docker-compose logs -f compreface-api

# Pressione Ctrl+C para sair
```

### 1.4 Aguardar inicialização completa

⏳ O CompreFace pode levar **2-3 minutos** para iniciar completamente na primeira vez.

Você saberá que está pronto quando ver nos logs:
```
compreface-ui    | Listening on port 80
compreface-api   | Started Application
```

### 1.5 Acessar a interface web

Abra o navegador e acesse:
```
http://localhost:8000
```

Se tudo estiver correto, você verá a tela de login do CompreFace.

---

## 🔑 PASSO 2: Configurar API Key

### 2.1 Criar conta no CompreFace

1. Acesse `http://localhost:8000`
2. Clique em **"Sign Up"**
3. Preencha:
   - **First Name**: Seu nome
   - **Last Name**: Seu sobrenome
   - **Email**: seu.email@example.com
   - **Password**: Senha123! (ou outra de sua preferência)
4. Clique em **"Sign Up"**
5. Faça login com as credenciais criadas

### 2.2 Criar Aplicação

1. Após login, você verá o dashboard
2. Clique em **"Create"** ou **"+ Application"**
3. Digite um nome: **"Taskly Face Recognition"**
4. Clique em **"Create"**

### 2.3 Criar Serviço de Recognition

1. Dentro da aplicação criada, clique em **"Services"**
2. Clique em **"Add Service"**
3. Escolha **"Recognition Service"**
4. Digite um nome: **"Taskly Recognition"**
5. Clique em **"Save"**

### 2.4 Copiar API Key

1. Na lista de serviços, você verá o serviço **"Taskly Recognition"**
2. Clique em **"API Key"** ou no ícone de chave 🔑
3. **COPIE** a API Key (formato: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)
4. **GUARDE** essa chave, você vai usar no próximo passo

Exemplo de API Key:
```
00000000-0000-0000-0000-000000000002
```

---

## ⚙️ PASSO 3: Configurar Variáveis de Ambiente

### 3.1 Atualizar arquivo .env

Crie ou edite o arquivo `.env` na raiz do projeto:

```bash
# No terminal, no diretório do projeto:
cd /Users/igorguerreiro/Documents/Projects/taskly

# Criar/editar .env
nano .env
# ou use seu editor preferido (VSCode, IntelliJ, etc)
```

### 3.2 Adicionar variáveis do CompreFace

Cole as seguintes variáveis no arquivo `.env`:

```bash
# CompreFace Configuration
COMPREFACE_URL=http://localhost:8000
COMPREFACE_API_KEY=COLE_SUA_API_KEY_AQUI

# Exemplo:
# COMPREFACE_API_KEY=00000000-0000-0000-0000-000000000002
```

**⚠️ IMPORTANTE:** Substitua `COLE_SUA_API_KEY_AQUI` pela API Key que você copiou no passo 2.4.

### 3.3 Verificar arquivo .env completo

Seu arquivo `.env` deve estar assim:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/taskly
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres

# JWT
JWT_SECRET=sua-chave-secreta-super-segura-aqui
JWT_ISSUER=taskly-api
JWT_EXPIRATION_MS=3600000
REFRESH_TOKEN_EXPIRATION_DAYS=30

# Server
SERVER_PORT=7171
ENVIRONMENT=development

# Security
BCRYPT_ROUNDS=12
RATE_LIMIT_MAX_REQUESTS=100
RATE_LIMIT_WINDOW_SECONDS=60
AUTH_RATE_LIMIT_MAX_REQUESTS=5

# CORS
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200

# CompreFace (NOVO)
COMPREFACE_URL=http://localhost:8000
COMPREFACE_API_KEY=00000000-0000-0000-0000-000000000002
```

### 3.4 Salvar e fechar

- Se usando `nano`: pressione `Ctrl+O` (salvar), depois `Ctrl+X` (sair)
- Se usando outro editor: salve normalmente

---

## ✅ PASSO 4: Verificar Configuração

### 4.1 Testar conectividade com CompreFace

Execute este comando para testar a API:

```bash
curl -X GET "http://localhost:8000/api/v1/recognition/subjects" \
  -H "x-api-key: SUA_API_KEY_AQUI"
```

**Resposta esperada:**
```json
{
  "subjects": []
}
```

Se receber isso, significa que está tudo configurado corretamente! ✅

### 4.2 Verificar variáveis de ambiente na aplicação

```bash
# Ver se as variáveis estão sendo carregadas
echo $COMPREFACE_URL
echo $COMPREFACE_API_KEY
```

---

## 🚀 PASSO 5: Rodar a Aplicação

### 5.1 Compilar e executar

```bash
# No diretório do projeto
mvn clean compile exec:java
```

### 5.2 Verificar logs

Procure por essas linhas nos logs:
```
✅ Migrations executadas: 1 (V8)
✅ Hibernate EntityManagerFactory configurado com sucesso
✅ Servidor iniciado com sucesso!
🚀 http://localhost:7171
```

Se aparecer tudo isso, sua aplicação está rodando com validação facial ativada! 🎉

---

## 🧪 PASSO 6: Testar a Validação Facial

### 6.1 Fazer login e obter JWT

```bash
# 1. Registrar usuário (se ainda não tiver)
curl -X POST http://localhost:7171/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com",
    "password": "Senha123!"
  }'

# 2. Fazer login
curl -X POST http://localhost:7171/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com",
    "password": "Senha123!"
  }'

# Copie o accessToken da resposta
```

### 6.2 Registrar rosto (primeira vez)

**Opção 1: Via curl com imagem de teste**

```bash
# Baixar uma imagem de exemplo
curl -o test-face.jpg https://via.placeholder.com/400x400/000000/FFFFFF/?text=Face

# Converter para base64
IMAGE_BASE64=$(base64 test-face.jpg)

# Registrar rosto
curl -X POST http://localhost:7171/api/v1/face/register \
  -H "Authorization: Bearer SEU_JWT_TOKEN_AQUI" \
  -H "Content-Type: application/json" \
  -d "{\"image\": \"data:image/jpeg;base64,$IMAGE_BASE64\"}"
```

**Opção 2: Usar Postman ou Insomnia**

1. Abra Postman
2. Crie uma requisição POST para `http://localhost:7171/api/v1/face/register`
3. Headers:
   - `Authorization: Bearer SEU_JWT_TOKEN`
   - `Content-Type: application/json`
4. Body (raw JSON):
```json
{
  "image": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
}
```

### 6.3 Verificar rosto e obter token de validação

```bash
curl -X POST http://localhost:7171/api/v1/face/verify \
  -H "Authorization: Bearer SEU_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"image\": \"data:image/jpeg;base64,$IMAGE_BASE64\"}"
```

**Resposta esperada:**
```json
{
  "success": true,
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "expiresAt": "2026-04-01T01:05:00",
  "message": "Rosto verificado com sucesso"
}
```

### 6.4 Criar todo com validação facial

```bash
curl -X POST http://localhost:7171/api/v1/todos \
  -H "Authorization: Bearer SEU_JWT_TOKEN" \
  -H "X-Face-Token: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Minha primeira tarefa com validação facial!"
  }'
```

**Sucesso!** Se receber status 201, sua validação facial está funcionando perfeitamente! 🎉

---

## 🛠️ Comandos Úteis do Docker

### Parar CompreFace
```bash
docker-compose stop
```

### Iniciar CompreFace novamente
```bash
docker-compose start
```

### Parar e remover containers (mas manter dados)
```bash
docker-compose down
```

### Parar e remover TUDO (incluindo dados)
```bash
docker-compose down -v
```

### Ver uso de recursos
```bash
docker stats
```

### Ver logs em tempo real
```bash
docker-compose logs -f
```

---

## ⚠️ Troubleshooting

### Problema: Porta 8000 já está em uso

```bash
# Verificar o que está usando a porta
lsof -i :8000

# Matar o processo
kill -9 PID
```

Ou edite o `docker-compose.yml` para usar outra porta:
```yaml
ports:
  - "8001:80"  # Mudar de 8000 para 8001
```

Não esqueça de atualizar o `.env`:
```bash
COMPREFACE_URL=http://localhost:8001
```

### Problema: CompreFace não inicia

```bash
# Ver logs detalhados
docker-compose logs compreface-api

# Reiniciar com rebuild
docker-compose down
docker-compose up -d --build
```

### Problema: Erro de memória

Se o CompreFace não tiver memória suficiente:

1. Edite `docker-compose.yml`
2. Reduza o valor de `-Xmx8g` para `-Xmx4g` ou `-Xmx2g`
3. Reinicie: `docker-compose restart`

### Problema: API Key não funciona

1. Verifique se copiou corretamente (sem espaços)
2. Verifique se está usando o serviço **Recognition** (não Detection ou Verification)
3. Recrie o serviço no CompreFace UI

---

## 📚 Recursos Adicionais

- [Documentação oficial CompreFace](https://github.com/exadel-inc/CompreFace)
- [CompreFace API Reference](https://exadel-inc.github.io/CompreFace/)
- [Postman Collection](https://github.com/exadel-inc/CompreFace/tree/master/docs/Postman-collections)

---

✅ **Setup Completo!** Agora você está pronto para usar validação facial na sua aplicação Taskly!
