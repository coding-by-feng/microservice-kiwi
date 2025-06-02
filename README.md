# Kiwi Microservice Platform

A comprehensive microservice platform for vocabulary learning and management, featuring AI-powered content processing, web crawling, and distributed storage capabilities.

## 🚀 Quick Start

### Prerequisites

- **macOS**: Docker Desktop, Git, Maven
- **Raspberry Pi/Linux**: Docker, Docker Compose, Java 17, Maven, Git
- At least 8GB RAM recommended
- 20GB free disk space

### One-Command Setup

**For macOS:**
```bash
curl -fsSL https://raw.githubusercontent.com/coding-by-feng/microservice-kiwi/master/kiwi-deploy/set_up_mac.sh | bash
```

**For Raspberry Pi/Linux:**
```bash
curl -fsSL https://raw.githubusercontent.com/coding-by-feng/microservice-kiwi/master/kiwi-deploy/set_up.sh | sudo bash
```

## 📋 Platform Support

| Platform | Setup Script | Container Platform | Notes |
|----------|--------------|-------------------|-------|
| macOS (Intel/Apple Silicon) | `set_up_mac.sh` | Docker Desktop | Full support |
| Raspberry Pi OS | `set_up.sh` | Docker + Podman | ARM64 optimized |
| Ubuntu/Debian | `set_up.sh` | Docker | Full support |

## 🏗️ Architecture Overview

### Core Services
- **Eureka** - Service Discovery (Port: 8762)
- **Config Server** - Configuration Management (Port: 7771)
- **Gateway** - API Gateway
- **Auth Service** - Authentication & Authorization
- **UPMS** - User Permission Management System

### Business Services
- **Word Service** - Vocabulary management and processing
- **AI Service** - AI-powered content analysis and generation
- **Crawler Service** - Web content extraction and processing

### Infrastructure
- **MySQL** - Primary database (Port: 3306)
- **Redis** - Cache and session storage (Port: 6379)
- **RabbitMQ** - Message queue (Port: 5672, Management: 15672)
- **Elasticsearch** - Search engine (Port: 9200/9300)
- **FastDFS** - Distributed file storage (Port: 22122/23000)
- **Nginx** - Web server and UI hosting (Port: 80)

## 🛠️ Installation Guide

### Automatic Setup

The setup scripts handle everything automatically:

1. **System dependencies installation**
2. **Docker and container setup**
3. **Database initialization**
4. **Service configuration**
5. **Environment variable setup**

### Manual Setup Steps

If you prefer manual installation:

#### 1. Install Dependencies

**macOS:**
```bash
# Install Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install required packages
brew install git maven docker
```

**Linux/Raspberry Pi:**
```bash
# Update system
sudo apt update

# Install dependencies
sudo apt install -y docker.io docker-compose python3 python3-pip openjdk-17-jdk maven git

# Add user to docker group
sudo usermod -aG docker $USER
```

#### 2. Clone Repository
```bash
git clone https://github.com/coding-by-feng/microservice-kiwi.git
cd microservice-kiwi
```

#### 3. Environment Configuration

Create a `.env` file or set environment variables:
```bash
export KIWI_ENC_PASSWORD="your_encryption_password"
export GROK_API_KEY="your_grok_api_key"
export DB_IP="127.0.0.1"
export MYSQL_ROOT_PASSWORD="your_mysql_password"
export REDIS_PASSWORD="your_redis_password"
export FASTDFS_HOSTNAME="fastdfs.fengorz.me"
export ES_ROOT_PASSWORD="your_elasticsearch_password"
export ES_USER_NAME="kiwi_user"
export ES_USER_PASSWORD="your_es_user_password"
```

#### 4. Run Setup
```bash
# For macOS
./kiwi-deploy/set_up_mac.sh

# For Linux/Raspberry Pi
sudo ./kiwi-deploy/set_up.sh
```

## 🎮 Usage

### Available Commands

After setup, you'll have convenient shortcuts in your home directory:

