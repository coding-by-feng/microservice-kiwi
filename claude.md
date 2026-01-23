# Kiwi Monolith - Claude Context

## Project Overview

**Kiwi Monolith** is a Spring Boot 3.2 + Java 17 educational and productivity platform consolidated from microservices. It provides:

- **UPMS**: User & permission management with RBAC
- **Word**: Dictionary/word learning system with content processing
- **AI**: YouTube subtitle translation, text-to-speech, grammar assistance, AI conversations
- **Tools**: Todo, project management, focus timer, exports
- **Flow**: Workflow automation via Flowable (currently disabled in dev)

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.1, Spring Cloud 2023.0.0 |
| Build | Maven 3.6+ (multi-module) |
| ORM | MyBatis Plus 3.5.9 |
| Database | MySQL 8.x |
| Cache | Redis + Redisson 3.25.2 |
| Search | Elasticsearch 8.11.3 |
| Storage | MinIO 8.5.7 |
| Security | OAuth2 (Google), JWT Bearer tokens |
| AI | Google Cloud TTS, yt-dlp CLI |
| Docs | SpringDoc OpenAPI 2.3.0 |
| Testing | JUnit 5, Mockito, TestContainers |

## Project Structure

```
microservice-kiwi/
├── kiwi-monolith/                  # Main application
│   ├── src/main/java/me/fengorz/kiwi/
│   │   ├── KiwiApplication.java    # Entry point
│   │   ├── api/                    # REST Controllers
│   │   │   ├── ai/                 # AI/YouTube endpoints
│   │   │   ├── auth/               # Authentication
│   │   │   ├── flow/               # Workflow
│   │   │   ├── tools/              # Productivity tools
│   │   │   ├── upms/               # User management
│   │   │   └── word/               # Word/dictionary
│   │   ├── domain/                 # Business logic (6 domains)
│   │   ├── config/                 # Spring configurations
│   │   ├── security/               # OAuth2 & JWT security
│   │   ├── messaging/              # Redis pub/sub
│   │   ├── common/                 # Shared utilities
│   │   └── ws/                     # WebSocket support
│   ├── src/main/resources/
│   │   ├── application.yml         # Base config
│   │   ├── application-{env}.yml   # Environment configs (dev/test/prod)
│   │   ├── db/changelog/           # Liquibase migrations
│   │   └── mapper/                 # MyBatis XML mappers
│   ├── src/test/java/              # Tests
│   ├── http-tests/                 # Manual API testing (.http files)
│   ├── deploy/                     # Docker & deployment scripts
│   └── pom.xml
└── pom.xml                         # Root POM
```

## Key Patterns & Conventions

### Architecture
- **Layered Architecture**: Controller → Service → Mapper → Entity
- **Domain-Driven Design**: 6 independent domains (AI, Word, UPMS, Tools, Flow, Auth)

### Naming Conventions
- **Package**: `me.fengorz.kiwi.domain.{name}.{layer}`
- **Entity**: CamelCase (e.g., `WordMain`, `AiCallHistory`)
- **DTO/VO**: Suffix with `Dto`/`Vo`
- **Mapper**: Suffix with `Mapper`
- **Service**: Suffix with `Service`
- **Controller**: Suffix with `Controller`

### Database Conventions
- **Soft delete**: `isDel` field (MyBatis Plus logic-delete)
- **Auto ID**: Primary key auto-increment
- **Schema**: snake_case columns → CamelCase Java properties (auto-mapped)

### API Response Format
```json
// Success
{ "code": 0, "msg": "Success", "data": {} }

// Error
{ "code": 1, "msg": "Error message", "data": null }
```

### Cache Key Pattern
```
KIWI:{DOMAIN}:{FEATURE}:{identifier}
Example: KIWI:GROK:SUBTITLE:{videoHash}:{language}
```

## Development

### Prerequisites
- Java 17
- Maven 3.6+
- MySQL 8.x (local port 3307 for dev)
- Redis (local port 6380 for dev)
- Elasticsearch (local port 9201 for dev)

