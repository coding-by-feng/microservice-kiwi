# Tools API Documentation

Base URL: `/api/tools`

All endpoints require authentication via Bearer token in the `Authorization` header.

---

## Todo APIs

Base path: `/api/tools/todo`

### Tasks

#### List Tasks
```
GET /api/tools/todo/tasks
```

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | Integer | No | 1 | Page number |
| pageSize | Integer | No | 20 | Items per page (max 100) |
| status | String | No | all | Filter by status: `pending`, `success`, `fail`, `all` |
| frequency | String | No | all | Filter by frequency: `once`, `daily`, `weekly`, `custom`, `all` |
| search | String | No | - | Search in title and description |
| sort | String | No | points_desc | Sort order: `points_desc`, `created_desc`, `updated_desc` |
| date | String | No | - | Filter by creation date (YYYY-MM-DD) |

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "data": [...],
    "meta": {
      "page": 1,
      "pageSize": 20,
      "total": 100
    }
  }
}
```

#### Create Task
```
POST /api/tools/todo/tasks
Content-Type: application/json
```

**Request Body:**
```json
{
  "title": "Task title",
  "description": "Task description",
  "successPoints": 10,
  "failPoints": -5,
  "frequency": "once",
  "customDays": null
}
```

**Response Headers:** `ETag`

**Response (201 Created):**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "data": {
      "id": "1234567890",
      "title": "Task title",
      ...
    }
  }
}
```

#### Get Task by ID
```
GET /api/tools/todo/tasks/{id}
```

**Response Headers:** `ETag`

#### Update Task
```
PATCH /api/tools/todo/tasks/{id}
Content-Type: application/json
If-Match: "{etag}"
```

**Request Body:** (partial update - only include fields to update)
```json
{
  "title": "Updated title",
  "description": "Updated description"
}
```

**Response Headers:** `ETag`

#### Delete Task (Move to Trash)
```
DELETE /api/tools/todo/tasks/{id}
```

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "data": { "ok": true }
  }
}
```

#### Complete Task
```
POST /api/tools/todo/tasks/{id}/complete
Content-Type: application/json
```

**Request Body:**
```json
{
  "status": "success"  // or "fail"
}
```

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "data": {
      "task": {...},
      "history": {...},
      "ranking": {...}
    }
  }
}
```

#### Reset Task Status
```
POST /api/tools/todo/tasks/{id}/reset-status
```

#### Reset All Task Statuses
```
POST /api/tools/todo/tasks/reset-statuses
```

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "resetCount": 5
  }
}
```

#### Seed Demo Tasks
```
POST /api/tools/todo/tasks/demo
```

---

### Trash

#### List Trash
```
GET /api/tools/todo/trash
```

**Query Parameters:**
| Parameter | Type | Required | Default |
|-----------|------|----------|---------|
| page | Integer | No | 1 |
| pageSize | Integer | No | 20 |

#### Clear All Trash
```
DELETE /api/tools/todo/trash
```

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "data": { "deletedCount": 10 }
  }
}
```

#### Delete Trash Item Permanently
```
DELETE /api/tools/todo/trash/{id}
```

#### Restore from Trash
```
POST /api/tools/todo/trash/{id}/restore
```

---

### History

#### List History
```
GET /api/tools/todo/history
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| date | String | **Yes** | Date filter (YYYY-MM-DD) |
| page | Integer | No | Page number |
| pageSize | Integer | No | Items per page |

#### Delete History Record
```
DELETE /api/tools/todo/history/{id}
```

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "data": { "ok": true },
    "meta": { "ranking": {...} }
  }
}
```

---

### Analytics

#### Monthly Analytics
```
GET /api/tools/todo/analytics/monthly
```

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| months | Integer | No | 6 | Number of months (1-24) |

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "data": {
      "labels": ["2024-07", "2024-08", ...],
      "points": [100, 200, ...]
    }
  }
}
```

#### Analytics Summary
```
GET /api/tools/todo/analytics/summary
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| month | String | **Yes** | Month (YYYY-MM) |

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "data": {
      "month": "2024-08",
      "totalPoints": 500,
      "completedCount": 25,
      "successRatePct": 80
    }
  }
}
```

---

### Ranking

#### Get Current User Ranking
```
GET /api/tools/todo/ranking/current
```

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "data": {
      "totalPoints": 1500,
      "currentRank": {
        "key": "trainee",
        "threshold": 1000,
        "level": 2
      },
      "nextRank": {
        "key": "novice",
        "threshold": 3000,
        "level": 3
      },
      "progressPct": 25.0
    }
  }
}
```