```bash
# Deploy all services
~/easy-deploy

# Stop all services
~/easy-stop

# Deploy UI only
~/easy-deploy-ui

# Check container status
~/easy-check

# Re-run setup
~/easy-setup

# Clean setup files
~/easy-clean-setup
```

### Deployment Options

The main deployment script supports various modes:

```bash
# Full deployment (default)
sudo -E ~/easy-deploy

# Skip git operations
sudo -E ~/easy-deploy -mode=sg

# Skip Maven build
sudo -E ~/easy-deploy -mode=sm

# Skip Docker build
sudo -E ~/easy-deploy -mode=sbd

# Skip all (containers only)
sudo -E ~/easy-deploy -mode=sa

# Enable auto-restart monitoring
sudo -E ~/easy-deploy -c
```

### Container Management

**Check status of all containers:**
```bash
~/easy-check
```

**Manual container operations:**
```bash
# View all containers
docker ps -a

# Check specific service logs
docker logs kiwi-mysql
docker logs kiwi-eureka

# Restart a service
docker restart kiwi-mysql

# Stop/start individual services
docker stop kiwi-crawler
docker start kiwi-crawler
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `KIWI_ENC_PASSWORD` | Encryption password for sensitive data | Required |
| `GROK_API_KEY` | API key for Grok AI services | Required |
| `DB_IP` | Database server IP | 127.0.0.1 |
| `MYSQL_ROOT_PASSWORD` | MySQL root password | Required |
| `REDIS_PASSWORD` | Redis authentication password | Required |
| `FASTDFS_HOSTNAME` | FastDFS server hostname | Required |
| `ES_ROOT_PASSWORD` | Elasticsearch root password | Required |
| `ES_USER_NAME` | Additional Elasticsearch user | Required |
| `ES_USER_PASSWORD` | Additional Elasticsearch user password | Required |

### Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Nginx/UI | 80 | Web interface |
| MySQL | 3306 | Database |
| Redis | 6379 | Cache |
| RabbitMQ | 5672 | Message queue |
| RabbitMQ Management | 15672 | Web management |
| Eureka | 8762 | Service discovery |
| Config Server | 7771 | Configuration |
| Elasticsearch | 9200, 9300 | Search engine |
| FastDFS Tracker | 22122 | File storage tracker |
| FastDFS Storage | 23000 | File storage |

### Hosts Configuration

The setup automatically adds these entries to `/etc/hosts`:
```
127.0.0.1    fastdfs.fengorz.me
127.0.0.1    kiwi-microservice-local
127.0.0.1    kiwi-microservice
127.0.0.1    kiwi-ui
127.0.0.1    kiwi-eureka
127.0.0.1    kiwi-redis
127.0.0.1    kiwi-rabbitmq
127.0.0.1    kiwi-db
127.0.0.1    kiwi-es
127.0.0.1    kiwi-config
127.0.0.1    kiwi-auth
127.0.0.1    kiwi-upms
127.0.0.1    kiwi-gate
127.0.0.1    kiwi-ai
127.0.0.1    kiwi-crawler
```

## 🐳 Docker Configuration

### Images Used

- **MySQL**: `mysql:latest`
- **Redis**: `redis:latest`
- **RabbitMQ**: `rabbitmq:management`
- **Elasticsearch**: `elasticsearch:7.17.9`
- **Nginx**: `nginx:latest`
- **FastDFS**: `delron/fastdfs` (macOS), `fyclinux/fastdfs-arm64:6.04` (Raspberry Pi)

### Volume Mounts

```yaml
# MySQL
~/docker/mysql:/mysql_tmp

# RabbitMQ
~/docker/rabbitmq:/tmp

# FastDFS
~/storage_data:/fastdfs/storage/data
~/store_path:/fastdfs/store_path

# Nginx/UI
~/docker/ui/dist:/usr/share/nginx/html

# Application logs
~/docker/kiwi/[service]/logs:/logs
```

## 🔍 Troubleshooting

### Common Issues

**1. Port 80 in use:**
```bash
# Check what's using port 80
sudo lsof -i :80

