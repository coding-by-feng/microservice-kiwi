# YouTube Favorites – Frontend Integration Guide

Updated APIs introduce channel/video favorites and related lists. This guide describes endpoints, response shapes, and concrete UI changes for the frontend to adopt.

## Auth and envelope
- Auth: use the same auth mechanism as existing AI/YouTube endpoints (e.g., Bearer token or session as configured via gateway). 401/403 should redirect to login.
- Standard response wrapper R<T>:
```
{
  "code": 1,            // 1=success, 0/-X=failure
  "msg": "string|null", // optional human-readable message
  "data": T              // payload
}
```
- Pagination envelope (IPage) used by list endpoints:
```
{
  "records": [ ... ],
  "total": 42,
  "size": 10,
  "current": 1
}
```

Result codes come from backend constants: SUCCESS=1, FAIL=0, errors are negative. Treat code===1 as success.

## VO shapes used in lists
YtbChannelVO:
```
{
  "channelName": "string",
  "channelId": 123,           // long
  "status": 0|1|2,            // backend-defined; treat as opaque for now
  "favorited": true|false,
  "favoriteCount": 0          // long
}
```

YtbChannelVideoVO:
```
{
  "id": 456,                  // video id (long)
  "videoTitle": "string",
  "videoLink": "https://...",
  "publishedAt": "2025-10-21T15:30:00" | null, // ISO-8601
  "status": 0|1|2,
  "favorited": true|false,
  "favoriteCount": 0
}
```
Note: see `kiwi-docs/ytb/youtube-video-publishedAt-api-requirements.md` for date handling guidance.

## Endpoints
Base path: `/ai/ytb/channel`

1) Submit a YouTube channel
- POST `/ai/ytb/channel`
- Params: `channelLinkOrName` (form or query, non-empty)
- Response: `R<Long>` where data is created/queued channel ID
- Errors: empty param -> `R.failed("Channel link or name cannot be empty")`

2) Favorite a channel
- POST `/ai/ytb/channel/{channelId}/favorite`
- Response: `R<Boolean>` (true on success)
- Errors: missing/invalid id -> `R.failed("Channel ID cannot be empty")`

3) Unfavorite a channel
- DELETE `/ai/ytb/channel/{channelId}/favorite`
- Response: `R<Boolean>`
- Errors: missing/invalid id -> `R.failed("Channel ID cannot be empty")`

4) Favorite a video
- POST `/ai/ytb/channel/video/{videoId}/favorite`
- Response: `R<Boolean>`
- Errors: missing/invalid id -> `R.failed("Video ID cannot be empty")`

5) Unfavorite a video
- DELETE `/ai/ytb/channel/video/{videoId}/favorite`
- Response: `R<Boolean>`
- Errors: missing/invalid id -> `R.failed("Video ID cannot be empty")`

6) List favorite channels (paginated)
- GET `/ai/ytb/channel/favorites/channels?current=1&size=10`
- Response: `R<IPage<YtbChannelVO>>`

7) List favorite videos (paginated)
- GET `/ai/ytb/channel/favorites/videos?current=1&size=10`
- Response: `R<IPage<YtbChannelVideoVO>>`

8) User's own channel page (existing but relevant for heart state)
- GET `/ai/ytb/channel/page?current=1&size=10`
- Response: `R<IPage<YtbChannelVO>>`

9) Videos by channel (existing but relevant for heart state)
- GET `/ai/ytb/channel/{channelId}/videos?current=1&size=10`
- Response: `R<IPage<YtbChannelVideoVO>>`

Pagination validation: BE enforces 1 <= size <= 100; current >= 1.

## Sample requests (zsh)
Submit a channel:
```
curl -X POST "$BASE/ai/ytb/channel" \
  -H "Authorization: Bearer $TOKEN" \
  --data-urlencode "channelLinkOrName=https://www.youtube.com/@SomeChannel"
```

