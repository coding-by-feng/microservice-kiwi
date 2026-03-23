# Review Audio API Specification

## Overview

The audio review system provides TTS-based vocabulary review with spaced repetition tracking. It generates, stores, and serves audio files for paraphrase definitions, and tracks user learning progress (remember/forget/keepInMind).

---

## 1. Review Audio Generation & Management

**Base path:** `/api/word/review`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/audio/generate/list/{listId}` | Generate TTS audio for ALL paraphrases in a star list |
| `POST` | `/audio/generate/review-items/{listId}` | Generate audio only for non-remembered items (isRemember=0) |
| `POST` | `/audio/generate/paraphrase/{paraphraseId}` | Generate audio for a single paraphrase |
| `GET` | `/audio/exists/{paraphraseId}` | Check if audio file exists |
| `GET` | `/audio/download/{paraphraseId}` | Download audio as MP3 stream |
| `DELETE` | `/audio/{paraphraseId}` | Delete audio file |
| `POST` | `/audio/regenerate/{paraphraseId}` | Delete + regenerate audio |
| `GET` | `/audio/config` | Get audio generation config |

### Response Formats

**`/audio/config`**
```json
{
  "enabled": true,
  "maxGenerationCount": 100,
  "contentMode": "both",
  "asyncGeneration": true
}
```

**`/audio/generate/list/{listId}`**
```json
{
  "successCount": 15,
  "failCount": 0,
  "skipCount": 5
}
```

### Known Issues

| Priority | Issue | Location |
|----------|-------|----------|
| High | `Files.readAllBytes()` loads entire audio file into memory — OOM risk for large files | `ReviewAudioService:144`, `WordReviewController:117` |
| Medium | Hardcoded `audio/mpeg` content type — doesn't check actual format | `WordReviewController:123` |
| Medium | `saveAudioFile()` logs errors but returns `false` silently — caller doesn't know why | `ReviewAudioService:335` |
| Medium | Broad `Exception` catch in `generateAudioBytes()` loses error context | `ReviewAudioService:312` |

### Suggested Improvements

- Stream audio files instead of loading into memory (`StreamingResponseBody` or `InputStreamResource`)
- Return proper error messages on save failure instead of silent `false`
- Add retry logic for transient TTS API failures
- Validate storage path exists and is writable on startup

---

## 2. Paraphrase Star List (Collections) CRUD

**Base path:** `/api/word/paraphrase/star/list`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/getCurrentUserList` | Get all user's paraphrase collections |
| `POST` | `/save` | Create new collection (`{listName, remark}`) |
| `PUT` | `/updateById` | Update collection (`{id, listName, remark}`) |
| `DELETE` | `/delById/{id}` | Delete collection |
| `PUT` | `/putIntoStarList` | Add paraphrase to collection (params: `paraphraseId`, `listId`) |
| `DELETE` | `/removeParaphraseStar` | Remove paraphrase from collection (`{paraphraseId, listId}`) |

---

## 3. Review Items & Pagination

**Base path:** `/api/word/paraphrase/star/list`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/getListItems/{size}/{current}/{listId}` | Browse ALL items (paginated, `current` is 0-based) |
| `GET` | `/getReviewListItems/{size}/{current}/{listId}` | Get review items only (isRemember=0) |
| `GET` | `/getRememberListItems/{size}/{current}/{listId}` | Get remembered items (isRemember=1, isKeepInMind=0) |
| `GET` | `/getItemDetail/{paraphraseId}` | Get full paraphrase detail (definitions, examples, pronunciation) |

### Response Format

```json
{
  "records": [ParaphraseVO, ...],
  "pages": 5,
  "current": 0,
  "total": 100
}
```

### Known Issues

| Priority | Issue | Location |
|----------|-------|----------|
| High | **N+1 query**: `toParaphraseVOPage()` calls `findParaphraseVO()` for each relation individually | `ParaphraseController:290` |
| High | Manual pagination loads ALL relations into memory then slices | `ParaphraseController:179-192` |

### Suggested Improvements

- Use a JOIN query or batch fetch to load all ParaphraseVO objects in one query
- Use database-level pagination (LIMIT/OFFSET) instead of loading all + slicing in Java
- Add caching for frequently accessed paraphrase details

---

## 4. Spaced Repetition — Remember / Forget / KeepInMind

**Base path:** `PUT /api/word/paraphrase/star/list`

| Method | Endpoint | Purpose | DB Update |
|--------|----------|---------|-----------|
| `PUT` | `/rememberOne` | Mark as remembered | `isRemember=1`, `rememberTime=NOW` |
| `PUT` | `/keepInMind` | Mark as well-known | `isKeepInMind=1`, `keepInMindTime=NOW` |
| `PUT` | `/forgetOne` | Mark as forgotten | `isRemember=0`, `isKeepInMind=0` |

**Parameters:** `paraphraseId` (required), `listId` (optional for `forgetOne` — null resets in ALL lists)

---

## 5. Review Counter / Tracking

**Base path:** `/api/word/review`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/counter/{type}` | Get counter for type (1=remember, 2=keepInMind, 3=review) |
| `GET` | `/counter/all` | Get all counter types |
| `POST` | `/counter/increase/{type}` | Increment daily counter |
| `GET` | `/breakpoint/{listId}` | Get resume page number for interrupted review |

