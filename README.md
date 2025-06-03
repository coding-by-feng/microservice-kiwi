# Kiwi Microservice Deployment Guide

A comprehensive guide for deploying the Kiwi microservice platform using automated Docker-based scripts.

## 🚀 Quick Start

### Prerequisites

- Ubuntu/Debian-based Linux system (Raspberry Pi OS recommended)
- Root/sudo access
- Internet connection
- At least 4GB RAM (8GB+ recommended)
- 20GB+ free disk space

### Initial Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/coding-by-feng/microservice-kiwi.git
   cd microservice-kiwi
   ```

2. **Move setup script to home directory:**
   ```bash
   cp kiwi-deploy/set_up.sh ~/
   cd ~
   ```

3. **Run the setup script:**
   ```bash
   sudo ./set_up.sh
   ```

4. **Follow the interactive prompts to configure:**
    - Environment passwords (KIWI_ENC_PASSWORD, GROK_API_KEY)
    - Database passwords (MySQL, Redis, Elasticsearch)
    - Network configuration (Infrastructure IP, Service IP, FastDFS hostname)
    - User sudo privileges

5. **Reboot or reload environment:**
   ```bash
   # Apply Docker group permissions
   newgrp docker
   # Or log out and back in
   
   # Load environment variables
   source ~/.bashrc
   ```

## 📋 What Gets Installed

The setup script automatically installs and configures:

### Core Infrastructure
- **Docker & Docker Compose** - Container orchestration
- **MySQL 8.0** - Primary database
- **Redis** - Caching and session storage
- **RabbitMQ** - Message queuing
- **Elasticsearch 7.17.9** - Search engine with IK tokenizer
- **FastDFS** - Distributed file system
- **Nginx** - Web server and reverse proxy

### Development Tools
- **OpenJDK 17** - Java runtime
- **Maven** - Build automation
- **Python 3** - Scripting and utilities
- **Git** - Version control

### Kiwi Services
- **Eureka** - Service discovery
- **Config Service** - Configuration management
- **API Gateway** - Request routing
- **Auth Service** - Authentication & authorization
- **UPMS** - User permission management
- **Word Service** - Document processing
- **AI Service** - Machine learning features
- **Crawler Service** - Web scraping

## 🛠️ Command Reference

After successful setup, you'll have access to these convenient commands:

### Core Deployment Commands

#### `~/easy-deploy` - Deploy Kiwi Services
Builds and deploys all microservices with various options.

**Basic Usage:**
```bash
# Full deployment (git pull + maven build + docker deploy)
sudo -E ~/easy-deploy

# Skip git operations (use existing code)
sudo -E ~/easy-deploy -mode=sg

# Skip maven build (use existing JARs)
sudo -E ~/easy-deploy -mode=sm

# Skip Docker building (use existing images)
sudo -E ~/easy-deploy -mode=sbd

# Skip all build steps (deploy existing containers)
sudo -E ~/easy-deploy -mode=sa

# Enable auto-restart monitoring for crawler service
sudo -E ~/easy-deploy -c
```

**Options:**
- `-mode=sg` - Skip git operations
- `-mode=sm` - Skip maven build
- `-mode=sbd` - Skip Docker building
- `-mode=sa` - Skip all build operations
- `-c` - Enable autoCheckService for automatic container monitoring

#### `~/easy-stop` - Stop All Services
Gracefully stops all Kiwi containers and services.

```bash
~/easy-stop
```

**What it does:**
- Stops autoCheckService monitoring
- Stops containers in dependency order
- Removes stopped containers
- Optionally cleans up Docker images and networks

#### `~/easy-deploy-ui` - Deploy UI
Updates the web interface with new frontend builds.

```bash
# Deploy UI from dist.zip file
~/easy-deploy-ui

# Deploy with explicit mode
~/easy-deploy-ui -mode=d