Favorite/unfavorite a channel:
```
curl -X POST "$BASE/ai/ytb/channel/123/favorite" -H "Authorization: Bearer $TOKEN"
curl -X DELETE "$BASE/ai/ytb/channel/123/favorite" -H "Authorization: Bearer $TOKEN"
```

Favorite/unfavorite a video:
```
curl -X POST "$BASE/ai/ytb/channel/video/456/favorite" -H "Authorization: Bearer $TOKEN"
curl -X DELETE "$BASE/ai/ytb/channel/video/456/favorite" -H "Authorization: Bearer $TOKEN"
```

List favorites:
```
curl "$BASE/ai/ytb/channel/favorites/channels?current=1&size=10" -H "Authorization: Bearer $TOKEN"
curl "$BASE/ai/ytb/channel/favorites/videos?current=1&size=10" -H "Authorization: Bearer $TOKEN"
```

## Frontend adjustments

UI elements
- Add a heart/favorite toggle to:
  - Channel list items (e.g., user channels page and favorites list)
  - Video list items (e.g., channel videos page and favorites list)
- Display `favoriteCount` next to the heart (optional for mobile to reduce clutter).
- Reflect `favorited` state to set the heart filled/outlined.

Navigation
- Add two dedicated views/tabs:
  - “My Favorite Channels” -> `/ai/ytb/channel/favorites/channels`
  - “My Favorite Videos” -> `/ai/ytb/channel/favorites/videos`
  Both are paginated and should reuse existing list components.

Behavior
- Optimistic toggle: update UI immediately on click, then call API. If API fails, revert and surface error toast using `msg` or a generic message.
- Disable the toggle while the request is pending to prevent double-click races.
- Idempotency: multiple favorites should not increase count multiple times; trust server state on response.
- Keep counts in sync:
  - On favorite: `favorited=true`, `favoriteCount += 1` (pessimistic: refetch row if unsure)
  - On unfavorite: `favorited=false`, `favoriteCount -= 1` but not below 0
  - When the same item appears in multiple lists, propagate the updated state via store or by invalidating affected queries.
- Error handling: check `code !== 1` or network errors; show `msg` if present.
- Pagination: pass `current` and `size`. Respect backend constraints (size 1..100). Use infinite scroll or pager.
- Dates: render `publishedAt` in user locale or relative time; handle null as “Unknown”.

State management
- Recommended to keep a normalized cache keyed by `channelId` and `videoId` for cross-list sync of `favorited` and `favoriteCount`.
- If using SWR/React Query/RTK Query, invalidate the affected queries after toggle or update cache directly.

Accessibility/i18n
- Buttons must be accessible (aria-pressed reflects favorited state). Provide i18n strings for tooltips/labels.

Empty/loading/error states
- Channels/videos favorites lists should show empty states when `total===0`.
- Use skeletons/placeholders while loading.
- Show inline retry when fetch fails.

## TypeScript helpers (optional)
```
export interface R<T> { code: number; msg?: string | null; data: T }
export interface Page<T> { records: T[]; total: number; size: number; current: number }
export interface YtbChannelVO { channelName: string; channelId: number; status: number; favorited: boolean; favoriteCount: number }
export interface YtbChannelVideoVO { id: number; videoTitle: string; videoLink: string; publishedAt?: string | null; status: number; favorited: boolean; favoriteCount: number }
```

## Acceptance checklist
- [ ] Channel list shows heart + count; toggling calls POST/DELETE and persists.
- [ ] Video list shows heart + count; toggling calls POST/DELETE and persists.
- [ ] Favorites pages load with pagination and correct envelopes.
- [ ] Optimistic updates with rollback on failure.
- [ ] Counts never drop below zero; synced across lists.
- [ ] 401/403 flows redirect to login.
- [ ] Date rendering for videos matches locale and null-safe handling.

## Notes
- Backend validates `current>=1` and `1<=size<=100` and logs server-side errors. On unexpected failure, retry with backoff or prompt user.
- Status codes/enums are currently opaque; only use them for styling if you own the mapping.

