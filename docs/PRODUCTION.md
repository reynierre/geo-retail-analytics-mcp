# Production Deployment Guide

## Prerequisites

- Docker & Docker Compose 3.8+
- 8GB+ RAM (16GB recommended)
- 50GB+ SSD storage
- Network access to POS systems
- Ollama installed (for local LLM)

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        PRODUCTION ENVIRONMENT                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐            │
│  │   Geocom     │────►│    Kafka     │────►│  ClickHouse  │            │
│  │  POS/Listener│     │  (Streaming) │     │   (OLAP)     │            │
│  └──────────────┘     └──────────────┘     └──────┬───────┘            │
│                                                    │                     │
│                                            ┌───────▼───────┐            │
│  ┌──────────────┐     ┌──────────────┐    │  Spring Boot  │            │
│  │   Superset   │◄────│  Prometheus  │◄───│   Backend     │            │
│  │ (Dashboards) │     │  (Metrics)   │    │  (API + LLM)  │            │
│  └──────────────┘     └──────────────┘    └───────┬───────┘            │
│                                                    │                     │
│                                            ┌───────▼───────┐            │
│                                            │    Ollama     │            │
│                                            │ (Local LLM)   │            │
│                                            └───────────────┘            │
│                                                    │                     │
│                                            ┌───────▼───────┐            │
│                                            │   Angular     │            │
│                                            │   Frontend    │            │
│                                            └───────────────┘            │
└─────────────────────────────────────────────────────────────────────────┘
```

## Deployment Steps

### Step 1: Prepare Server

```bash
# Create directories
sudo mkdir -p /data/{clickhouse,postgres,kafka,zookeeper,prometheus,superset,ollama}
sudo mkdir -p /data/clickhouse/backups
sudo mkdir -p /data/zookeeper/{data,log}
sudo mkdir -p /data/superset/{db,cache}

# Set permissions
sudo chown -R 101:101 /data/clickhouse
sudo chown -R 999:999 /data/postgres
```

### Step 2: Configure Environment

```bash
cd geo-retail-analytics-mcp/docker

# Copy and configure production environment
cp .env.prod.example .env.prod

# Edit with your values
nano .env.prod
```

**Required variables:**
```env
CLICKHOUSE_PASSWORD=your_secure_password
POSTGRES_PASSWORD=your_secure_password
SUPERSET_SECRET_KEY=your_very_long_random_secret_key
SUPERSET_DB_PASSWORD=your_secure_password
ANTHROPIC_API_KEY=your_api_key  # Optional, if using Claude
```

### Step 3: Deploy Infrastructure

```bash
# Start all services
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Verify services are running
docker-compose -f docker-compose.prod.yml ps

# Check logs
docker-compose -f docker-compose.prod.yml logs -f
```

### Step 4: Initialize Database

The schema is auto-loaded from `docker-entrypoint-initdb.d`.

```bash
# Verify tables
docker exec -it geo-retail-clickhouse-prod clickhouse-client \
  --query="SHOW TABLES FROM geo_retail_analytics"

# Expected output:
# dim_card_brands
# dim_companies
# dim_payment_methods
# dim_products
# dim_promotions
# dim_stores
# dim_taxes
# fact_sale_item_discounts
# fact_sale_items
# fact_sale_payments
# fact_sales
# mv_card_brand_summary
# mv_daily_sales
# mv_hourly_sales
# mv_payment_summary
# mv_product_sales
# mv_promotion_summary
```

### Step 5: Load Dimension Tables

```bash
# Connect to ClickHouse
docker exec -it geo-retail-clickhouse-prod clickhouse-client

# Load company
INSERT INTO dim_companies (company_id, company_name, tax_number)
VALUES (1, 'Your Company Name', '212345678901');

# Load stores (example)
INSERT INTO dim_stores (store_id, company_id, store_code, store_name, region, city, store_format)
VALUES
  (1, 1, '001', 'Tienda Centro', 'Montevideo', 'Montevideo', 'super'),
  (2, 1, '002', 'Tienda Norte', 'Montevideo', 'Montevideo', 'express'),
  (3, 1, '003', 'Tienda Costa', 'Maldonado', 'Punta del Este', 'hiper');

# Load payment methods
INSERT INTO dim_payment_methods (payment_method_id, payment_method_name, payment_type)
VALUES
  ('EF', 'Efectivo', 'CASH'),
  ('TC', 'Tarjeta Crédito', 'CARD'),
  ('TD', 'Tarjeta Débito', 'CARD'),
  ('TR', 'Transferencia', 'OTHER');
```

### Step 6: Configure Kafka Topic

```bash
docker exec -it geo-retail-kafka-prod kafka-topics --create \
  --topic sales-events \
  --bootstrap-server localhost:9092 \
  --partitions 6 \
  --replication-factor 1 \
  --config retention.ms=604800000

# Verify
docker exec -it geo-retail-kafka-prod kafka-topics --describe \
  --topic sales-events \
  --bootstrap-server localhost:9092
```

### Step 7: Configure Ollama

```bash
# Download model
docker exec -it geo-retail-ollama-prod ollama pull llama3.1:8b

# Keep model loaded
docker exec -it geo-retail-ollama-prod ollama run llama3.1:8b --keepalive 24h

# Verify
curl http://localhost:11434/api/tags
```

### Step 8: Deploy Backend

```bash
cd backend

# Build
./gradlew clean build -x test

# Build Docker image
docker build -t geo-retail-backend:latest .

# Run
docker run -d --name geo-retail-backend \
  --network geo-retail-network-prod \
  -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
  -e CLICKHOUSE_HOST=clickhouse \
  -e OLLAMA_BASE_URL=http://ollama:11434 \
  geo-retail-backend:latest

