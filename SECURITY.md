# Guia de Segurança - Taskly API

## Resumo das Implementações de Segurança

Esta aplicação implementa práticas modernas de segurança para APIs REST.

## 🔐 Autenticação e Autorização

### JWT (JSON Web Tokens)
- **Algoritmo**: HMAC256
- **Expiração**: Configurável (padrão: 1 hora)
- **Claims**: userId, email, roles
- **Refresh Tokens**: Tokens de longa duração (30 dias) armazenados no banco

### Estratégia de Tokens
```
1. Login → Access Token (1h) + Refresh Token (30 dias)
2. Access Token expira → Use Refresh Token para gerar novo Access Token
3. Logout → Revoga todos os Refresh Tokens do usuário
```

### RBAC (Role-Based Access Control)
- Roles suportadas: `USER`, `ADMIN`
- Middleware `RequireRole` para proteção de rotas
- Validação de permissões em nível de domínio

## 🔒 Proteção de Senhas

### BCrypt
- **Rounds**: 12 (configurável)
- **Validação**: Mínimo 8 caracteres, maiúscula, minúscula, número
- Hashes nunca são expostos na API

## 🛡️ Proteção contra Ataques

### SQL Injection
- ✅ **Prepared Statements**: Todas as queries usam PreparedStatement
- ✅ **Parameterização**: Nenhuma string concatenada em SQL
- ✅ **ORM-like**: Repositories abstraem acesso ao banco

### Rate Limiting
- **Global**: 100 requisições/minuto por IP
- **Autenticação**: 5 tentativas/minuto por IP
- **Headers**: `X-RateLimit-Limit`, `X-RateLimit-Remaining`

### XSS (Cross-Site Scripting)
- ✅ Headers: `X-Content-Type-Options: nosniff`
- ✅ Validação de entrada
- ✅ Sanitização de dados

### CSRF (Cross-Site Request Forgery)
- ✅ Stateless JWT (sem cookies)
- ✅ CORS configurado

### Clickjacking
- ✅ Header: `X-Frame-Options: DENY`

## 📝 Auditoria e Logs

### Eventos Auditados
```kotlin
AUTH_SUCCESS    - Login bem-sucedido
AUTH_FAILURE    - Tentativa de login falha
USER_REGISTERED - Novo usuário
TOKEN_REFRESHED - Renovação de token
ACCESS_DENIED   - Acesso negado a recurso
SENSITIVE_ACTION - Ações críticas
PASSWORD_CHANGED - Alteração de senha
ACCOUNT_DEACTIVATED - Conta desativada
```

### Formato de Logs
```
2024-01-01 12:00:00 [AUDIT] AUTH_SUCCESS | email=user@example.com | ip=192.168.1.1
```

## 🌐 Headers de Segurança HTTP

```http
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'
Strict-Transport-Security: max-age=31536000; includeSubDomains (produção)
```

## 🔧 Configuração de Produção

### Variáveis Obrigatórias
```bash
ENVIRONMENT=production
JWT_SECRET=<256-bit-random-secret>
DATABASE_PASSWORD=<secure-password>
```

### Checklist de Produção
- [ ] JWT_SECRET com 256+ bits de entropia
- [ ] DATABASE_PASSWORD forte e única
- [ ] SSL/TLS habilitado no PostgreSQL
- [ ] HTTPS obrigatório (HSTS)
- [ ] CORS configurado apenas para domínios confiáveis
- [ ] Rate limiting ajustado para carga esperada
- [ ] Logs centralizados (ELK, CloudWatch, etc)
- [ ] Monitoramento de tentativas de ataque
- [ ] Backup regular de refresh_tokens

## 🚨 Monitoramento de Segurança

### Alertas Recomendados
1. **Múltiplas tentativas de login falhadas** (mesmo IP)
2. **Uso de refresh tokens revogados**
3. **Rate limit atingido repetidamente**
4. **Padrões anormais de acesso**
5. **Exceções não tratadas**

### Métricas de Segurança
- Taxa de sucesso/falha de autenticação
- Número de tokens revogados
- Requisições bloqueadas por rate limiting
- Tentativas de acesso não autorizado

## 🔄 Rotação de Credenciais

### JWT Secret
```bash
# Gerar novo secret seguro
openssl rand -base64 32

# Estratégia de rotação:
# 1. Adicionar novo secret como JWT_SECRET_NEW
# 2. Aceitar ambos por período de transição
# 3. Remover secret antigo
```

### Database Password
```sql
-- Rotação periódica (recomendado: a cada 90 dias)
ALTER USER postgres WITH PASSWORD 'new_secure_password';
```

## 🐛 Relatório de Vulnerabilidades

Se encontrar uma vulnerabilidade de segurança:
1. **NÃO** abra uma issue pública
2. Entre em contato: security@taskly.com
3. Aguarde confirmação antes de divulgar

## 📚 Referências

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
