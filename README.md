# 🛒 Geo Retail Analytics MCP

Sistema de consultas conversacionales para retail usando LLM + MCP Tools.

## ¿Qué es?

Un chat donde puedes preguntar en lenguaje natural sobre datos de ventas:

```
Usuario: "¿Cuánto vendió el local 001 en enero?"
Sistema: "El local 001 vendió $45.678.901 en enero 2024, con 12.345 tickets..."
```

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          PRODUCCIÓN                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Geocom POS ──► Kafka ──► Consumer ──► ClickHouse                       │
│                                              │                           │
│                                       ┌──────▼──────┐                   │
│  Superset ◄── Prometheus ◄── Spring Boot Backend ──► Ollama            │
│                                       └──────┬──────┘                   │
│                                              │                           │
│                                       Angular Frontend                   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Desarrollo

```bash
# 1. Clonar
git clone <repo>
cd geo-retail-analytics-mcp

# 2. Configurar .env
cd docker
cp .env.example .env

# 3. Infraestructura (ClickHouse, Kafka, Prometheus, Superset)
docker-compose -f docker-compose.dev.yml up -d

# 4. Verificar servicios
curl http://localhost:8123/ping          # ClickHouse
curl http://localhost:9090/-/healthy     # Prometheus
curl http://localhost:8088/health        # Superset

# 5. Configurar Grok API Key (gratuito para desarrollo)
# Obtener en: https://console.x.ai
export XAI_API_KEY=your_api_key

# 6. Backend (Gradle)
cd ../backend
./gradlew bootRun --args='--spring.profiles.active=dev'

# 7. Frontend
cd ../frontend
npm install && ng serve

# 8. Abrir http://localhost:4200
```

### Producción

```bash
# 1. Preparar servidor
sudo mkdir -p /data/{clickhouse,postgres,kafka,zookeeper,prometheus,superset,ollama}
sudo mkdir -p /data/superset/{db,cache}

# 2. Configurar
cd docker
cp .env.prod.example .env.prod
nano .env.prod  # Configurar con valores reales

# 3. Deploy
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 4. Verificar
docker-compose -f docker-compose.prod.yml ps
```

## 📊 URLs de Servicios

| Servicio | Dev URL | Descripción |
|----------|---------|-------------|
| Frontend | http://localhost:4200 | Chat UI |
| Backend | http://localhost:8081 | API + LLM |
| ClickHouse | http://localhost:8123 | OLAP Database |
| Kafka UI | http://localhost:8080 | Event streaming UI |
| Superset | http://localhost:8088 | BI & Dashboards (admin/admin) |
| Prometheus | http://localhost:9090 | Metrics |
| PostgreSQL | localhost:5434 | Chat history |

## 📁 Estructura

```
geo-retail-analytics-mcp/
├── CLAUDE.md              # Contexto para Claude Code
├── .claude/
│   ├── agents/            # Agentes especializados
│   │   ├── angular-expert.md
│   │   ├── spring-boot-expert.md
│   │   ├── clickhouse-expert.md
│   │   ├── llm-integration-expert.md
│   │   └── devops-expert.md       # NEW
│   └── skills/            # Skills de conocimiento
│       ├── angular21/
│       ├── spring-ai-mcp/
│       ├── clickhouse/
│       ├── ollama-integration/
│       ├── retail-domain/
│       ├── kafka-integration/     # NEW
│       └── monitoring/            # NEW
├── frontend/              # Angular 21
├── backend/               # Spring Boot + DDD
├── database/              # Scripts ClickHouse
├── docker/                # Docker Compose
│   ├── docker-compose.dev.yml
│   ├── docker-compose.prod.yml    # NEW
│   ├── prometheus/                # NEW
│   └── superset/                  # Superset config (Apache 2.0)
└── docs/
    ├── ARCHITECTURE.md
    ├── PRODUCTION.md              # NEW
    └── DATA-INGESTION.md          # NEW
```

## 🛠️ Stack

| Componente | Tecnología |
|------------|------------|
| Frontend | Angular 21, Tailwind CSS |
| Backend | Spring Boot 3.4, Spring AI, Gradle 8.11 |
| Arquitectura | DDD (Domain-Driven Design) + Clean Code |
| LLM (dev/MVP) | Grok (xAI) - Gratuito |
| LLM (prod) | Ollama + Llama 3.1 (Local) |
| Database | ClickHouse |
| Streaming | Apache Kafka |
| Monitoring | Prometheus + Superset (Apache 2.0) |
| Chat History | PostgreSQL |

## 🔄 Data Ingestion

El sistema soporta ingesta de datos desde Geocom POS:

```
Geocom POS ──► Listener ──► Kafka ──► ClickHouse
```

Ver [docs/DATA-INGESTION.md](docs/DATA-INGESTION.md) para detalles.

## 📈 Monitoreo

- **Superset**: BI & Dashboards para ClickHouse con licencia Apache 2.0 (comercial-friendly)
- **Prometheus**: Métricas y alertas
- **Kafka UI**: Visualización de topics y consumer groups

## 🤖 Trabajar con Claude Code

```bash
# Inicia Claude Code en el proyecto
cd geo-retail-analytics-mcp
claude

# Claude leerá CLAUDE.md y tendrá todo el contexto
# Usa los agentes especializados:
> Use the devops-expert agent for infrastructure
> Use the clickhouse-expert agent for queries
> Use the spring-boot-expert agent for backend
```

## 📚 Documentación

- [CLAUDE.md](CLAUDE.md) - Contexto completo del proyecto
- [docs/PRODUCTION.md](docs/PRODUCTION.md) - Guía de producción
- [docs/DATA-INGESTION.md](docs/DATA-INGESTION.md) - Ingesta de datos
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - Arquitectura

## 📝 Licencia

MIT
