# Checklist de Produção - Taskly API

## 🔐 Segurança

### Variáveis de Ambiente
- [ ] `ENVIRONMENT=production` configurado
- [ ] `JWT_SECRET` com 256+ bits de entropia (gerado com `openssl rand -base64 32`)
- [ ] `DATABASE_PASSWORD` forte e única
- [ ] Arquivo `.env` **NUNCA** commitado no Git
- [ ] Secrets armazenados em vault seguro (AWS Secrets Manager, HashiCorp Vault, etc)

### Banco de Dados
- [ ] SSL/TLS habilitado (`sslmode=require` ou `verify-full`)
- [ ] Usuário do banco com privilégios mínimos (não usar `postgres`)
- [ ] Senha do banco rotacionada periodicamente (90 dias)
- [ ] Backup automático configurado
- [ ] Connection pooling configurado (HikariCP)
- [ ] Índices criados em colunas usadas em WHERE/JOIN

### JWT e Autenticação
- [ ] `JWT_SECRET` único por ambiente (dev, staging, prod)
- [ ] Expiração de JWT configurada adequadamente (1h recomendado)
- [ ] Refresh tokens com expiração (30 dias recomendado)
- [ ] Rotação de refresh tokens habilitada
- [ ] Logs de tentativas de autenticação falhadas

### Rate Limiting
- [ ] Rate limiting global ajustado para carga esperada
- [ ] Rate limiting de autenticação configurado (5 req/min recomendado)
- [ ] Monitoramento de IPs bloqueados
- [ ] Whitelist para IPs confiáveis (se necessário)

