# AI History: Favorite Feature (FE Integration Guide)

## Overview
Some AI history items are important and users want to “star” them for quick access. This change adds:
- A favorite flag per history item
- An endpoint to toggle favorite
- A filter to list only favorites

Everything else remains backward-compatible.

---

## Data model changes (visible to FE)
- New field on history items: `isFavorite: boolean`
    - Returned in `GET /ai/history` responses as part of each item.

Example item (AiCallHistoryVO):
```json
{
  "id": 123,
  "aiUrl": "/ai/directly-translation/...",
  "prompt": "Hello",
  "promptMode": "DIRECTLY_TRANSLATION",
  "targetLanguage": "zh-CN",
  "nativeLanguage": "en-US",
  "timestamp": "2025-11-11T12:34:56",
  "createTime": "2025-11-11T12:34:57",
  "isFavorite": true
}
```

---

## API surface

### 1) List history (with favorite filter)
- Method: GET
- URL: `/ai/history`
- Query params:
    - `current` (default `1`) – page number
    - `size` (default `20`) – page size (1–100)
    - `filter` (default `normal`) – one of:
        - `normal` – non-archived items only
        - `archived` – archived items only
        - `all` – both archived and non-archived
        - `favorite` – items the user marked as favorite (returns both archived and non-archived favorites)

Response: standard `R<IPage<AiCallHistoryVO>>` wrapper.
- Each record includes `isFavorite`.

Notes:
- Default behavior unchanged: `filter=normal` if not provided.
- `favorite` filter ignores archive status by design; FE can client-filter if needed.

### 2) Toggle favorite on a history item
- Method: PUT
- URL: `/ai/history/{id}/favorite`
- Path param: `id` – history item ID
- Body:
```json
{ "favorite": true }
```
- `true` marks as favorite, `false` unmarks.

Response: standard `R<String>` wrapper with a message:
- On success: `"Marked as favorite"` or `"Unmarked as favorite"`
- On failure: message explaining the error

Auth/ownership:
- Only the owner (current logged-in user) can toggle favorite.

---

## Request/response examples

### List favorites (page 1, size 20)
```http
GET /ai/history?current=1&size=20&filter=favorite HTTP/1.1
Authorization: Bearer <token>
```

### Mark as favorite
```http
PUT /ai/history/123/favorite HTTP/1.1
Content-Type: application/json
Authorization: Bearer <token>

{ "favorite": true }
```

### Unmark favorite
```http
PUT /ai/history/123/favorite HTTP/1.1
Content-Type: application/json
Authorization: Bearer <token>

{ "favorite": false }
```

---

## UI guidance
- History list items:
    - Show a star icon reflecting `isFavorite`.
    - Clicking the star toggles favorite via `PUT /ai/history/{id}/favorite` with `{ "favorite": !isFavorite }`.
    - On success, update the local item’s `isFavorite` and give a small toast.

- Filtering options:
    - Add a “Favorites” filter/tab using `filter=favorite`.
    - Existing filters remain: Normal (default), Archived, All.

- Empty states:
    - Favorites filter: show an empty state when no favorites yet and provide a hint to star items.

- Error handling:
    - If API returns failed with `User not authenticated`, redirect to login.
    - If `Invalid history item ID` or `Failed to update favorite status...`, keep UI state unchanged and show an error toast.

---

## Validation and constraints
- `PUT /ai/history/{id}/favorite` requires:
    - Authenticated user
    - Valid `id > 0`
    - Body must include `{ "favorite": true|false }`

- `GET /ai/history` requires:
    - `current >= 1`
    - `1 <= size <= 100`

---

## Performance
- Optimized by a composite DB index on `(user_id, is_favorite)` to accelerate the favorites view.

---

## Backward compatibility
- Default listing behavior unchanged (`filter=normal`).
- Existing consumers that do not care about favorites are unaffected.

---

## Quick QA checklist
- [ ] Toggling star updates the item immediately without refresh
- [ ] Favorites filter shows only favorited items (archived + non-archived)
- [ ] Normal filter still excludes archived items
- [ ] All filter shows everything
- [ ] Unauthenticated users are redirected when toggling/listing
- [ ] Pagination works correctly under favorites filter
- [ ] Empty state shows when no favorites

---

## Reference
- Toggle endpoint: `PUT /ai/history/{id}/favorite`
- List endpoint: `GET /ai/history?current=1&size=20&filter=favorite`
- Response item field: `isFavorite: boolean`