# Verify
curl http://localhost:8081/actuator/health
```

### Step 9: Deploy Frontend

```bash
cd frontend

# Build
npm run build --prod

# Serve with nginx or deploy to CDN
docker run -d --name geo-retail-frontend \
  -p 80:80 \
  -v $(pwd)/dist/geo-retail-frontend:/usr/share/nginx/html:ro \
  nginx:alpine
```

## Backup Strategy

### Automated Daily Backup

Add to crontab:
```bash
# Edit crontab
crontab -e

# Add backup job (runs at 2 AM)
0 2 * * * /opt/scripts/backup-clickhouse.sh >> /var/log/backup.log 2>&1
```

**backup-clickhouse.sh:**
```bash
#!/bin/bash
set -e

DATE=$(date +%Y%m%d)
BACKUP_NAME="backup_${DATE}"
BACKUP_DIR="/data/clickhouse/backups"
S3_BUCKET="${BACKUP_S3_BUCKET}"

echo "$(date): Starting backup ${BACKUP_NAME}"

# Create backup
docker exec geo-retail-clickhouse-prod clickhouse-client \
  --query="BACKUP DATABASE geo_retail_analytics TO Disk('backups', '${BACKUP_NAME}.zip')"

# Upload to S3 (if configured)
if [ -n "$S3_BUCKET" ]; then
  aws s3 cp ${BACKUP_DIR}/${BACKUP_NAME}.zip s3://${S3_BUCKET}/clickhouse/
  echo "$(date): Backup uploaded to S3"
fi

# Cleanup old local backups (keep 7 days)
find ${BACKUP_DIR} -name "backup_*.zip" -mtime +7 -delete

echo "$(date): Backup completed"
```

### Restore from Backup

```bash
# List available backups
docker exec geo-retail-clickhouse-prod ls -la /backups/

# Restore specific backup
docker exec geo-retail-clickhouse-prod clickhouse-client \
  --query="RESTORE DATABASE geo_retail_analytics FROM Disk('backups', 'backup_20250103.zip')"
```

## Monitoring

### Superset Dashboards

1. Open http://your-server:8088
2. Login: admin / (your password)
3. Add ClickHouse database connection:
   - Go to Settings -> Database Connections
   - Click "+ Database"
   - Select ClickHouse
   - SQLAlchemy URI: `clickhousedb://default:password@clickhouse:8123/geo_retail_analytics`
4. Create datasets from materialized views:
   - mv_daily_sales
   - mv_hourly_sales
   - mv_payment_summary
   - mv_product_sales
5. Build dashboards for:
   - Sales Overview
   - Payment Analysis
   - Product Performance
   - Store Comparison

### Key Alerts to Configure

| Alert | Condition | Severity |
|-------|-----------|----------|
| ClickHouse Down | up{job="clickhouse"} == 0 | Critical |
| Kafka Consumer Lag | lag > 10000 | Warning |
| API Error Rate | 5xx rate > 5% | Critical |
| Disk Usage | usage > 80% | Warning |
| Memory Usage | usage > 90% | Warning |

### Health Check Script

```bash
#!/bin/bash
# health-check.sh

check_service() {
  local name=$1
  local url=$2
  local expected=$3

  response=$(curl -s -o /dev/null -w "%{http_code}" $url)
  if [ "$response" == "$expected" ]; then
    echo "✓ $name: OK"
  else
    echo "✗ $name: FAILED (expected $expected, got $response)"
    exit 1
  fi
}

echo "Health Check - $(date)"
echo "========================"

check_service "ClickHouse" "http://localhost:8123/ping" "200"
check_service "Prometheus" "http://localhost:9090/-/healthy" "200"
check_service "Superset" "http://localhost:8088/health" "200"
check_service "Backend" "http://localhost:8081/actuator/health" "200"
check_service "Ollama" "http://localhost:11434/api/tags" "200"

echo "========================"
echo "All services healthy!"
```

## Scaling

### Horizontal Scaling

- **Kafka**: Add more partitions for higher throughput
- **ClickHouse**: Add replicas for read scaling
- **Backend**: Use load balancer for multiple instances

### Vertical Scaling

Modify resource limits in `docker-compose.prod.yml`:

```yaml
deploy:
  resources:
    limits:
      cpus: '8'
      memory: 16G
    reservations:
      cpus: '4'
      memory: 8G
```

## Troubleshooting

### Common Issues

1. **ClickHouse connection refused**
   ```bash
   docker logs geo-retail-clickhouse-prod | tail -50
   docker exec geo-retail-clickhouse-prod clickhouse-client --query="SELECT 1"
   ```

2. **Kafka consumer lag increasing**
   ```bash
   docker exec geo-retail-kafka-prod kafka-consumer-groups \
     --describe --group geo-retail-analytics --bootstrap-server localhost:9092
   ```

3. **Ollama slow responses**
   ```bash
   # Check if model is loaded
   curl http://localhost:11434/api/tags

   # Pre-load model
   curl http://localhost:11434/api/generate -d '{"model":"llama3.1:8b","keep_alive":"24h"}'
   ```

4. **Out of disk space**
   ```bash
   docker system df
   docker system prune -a
   ```

## Security Checklist

- [ ] Change all default passwords
- [ ] Enable TLS for all services
- [ ] Configure firewall rules
- [ ] Set up VPN for remote access
- [ ] Enable audit logging
- [ ] Regular security updates
- [ ] Backup encryption

## References

- [ClickHouse Operations](https://clickhouse.com/docs/en/operations/)
- [Kafka Operations](https://kafka.apache.org/documentation/#operations)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/)
- [Apache Superset Documentation](https://superset.apache.org/docs/)
- [Ollama Documentation](https://ollama.com/docs)
