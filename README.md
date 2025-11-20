# Kiwi Microservice Platform

A modular Spring Cloud (Hoxton) / Spring Boot 2.3 based microservice platform that bundles infrastructure automation, service discovery, centralized config, API gateway, auth, content & document processing, AI utilities (YouTube subtitle translation + TTS), and deployment helper scripts optimized for edge devices (e.g. Raspberry Pi) and standard Linux servers.

---
## 1. Core Highlights
- Opinionated production-esque microservice baseline (Eureka + Config + Gateway)
- Unified build (Maven multi‑module) with shared common libraries (common-* packages)
- Turn‑key infra bootstrap: MySQL 8, Redis, RabbitMQ, Elasticsearch 7.17.9, FastDFS, Nginx
- Automated deployment & lifecycle management via easy-* shell scripts
- AI Service: real‑time YouTube subtitle extraction + translation (WebSocket streaming) & related utilities
- Caching abstractions (Spring Cache + custom key prefixes) for expensive AI / network calls
- Pluggable security & auth (OAuth2 legacy stack) + UPMS (user / permission management)
- Extensible architecture ready for domain expansion (finance, flow, word, crawler modules, etc.)

---
## 2. Repository Layout (Key Modules)
(Only major, high-signal modules listed; supporting submodules exist under common / api / biz layers.)

| Module | Purpose |
|--------|---------|
| kiwi-eureka | Service discovery (Eureka Server) |
| kiwi-config | Centralized configuration server |
| kiwi-gateway | Edge routing / reverse proxy (Spring Cloud Gateway) |
| kiwi-auth | Auth / token issuing (OAuth2 legacy) |
| kiwi-upms | User & permission management (RBAC) |
| kiwi-word | Document & content handling (includes crawler submodule) |
| kiwi-ai | AI utilities (subtitle translation streaming, possibly TTS, NLP hooks) |
| kiwi-common | Shared libraries: cache, DB, DFS, ES, MQ, SDK, YouTube, TTS, etc. |
| kiwi-test | Test scaffolding / integration experiments |
| kiwi-deploy | Infra + deployment scripts, docker resources |
| kiwi-sql | Database initialization & maintenance SQL scripts |
| kiwi-docs | Supplemental design / integration notes |

Additional domain modules (finance, flow, generator, aws, etc.) are experimental or auxiliary.

---
## 3. Technology Stack
- Language: Java (compiled for 1.8 target, JDK 8+; runtime commonly JDK 17 supported)
- Framework: Spring Boot 2.3.12.RELEASE, Spring Cloud Hoxton.SR9
- Persistence: MyBatis-Plus, MySQL 8.x
- Messaging: RabbitMQ
- Caching & Coordination: Redis, Redisson
- Search: Elasticsearch 7.17.9 + IK analyzer (configured externally)
- Object Storage / File Serving: FastDFS + Nginx
- AI / External: OpenAI-compatible clients, Google Cloud Speech, YouTube parsing helpers, OkHttp
- Build: Maven (multi-module) + Lombok + AspectJ (selective AOP)
- Packaging: Docker (per service) orchestrated by helper scripts (not full docker-compose YAML here—scripts assemble/build containers)

---
## 4. Architecture Overview
1. Entry → kiwi-gateway (API Gateway) handles routing, auth delegation, cross-cutting filters.
2. Config externalization through kiwi-config (backed by Git repo / file system—configure in application.yml).
3. Discovery via kiwi-eureka; each service registers & uses logical names.
4. Core services (auth, upms, word, ai, crawler) implement domain logic; rely on common libs for consistency.
5. Data & infra layer: MySQL (relational), Redis (cache / session), RabbitMQ (async), ES (search), FastDFS (files), Nginx (static + reverse proxy UI).
6. AI pipeline (YouTube subtitle translation): WebSocket streaming endpoint emits tokens progressively; results cached to avoid repeated Grok / LLM streaming calls for same video URL + target language.

---
## 5. Development Quick Start
### 5.1 Prerequisites
- JDK 8+ (recommended JDK 17 for tooling; ensure target 1.8 compatibility remains)
- Maven 3.6+
- Docker & Docker Buildx (for container packaging)
- Redis / MySQL locally OR run infra via provided deployment script(s)

### 5.2 Clone
```bash
git clone https://github.com/coding-by-feng/microservice-kiwi.git
cd microservice-kiwi
```

### 5.3 Build All
```bash
mvn -T 1C clean package -DskipTests
```
(Adjust parallelism as needed.)

### 5.4 Run Core Services (Local Dev)
Order (suggested):
1. kiwi-eureka
2. kiwi-config
3. kiwi-gateway
4. kiwi-auth / kiwi-upms / kiwi-word / kiwi-ai (as needed)

Example:
```bash
cd kiwi-eureka && mvn spring-boot:run
```
Repeat per service. Provide required application-local.yml overrides (DB creds, Redis, etc.).

---
## 6. Automated Deployment Scripts (Home Directory Symlinks)
The legacy README documented these—condensed & clarified below.