### HTTPS
- [ ] Certificado SSL/TLS válido instalado
- [ ] HSTS habilitado (`Strict-Transport-Security` header)
- [ ] Redirecionamento HTTP → HTTPS
- [ ] Certificado renovado automaticamente (Let's Encrypt)

### CORS
- [ ] `ALLOWED_ORIGINS` configurado apenas para domínios confiáveis
- [ ] Wildcard (`*`) **NUNCA** usado em produção
- [ ] Credentials habilitado apenas se necessário

## 🗄️ Banco de Dados

### Migrations
- [ ] Todas as migrations testadas em staging
- [ ] Rollback plan preparado
- [ ] Backup antes de migrations
- [ ] Migrations executadas em janela de manutenção

### Performance
- [ ] Índices criados em:
  - [ ] `users.email` ✅
  - [ ] `todos.user_id` ✅
  - [ ] `refresh_tokens.token` ✅
  - [ ] `refresh_tokens.user_id` ✅
- [ ] Query performance analisada (EXPLAIN)
- [ ] N+1 queries evitadas
- [ ] Connection pool dimensionado (HikariCP)

### Backups
- [ ] Backup automático diário
- [ ] Backup testado (restore)
- [ ] Retenção de backups definida (30 dias)
- [ ] Backups em região diferente

## 📊 Observabilidade

### Logs
- [ ] Logs centralizados (ELK, CloudWatch, Datadog)
- [ ] Log level configurado (`INFO` em prod, `DEBUG` em dev)
- [ ] Audit logs separados
- [ ] Dados sensíveis **NUNCA** logados (senhas, tokens)
- [ ] Request ID tracking implementado ✅

### Monitoramento
- [ ] Healthcheck endpoint monitorado (`/health`)
- [ ] Alertas configurados:
  - [ ] Taxa de erro > 5%
  - [ ] Latência > 1s
  - [ ] CPU > 80%
  - [ ] Memória > 85%
  - [ ] Disco > 90%
- [ ] Dashboard com métricas chave

### Métricas
- [ ] Taxa de sucesso/falha de autenticação
- [ ] Requisições por segundo (RPS)
- [ ] Latência (p50, p95, p99)
- [ ] Erros 4xx e 5xx
- [ ] Conexões ativas do database

## 🚀 Deploy

### Infraestrutura
- [ ] Load balancer configurado
- [ ] Auto-scaling habilitado
- [ ] Health checks configurados
- [ ] Zero-downtime deployment
- [ ] Rollback automático em caso de erro

### Containers (Docker)
- [ ] Imagem Docker otimizada (multi-stage build)
- [ ] Image scan de vulnerabilidades
- [ ] Resource limits definidos (CPU, RAM)
- [ ] Non-root user
- [ ] Secrets via environment variables (não build time)

### CI/CD
- [ ] Testes automatizados
- [ ] Code coverage > 80%
- [ ] Security scan (SAST, dependency check)
- [ ] Deploy automático em staging
- [ ] Aprovação manual para produção

## 🔍 Testes

### Unitários
- [ ] Cobertura > 80%
- [ ] Domain entities testadas
- [ ] Services testados
- [ ] Validações testadas

### Integração
- [ ] Endpoints testados
- [ ] Banco de dados testado (testcontainers)
- [ ] Autenticação testada
- [ ] Rate limiting testado

### E2E
- [ ] Fluxo completo de registro → login → uso
- [ ] Refresh token flow
- [ ] Cenários de erro

### Segurança
- [ ] Penetration testing
- [ ] OWASP Top 10 verificado
- [ ] Dependency vulnerabilities checadas
- [ ] SQL injection testado
- [ ] XSS testado

## 📋 Compliance

### LGPD/GDPR (se aplicável)
- [ ] Política de privacidade
- [ ] Termo de uso
- [ ] Consent management
- [ ] Right to be forgotten (delete account)
- [ ] Data portability
- [ ] Encryption at rest
- [ ] Encryption in transit

### Auditoria
- [ ] Logs de auditoria ✅
- [ ] Rastreabilidade de ações
- [ ] Retention policy definida
- [ ] Acesso aos logs controlado

## 🔧 Configuração

### Environment Variables
```bash
# Required
ENVIRONMENT=production
DATABASE_PASSWORD=<secure>
JWT_SECRET=<256-bit-random>

# Optional (with good defaults)
SERVER_PORT=7070
DATABASE_URL=jdbc:postgresql://prod-db:5432/taskly
JWT_EXPIRATION_MS=3600000
REFRESH_TOKEN_EXPIRATION_DAYS=30
BCRYPT_ROUNDS=12
RATE_LIMIT_MAX_REQUESTS=100
AUTH_RATE_LIMIT_MAX_REQUESTS=5
ALLOWED_ORIGINS=https://taskly.com
```

### Arquivo .env (Produção)
```bash
# NÃO commitar este arquivo!
ENVIRONMENT=production
SERVER_PORT=7070

# Database
DATABASE_URL=jdbc:postgresql://prod-db.example.com:5432/taskly
DATABASE_USER=taskly_app
DATABASE_PASSWORD=<from-vault>

# JWT
JWT_SECRET=<from-vault>
JWT_ISSUER=taskly-api-prod
JWT_EXPIRATION_MS=3600000
REFRESH_TOKEN_EXPIRATION_DAYS=30

# Security
BCRYPT_ROUNDS=12
RATE_LIMIT_MAX_REQUESTS=1000
RATE_LIMIT_WINDOW_SECONDS=60
AUTH_RATE_LIMIT_MAX_REQUESTS=5

# CORS
ALLOWED_ORIGINS=https://taskly.com,https://app.taskly.com
```

## 📈 Performance

### Otimizações
- [ ] Connection pooling configurado ✅
- [ ] Prepared statement caching ✅
- [ ] Índices no banco ✅
- [ ] Response compression (gzip)
- [ ] CDN para assets estáticos
- [ ] Cache layer (Redis) para refresh tokens

### Load Testing
- [ ] Teste de carga executado (JMeter, k6)
- [ ] Baseline de performance estabelecida
- [ ] Limites conhecidos (max RPS, max users)

## 🚨 Disaster Recovery

### Plano de Recuperação
- [ ] RTO (Recovery Time Objective) definido
- [ ] RPO (Recovery Point Objective) definido
- [ ] Runbook de incidentes
- [ ] On-call rotation definida
- [ ] Plano de comunicação

### Backup
- [ ] Backup diário automatizado
- [ ] Teste de restore mensal
- [ ] Backup em região diferente
- [ ] Retenção: 30 dias

## ✅ Launch Day

### Pré-lançamento
- [ ] Smoke tests em produção
- [ ] DNS configurado
- [ ] SSL certificado válido
- [ ] Monitoring ativos
- [ ] Alertas configurados

### Lançamento
- [ ] Deploy em horário de baixo tráfego
- [ ] Equipe de plantão
- [ ] Monitoramento ativo
- [ ] Plano de rollback pronto

### Pós-lançamento
- [ ] Verificar métricas
- [ ] Verificar logs de erro
- [ ] Verificar performance
- [ ] Comunicar sucesso/issues

## 📞 Contatos de Emergência

- **DevOps Lead**: [nome] - [contato]
- **Database Admin**: [nome] - [contato]
- **Security Lead**: [nome] - [contato]
- **Product Owner**: [nome] - [contato]

## 📚 Documentação

- [ ] README.md atualizado ✅
- [ ] API.md com todos os endpoints ✅
- [ ] SECURITY.md com práticas de segurança ✅
- [ ] ARCHITECTURE.md com diagramas ✅
- [ ] Runbook de operação
- [ ] Troubleshooting guide

---

## 🎯 Checklist Resumido

### Crítico (Blocker para produção)
- [ ] JWT_SECRET seguro configurado
- [ ] DATABASE_PASSWORD seguro
- [ ] HTTPS habilitado
- [ ] Backups configurados
- [ ] Logs centralizados
- [ ] Monitoring básico

### Importante (Deve ter)
- [ ] Rate limiting configurado
- [ ] Testes automatizados
- [ ] CI/CD configurado
- [ ] Alertas configurados
- [ ] Load testing executado

### Desejável (Nice to have)
- [ ] Cache layer
- [ ] Auto-scaling
- [ ] Advanced monitoring
- [ ] Chaos engineering

---

**Status**: [ ] Pronto para produção

**Aprovado por**: _______________ Data: ___/___/___
