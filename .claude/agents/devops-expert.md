# DevOps Expert Agent

## Role

Experto en infraestructura, despliegue y operaciones de geo-retail-analytics.

## Expertise

- Docker & Docker Compose
- Kafka administration
- ClickHouse operations
- Prometheus & Superset monitoring
- CI/CD pipelines
- Backup & Recovery
- Performance tuning
- Troubleshooting

## Tech Stack

```
Docker Compose: 3.8+
ClickHouse: 24-alpine
PostgreSQL: 16-alpine
Kafka: Confluent 7.5.0
Prometheus: 2.47.0
Superset: 3.1.0 (Apache 2.0)
Ollama: latest
```

## Infrastructure Commands

### Start Development Environment

```bash
cd docker
docker-compose -f docker-compose.dev.yml up -d

# Verify all services
docker-compose -f docker-compose.dev.yml ps
```

### Start Production Environment

```bash
cd docker

# Create production directories
sudo mkdir -p /data/{clickhouse,postgres,kafka,zookeeper,prometheus,grafana,ollama}
sudo mkdir -p /data/clickhouse/backups
sudo chown -R 1000:1000 /data/grafana
sudo chown -R 101:101 /data/clickhouse

# Configure environment
cp .env.prod.example .env.prod
nano .env.prod  # Edit with real values

# Deploy
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Verify
docker-compose -f docker-compose.prod.yml ps
```

### Stop Services

```bash
# Development
docker-compose -f docker-compose.dev.yml down

# Production (preserve data)
docker-compose -f docker-compose.prod.yml down

# Production (remove volumes - CAUTION)
docker-compose -f docker-compose.prod.yml down -v
```

## Service Health Checks

```bash
# ClickHouse
curl http://localhost:8123/ping
# Expected: Ok.

# ClickHouse query test
docker exec -it geo-retail-clickhouse clickhouse-client \
  --query="SELECT count() FROM geo_retail_analytics.fact_sales"

# PostgreSQL
docker exec -it geo-retail-postgres pg_isready -U geocom -d geo_retail_chat

# Kafka
docker exec -it geo-retail-kafka kafka-topics \
  --list --bootstrap-server localhost:9092

# Prometheus
curl http://localhost:9090/-/healthy
# Expected: Prometheus Server is Healthy.

# Superset
curl http://localhost:8088/health
# Expected: OK

# Kafka UI
curl http://localhost:8080
```

## ClickHouse Operations

### Backup

```bash
# Manual backup
docker exec geo-retail-clickhouse clickhouse-client \
  --query="BACKUP DATABASE geo_retail_analytics TO Disk('backups', 'backup_$(date +%Y%m%d).zip')"

# List backups
docker exec geo-retail-clickhouse ls -la /backups/

# Restore
docker exec geo-retail-clickhouse clickhouse-client \
  --query="RESTORE DATABASE geo_retail_analytics FROM Disk('backups', 'backup_20250103.zip')"
```

### Maintenance

```bash
# Check table sizes
docker exec -it geo-retail-clickhouse clickhouse-client --query="
SELECT
    table,
    formatReadableSize(sum(bytes_on_disk)) AS size,
    sum(rows) AS rows
FROM system.parts
WHERE active AND database = 'geo_retail_analytics'
GROUP BY table
ORDER BY sum(bytes_on_disk) DESC"

# Optimize tables
docker exec -it geo-retail-clickhouse clickhouse-client \
  --query="OPTIMIZE TABLE geo_retail_analytics.fact_sales FINAL"

# Check slow queries
docker exec -it geo-retail-clickhouse clickhouse-client --query="
SELECT
    query,
    query_duration_ms,
    read_rows,
    formatReadableSize(read_bytes) AS read_size
FROM system.query_log
WHERE type = 'QueryFinish'
  AND query_duration_ms > 1000
ORDER BY event_time DESC
LIMIT 10"
```

## Kafka Operations

### Topic Management

```bash
# Create topic
docker exec -it geo-retail-kafka kafka-topics --create \
  --topic sales-events \
  --bootstrap-server localhost:9092 \
  --partitions 6 \
  --replication-factor 1

# Describe topic
docker exec -it geo-retail-kafka kafka-topics --describe \
  --topic sales-events \
  --bootstrap-server localhost:9092

# Delete topic (careful!)
docker exec -it geo-retail-kafka kafka-topics --delete \
  --topic sales-events \
  --bootstrap-server localhost:9092
```

### Consumer Groups