| Script | Function |
|--------|----------|
| easy-deploy | Build & deploy all backend microservices (flags to skip phases) |
| easy-stop | Gracefully stop / remove containers |
| easy-deploy-ui | Deploy / refresh UI inside Nginx container (dist.zip) |
| easy-ui-initialize | Initialize Nginx reverse proxy & route mappings |
| easy-check | Interactive container status/log/metrics tool |
| easy-setup | Re-run initial infra provisioning (selective steps) |
| easy-clean-setup | Reset stored setup progress / credentials |

### 6.1 Common Flags (easy-deploy)
- -mode=sg  Skip git pull
- -mode=sm  Skip Maven build
- -mode=sbd Skip Docker build
- -c        Enable auto-restart monitoring (crawler focus)

### 6.2 Advanced Modes
- -mode=obm    Only build with Maven, copy jars into ~/built_jar (use -s=svc1,svc2 to limit)
- -mode=ouej   Use existing jars from ~/built_jar for deployment (skip git and maven)
- -mode=osj    Only send jars from ~/built_jar to a remote host (skip git, maven, docker)
- -mode=og     Only run git stash + pull (no build/deploy)

Example (send-only):
```bash
sudo -E ./easy-deploy -mode=osj
```

### 6.3 Port / Process Utilities
Kill port (manual fallback):
```bash
sudo kill -9 $(lsof -ti :8080)
```
(Some script modes expose -mode=kp with -port=PORT for convenience.)

---
## 7. Configuration & Environment
Populate (e.g. in ~/.bashrc or deployment .env injection):

| Variable | Meaning |
|----------|---------|
| KIWI_ENC_PASSWORD | Encryption password for jasypt-secured properties |
| GROK_API_KEY | API key for Grok / LLM integration |
| DB_IP | MySQL host (infra IP) |
| MYSQL_ROOT_PASSWORD | MySQL root password |
| REDIS_PASSWORD | Redis password (if enabled) |
| ES_ROOT_PASSWORD | Elasticsearch bootstrap password |
| ES_USER_NAME / ES_USER_PASSWORD | ES application credentials |
| FASTDFS_HOSTNAME | Public hostname for FastDFS |
| FASTDFS_NON_LOCAL_IP | External IP for FastDFS storage |
| INFRASTRUCTURE_IP | Infra tier host (db/cache/message/search/ui) |
| SERVICE_IP | Microservice tier host (Java services) |

Store secrets securely; avoid committing plaintext credentials.

---
## 8. AI YouTube Subtitle Translation (Streaming)
### 8.1 WebSocket Endpoint (Preferred)
Endpoint:
```
/ai/ws/ytb/subtitle
```

Establish a WebSocket connection (browser or client). Immediately send a JSON payload:
```json
{
  "videoUrl": "https://www.youtube.com/watch?v=XXXXXXX",
  "targetLang": "en",          
  "sourceLangHint": "auto",    
  "mode": "translate",         
  "requestId": "client-uuid-123" 
}
```
Server streams frames/messages containing partial or incremental translated segments. A final frame will contain completion metadata (e.g. end=true, totalSegments, cached=true if reused).

### 8.2 Deprecated HTTP Endpoint
The former synchronous /subtitles/translated/download HTTP endpoint is deprecated. Migrate clients to the WebSocket flow for:
- Lower latency (token-level streaming)
- Better UX (progressive rendering)
- Caching reuse (see below)

### 8.3 Caching Strategy
Repeated translation for the same videoUrl (+ targetLang) should reuse cached result without re-invoking upstream Grok streaming.

Pattern (illustrative):
```java
@KiwiCacheKeyPrefix(AiConstants.CACHE_KEY_PREFIX_GROK.VIDEO_TITLE)
@Cacheable(
  cacheNames = AiConstants.CACHE_NAMES,
  keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
  unless = "#result == null")
public SubtitleResult translateAndCache(String videoUrl, String targetLang) { ... }
```
Recommended cache key structure:
```
KIWI:GROK:SUBTITLE:{hash(videoUrl)}:{targetLang}
```
If a cached aggregate transcript exists, server may:
1. Immediately send a priming frame { cached:true, progress:1.0 }
2. Stream previously cached segments quickly (batched) or send a single final payload.
3. Provide an ETag-style checksum for client diff logic (optional future enhancement).

### 8.4 Client Consumption Tips
- Implement exponential backoff + retry for initial WebSocket connect fails.
- Interpret partial frames in order; buffer until punctuation for smoother UI.
- Detect cached=true to disable spinners.
- Handle close codes gracefully (retry limited times).

---
## 9. Logging & Observability
Suggestions (some implemented via common libs):
- Structured JSON logs (upgrade path) for gateway & ai service
- Add per-file move operations detailed logs (if using file storage migration jobs) including: source path, destination path, bytes, checksum, latency, success/failure cause
- Consider Zipkin / tracing integration (zipkin version properties present) for cross-service call insight (activation optional)

---
## 10. Data & SQL Assets
`kiwi-sql/` contains initialization & maintenance scripts:
- `init.sql`, `common.sql`, `ai_initialize.sql`, `ytb_table_initialize.sql`, etc.
Run selectively rather than blind import—review for destructive operations (e.g. cleaner.sql, truncateAll.sql).

