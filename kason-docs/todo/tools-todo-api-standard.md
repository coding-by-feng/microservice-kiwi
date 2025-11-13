# Kason Tools - Todo API Standard (FE Reference)

Purpose: This spec is for frontend refactor away from local browser-cache-driven logic to a server-source-of-truth pattern. It documents the exact request/response shapes, headers, enums, and conventions implemented by the Todo controller and DTOs.

Base path: /tools/todo
Auth: Bearer token via Authorization header (required; userId comes from auth context)
Content types: application/json; multipart/form-data only for import endpoint

Global response envelope
- All endpoints return a common wrapper R<T>:
  { code: number, msg: string|null, data: T }
  - code: 1=success, 0=fail, negatives are errors; see kason-common ApiContants
  - msg: message (usually null on success)
  - data: actual payload (often another envelope with data/meta)

Inner envelope (Todo endpoints)
- Most Todo responses use an inner envelope with fields: { data: <payload>, meta?: <object> }
- Example list response shape: { code:1, data: { data: TaskDTO[], meta: { page, pageSize, total } } }

Concurrency
- ETag/If-Match (optimistic concurrency):
  - GET /tasks/{id} and POST /tasks return an ETag response header.
  - PATCH /tasks/{id} requires header If-Match with the latest ETag; server returns 412 if mismatched (exposed as error via R wrapper).

Pagination, filtering, sorting
- Common query params on lists:
  - page: number (1-based; default 1)
  - pageSize: number (default 20, max 100)
  - meta in response: { page, pageSize, total }
- Filters on GET /tasks:
  - status: string (pending|success|fail|all). If omitted, defaults to active tasks with current status filter.
  - frequency: string (once|daily|weekly|monthly|custom|all)
  - search: string (applies to title/description)
  - date: string (YYYY-MM-DD) to match created_at date
  - sort: string, one of [points_desc, created_desc, updated_desc] (default points_desc)

Enums
- TaskStatus: pending | success | fail
- Frequency: once | daily | weekly | monthly | custom

Models
- TaskDTO
  - id: string
  - userId: string
  - title: string
  - description: string|null
  - successPoints: number
  - failPoints: number
  - frequency: string (enum Frequency)
  - customDays: number|null
  - status: string (enum TaskStatus)
  - createdAt: string (ISO timestamp)
  - updatedAt: string (ISO timestamp)
  - deletedAt: string|null (ISO timestamp)
  - metadata: object|null
- HistoryRecordDTO
  - id, userId, taskId, title, description, successPoints, failPoints
  - status: string (success|fail)
  - pointsApplied: number
  - completedAt: string (ISO timestamp)
- TrashItemDTO
  - id, title, description, successPoints, failPoints, frequency, customDays, status
  - originalDate: string (ISO timestamp)
  - deletedDate: string (ISO timestamp)
- PageMeta: { page: number, pageSize: number, total: number }
- RankingDTO: { totalPoints: number, currentRank: RankDefinitionDTO, nextRank: RankDefinitionDTO|null, progressPct: number }
- RankDefinitionDTO: { key: string, threshold: number, level: number }

Endpoints
1) Tasks
- GET /tools/todo/tasks
  Query: page, pageSize, status, frequency, search, sort, date
  200 body: R<TaskListResponse>
    data: { data: TaskDTO[], meta: PageMeta }

- POST /tools/todo/tasks
  Body: TaskCreateRequest { title, description?, successPoints?, failPoints?, frequency?, customDays? }
  201 headers: ETag: string
  201 body: R<SingleTaskResponse> -> data: { data: TaskDTO }

- GET /tools/todo/tasks/{id}
  200 headers: ETag: string
  200 body: R<SingleTaskResponse> -> data: { data: TaskDTO }

- PATCH /tools/todo/tasks/{id}
  Headers: If-Match: string (ETag from latest GET/POST)
  Body: TaskUpdateRequest { title?, description?, successPoints?, failPoints?, frequency?, customDays? }
  200 headers: ETag: string (new)
  200 body: R<SingleTaskResponse> -> data: { data: TaskDTO }
  Errors: 412 precondition_failed when ETag mismatch

- DELETE /tools/todo/tasks/{id}
  200 body: R<DeleteOkResponse> -> data: { data: { ok: true } }
  Note: soft delete to trash; use trash endpoints to manage.

- POST /tools/todo/tasks/{id}/complete
  Body: CompleteTaskRequest { status: "success"|"fail" }
  200 body: R<CompleteTaskResponse>
    data: { task: TaskDTO, history: HistoryRecordDTO, ranking: RankingDTO }

- POST /tools/todo/tasks/{id}/reset-status
  200 body: R<SingleTaskResponse> -> data: { data: TaskDTO } (status reset to pending)

- POST /tools/todo/tasks/reset-statuses
  200 body: R<Map<String, Integer>> -> data: { resetCount: number }

- POST /tools/todo/tasks/demo
  200 body: R<DemoSeedResponse> -> data: { tasksCreated: number, historyCreated: 0, trashCreated: 0 }

2) Trash
- GET /tools/todo/trash
  Query: page, pageSize
  200 body: R<TrashListResponse>
    data: { data: TrashItemDTO[], meta: PageMeta }

