# Tools API updates for Frontend (Oct 2025)

This document summarizes recent API additions and behavior changes you may need to adopt on the frontend. It focuses on Project photo management and Projects listing filters.

Last updated: 2025-10-17

## Summary

- New DELETE endpoints for Project photos:
  - DELETE /api/projects/{id}/photo/{token} — delete a photo by its download token (idempotent)
  - DELETE /api/projects/{id}/photos — delete all photos for a project (idempotent)
- Listing behavior change for Projects: when `includeArchived=true`, the list returns only archived projects.
- No breaking changes to existing photo upload, list, download, or delete-by-id endpoints.

---

## Photo management APIs

Base path: `/api`

### Upload a project photo
- POST `/api/projects/{id}/photo`
- Content type: `multipart/form-data`
- Form fields:
  - `file` (preferred) or `photo` — the image file
- Constraints:
  - Only `image/*` content types accepted; max size 5MB (configurable)
- Response: `200 OK` with ProjectPhoto JSON

Response shape:
```json
{
  "id": "pht_123",           // server-generated
  "projectId": "proj_1",
  "dfsFileId": "ftp/2025/10/17/uuid.png",  // internal file id (group/path)
  "token": "<URL-safe token>",
  "contentType": "image/png",
  "size": 2048,
  "sortOrder": 1,
  "caption": null,
  "createdAt": "2025-10-17T10:31:22"
}
```

Example:
```bash
curl -X POST \
  -F "file=@/path/to/photo.png;type=image/png" \
  http://<host>/api/projects/123/photo
```

### List photos for a project
- GET `/api/projects/{id}/photos`
- Response: `200 OK` with an array of ProjectPhoto

Example:
```bash
curl http://<host>/api/projects/123/photos
```

### Download a photo by token
- GET `/api/projects/{id}/photo/{token}`
- Response: `200 OK`, content-type inferred from filename/token
- Cache headers: `Cache-Control: max-age=31536000, public`

Example:
```bash
curl -L http://<host>/api/projects/123/photo/<token> -o photo.png
```

### Delete a photo by id (existing)
- DELETE `/api/projects/{id}/photos/{photoId}`
- Response: `204 No Content`

### Delete a photo by token (new)
- DELETE `/api/projects/{id}/photo/{token}`
- Behavior: Idempotent — returns `204 No Content` whether or not the token exists
- Use this when the client only has the download token

Example:
```bash
curl -X DELETE http://<host>/api/projects/123/photo/<token>
```

### Delete all photos for a project (new)
- DELETE `/api/projects/{id}/photos`
- Behavior: Idempotent — removes all photos for the project; `204 No Content`

Example:
```bash
curl -X DELETE http://<host>/api/projects/123/photos
```

### Error responses (photo upload)
- `400 Bad Request`: missing file
- `415 Unsupported Media Type`: non-image content type
- `413 Payload Too Large`: file exceeds max size

---

## Projects listing: archived filters

Endpoint: GET `/api/projects`

Query params (selected):
- `q`: free-text query
- `status`: `not_started | in_progress | completed`
- `page`: number (1-based), default 1
- `pageSize`: number, default 50, max 200
- `archived`: boolean, filter by archived flag (ignored when `includeArchived=true`)
- `includeArchived`: boolean; when `true`, returns only archived projects (changed behavior)

Behavior (effective archived filter):
- `includeArchived = true`  => only archived projects are returned
- `includeArchived != true` => `archived` flag is applied; if absent, defaults to active only (`archived=false`)

Examples:
```bash
# Active projects only (default)
curl "http://<host>/api/projects?page=1&pageSize=10"

# Only archived projects
curl "http://<host>/api/projects?includeArchived=true&page=1&pageSize=10"

# Explicit active or archived
curl "http://<host>/api/projects?archived=false&page=1&pageSize=10"
curl "http://<host>/api/projects?archived=true&page=1&pageSize=10"
```

Response envelope:
```json
{
  "items": [ /* Project[] */ ],
  "page": 1,
  "pageSize": 10,
  "total": 42
}
```

---

## Frontend impact checklist

- [ ] Add UI affordance to delete a photo by token if you only have the token (call DELETE `/api/projects/{id}/photo/{token}`)
- [ ] Add bulk-delete action for project photos (call DELETE `/api/projects/{id}/photos`)
- [ ] If the archived filter UI uses `includeArchived`, note that `includeArchived=true` now shows only archived items
  - If you want a mixed view (active + archived), use two queries or a different UX; the API does not return mixed sets with a single flag anymore
- [ ] No changes to upload, list, or download flows required

### Optional UI hints
- Use the `token` field from the photo record to build download URLs: `/api/projects/{id}/photo/{token}`
- For delete by id vs token: prefer `photoId` when you already have it from listing; use token-based delete in contexts where only a token is available

---

## Compatibility notes
- Existing endpoints remain unchanged; new delete endpoints are additive
- Download token and public URL patterns are unchanged
- Content type inference uses filename extension in the token; ensure the uploaded file has a correct extension

