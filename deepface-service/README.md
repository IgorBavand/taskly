# DeepFace Service

Microserviço de reconhecimento facial usando DeepFace, construído com FastAPI.

## Por que DeepFace?

Este serviço substitui o CompreFace original pelos seguintes motivos:
- **Compatibilidade Apple Silicon (ARM64)**: DeepFace roda nativamente em processadores ARM64
- **Performance**: Mais rápido e eficiente em recursos
- **Privacidade**: Armazena apenas embeddings (vetores), não as imagens faciais
- **Open Source**: Sem restrições comerciais
- **Simplicidade**: API REST minimalista e fácil de integrar

## Arquitetura

```
┌─────────────────┐
│ Taskly Backend  │
│  (Kotlin/Java)  │
└────────┬────────┘
         │ HTTP REST
         ▼
┌─────────────────┐
│ DeepFace Service│
│   (FastAPI)     │
└────────┬────────┘
         │
    ┌────┴─────┬──────────┬──────────┐
    │          │          │          │
    ▼          ▼          ▼          ▼
DeepFace  TensorFlow  OpenCV   Facenet512
```

## Funcionalidades

### 1. Extração de Embedding
Extrai um vetor numérico (embedding) que representa características únicas do rosto.

**Endpoint:** `POST /face/embedding`

**Request:**
```json
{
  "image_base64": "data:image/jpeg;base64,/9j/4AAQ..."
}
```

**Response:**
```json
{
  "embedding": [0.123, -0.456, 0.789, ...],  // 512 dimensões
  "model": "Facenet512",
  "dimensions": 512
}
```

### 2. Verificação Facial
Verifica se duas faces correspondem à mesma pessoa.

**Endpoint:** `POST /face/verify`

**Opção 1 - Comparar duas imagens:**
```json
{
  "img1_base64": "data:image/jpeg;base64,/9j/4AAQ...",
  "img2_base64": "data:image/jpeg;base64,/9j/4AAQ..."
}
```

**Opção 2 - Comparar imagem com embedding armazenado:**
```json
{
  "img1_base64": "data:image/jpeg;base64,/9j/4AAQ...",
  "embedding": [0.123, -0.456, 0.789, ...]
}
```

**Response:**
```json
{
  "verified": true,
  "distance": 0.35,
  "similarity": 82.5,
  "threshold": 0.4,
  "model": "Facenet512"
}
```

### 3. Health Check
Verifica se o serviço está operacional.

**Endpoint:** `GET /health`

**Response:**
```json
{
  "status": "healthy",
  "model": "Facenet512",
  "backend": "opencv"
}
```

## Modelos Disponíveis

| Modelo | Dimensões | Precisão | Velocidade |
|--------|-----------|----------|------------|
| **Facenet512** (padrão) | 512 | Alta | Média |
| Facenet | 128 | Média | Rápida |
| VGG-Face | 4096 | Alta | Lenta |
| ArcFace | 512 | Muito alta | Média |

### Como trocar o modelo

Edite `.env` ou `docker-compose.yml`:
```bash
DEEPFACE_MODEL_NAME=ArcFace  # ou Facenet, VGG-Face
```

## Métricas de Distância

| Métrica | Descrição | Threshold padrão |
|---------|-----------|------------------|
| **cosine** (padrão) | Similaridade do cosseno | 0.4 |
| euclidean | Distância euclidiana | 23.56 |
| euclidean_l2 | Euclidiana normalizada | 1.13 |

### Como trocar a métrica

```bash
DEEPFACE_DISTANCE_METRIC=euclidean
DEEPFACE_VERIFICATION_THRESHOLD=23.56
```

## Backends de Detecção

| Backend | Velocidade | Precisão | Uso de Memória |
|---------|-----------|----------|----------------|
| **opencv** (padrão) | Muito rápida | Boa | Baixo |
| ssd | Rápida | Muito boa | Médio |
| mtcnn | Lenta | Excelente | Alto |
| retinaface | Média | Excelente | Médio |

### Como trocar o backend

```bash
DEEPFACE_DETECTOR_BACKEND=mtcnn  # Melhor precisão, mais lento
```

## Configuração

### Variáveis de Ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `DEEPFACE_MODEL_NAME` | Facenet512 | Modelo de reconhecimento |
| `DEEPFACE_DISTANCE_METRIC` | cosine | Métrica de similaridade |
| `DEEPFACE_DETECTOR_BACKEND` | opencv | Backend de detecção facial |
| `DEEPFACE_VERIFICATION_THRESHOLD` | 0.4 | Threshold de verificação |
| `PORT` | 8080 | Porta do serviço |
| `WORKERS` | 1 | Número de workers Uvicorn |

### Ajuste de Threshold

O threshold determina quão rigorosa é a verificação:

- **0.3** - Muito rigoroso (pode rejeitar mesmas pessoas)
- **0.4** - Padrão balanceado (recomendado)
- **0.5** - Mais permissivo (pode aceitar pessoas diferentes)