- DELETE /tools/todo/trash
  200 body: R<ClearTrashResponse> -> data: { deletedCount: number }

- DELETE /tools/todo/trash/{id}
  200 body: R<DeleteOkResponse> -> data: { data: { ok: true } }

- POST /tools/todo/trash/{id}/restore
  200 body: R<SingleTaskResponse> -> data: { data: TaskDTO } (restored as new task, status pending)

3) History
- GET /tools/todo/history
  Query: date: YYYY-MM-DD (required), page?, pageSize?
  200 body: R<HistoryListResponse>
    data: { data: HistoryRecordDTO[], meta: { page, pageSize, total, date } }

- DELETE /tools/todo/history/{id}
  200 body: R<DeleteOkWithRankingMetaResponse>
    data: { data: { ok: true }, meta: { ranking: RankingDTO } }

4) Analytics
- GET /tools/todo/analytics/monthly
  Query: months?: number (default 6, 1..24)
  200 body: R<AnalyticsMonthlyResponse>
    data: { labels: string[], points: number[] }

- GET /tools/todo/analytics/summary
  Query: month: YYYY-MM (required)
  200 body: R<AnalyticsSummaryResponse>
    data: { month: string, totalPoints: number, completedCount: number, successRatePct: number }

5) Ranking
- GET /tools/todo/ranking/current
  200 body: R<RankingResponse> -> data: RankingDTO

- GET /tools/todo/ranking/ranks
  200 body: R<List<RankDefinitionDTO>>

6) Import / Export
- GET /tools/todo/export/todo
  200 body: R<TodoExportResponse>
    data: {
      version: "1.1",
      exportDate: string (ISO timestamp),
      tasks: { [YYYY-MM-DD]: TaskDTO[] },
      history: { [YYYY-MM-DD]: HistoryRecordDTO[] },
      trash: TrashItemDTO[],
      metadata: {
        totalTasks: number,
        totalHistoryRecords: number,
        totalTrashItems: number,
        exportedDates: string[]
      }
    }

- POST /tools/todo/import/todo
  Content-Type: application/json or multipart/form-data (field name: file)
  Headers: (write ops may be rate-limited by IP)
  Body JSON or file contents: an object mirroring the export data structure; accepts any subset of keys
  200 body: R<TodoImportResponse>
    data: { importedTasks: number, importedHistory: number, importedTrash: number, skippedDuplicates: number }

Headers
- Authorization: Bearer <token>
- ETag: string (response header on GET /tasks/{id} and POST /tasks)
- If-Match: string (request header on PATCH /tasks/{id})

Error handling
- Errors are returned using the R wrapper with code != 1 and a message in msg.
- Typical validation errors use msg like "title is required", "month is required", etc.
- Concurrency error (If-Match mismatch) uses msg "ETag mismatch".
- Not found uses msg "Task not found" or "Trash item not found".
- Rate limit (import) raises "Too many requests".
Note: A global exception handler wraps exceptions as R.error(msg) with HTTP 500 in current setup. FE should primarily rely on R.code (1 success; otherwise treat as error) and display msg. Over time, services may return more specific HTTP statuses (e.g., 400/404/412/429); FE should not hardcode on status alone.

FE migration guidance (from local browser cache)
- Treat server as source of truth:
  - Always read list and item data from the API; optionally cache in memory keyed by route filters (page,pageSize,status,frequency,search,date,sort).
  - On edit flows, fetch the item first to obtain the latest ETag, then send PATCH with If-Match and update local cache with the response and new ETag.
  - On create, store returned ETag with the new task.
  - On complete action, update task status and ranking from response.
- Retry/dup protection:
  - If network retries occur, the same request will be safe due to ETag/If-Match.
- Optimistic UI:
  - You may update UI immediately, but reconcile with server response; on 412/ETag mismatch, refetch the task and prompt user to re-apply changes.
- Pagination:
  - Use page/meta from response; avoid client-side slicing.
- Error UX:
  - Show msg from R wrapper for validation or conflict errors; for unknown errors, show a generic message and log details.

Examples
- Create task
  Request: POST /tools/todo/tasks
  { "title": "Read 10 pages", "successPoints": 10, "failPoints": -5, "frequency": "daily" }
  Response: 201 (ETag: "abc123...")
  { "code": 1, "data": { "data": TaskDTO } }

- Update task
  Request: PATCH /tools/todo/tasks/123 (If-Match: "abc123...")
  { "title": "Read 20 pages" }
  Response: 200 (ETag: "def456...")
  { "code": 1, "data": { "data": TaskDTO } }

- Complete task
  Request: POST /tools/todo/tasks/123/complete
  { "status": "success" }
  Response: 200
  { "code": 1, "data": { "task": TaskDTO, "history": HistoryRecordDTO, "ranking": RankingDTO } }

Versioning and compatibility
- Response field names and enums match the backend DTOs in kason-tools-api.
- Export/import uses a data contract intended to be stable across minor versions (export version currently "1.1").

Notes
- Dates passed in query params use server date functions; ensure format is exactly YYYY-MM-DD (lists) or YYYY-MM (summary).
- Sorting values are fixed strings; FE should present them from a constant list.
- Rank definitions are static but served by API; FE should not hardcode thresholds.
