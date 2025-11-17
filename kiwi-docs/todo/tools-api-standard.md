# Kiwi Tools API Standard (Projects)

Purpose: This is the definitive spec FE can use to refactor code. Field names, enums, request/response shapes, and endpoints below match the latest backend. Swagger annotations were added to controllers and models to keep this in sync.

Base URL: /api
Auth: Bearer token via Authorization header (optional at service level; can be enforced at gateway)
Content types:
- JSON for CRUD
- multipart/form-data for photo upload

Conventions
- Field naming: camelCase (e.g., projectCode, startDate)
- Dates: string YYYY-MM-DD
- Timestamps: ISO 8601 UTC
- Enums: string codes
- Pagination: page (1-based), pageSize; response envelope has items, page, pageSize, total
- Sorting: sortBy (snake_case column), sortOrder (asc|desc)
- Soft archive: archived boolean; includeArchived overrides default filter

Models
- Project
  - id: string (server-generated)
  - projectCode: string (server-generated, e.g., "P-001")
  - name: string (1..100) Required
  - clientName: string (0..100)
  - address: string (0..200)
  - salesPerson: string (0..100)
  - installer: string (0..100)
  - teamMembers: string (free text)
  - startDate: string (YYYY-MM-DD)
  - endDate: string (YYYY-MM-DD)
  - status: string enum one of [not_started, in_progress, completed]
  - todayTask: string
  - progressNote: string
  - createdAt: string (timestamp)
  - archived: boolean (default false)

- ProjectPhoto
  - id: string
  - projectId: string
  - dfsFileId: string (group/path)
  - token: string (download token)
  - contentType: string (e.g., image/jpeg)
  - size: number (bytes)
  - sortOrder: number
  - caption: string
  - createdAt: string (timestamp)

Enum: ProjectStatus
- not_started, in_progress, completed

Error format (recommended for FE handling)
- HTTP status as returned by endpoint, with JSON body (when available):
  { code: string, message: string, details?: object }
Note: Server throws ToolsException with code/message; a global exception handler can return this JSON shape. Some legacy errors might be empty body; FE should primarily rely on HTTP status.

Endpoints
- GET /api/projects
  Query params: q, status, start, end, page, pageSize, sortBy, sortOrder, archived, includeArchived
  Response 200: { items: Project[], page: number, pageSize: number, total: number }

- GET /api/projects/{id}
  Response 200: Project
  Response 404: Not Found

- POST /api/projects
  Body JSON: any subset of Project fields except id/projectCode/createdAt/archived; name recommended; status accepts codes
  Response 201: Project (server fills id, projectCode, createdAt, archived=false)
  Response 400: Validation error

- PUT /api/projects/{id}
  Body JSON: partial semantics; any subset except id/projectCode/createdAt
  Response 200: Project
  Response 400: Validation error
  Response 404: Not Found

- PATCH /api/projects/{id}
  Body JSON: partial semantics; may include archived flag
  Response 200: Project
  Response 400: Validation error
  Response 404: Not Found

- POST /api/projects/{id}/archive?archived=true|false
  Optional body: { archived: boolean } (used if query param absent). Defaults to archived=true if both absent.
  Response 200: Project
  Response 404: Not Found

- DELETE /api/projects/{id}
  Response 204: No Content
  Response 404: Not Found

Photos
- POST /api/projects/{id}/photo
  Content-Type: multipart/form-data; field name: "file" or "photo"
  Accepts only image/*
  Response 200: ProjectPhoto
  Response 400: Validation error (file required)

- GET /api/projects/{id}/photos
  Response 200: ProjectPhoto[] (ordered)

- GET /api/projects/{id}/photo/{token}
  Response 200: binary stream; headers: Cache-Control: max-age=31536000, public; Content-Type inferred from token

- DELETE /api/projects/{id}/photos/{photoId}
  Response 204: No Content
  Response 404: Not Found

Exports
- GET /api/export/excel?start&end&archived&includeArchived
  Response 200: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet (attachment)

- GET /api/export/pdf?start&end&archived&includeArchived
  Response 200: application/pdf (attachment)

Breaking changes for FE
- status values now must be English codes: not_started | in_progress | completed
- photoUrl field removed from Project; use photo endpoints instead
- list endpoint returns page envelope {items, page, pageSize, total}; FE must read fields from items[]
- archived filtering: defaults to active-only; includeArchived=true shows both. archived query param forces archived-only when true, active-only when false.
- sortBy uses snake_case DB columns (e.g., created_at, start_date); previous variants must be updated.
- clientPhone field has been removed from Project model and APIs. Use clientName and address for contact reference.

Examples
- Create
  POST /api/projects
  { "name": "Kitchen remodel", "status": "not_started", "startDate": "2025-10-14" }
  -> 201 { "id": "123", "projectCode": "P-123", "name": "...", "status": "not_started", "archived": false, ... }

- List
  GET /api/projects?page=1&pageSize=20&sortBy=created_at&sortOrder=desc
  -> 200 { "items": [Project,...], "page": 1, "pageSize": 20, "total": 345 }

- Archive
  POST /api/projects/123/archive?archived=true
  -> 200 Project{ archived: true }

Notes
- Swagger annotations added to controllers and models. Import the generated OpenAPI (via springfox if enabled) or rely on this markdown for FE AI refactor prompts.
