# Kiwi Monolith Deployment

Simple deployment scripts for the Kiwi monolith Spring Boot application.

## Quick Start (Development)

```bash
# 1. Copy environment template
cp .env.dev .env

# 2. Start infrastructure (MySQL, Redis, Elasticsearch)
./infra.sh start

# 3. Build and run the application
./deploy.sh dev
```

## Scripts

| Script | Description |
|--------|-------------|
| `infra.sh` | Manage infrastructure services (MySQL, Redis, ES) |
| `run.sh` | Run the application with environment config |
| `deploy.sh` | Full deployment: build + run |

## Infrastructure Management

```bash
# Start all services
./infra.sh start

# Stop all services
./infra.sh stop

# Check status
./infra.sh status

# View logs
./infra.sh logs              # All services
./infra.sh logs kiwi-mysql   # Specific service

# Clean up (WARNING: removes data)
./infra.sh clean
```

## Running the Application

### Option 1: Quick Run (pre-built JAR)
```bash
./run.sh dev          # Development
./run.sh test         # Test/Staging
./run.sh prod         # Production
```

### Option 2: Build and Run
```bash
./run.sh dev --build
```

### Option 3: Debug Mode
```bash
./run.sh dev --debug  # Enables remote debugging on port 5005
```

### Option 4: Full Deployment
```bash
./deploy.sh dev       # Starts infra + builds + runs
```

## Environment Configuration

Environment files:
- `.env.dev` - Development defaults
- `.env.test` - Test/Staging configuration
- `.env.prod` - Production template

To use:
```bash
# Copy the appropriate template
cp .env.dev .env

# Edit as needed
vim .env
```

### Key Environment Variables

| Variable | Default (Dev) | Description |
|----------|---------------|-------------|
| `DB_HOST` | localhost | MySQL host |
| `DB_PORT` | 3307 | MySQL port |
| `DB_NAME` | kiwi_db | Database name |
| `DB_USERNAME` | root | Database user |
| `DB_PASSWORD` | kiwi123 | Database password |
| `REDIS_HOST` | localhost | Redis host |
| `REDIS_PORT` | 6380 | Redis port |
| `REDIS_PASSWORD` | kiwi123 | Redis password |
| `ES_URIS` | http://localhost:9201 | Elasticsearch URL |
| `ES_USERNAME` | elastic | ES username |
| `ES_PASSWORD` | changeme | ES password |
| `KIWI_ENC_PASSWORD` | coding-by-feng | Jasypt encryption key |

## Service Ports

### Development (localhost)
- Application: `8088`
- MySQL: `3307`
- Redis: `6380`
- Elasticsearch: `9201`

### Production (Docker network)
- Application: `8080`
- MySQL: `3306` (kiwi-mysql)
- Redis: `6379` (kiwi-redis)
- Elasticsearch: `9200` (kiwi-elasticsearch)

## Docker Deployment

### Build Image
```bash
docker build -t kiwi-monolith -f Dockerfile ..
```

### Run Container
```bash
docker run -d \
  --name kiwi-app \
  --network host \
  --env-file .env.prod \
  -e SPRING_PROFILES_ACTIVE=prod \
  kiwi-monolith
```

### Full Stack (docker-compose)
```bash
# Start everything
docker-compose --env-file .env up -d

# Check logs
docker-compose logs -f
```

## Troubleshooting

### MySQL connection refused
```bash
# Check if MySQL is running
docker ps | grep kiwi-mysql

# Check MySQL logs
docker logs kiwi-mysql
```

### Elasticsearch index not found
```bash
# The infra.sh script creates the index automatically
# To manually create:
curl -X PUT "localhost:9201/kiwi_vocabulary" \
  -u elastic:changeme \
  -H "Content-Type: application/json" \
  -d '{"settings": {"number_of_shards": 1}}'
```

### Application won't start
```bash
# Check Java version (17+ required)
java -version

# Verify environment variables
cat .env

# Check if ports are in use
lsof -i :8088
```

### Reset everything
```bash
./infra.sh clean    # Removes all data!
./infra.sh start    # Fresh start
```