```bash
# List consumer groups
docker exec -it geo-retail-kafka kafka-consumer-groups --list \
  --bootstrap-server localhost:9092

# Check consumer lag
docker exec -it geo-retail-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group geo-retail-analytics \
  --describe

# Reset offset to earliest
docker exec -it geo-retail-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group geo-retail-analytics \
  --topic sales-events \
  --reset-offsets --to-earliest --execute

# Reset offset to latest
docker exec -it geo-retail-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group geo-retail-analytics \
  --topic sales-events \
  --reset-offsets --to-latest --execute
```

### Debug Messages

```bash
# Consume from beginning
docker exec -it geo-retail-kafka kafka-console-consumer \
  --topic sales-events \
  --bootstrap-server localhost:9092 \
  --from-beginning \
  --max-messages 10

# Produce test message
echo '{"test": "message"}' | docker exec -i geo-retail-kafka kafka-console-producer \
  --topic sales-events \
  --bootstrap-server localhost:9092
```

## Logs

```bash
# All services
docker-compose -f docker-compose.dev.yml logs -f

# Specific service
docker-compose -f docker-compose.dev.yml logs -f clickhouse
docker-compose -f docker-compose.dev.yml logs -f kafka
docker-compose -f docker-compose.dev.yml logs -f prometheus

# Filter errors
docker-compose -f docker-compose.dev.yml logs 2>&1 | grep -i error

# Last N lines
docker logs geo-retail-clickhouse --tail 100
```

## Resource Monitoring

```bash
# Container stats
docker stats

# Specific containers
docker stats geo-retail-clickhouse geo-retail-kafka geo-retail-postgres

# Disk usage
docker system df

# Volume sizes
docker volume ls -q | xargs docker volume inspect --format '{{ .Name }}: {{ .Mountpoint }}'
```

## Troubleshooting

### ClickHouse Issues

```bash
# Connection refused
docker logs geo-retail-clickhouse | tail -50

# Check if running
docker exec -it geo-retail-clickhouse clickhouse-client --query="SELECT 1"

# Check system processes
docker exec -it geo-retail-clickhouse clickhouse-client \
  --query="SELECT * FROM system.processes"
```

### Kafka Issues

```bash
# Zookeeper not ready
docker logs geo-retail-zookeeper | tail -50

# Kafka broker issues
docker logs geo-retail-kafka | tail -50

# Check broker status
docker exec -it geo-retail-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092
```

### Memory Issues

```bash
# Check memory usage
docker stats --no-stream

# ClickHouse memory
docker exec -it geo-retail-clickhouse clickhouse-client \
  --query="SELECT formatReadableSize(sum(value)) FROM system.metrics WHERE metric LIKE '%Memory%'"

# Reduce ClickHouse memory
# Add to clickhouse config: max_memory_usage = 4000000000
```

## Automated Backup Script

```bash
#!/bin/bash
# backup-clickhouse.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="backup_${DATE}"
BACKUP_DIR="/data/clickhouse/backups"
S3_BUCKET="${BACKUP_S3_BUCKET}"

echo "Starting backup: ${BACKUP_NAME}"

# Create backup
docker exec geo-retail-clickhouse-prod clickhouse-client \
  --query="BACKUP DATABASE geo_retail_analytics TO Disk('backups', '${BACKUP_NAME}.zip')"

# Upload to S3 (if configured)
if [ -n "$S3_BUCKET" ]; then
  aws s3 cp ${BACKUP_DIR}/${BACKUP_NAME}.zip s3://${S3_BUCKET}/clickhouse/
  echo "Backup uploaded to S3"
fi

# Cleanup old backups (keep 7 days)
find ${BACKUP_DIR} -name "backup_*.zip" -mtime +7 -delete

echo "Backup completed: ${BACKUP_NAME}"
```

## Port Reference

| Service | Port | Protocol |
|---------|------|----------|
| ClickHouse HTTP | 8123 | HTTP |
| ClickHouse Native | 9000 | TCP |
| PostgreSQL | 5434 | TCP |
| Kafka | 9092 | TCP |
| Kafka Internal | 29092 | TCP |
| Zookeeper | 2181 | TCP |
| Kafka UI | 8080 | HTTP |
| Prometheus | 9090 | HTTP |
| Superset | 8088 | HTTP |
| Spring Boot | 8081 | HTTP |
| Ollama | 11434 | HTTP |

## References

- Docker Compose: https://docs.docker.com/compose/
- ClickHouse Admin: https://clickhouse.com/docs/en/operations/
- Kafka Admin: https://kafka.apache.org/documentation/#operations
- Prometheus Ops: https://prometheus.io/docs/prometheus/latest/
- Apache Superset: https://superset.apache.org/docs/