## Desenvolvimento Local

### Instalação

```bash
cd deepface-service

# Criar ambiente virtual
python -m venv venv
source venv/bin/activate  # Linux/Mac
# ou
venv\Scripts\activate     # Windows

# Instalar dependências
pip install -r requirements.txt
```

### Executar

```bash
# Com uvicorn diretamente
uvicorn app.main:app --reload --port 8080

# Ou com Python
python -m uvicorn app.main:app --reload --port 8080
```

### Testar

```bash
# Health check
curl http://localhost:8080/health

# Extrair embedding (use uma imagem base64)
curl -X POST http://localhost:8080/face/embedding \
  -H "Content-Type: application/json" \
  -d '{"image_base64":"data:image/jpeg;base64,/9j/..."}'
```

## Docker

### Build

```bash
docker build -t taskly-deepface .
```

### Run

```bash
docker run -p 8080:8080 \
  -e DEEPFACE_MODEL_NAME=Facenet512 \
  -e DEEPFACE_THRESHOLD=0.4 \
  taskly-deepface
```

### Docker Compose

O serviço já está configurado no `docker-compose.yml` raiz do projeto:

```bash
# Subir apenas o DeepFace service
docker-compose up deepface-service

# Subir toda a stack
docker-compose up
```

## Performance

### Benchmarks (Apple Silicon M1)

| Operação | Tempo médio | Observações |
|----------|-------------|-------------|
| Extração de embedding | ~200ms | Primeira vez (carrega modelo) |
| Extração subsequente | ~50ms | Com modelo em cache |
| Verificação (2 imagens) | ~100ms | Inclui extração de ambos |
| Verificação (embedding) | ~50ms | Apenas extração de 1 imagem |

### Otimizações

- **Cache de modelo**: O modelo é carregado na inicialização
- **Cleanup automático**: Arquivos temporários são limpos
- **Workers**: Configure mais workers em produção (`WORKERS=4`)

## Troubleshooting

### Erro: "No face detected"

**Causa:** Nenhum rosto encontrado na imagem.

**Solução:**
- Certifique-se de que há um rosto visível
- Aumente a qualidade da imagem
- Tente um backend mais preciso: `DEEPFACE_DETECTOR_BACKEND=mtcnn`

### Erro: "Multiple faces detected"

**Causa:** Mais de um rosto na imagem.

**Solução:**
- Envie imagens com apenas um rosto
- Recorte a imagem antes de enviar

### Performance lenta

**Causa:** Modelo pesado ou backend lento.

**Soluções:**
- Use `DEEPFACE_MODEL_NAME=Facenet` (mais rápido, menos preciso)
- Use `DEEPFACE_DETECTOR_BACKEND=opencv` (mais rápido)
- Aumente workers: `WORKERS=4`

### Erro de memória

**Causa:** Modelo muito grande.

**Soluções:**
- Use modelo menor: `DEEPFACE_MODEL_NAME=Facenet`
- Reduza workers: `WORKERS=1`
- Aumente memória do container

## Segurança

- As imagens são processadas em memória e nunca persistidas
- Arquivos temporários são limpos automaticamente
- Apenas embeddings (vetores) são retornados, nunca as imagens
- CORS configurado para desenvolvimento (remover em produção)

## Privacidade (LGPD/GDPR)

Este serviço foi projetado pensando em privacidade:

1. **Nenhuma imagem é armazenada**: Apenas embeddings (vetores matemáticos)
2. **Embeddings são irreversíveis**: Não é possível reconstruir a face a partir do vetor
3. **Processamento efêmero**: Imagens existem apenas durante a requisição
4. **Cleanup automático**: Arquivos temporários são deletados
5. **Sem logs de imagens**: Apenas metadados são logados

## Licença

Este microserviço usa:
- **DeepFace**: MIT License
- **TensorFlow**: Apache 2.0 License
- **FastAPI**: MIT License

Todos são open source e permitem uso comercial.

## Comparação com CompreFace

| Aspecto | DeepFace Service | CompreFace |
|---------|------------------|------------|
| Compatibilidade ARM64 | ✅ Nativo | ❌ Emulação lenta |
| Tamanho da stack | ~500MB | ~2GB+ |
| Armazenamento | Embeddings (TEXT) | Imagens (BLOB) |
| Inicialização | 10-20s | 5-10 min |
| API | Simples (2 endpoints) | Complexa (UI, Admin, Core) |
| Licença comercial | ✅ MIT | ✅ Apache 2.0 |
| Dependências | Python + TF | Docker (4 containers) |

## Próximos Passos

Possíveis melhorias:
- [ ] Detecção de vivacidade (liveness detection)
- [ ] Suporte a múltiplas faces
- [ ] Cache de embeddings com Redis
- [ ] Busca vetorial com FAISS/Milvus
- [ ] Métricas com Prometheus
- [ ] API Key authentication