#### Get Rank Definitions
```
GET /api/tools/todo/ranking/ranks
```

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": [
    { "key": "beginner", "threshold": 0, "level": 1 },
    { "key": "trainee", "threshold": 1000, "level": 2 },
    { "key": "novice", "threshold": 3000, "level": 3 },
    { "key": "apprentice", "threshold": 6000, "level": 4 },
    { "key": "wood", "threshold": 10000, "level": 5 }
  ]
}
```

---

## Project APIs

Base path: `/api/tools/project`

### Projects

#### List Projects
```
GET /api/tools/project/projects
```

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| q | String | No | - | Search query |
| glass | Boolean | No | - | Filter by glass stage |
| frame | Boolean | No | - | Filter by frame stage |
| purchase | Boolean | No | - | Filter by purchase stage |
| transport | Boolean | No | - | Filter by transport stage |
| install | Boolean | No | - | Filter by install stage |
| repair | Boolean | No | - | Filter by repair stage |
| start | String | No | - | Start date filter |
| end | String | No | - | End date filter |
| page | Integer | No | 1 | Page number |
| pageSize | Integer | No | 20 | Items per page |
| sortBy | String | No | createdAt | Sort field |
| sortOrder | String | No | desc | Sort order: `asc`, `desc` |
| archived | Boolean | No | false | Filter archived projects |
| includeArchived | Boolean | No | - | Include both archived and active |

#### Get Project by ID
```
GET /api/tools/project/projects/{id}
```

#### Create Project
```
POST /api/tools/project/projects
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Project name",
  "address": "Project address",
  "glass": false,
  "frame": false,
  "purchase": false,
  "transport": false,
  "install": false,
  "repair": false
}
```

#### Update Project (Full)
```
PUT /api/tools/project/projects/{id}
Content-Type: application/json
```

#### Patch Project (Partial)
```
PATCH /api/tools/project/projects/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Updated name",
  "archived": true
}
```

#### Archive/Unarchive Project
```
POST /api/tools/project/projects/{id}/archive
Content-Type: application/json
```

**Request Body:**
```json
{
  "archived": true
}
```

#### Delete Project
```
DELETE /api/tools/project/projects/{id}
```

**Response:** `204 No Content`

#### Update Project Stages
```
PATCH /api/tools/project/projects/{id}/stages
Content-Type: application/json
```

**Request Body:**
```json
{
  "glass": true,
  "frame": false,
  "install": true
}
```

---

### Project Photos

#### Upload Photo
```
POST /api/tools/project/projects/{id}/photo
Content-Type: multipart/form-data
```

**Form Data:**
- `file` or `photo`: The image/video file

#### List Photos
```
GET /api/tools/project/projects/{id}/photos
```

#### Download Photo
```
GET /api/tools/project/projects/{id}/photo/{token}
```

#### Delete Photo by ID
```
DELETE /api/tools/project/projects/{id}/photos/{photoId}
```

#### Delete Photo by Token
```
DELETE /api/tools/project/projects/{id}/photo/{token}
```

#### Delete All Photos for Project
```
DELETE /api/tools/project/projects/{id}/photos
```

---

## API Migration Summary

| Old Path | New Path |
|----------|----------|
| `/todo/*` | `/api/tools/todo/*` |
| `/rangi_windows/api/*` | `/api/tools/project/*` |

### Removed APIs
- `GET /todo/export/todo` - Export all tasks
- `POST /todo/import/todo` - Import tasks
- `GET /rangi_windows/api/export/excel` - Export to Excel
- `GET /rangi_windows/api/export/pdf` - Export to PDF

---

## TypeScript Interfaces

```typescript
// Task Types
interface TaskDTO {
  id: string;
  userId: string;
  title: string;
  description: string | null;
  successPoints: number;
  failPoints: number;
  frequency: 'once' | 'daily' | 'weekly' | 'monthly' | 'custom';
  customDays: number | null;
  status: 'pending' | 'success' | 'fail';
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
  metadata: Record<string, any>;
}

interface TaskCreateRequest {
  title: string;
  description?: string;
  successPoints?: number;
  failPoints?: number;
  frequency?: string;
  customDays?: number;
}

interface TaskUpdateRequest {
  title?: string;
  description?: string;
  successPoints?: number;
  failPoints?: number;
  frequency?: string;
  customDays?: number;
}

// History Types
interface HistoryRecordDTO {
  id: string;
  userId: string;
  taskId: string;
  title: string;
  description: string | null;
  successPoints: number;
  failPoints: number;
  status: 'success' | 'fail';
  pointsApplied: number;
  completedAt: string;
}

// Trash Types
interface TrashItemDTO {
  id: string;
  title: string;
  description: string | null;
  successPoints: number;
  failPoints: number;
  frequency: string;
  customDays: number | null;
  status: string;
  originalDate: string;
  deletedDate: string;
}

// Ranking Types
interface RankDefinitionDTO {
  key: string;
  threshold: number;
  level: number;
}

interface RankingDTO {
  totalPoints: number;
  currentRank: RankDefinitionDTO;
  nextRank: RankDefinitionDTO | null;
  progressPct: number;
}

// Project Types
interface Project {
  id: string;
  name: string;
  address: string;
  glass: boolean;
  frame: boolean;
  purchase: boolean;
  transport: boolean;
  install: boolean;
  repair: boolean;
  archived: boolean;
  createdAt: string;
}

interface ProjectPhoto {
  id: string;
  projectId: string;
  originalName: string;
  contentType: string;
  size: number;
  token: string;
  createdAt: string;
}

// API Response Wrapper
interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

interface PageMeta {
  page: number;
  pageSize: number;
  total: number;
}
```

---

## ETag Handling for Task Updates

1. When fetching a task, store the `ETag` header value
2. When updating, send the stored ETag in the `If-Match` header
3. If you receive a 412 error, refetch the task and merge changes

```typescript
async function updateTask(id: string, updates: TaskUpdateRequest, etag: string) {
  const response = await fetch(`/api/tools/todo/tasks/${id}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'If-Match': etag,
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(updates)
  });

  if (response.status === 412) {
    throw new Error('Task was modified. Please refresh and try again.');
  }

  const newEtag = response.headers.get('ETag');
  const data = await response.json();
  return { data, etag: newEtag };
}
```