### Running Locally
```bash
cd kiwi-monolith
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or with explicit environment:
```bash
DB_USERNAME=root DB_PASSWORD=kiwi123 \
REDIS_HOST=localhost REDIS_PASSWORD=kiwi123 \
mvn spring-boot:run
```

### Building
```bash
# Full build
cd kiwi-monolith && mvn clean package

# Skip tests (faster)
mvn -T 1C clean package -DskipTests
```

### Local Development Ports
| Service | Port |
|---------|------|
| Application | 8088 (dev) / 8080 (default) |
| MySQL | 3307 |
| Redis | 6380 |
| Elasticsearch | 9201 |

## Testing

### Running Tests
```bash
cd kiwi-monolith
mvn test
```

### HTTP Manual Tests
Located in `kiwi-monolith/http-tests/`. Use IntelliJ IDEA or VS Code REST Client extension.

Key test files:
- `01-auth.http` - Authentication flows
- `02-upms.http` - User management
- `03-word-main.http` - Word operations
- `09-ai-assistant.http` - AI features
- `11-youtube-video.http` - YouTube processing

### Test Order
1. Run auth tests first to get bearer token
2. Use token in subsequent API tests

## Configuration

### Profiles
- **dev**: Local development (Flowable disabled, Liquibase disabled)
- **test**: Staging environment
- **prod**: Production (uses encrypted secrets via Jasypt)

### Key Config Files
- `application.yml` - Base/common settings
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production settings
- `http-client.env.json` - HTTP test environment variables

### Sensitive Config
Uses Jasypt encryption for sensitive properties. Pattern: `ENC(encrypted_value)`

## Deployment

### Docker Build
```bash
cd kiwi-monolith/deploy
docker build -t kiwi-monolith:latest ..
```

### Deploy Script
```bash
cd kiwi-monolith/deploy
./deploy.sh
# Follow prompts for environment and memory selection
```

### Memory Options
- 512m-1g (default)
- 1g-2g
- 2g-4g

## Important Files

| File | Purpose |
|------|---------|
| `KiwiApplication.java` | Application entry point |
| `SecurityConfig.java` | OAuth2 and security configuration |
| `RedisCacheConfig.java` | Redis cache setup |
| `BearerTokenAuthenticationFilter.java` | Custom JWT auth filter |
| `db.changelog-master.xml` | Liquibase migration master file |

## Common Tasks

### Adding a New API Endpoint
1. Create/update controller in `api/{domain}/`
2. Add service interface and implementation in `domain/{domain}/service/`
3. Add mapper interface in `domain/{domain}/mapper/` if DB access needed
4. Add mapper XML in `resources/mapper/{domain}/`
5. Add HTTP test file in `http-tests/`

### Adding a Database Table
1. Add entity class in `domain/{domain}/entity/`
2. Add mapper interface
3. Add Liquibase changeset in `db/changelog/`
4. Run with Liquibase enabled to apply migration

### Adding a New Domain
1. Create package structure: `domain/{name}/{entity,mapper,service}/`
2. Create API package: `api/{name}/`
3. Add corresponding HTTP test files

## Gotchas & Tips

1. **Flowable is disabled in dev** - Enable in `application-dev.yml` if needed
2. **Liquibase is disabled in dev** - Schema changes need manual application or temp enable
3. **WebSocket path prefix** - All WebSocket endpoints start with `/ws/`
4. **Cache annotations** - Use Spring `@Cacheable`/`@CacheEvict` with defined cache names
5. **File uploads** - Max 10MB in dev, configured in application-{env}.yml
6. **OAuth2** - Google OAuth2 configured; check `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` env vars

## API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html (when running)
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Manual docs**: See `API-DOCUMENTATION.md`

## Git Workflow

- **Main branch**: `master`
- **Feature branches**: Create from master, merge back via PR
- **Commit style**: Conventional commits (feat:, fix:, docs:, etc.)
