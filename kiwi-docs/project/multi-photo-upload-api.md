# Project Photos: Multiple Upload API

This document describes the new API for uploading multiple photos to a project and the expected client-side changes.

Base path: `/rangi_windows/api`

## Upload multiple photos

- Method: `POST`
- URL: `/projects/{id}/photos`
- Content-Type: `multipart/form-data`
- Auth: optional (gateway may enforce)
- Fields (any of these forms are accepted):
  - `files`: array of image files (e.g., `files[]`)
  - `photos`: array of image files (e.g., `photos[]`)
  - Repeated single fields: `file` and/or `photo` can be repeated
- Constraints:
  - MIME type must start with `image/`
  - Max single file size: configured via `tools.photo-max-size` (currently 5MB)
  - Server also enforces Spring multipart limits (`spring.servlet.multipart.*`)

### Response

`200 OK` with JSON array of `ProjectPhotoResponse` items:

Each item structure:
- `id`: string, photo id
- `projectId`: string, project id
- `dfsFileId`: string, DFS id (group/path)
- `token`: string, opaque download token
- `contentType`: string, e.g., `image/jpeg`
- `size`: number, bytes
- `sortOrder`: number | null
- `caption`: string | null
- `createdAt`: ISO 8601 timestamp

Example response:
[
  {
    "id": "pht_123",
    "projectId": "42",
    "dfsFileId": "group1/M00/00/01/xxx.jpg",
    "token": "AbCdEf...",
    "contentType": "image/jpeg",
    "size": 204800,
    "sortOrder": 1,
    "caption": null,
    "createdAt": "2025-11-15T12:34:56"
  }
]

### Frontend examples

Browser fetch with FormData (multiple selectors allowed):

```js
async function uploadProjectPhotos(projectId, files) {
  const form = new FormData();
  // Preferred: use the plural field
  for (const f of files) form.append('files', f);
  const res = await fetch(`/rangi_windows/api/projects/${projectId}/photos`, {
    method: 'POST',
    body: form,
  });
  if (!res.ok) throw new Error(`Upload failed: ${res.status}`);
  return res.json(); // returns array of ProjectPhotoResponse
}
```

curl example:

```bash
curl -X POST \
  -F "files=@/path/pic1.jpg" \
  -F "files=@/path/pic2.png" \
  http://localhost:9005/rangi_windows/api/projects/42/photos
```

Alternate forms (all accepted):
- `photos[]` as the field name
- Repeated `file` or `photo` fields

## Single upload (backward compatible)

- Method: `POST`
- URL: `/projects/{id}/photo`
- Fields: `file` or `photo`
- Response: single `ProjectPhotoResponse` object

## Download by token (unchanged)

- Method: `GET`
- URL: `/projects/{id}/photo/{token}`
- Streams inline, sets `Content-Type` based on file name, plus:
  - `Cache-Control: max-age=31536000, public`

Example:

```bash
curl -L -o out.jpg \
  http://localhost:9005/rangi_windows/api/projects/42/photo/AbCdEf
```

## Server-side limits and errors

- Payload too large (per-file): `413 Payload Too Large`
- Unsupported media type (non-image): `415 Unsupported Media Type`
- Missing file(s): `400 Bad Request`

Tune via `application.yml`:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 6MB

tools:
  photo-max-size: 5MB # per-file
```

