# Superset Configuration for Geo Retail Analytics
# Apache 2.0 License - Commercial friendly

import os
from datetime import timedelta

# Security
SECRET_KEY = os.environ.get('SUPERSET_SECRET_KEY', 'your-secret-key-change-in-production')

# Database
SQLALCHEMY_DATABASE_URI = os.environ.get('DATABASE_URL', 'postgresql://superset:superset@superset-db:5432/superset')

# Cache
CACHE_CONFIG = {
    'CACHE_TYPE': 'RedisCache',
    'CACHE_DEFAULT_TIMEOUT': 300,
    'CACHE_KEY_PREFIX': 'superset_',
    'CACHE_REDIS_URL': os.environ.get('REDIS_URL', 'redis://superset-cache:6379/0'),
}

# Feature flags
FEATURE_FLAGS = {
    'ENABLE_TEMPLATE_PROCESSING': True,
    'DASHBOARD_NATIVE_FILTERS': True,
    'DASHBOARD_CROSS_FILTERS': True,
    'DASHBOARD_NATIVE_FILTERS_SET': True,
    'ALERT_REPORTS': True,
}

# SQL Lab
SQL_MAX_ROW = 100000
SQLLAB_TIMEOUT = 300

# Async queries
SQLLAB_ASYNC_TIME_LIMIT_SEC = 300

# Enable ClickHouse
PREFERRED_DATABASES = [
    'clickhouse',
    'postgresql',
]

# Theme
APP_NAME = "Geo Retail Analytics"
APP_ICON = "/static/assets/images/superset-logo-horiz.png"

# Session
PERMANENT_SESSION_LIFETIME = timedelta(hours=24)

# CORS (for embedding)
ENABLE_CORS = True
CORS_OPTIONS = {
    'supports_credentials': True,
    'allow_headers': ['*'],
    'resources': ['*'],
    'origins': ['*'],
}

# Embedding dashboards
PUBLIC_ROLE_LIKE = "Gamma"
GUEST_ROLE_NAME = "Public"
GUEST_TOKEN_JWT_SECRET = SECRET_KEY
GUEST_TOKEN_JWT_ALGO = "HS256"
GUEST_TOKEN_HEADER_NAME = "X-GuestToken"
GUEST_TOKEN_JWT_EXP_SECONDS = 300