# Kill the process
sudo kill -9 <PID>

# Or use the built-in port management
~/easy-deploy-ui -mode=kp -port=80
```

**2. Docker permission issues:**
```bash
# Add user to docker group
sudo usermod -aG docker $USER

# Apply group changes
newgrp docker

# Or restart your terminal
```

**3. Container startup failures:**
```bash
# Check container logs
docker logs <container-name>

# Check system resources
docker system df
docker stats

# Clean up resources if needed
docker system prune -a --volumes
```

**4. Database connection issues:**
```bash
# Test MySQL connection
docker exec kiwi-mysql mysql -u root -p -e "SHOW DATABASES;"

# Reset MySQL container
docker stop kiwi-mysql
docker rm kiwi-mysql
# Re-run setup to recreate
```

### Log Locations

- **Application logs**: `~/docker/kiwi/[service]/logs/`
- **Setup progress**: `./.kiwi_setup_progress`
- **Configuration**: `./.kiwi_setup_config`
- **Auto-check logs**: `~/autoCheck.log`

### Health Checks

**Check all services:**
```bash
~/easy-check
```

**Manual health verification:**
```bash
# Eureka
curl http://localhost:8762/health

# Config Server
curl http://localhost:7771/health

# Elasticsearch
curl http://localhost:9200/_cluster/health

# RabbitMQ
curl http://localhost:15672

# UI
curl http://localhost:80
```

## 🧹 Cleanup

### Partial Cleanup
```bash
# Clean setup files only
~/easy-clean-setup

# Stop all services
~/easy-stop
```

### Complete Cleanup

**macOS:**
```bash
./kiwi-deploy/clean_all_resource_mac.sh
```

**Linux/Raspberry Pi:**
```bash
# Remove containers and images
docker stop $(docker ps -aq)
docker rm $(docker ps -aq)
docker rmi $(docker images -q)

# Remove volumes
docker volume prune -f

# Remove directories
rm -rf ~/microservice-kiwi ~/docker ~/storage_data ~/store_path ~/tracker_data

# Remove shortcuts
rm -f ~/easy-* ~/deployKiwi.sh

# Clean environment variables from shell profile
```

## 🔄 Updates

### Update Application
```bash
cd ~/microservice-kiwi
git pull
sudo -E ~/easy-deploy
```

### Update UI Only
```bash
# Place new dist.zip in home directory
~/easy-deploy-ui
```

### Selective Updates
```bash
# Update without rebuilding everything
sudo -E ~/easy-deploy -mode=sg  # Skip git
sudo -E ~/easy-deploy -mode=sm  # Skip Maven
sudo -E ~/easy-deploy -mode=sbd # Skip Docker build
```

## 📊 Monitoring

### Auto-Check Service

Enable automatic service monitoring:
```bash
sudo -E ~/easy-deploy -c
```

This starts a background service that:
- Monitors the crawler service health
- Automatically restarts failed services
- Logs activity to `~/autoCheck.log`

### Manual Monitoring
```bash
# Check container status
docker ps

# Monitor resource usage
docker stats

# View service logs
docker logs -f kiwi-crawler

# Check application logs
tail -f ~/docker/kiwi/crawler/logs/application.log
```

## 🔗 Web Interfaces

After successful deployment:

- **Kiwi UI**: http://localhost:80
- **RabbitMQ Management**: http://localhost:15672 (username/password)
- **Elasticsearch**: http://localhost:9200 (root/your_password)
- **Eureka Dashboard**: http://localhost:8762

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test with the deployment scripts
5. Submit a pull request

## 📝 License

This project is licensed under the terms specified in the main repository.

## 🆘 Support

For issues and questions:
1. Check the troubleshooting section above
2. Review container logs: `docker logs <container-name>`
3. Check the setup logs and configuration files
4. Open an issue in the GitHub repository

---

**Note**: This platform includes AI-powered features and web crawling capabilities. Ensure you have appropriate API keys and respect robots.txt and rate limiting when using these features.