---
## 11. Docker & Images
Each service often has a Dockerfile (e.g., kiwi-auth, kiwi-config, gateway, etc.). Build with:
```bash
mvn -T 1C clean package -DskipTests
# Example build
cd kiwi-auth
docker build -t kiwi-auth:2.0 .
```
The deployment scripts orchestrate multi-service builds with tagging consistency. For AI service Python utilities (e.g., yt-dlp) prefer installing via pip3 in a slim base image (refactor Dockerfiles accordingly; keep layers minimal, use --no-cache-dir, and pin versions for reproducibility).

Minimal pattern suggestion:
```
FROM eclipse-temurin:17-jre as runtime
# (Optional) Separate builder stage if layering

# If yt-dlp needed:
RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-pip \
  && pip3 install --no-cache-dir yt-dlp==<version> \
  && apt-get purge -y --auto-remove \
  && rm -rf /var/lib/apt/lists/*

COPY target/kiwi-ai-*.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

---
## 12. Port Reference (Condensed)
| Service | Default Port |
|---------|--------------|
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ | 5672 / 15672 (mgmt) |
| Elasticsearch | 9200 / 9300 |
| Nginx / UI | 80 |
| Eureka | 8762 |
| Config | 7771 |
| Gateway | 9991 |
| AI (example) | (Check application.yml; often dynamic) |

---
## 13. Troubleshooting (Essentials)
### 13.1 Port Conflicts
```bash
lsof -ti :8080 | xargs -r kill -9
```
### 13.2 Container Failures
```bash
docker ps -a
docker logs <container>
```
### 13.3 Cache Invalidation
If subtitles stale: flush selective key instead of full Redis FLUSHALL.
```bash
redis-cli --pass $REDIS_PASSWORD KEYS 'KIWI:GROK:SUBTITLE:*' | xargs -r redis-cli --pass $REDIS_PASSWORD DEL
```
### 13.4 Memory Constraints (Edge Devices)
- Lower ES heap (`ES_JAVA_OPTS=-Xms256m -Xmx256m`)
- Disable unneeded services (crawler, AI) in light deployments

---
## 14. Extending the Platform
| Area | Approach |
|------|----------|
| New microservice | Clone structure of existing (auth/upms) and register with Eureka + Config |
| Add caching | Use common cache key generator + prefix annotations |
| Add WebSocket feature | Mirror ai service WS endpoint pattern (stomp/plain) |
| Observability | Introduce Spring Boot Admin / Prometheus + Grafana (future) |
| Security hardening | Rotate credentials, enable TLS termination at gateway / nginx |

---
## 15. Contribution Workflow
1. Fork & branch: `feat/<short-name>`
2. Run full build + baseline tests: `mvn clean verify`
3. Keep changes atomic (one concern per PR)
4. Provide configuration / migration notes when altering DB schemas
5. Ensure no secrets committed (scan diffs)

---
## 16. Roadmap (Indicative)
- Upgrade Spring Boot / Cloud train (current version is legacy) to modern LTS
- Migrate OAuth2 stack → Spring Authorization Server or external IdP
- Introduce structured logging + tracing (Zipkin/OTel) fully
- Refine AI service with partial reliability & streaming resilience patterns
- Container composition via docker-compose / K8s manifests (a k8s/ dir exists—expand)

---
## 17. License
See root LICENSE file. (Ensure compatibility of third-party assets before distribution.)

---
## 18. Quick Command Cheat Sheet
```bash
# Build all (skip tests)
mvn -T 1C clean package -DskipTests
# Run eureka
docker run -d --name kiwi-eureka kiwi-eureka:2.0
# Kill process on 8080
lsof -ti :8080 | xargs -r kill -9
# Clear subtitle cache (pattern example)
redis-cli KEYS 'KIWI:GROK:SUBTITLE:*' | xargs -r redis-cli DEL
```

---
## 19. Support
1. Review service logs (`easy-check` or docker logs)
2. Verify env variables loaded
3. Inspect network / DNS for service discovery issues
4. Consult `kiwi-docs/` for integration specifics
5. Open an issue referencing reproduction steps

---
> This README is a condensed, modernized rewrite emphasizing architecture, extensibility, and the AI streaming subtitle workflow. For legacy step-by-step provisioning details, consult earlier revisions or the deployment scripts themselves.

## YouTube Data Source Switching

Set `youtube.mode` in your application configuration (application.yml / application.properties) to choose implementation:

- `api` (default): Uses YouTube Data API (OAuth/key required) via `YouTuBeApiHelper`.
- `yt-dlp`: Uses local yt-dlp CLI tool (no API quota required) via `YouTuBeHelper`.

Example (application.yml):

```
youtube:
  mode: yt-dlp
  video:
    command: yt-dlp
    download:
      path: /tmp/ytb-downloads
    subtitles:
      langs: en,en-GB,en-US
    large-subtitles:
      threshold: 200
```

When in `api` mode, video binary download is not supported; the `/ai/ytb/video/download` endpoint will return 400.
When in `yt-dlp` mode, subtitles and titles are resolved via the CLI. Caching logic remains unchanged.