# Kill process on specific port (troubleshooting)
~/easy-deploy-ui -mode=kp -port=80
```

**Prerequisites:**
- Place `dist.zip` file in your home directory before running
- Ensure kiwi-ui container exists and is accessible

#### `~/easy-ui-initialize` - Initialize UI Configuration
Sets up Nginx configuration for microservice proxying.

```bash
~/easy-ui-initialize
```

**What it configures:**
- Nginx reverse proxy settings
- Route mapping for microservices (/auth, /wordBiz, /admin, /ai-biz)
- SSL termination (if certificates available)
- Static file serving

### Monitoring and Debugging

#### `~/easy-check` - Check Container Status
Interactive container monitoring and management tool.

```bash
~/easy-check
```

**Features:**
- View logs for individual containers (static or real-time)
- Check container status and resource usage
- Start/stop individual containers
- Network connectivity testing
- Container shell access
- Port conflict resolution

**Menu Options:**
1. **Container Logs (1-15)** - View logs for specific services
2. **Check All Failed Containers (16)** - Batch log review
3. **Check Connectivity (17)** - Test service connections
4. **Show Resource Usage (18)** - CPU/memory statistics
5. **Show Container Status (19)** - Current state overview
6. **Start Stopped Containers (20)** - Batch container startup
7. **Start All Kiwi Containers (21)** - Full system startup
8. **Start Infrastructure Only (22)** - Database/cache services only
9. **Enter Container Shell (23)** - Debug inside containers
10. **Stop Single Container (24)** - Graceful individual shutdown

### Setup Management

#### `~/easy-setup` - Re-run Setup
Re-executes the setup script with step selection options.

```bash
sudo ~/easy-setup
```

**Setup Modes:**
- **Full Setup (0)** - Complete automated setup
- **Selective Steps (1)** - Choose specific steps to re-initialize
- **Status Review (2)** - View current setup status without changes

**Use Cases:**
- Update configuration parameters
- Retry failed installation steps
- Add new services or components
- Repair corrupted installations

#### `~/easy-clean-setup` - Clean and Re-setup
Interactive cleanup tool for resetting setup progress and configuration.

```bash
~/easy-clean-setup
```

**Cleanup Options:**
1. **Clean Progress Only** - Reset step tracking (keep passwords)
2. **Clean Config Only** - Remove saved passwords (keep progress)
3. **Full Reset** - Remove all setup data
4. **Show Status** - Review current configuration
5. **Backup Files** - Create timestamped backups
6. **Exit** - Leave without changes

## 🔧 Configuration Management

### Environment Variables
Key environment variables are automatically configured:

```bash
# Core Application
KIWI_ENC_PASSWORD="[your-encryption-password]"
GROK_API_KEY="[your-ai-api-key]"

# Database Configuration
DB_IP="[infrastructure-ip]"
MYSQL_ROOT_PASSWORD="[mysql-password]"
REDIS_PASSWORD="[redis-password]"

# Elasticsearch
ES_ROOT_PASSWORD="[es-root-password]"
ES_USER_NAME="[es-username]"
ES_USER_PASSWORD="[es-user-password]"

# File Storage
FASTDFS_HOSTNAME="[fastdfs-hostname]"
FASTDFS_NON_LOCAL_IP="[fastdfs-ip]"

# Network Configuration
INFRASTRUCTURE_IP="[infrastructure-services-ip]"
SERVICE_IP="[microservices-ip]"
```

### Network Architecture
The platform uses separate IP addresses for different service tiers:

**Infrastructure Services** (`INFRASTRUCTURE_IP`):
- kiwi-ui (Web Interface)
- kiwi-redis (Cache)
- kiwi-rabbitmq (Message Queue)
- kiwi-db (Database)
- kiwi-es (Search)

**Microservices** (`SERVICE_IP`):
- kiwi-eureka (Service Discovery)
- kiwi-config (Configuration)
- kiwi-auth (Authentication)
- kiwi-upms (User Management)
- kiwi-gate (API Gateway)
- kiwi-ai (AI Services)
- kiwi-crawler (Web Crawler)

**File Storage** (`FASTDFS_NON_LOCAL_IP`):
- fastdfs.fengorz.me (Distributed File System)

### Ports and Services

| Service | Container | Port | Purpose |
|---------|-----------|------|---------|
| MySQL | kiwi-mysql | 3306 | Database |
| Redis | kiwi-redis | 6379 | Cache |
| RabbitMQ | kiwi-rabbit | 5672, 15672 | Messaging, Management UI |
| Elasticsearch | kiwi-es | 9200, 9300 | Search Engine |
| FastDFS Tracker | tracker | 22122 | File System Coordination |
| FastDFS Storage | storage | 8888 | File Storage |
| Nginx/UI | kiwi-ui | 80 | Web Interface |
| Eureka | kiwi-eureka | 8762 | Service Discovery |
| Config Service | kiwi-config | 7771 | Configuration |
| API Gateway | kiwi-gate | 9991 | Request Routing |

## 🐛 Troubleshooting

### Common Issues

#### Port Conflicts
```bash
# Check what's using a port
sudo netstat -tlnp | grep :80