---

## 6. Pronunciation Audio

**Base path:** `/api/word/pronunciation`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/download/{pronunciationId}` | Download pronunciation audio — **NOT IMPLEMENTED (TODO stub)** |

---

## 7. TTS Configuration

```yaml
kiwi:
  tts:
    provider: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o-mini-tts        # steerable model
      english-voice: alloy           # alloy, echo, fable, onyx, nova, shimmer, coral, sage, ash
      chinese-voice: nova
      english-accent: UK             # US, UK, AU, IN (gpt-4o-mini-tts only)
      response-format: mp3           # mp3, opus, aac, flac, wav, pcm
      speed: 1.0                     # 0.25 to 4.0
  review:
    audio:
      enabled: false                 # master switch
      max-generation-count: 100      # per batch limit
      storage-path: /wordTmp/review-audio
      content-mode: both             # english | chinese | both
      api-call-delay-ms: 200         # rate limit between TTS API calls
      scheduler:
        enabled: false
        cron: "0 0 3 * * *"          # 3 AM daily
        max-per-run: 500
        lookback-days: 30
```

---

## 8. Data Model

### `word_paraphrase_star_rel`

| Field | Type | Description |
|-------|------|-------------|
| `listId` | Integer | FK to star list |
| `paraphraseId` | Integer | FK to paraphrase |
| `createTime` | LocalDateTime | When added to list |
| `isRemember` | Integer (0/1) | Spaced repetition progress |
| `rememberTime` | LocalDateTime | When marked remembered |
| `isKeepInMind` | Integer (0/1) | Mastery flag |
| `keepInMindTime` | LocalDateTime | When marked as mastered |

### Audio File Storage

- **Location:** Local filesystem at `{storage-path}/paraphrase_{paraphraseId}.mp3`
- **Default path:** `/wordTmp/review-audio`

---

## 9. All Known Issues Summary

| # | Priority | Issue | Location |
|---|----------|-------|----------|
| 1 | **High** | N+1 query — each pagination item calls `findParaphraseVO()` separately | `ParaphraseController:290` |
| 2 | **High** | `Files.readAllBytes()` loads entire audio into memory | `ReviewAudioService:144` |
| 3 | **High** | Pronunciation download endpoint is a TODO stub | `PronunciationController:73` |
| 4 | **High** | Raw SQL `last("LIMIT " + limit)` — SQL injection risk | `ReviewAudioScheduler:133` |
| 5 | **Medium** | Silent save failures — logs but doesn't propagate error | `ReviewAudioService:335` |
| 6 | **Medium** | No retry logic for TTS API calls | `OpenAiTtsService` |
| 7 | **Medium** | Manual JSON escape for OpenAI payload — potential injection | `OpenAiTtsService:146` |
| 8 | **Medium** | Thread.sleep() in scheduled job blocks Spring thread pool | `ReviewAudioScheduler:109` |
| 9 | **Low** | `asyncGeneration` config flag is never used | `ReviewAudioProperties:51` |
| 10 | **Low** | Hardcoded `audio/mpeg` content type | `WordReviewController:123` |

---

## 10. Key Source Files

| File | Purpose |
|------|---------|
| `kiwi-monolith/.../api/word/WordReviewController.java` | Audio generation & download endpoints |
| `kiwi-monolith/.../api/word/ParaphraseController.java` | Star list CRUD, pagination, review tracking |
| `kiwi-monolith/.../api/word/PronunciationController.java` | Pronunciation endpoints (download TODO) |
| `kiwi-monolith/.../domain/word/service/ReviewAudioService.java` | TTS generation, file storage, batch processing |
| `kiwi-monolith/.../common/tts/OpenAiTtsService.java` | OpenAI TTS API client |
| `kiwi-monolith/.../domain/word/scheduler/ReviewAudioScheduler.java` | Nightly batch audio generation |
| `kiwi-monolith/.../domain/word/config/ReviewAudioProperties.java` | Audio config properties |
| `kiwi-monolith/src/main/resources/application-dev.yml` | TTS & review audio config |
