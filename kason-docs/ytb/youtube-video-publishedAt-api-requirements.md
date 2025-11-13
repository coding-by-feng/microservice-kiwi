# YouTube Video PublishedAt – API Requirements (FE handoff)

Last updated: 2025-10-22
Owner: AI/YouTube Integrations
Scope: Add video publication datetime to channel video list and standardize sorting

## TL;DR for FE
- New field in channel video list: `publishedAt` (ISO-8601 datetime string, nullable)
- Backend now sorts videos by `publishedAt` DESC, then `createTime` DESC as a fallback
- Endpoint unchanged: GET `/ai/ytb/channel/{channelId}/videos?current=1&size=10`
- No request changes needed; consume the new field for display and optional client-side fallback sorting
- When `publishedAt` is null, show “Unknown” or omit the date

---

## What changed (non-breaking)
- Added DB column: `ytb_channel_video.published_at` (DATETIME, nullable)
- Added response field: `publishedAt` to `YtbChannelVideoVO`
- Server-side sorting: `ORDER BY published_at DESC, create_time DESC`
- Backfill behavior: during channel sync, we try to populate `published_at` for existing videos

### Database
- Table: `ytb_channel_video`
- Column: `published_at DATETIME NULL` (new)
- Indexes: `(channel_id, published_at)`, and on `published_at`
- Migration: `kason-ai/kason-ai-biz/src/main/resources/db/changelog/changes/0205-ytb-channel-video-published-at.xml`
  - Included by: `db/changelog/db.changelog-master.xml`

### Backend data sources for publication time
Order of attempts when syncing/ingesting videos:
1) YouTube Data API v3: `snippet.publishedAt` (RFC 3339/ISO-8601)
2) Fallback via yt-dlp:
   - Command: `yt-dlp --print '%(release_timestamp|timestamp|upload_date)s' <videoUrl>`
   - Parse precedence:
     - Epoch seconds (10 or 13 digits; 13 treated as ms)
     - `YYYYMMDD` (interpreted as start of day, local time)
     - ISO-8601/RFC-3339 (attempted parse)

Notes:
- Stored server-side type is `LocalDateTime` (no timezone). FE should present this in local timezone or with relative formatting.
- Some rare videos may not expose a timestamp via API or yt-dlp; we leave `publishedAt = null`.

---

## Endpoint and response

GET `/ai/ytb/channel/{channelId}/videos?current=1&size=10`

Query params:
- `current` (default 1): page number
- `size` (default 10): page size (1–100)

Sorting:
- Applied on server: `publishedAt DESC, createTime DESC`

Response item (YtbChannelVideoVO):
```
{
  "id": 1234567890123,                     // long
  "videoTitle": "Intro to Neural Nets",   // string
  "videoLink": "https://www.youtube.com/watch?v=XXXXXXXXXXX", // string
  "publishedAt": "2025-10-21T15:30:00",   // ISO-8601, may be null
  "status": 2                               // 0=ready,1=processing,2=finish
}
```

Envelope (IPage):
```
{
  "records": [ YtbChannelVideoVO, ... ],
  "total": 42,
  "size": 10,
  "current": 1
}
```

Backward compatibility:
- This is an additive change. Existing consumers that ignore unknown fields remain unaffected.

---

## FE guidance
- Display publication datetime using local timezone or relative time (e.g., “3 days ago”).
- When `publishedAt` is null, display “Unknown” or omit the date.
- Rely on backend ordering for the default view; if you also sort client-side, prefer `publishedAt` with a nulls-last strategy.
- Consider exposing a date filter/chip (e.g., “Last 7 days”, “This year”) powered by the existing paginated API if needed.

Accessibility/i18n:
- Ensure date formatting respects user locale/timezone.

---

## Operational notes (BE)
- Channel sync service now sets/updates `published_at` per video using the sources described above.
- Existing rows without `published_at` will be backfilled on next sync.
- yt-dlp invocation is best-effort and runs only when API data is missing.

Troubleshooting:
- If a video lacks `published_at` after sync, it likely failed both API and yt-dlp extraction; inspect logs with the video ID/URL.

---

## Testing checklist
- [ ] New records: `publishedAt` populated when channel is first synced
- [ ] Existing records: `publishedAt` backfilled on resync
- [ ] Sorting: videos appear newest-first by `publishedAt`, then `createTime`
- [ ] Null handling: UI behaves gracefully when `publishedAt` is missing

---

## Example yt-dlp fallback (for reference)
```
yt-dlp --print '%(release_timestamp|timestamp|upload_date)s' https://www.youtube.com/watch?v=XXXXXXXXXXX
```
Interpretation precedence:
- 10/13-digit epoch => convert to seconds; 13-digit treated as ms
- 8-digit `YYYYMMDD` => start of day (local time)
- Else attempt ISO-8601 parse