# Kill process using port (via easy-deploy-ui)
~/easy-deploy-ui -mode=kp -port=80

# Or manually
sudo kill -9 $(lsof -ti :80)
```

#### Container Issues
```bash
# Check container status
docker ps -a

# View container logs
docker logs kiwi-mysql

# Restart problematic container
docker restart kiwi-redis

# Enter container for debugging
docker exec -it kiwi-mysql bash
```

#### Permission Problems
```bash
# Fix Docker permissions
sudo usermod -aG docker $USER
newgrp docker

# Reload environment variables
source ~/.bashrc

# Re-run with proper sudo
sudo -E ~/easy-deploy
```

#### Memory Issues (Raspberry Pi)
```bash
# Check memory usage
free -h

# Stop non-essential services
sudo systemctl stop apache2 
sudo systemctl stop bluetooth

# Reduce Elasticsearch memory
docker exec kiwi-es sh -c 'echo "ES_JAVA_OPTS=-Xms256m -Xmx256m" >> /usr/share/elasticsearch/config/jvm.options'
docker restart kiwi-es
```

### Log Locations
- **Setup logs**: Home directory
- **Container logs**: `docker logs <container-name>`
- **Application logs**: `~/docker/kiwi/<service>/logs/`
- **Nginx logs**: Inside kiwi-ui container at `/var/log/nginx/`

### Recovery Procedures

#### Corrupted Git Repository
```bash
cd ~/microservice-kiwi
rm -rf .git
git init
git remote add origin https://github.com/coding-by-feng/microservice-kiwi.git
git fetch --all
git reset --hard origin/master
```

#### Database Recovery
```bash
# Restore from backup (if kiwi-db.sql exists)
docker exec -i kiwi-mysql mysql -u root -p[password] kiwi_db < ~/docker/mysql/kiwi-db.sql

# Recreate database
docker exec kiwi-mysql mysql -u root -p[password] -e "DROP DATABASE IF EXISTS kiwi_db; CREATE DATABASE kiwi_db;"
```

#### Full System Reset
```bash
# Stop all services
~/easy-stop

# Clean setup completely
~/easy-clean-setup
# Choose option 3 (Full Reset)

# Remove all containers and images
docker system prune -a -f

# Re-run setup
sudo ~/easy-setup
```

## 📚 Advanced Usage

### Development Workflow
1. **Code Changes**: Modify source code in `~/microservice-kiwi/`
2. **Quick Deploy**: `sudo -E ~/easy-deploy -mode=sg` (skip git)
3. **Test**: Use `~/easy-check` to monitor logs
4. **UI Updates**: Place `dist.zip` in home, run `~/easy-deploy-ui`

### Production Deployment
1. **Full Deploy**: `sudo -E ~/easy-deploy -c` (with monitoring)
2. **Health Check**: `~/easy-check` → option 17 (connectivity test)
3. **Monitor**: `~/easy-check` → option 18 (resource usage)

### Backup Strategy
```bash
# Backup databases
docker exec kiwi-mysql mysqldump -u root -p[password] --all-databases > backup.sql

# Backup configuration
~/easy-clean-setup → option 5 (backup files)

# Backup volumes
docker run --rm -v es_data:/data -v $(pwd):/backup ubuntu tar czf /backup/es_backup.tar.gz -C /data .
```

## 🤝 Support

For issues and questions:
1. Check container logs: `~/easy-check`
2. Review setup status: `~/easy-setup` → option 2
3. Consult troubleshooting section above
4. Check project repository: [microservice-kiwi](https://github.com/coding-by-feng/microservice-kiwi)

## 📝 License

This project is part of the Kiwi microservice platform. Check the main repository for license information.