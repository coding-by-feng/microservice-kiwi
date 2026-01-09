# Kiwi Monolith API Documentation

**Version:** 3.0.0
**Base URL:** `http://localhost:8080`
**Spring Boot:** 3.2.1
**Java:** 17

---

## Table of Contents

- [Authentication](#authentication)
- [UPMS (User Permission Management)](#upms-user-permission-management)
  - [User Management](#user-management)
  - [Department Management](#department-management)
  - [Menu Management](#menu-management)
  - [Role Management](#role-management)
- [Word Domain](#word-domain)
  - [Word Main](#word-main)
  - [Fetch Queue](#fetch-queue)
  - [Pronunciation](#pronunciation)
  - [Paraphrase](#paraphrase)
  - [Word Star List](#word-star-list)
  - [Example Star List](#example-star-list)
  - [Word Review](#word-review)
  - [Grammar](#grammar)
- [AI Domain](#ai-domain)
  - [AI Assistant](#ai-assistant)
  - [AI Call History](#ai-call-history)
  - [YouTube Video](#youtube-video)
  - [YouTube Channel](#youtube-channel)
- [Tools Domain](#tools-domain)
  - [Project Management](#project-management)
  - [Export](#export)
  - [Todo Tasks](#todo-tasks)
- [Flow Domain](#flow-domain)

---

## Authentication

**Base Path:** `/auth`

### Login

```http
POST /auth/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}
```

**Response:** User info with authentication token

### Logout

```http
POST /auth/logout
Authorization: Bearer <token>
```

### Get Current User

```http
GET /auth/me
Authorization: Bearer <token>
```

**Response:** Current user profile with authorities

### Refresh Token

```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "string"
}
```

---

## UPMS (User Permission Management)

### User Management

**Base Path:** `/api/upms/user`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/current` | Get current user info | Yes |
| GET | `/current/permissions` | Get current user permissions | Yes |
| GET | `/current/roles` | Get current user roles | Yes |
| GET | `/{userId}` | Get user by ID | ADMIN |
| GET | `/username/{username}` | Get user by username | ADMIN |
| GET | `/` | Get all users (paginated) | ADMIN |
| POST | `/` | Create new user | ADMIN |
| PUT | `/{userId}` | Update user | ADMIN |
| DELETE | `/{userId}` | Delete user | ADMIN |
| POST | `/{userId}/lock` | Lock user account | ADMIN |
| POST | `/{userId}/unlock` | Unlock user account | ADMIN |
| POST | `/{userId}/roles` | Assign roles to user | ADMIN |
| POST | `/change-password` | Change current user password | Yes |

#### Create User Example

```http
POST /api/upms/user
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "newuser",
  "email": "user@example.com",
  "password": "securepassword"
}
```

### Department Management

**Base Path:** `/api/sys/dept`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/page` | Get departments (paginated) | No |
| GET | `/{deptId}` | Get department by ID | No |
| GET | `/parent/{parentId}` | Get departments by parent ID | No |
| GET | `/tree` | Get department tree | No |
| POST | `/` | Create department | ADMIN |
| PUT | `/{deptId}` | Update department | ADMIN |
| DELETE | `/{deptId}` | Delete department | ADMIN |

### Menu Management

**Base Path:** `/api/sys/menu`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/page` | Get menus (paginated) | No |
| GET | `/{menuId}` | Get menu by ID | No |
| GET | `/parent/{parentId}` | Get menus by parent ID | No |
| GET | `/type/{type}` | Get menus by type | No |
| POST | `/` | Create menu | ADMIN |
| PUT | `/{menuId}` | Update menu | ADMIN |
| DELETE | `/{menuId}` | Delete menu | ADMIN |

### Role Management

**Base Path:** `/api/sys/role`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/page` | Get roles (paginated) | No |
| GET | `/{roleId}` | Get role by ID | No |
| GET | `/code/{roleCode}` | Get role by code | No |
| POST | `/` | Create role | ADMIN |
| PUT | `/{roleId}` | Update role | ADMIN |
| DELETE | `/{roleId}` | Delete role | ADMIN |

---

## Word Domain

### Word Main

**Base Path:** `/api/word/main`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/query/gate/{keyword}` | Smart query gate (Chinese/English routing) | No |
| GET | `/query/{wordName}` | Query word by name with full details | No |
| GET | `/queryById/{wordId}` | Query word by ID with full details | No |
| POST | `/fuzzyQueryList?wordName=` | Fuzzy search words | No |
| GET | `/listOverlapAnyway` | List overlapping words | No |
| POST | `/variant/insertVariant/{inputWordName}/{fetchWordName}` | Insert word variant relationship | No |
| DELETE | `/removeByWordName/{wordName}` | Remove word by name and trigger re-fetch | No |

#### Query Gate Example

The query gate automatically routes requests based on input:
- Chinese characters → Chinese meaning search
- English characters → English word query

```http
POST /api/word/main/query/gate/hello?current=1&size=20
```

#### Fuzzy Query Response

```json
{
  "code": 0,
  "data": [
    { "value": "hello" },
    { "value": "helloworld" }
  ]
}
```

### Fetch Queue

**Base Path:** `/api/word/fetch`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/{queueId}` | Get queue item by ID | No |
| GET | `/word/{wordName}` | Get queue item by word name | No |
| GET | `/waiting` | Get waiting items (paginated) | No |
| GET | `/stats` | Get queue statistics | No |
| POST | `/?wordName=&priority=` | Add word to fetch queue | No |
| POST | `/{queueId}/success?wordId=` | Mark fetch as success | ADMIN |
| POST | `/{queueId}/fail?errorMessage=` | Mark fetch as failed | ADMIN |
| POST | `/reset-failed?maxRetries=` | Reset failed items for retry | ADMIN |
| DELETE | `/{queueId}` | Delete queue item | ADMIN |

#### Queue Stats Response

```json
{
  "code": 0,
  "data": {
    "waiting": 10,
    "fetching": 2,
    "success": 150,
    "fail": 5
  }
}
```

### Pronunciation

**Base Path:** `/api/word/pronunciation`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/{pronunciationId}` | Get pronunciation by ID | No |
| GET | `/word/{wordId}` | Get pronunciations by word ID | No |
| GET | `/character/{characterId}` | Get pronunciations by character ID | No |
| GET | `/download/{pronunciationId}` | Download pronunciation audio | No |

### Paraphrase

**Base Path:** `/api/word/paraphrase`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/{paraphraseId}` | Get paraphrase by ID | No |
| PUT | `/modifyMeaningChinese` | Modify paraphrase Chinese meaning | No |
| GET | `/star/list/getItemDetail/{paraphraseId}` | Get paraphrase detail | No |

#### Paraphrase Star List Operations

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/star/list/save` | Create new paraphrase star list | Yes |
| PUT | `/star/list/updateById` | Update paraphrase star list | No |
| DELETE | `/star/list/delById/{id}` | Delete paraphrase star list | No |
| GET | `/star/list/getCurrentUserList` | Get current user's paraphrase star lists | Yes |
| PUT | `/star/list/putIntoStarList?paraphraseId=&listId=` | Add paraphrase to star list | No |
| GET | `/star/list/getListItems/{size}/{current}/{listId}` | Get paraphrase star list items | No |
| GET | `/star/list/getReviewListItems/{size}/{current}/{listId}` | Get review list items | No |
| GET | `/star/list/getRememberListItems/{size}/{current}/{listId}` | Get remember list items | No |
| PUT | `/star/list/rememberOne?paraphraseId=&listId=` | Mark paraphrase as remembered | Yes |
| PUT | `/star/list/keepInMind?paraphraseId=&listId=` | Keep paraphrase in mind | Yes |
| PUT | `/star/list/forgetOne?paraphraseId=&listId=` | Mark paraphrase as forgotten | No |
| DELETE | `/star/list/removeParaphraseStar?paraphraseId=&listId=` | Remove paraphrase from star list | No |

### Word Star List

**Base Path:** `/api/word/star/list`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/save` | Create new word star list | Yes |
| PUT | `/updateById` | Update word star list | No |
| DELETE | `/del/{id}` | Delete word star list | No |
| GET | `/getCurrentUserList` | Get current user's word star lists | Yes |
| GET | `/getListItems/{size}/{current}/{listId}` | Get word star list items | No |
| PUT | `/putWordStarList?wordId=&listId=` | Add word to star list | No |
| DELETE | `/removeWordStarList?wordId=&listId=` | Remove word from star list | No |
| GET | `/findAllWordId/{listId}` | Find all word IDs in list | No |
| GET | `/{listId}` | Get star list by ID | No |

#### Create Star List Example

```http
POST /api/word/star/list/save
Authorization: Bearer <token>
Content-Type: application/x-www-form-urlencoded

listName=My Vocabulary&remark=Daily words
```

### Example Star List

**Base Path:** `/api/word/example/star/list`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/` | Get user's example star lists | Yes |
| GET | `/{listId}` | Get example star list by ID | No |
| POST | `/` | Create example star list | Yes |
| PUT | `/{listId}` | Update example star list | No |
| DELETE | `/{listId}` | Delete example star list | No |
| POST | `/{listId}/example/{exampleId}` | Add example to list | No |
| DELETE | `/{listId}/example/{exampleId}` | Remove example from list | No |

### Word Review

**Base Path:** `/api/word/review`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/getReviewBreakpointPageNumber/{listId}` | Get review breakpoint page number | No |
| POST | `/createTheDays` | Create review days for current user | Yes |
| GET | `/refreshAllApiKey` | Refresh all TTS API keys | No |
| GET | `/getReviewCounterVO/{type}` | Get review counter by type | Yes |
| GET | `/getAllReviewCounterVO` | Get all review counters for current user | Yes |
| GET | `/downloadReviewAudio/{sourceId}/{type}` | Download review audio | No |
| GET | `/character/downloadReviewAudio/{characterCode}` | Download character review audio | No |
| POST | `/generateTtsVoiceFromParaphraseId/{paraphraseId}` | Generate TTS voice (deprecated) | No |
| PUT | `/increaseCounter/{type}` | Increase review counter | Yes |
| GET | `/autoSelectApiKey` | Auto select API key | No |
| PUT | `/increaseApiKeyUsedTime/{apiKey}` | Increase API key used time | No |
| PUT | `/deprecateApiKeyToday/{apiKey}` | Deprecate API key for today | No |
| DELETE | `/deprecate-review-audio/{sourceId}` | Deprecate review audio | No |
| DELETE | `/reGenReviewAudio/{sourceId}` | Regenerate review audio for paraphrase | No |

### Grammar

**Base Path:** `/api/grammar`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/mp3/{type}` | Download grammar MP3 | No |
| GET | `/srt/{type}` | Download grammar SRT | No |

---

## AI Domain

### AI Assistant

**Base Path:** `/api/ai`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/directly-translation/{language}/{originalText}` | Direct translation | No |
| GET | `/translation-and-explanation/{language}/{originalText}` | Translation with explanation | No |
| GET | `/grammar-explanation/{language}/{originalText}` | Grammar explanation | No |
| GET | `/grammar-correction/{language}/{originalText}` | Grammar correction | No |
| GET | `/vocabulary-explanation/{language}/{originalText}` | Vocabulary explanation | No |
| GET | `/synonym/{language}/{originalText}` | Find synonyms | No |
| GET | `/antonym/{language}/{originalText}` | Find antonyms | No |

#### Translation Example

```http
GET /api/ai/directly-translation/zh/Hello%20World
```

**Response:**
```json
{
  "code": 0,
  "data": {
    "originalText": "Hello World",
    "language": "zh",
    "result": "你好，世界"
  }
}
```

#### Supported Languages

| Code | Language |
|------|----------|
| `zh` | Chinese (Simplified) |
| `en` | English |
| `ja` | Japanese |
| `ko` | Korean |
| `fr` | French |
| `de` | German |
| `es` | Spanish |

### AI Call History

**Base Path:** `/api/ai/history`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/{id}` | Get history by ID | No |
| GET | `/` | Get current user's history (paginated) | Yes |
| GET | `/favorites` | Get current user's favorite history | Yes |
| GET | `/archived` | Get current user's archived history | Yes |
| GET | `/mode/{promptMode}` | Get history by prompt mode | Yes |
| POST | `/{id}/favorite` | Toggle favorite status | No |
| POST | `/{id}/archive` | Archive history item | No |
| DELETE | `/{id}` | Delete history (soft delete) | No |

#### History Response

```json
{
  "code": 0,
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 100,
        "promptMode": "directly-translation",
        "language": "zh",
        "originalText": "Hello",
        "result": "你好",
        "isFavorite": false,
        "isArchive": false,
        "createTime": "2025-01-01T10:00:00"
      }
    ],
    "totalElements": 50,
    "totalPages": 3
  }
}
```

### YouTube Video

**Base Path:** `/api/ai/ytb/video`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/download?url=` | Download YouTube video (streaming) | No |
| GET | `/subtitles/scrolling?url=` | Get scrolling subtitles | No |
| GET | `/subtitles/translated?url=&language=` | Get translated subtitles | No |
| GET | `/subtitles/translated/download?url=&language=` | Download translated subtitles as file | No |
| GET | `/subtitles/translated/stream` | Get WebSocket streaming info | No |
| DELETE | `/subtitles?url=&language=` | Clean subtitle cache | No |
| GET | `/title?url=` | Get video title | No |

#### Get Translated Subtitles Example

```http
GET /api/ai/ytb/video/subtitles/translated?url=https://youtube.com/watch?v=xxx&language=zh
```

#### Download Subtitles

The download endpoint returns a text file with the video title as filename:
```http
GET /api/ai/ytb/video/subtitles/translated/download?url=https://youtube.com/watch?v=xxx&language=zh
```

Response Headers:
```
Content-Type: text/plain; charset=UTF-8
Content-Disposition: attachment; filename="subtitles-VideoTitle-zh.txt"
```

### YouTube Channel

**Base Path:** `/api/ai/ytb/channel`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/{channelId}` | Get channel by ID | No |
| GET | `/link?channelLink=` | Get channel by link | No |
| GET | `/` | Get all channels (paginated) | No |
| GET | `/enabled` | Get enabled channels | No |
| GET | `/{channelId}/videos` | Get videos for channel | No |
| POST | `/` | Create new channel | ADMIN |
| PUT | `/{channelId}` | Update channel | ADMIN |
| POST | `/{channelId}/enable` | Enable channel | ADMIN |
| POST | `/{channelId}/disable` | Disable channel | ADMIN |
| DELETE | `/{channelId}` | Delete channel | ADMIN |

---

## Tools Domain

### Project Management

**Base Path:** `/rangi_windows/api`

#### Project CRUD

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/projects` | List projects with filters | No |
| GET | `/projects/{id}` | Get project by ID | No |
| POST | `/projects` | Create project | No |
| PUT | `/projects/{id}` | Update project | No |
| PATCH | `/projects/{id}` | Partial update project | No |
| POST | `/projects/{id}/archive` | Archive/unarchive project | No |
| DELETE | `/projects/{id}` | Delete project | No |

#### Project Query Parameters

```http
GET /rangi_windows/api/projects?q=search&glass=true&frame=false&page=1&pageSize=20&sortBy=createdAt&sortOrder=desc
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `q` | string | Search query |
| `glass` | boolean | Filter by glass stage |
| `frame` | boolean | Filter by frame stage |
| `purchase` | boolean | Filter by purchase stage |
| `transport` | boolean | Filter by transport stage |
| `install` | boolean | Filter by install stage |
| `repair` | boolean | Filter by repair stage |
| `start` | string | Start date (YYYY-MM-DD) |
| `end` | string | End date (YYYY-MM-DD) |
| `archived` | boolean | Filter archived only |
| `includeArchived` | boolean | Include archived projects |
| `page` | integer | Page number (default: 1) |
| `pageSize` | integer | Page size (default: 20) |
| `sortBy` | string | Sort field (default: createdAt) |
| `sortOrder` | string | Sort order: asc/desc |

#### Photo Operations

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/projects/{id}/photo` | Upload photo/video | No |
| GET | `/projects/{id}/photos` | List project photos | No |
| GET | `/projects/{id}/photo/{token}` | Download photo | No |
| DELETE | `/projects/{id}/photos/{photoId}` | Delete photo by ID | No |
| DELETE | `/projects/{id}/photo/{token}` | Delete photo by token | No |
| DELETE | `/projects/{id}/photos` | Delete all photos | No |

#### Stage Operations

```http
PATCH /rangi_windows/api/projects/{id}/stages
Content-Type: application/json

{
  "glass": true,
  "frame": false,
  "purchase": true
}
```

### Export

**Base Path:** `/rangi_windows/api/export`

| Method | Endpoint | Description | Response Type |
|--------|----------|-------------|---------------|
| GET | `/excel?start=&end=&archived=` | Export to Excel | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet |
| GET | `/pdf?start=&end=&archived=` | Export to PDF | application/pdf |

### Todo Tasks

**Base Path:** `/todo`

#### Task Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/tasks` | List tasks with filters | Yes |
| POST | `/tasks` | Create task | Yes |
| GET | `/tasks/{id}` | Get task by ID | Yes |
| PATCH | `/tasks/{id}` | Update task (with ETag) | Yes |
| DELETE | `/tasks/{id}` | Delete task (to trash) | Yes |
| POST | `/tasks/{id}/complete` | Complete task | Yes |
| POST | `/tasks/{id}/reset-status` | Reset task status | Yes |
| POST | `/tasks/reset-statuses` | Reset all statuses | Yes |

#### Optimistic Locking with ETag

```http
PATCH /todo/tasks/{id}
If-Match: "etag-value"
Content-Type: application/json

{
  "title": "Updated Title",
  "status": "in_progress"
}
```

**Error Response (412 Precondition Failed):**
```json
{
  "error": "Precondition Failed",
  "message": "ETag mismatch"
}
```

#### Trash Operations

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/trash` | List deleted tasks | Yes |
| DELETE | `/trash` | Clear trash permanently | Yes |
| DELETE | `/trash/{id}` | Permanently delete task | Yes |
| POST | `/trash/{id}/restore` | Restore from trash | Yes |

#### History & Analytics

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/history?date=` | List completed tasks | Yes |
| GET | `/analytics/monthly?months=` | Get monthly analytics | Yes |
| GET | `/analytics/summary?month=` | Get summary stats | Yes |
| GET | `/ranking/current` | Get user ranking | Yes |
| GET | `/ranking/ranks` | Get rank definitions | No |

#### Import/Export

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/export/todo` | Export all tasks | Yes |
| POST | `/import/todo` | Import tasks | Yes |

---

## Flow Domain

**Base Path:** `/api/flow`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/process` | Start process instance | No |
| GET | `/tasks?assignee=` | Get tasks by assignee | No |
| POST | `/tasks/{taskId}/complete` | Complete task | No |
| GET | `/process/{processId}/tasks` | Get tasks for process | No |

> **Note:** Flowable integration is currently disabled in dev profile.

---

## Common Response Formats

### Success Response (R<T>)

```json
{
  "code": 0,
  "msg": "Success",
  "data": { ... }
}
```

### Error Response

```json
{
  "code": 1,
  "msg": "Error message",
  "data": null
}
```

### Paginated Response

```json
{
  "code": 0,
  "data": {
    "content": [ ... ],
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0,
    "first": true,
    "last": false
  }
}
```

---

## Authentication

The API uses JWT Bearer token authentication.

### Headers

```http
Authorization: Bearer <jwt-token>
```

### Roles

- **USER** - Standard user access
- **ADMIN** - Administrative access

### Public Endpoints

The following endpoints do not require authentication:

- `/actuator/**`
- `/sys/user/**`
- `/log/**`
- Most GET endpoints for public data

---

## API Statistics

| Domain | Controllers | Endpoints |
|--------|-------------|-----------|
| Authentication | 1 | 4 |
| UPMS | 4 | 29 |
| Word | 8 | 60+ |
| AI | 4 | 25 |
| Tools | 3 | 54 |
| Flow | 1 | 4 |
| **Total** | **21** | **175+** |

---

## OpenAPI/Swagger

Interactive API documentation is available at:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
