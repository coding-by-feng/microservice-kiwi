# Backend API Test Cases Guideline

This document provides a comprehensive guideline for writing backend API test cases based on the frontend API requirements.

## Table of Contents

1. [Authentication APIs](#1-authentication-apis)
2. [Word APIs](#2-word-apis)
3. [Paraphrase APIs](#3-paraphrase-apis)
4. [Word Star List APIs](#4-word-star-list-apis)
5. [Example Star List APIs](#5-example-star-list-apis)
6. [Paraphrase Star List APIs](#6-paraphrase-star-list-apis)
7. [Review APIs](#7-review-apis)
8. [AI APIs](#8-ai-apis)
9. [YouTube APIs](#9-youtube-apis)
10. [Todo APIs](#10-todo-apis)

---

## 1. Authentication APIs

**Base Path:** `/auth`

### 1.1 Login

| Field | Value |
|-------|-------|
| Endpoint | `POST /auth/login` |
| Auth Required | No |
| Content-Type | `application/json` |

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Test Cases:**
- [ ] Valid credentials return 200 with access_token and refresh_token
- [ ] Invalid username returns 401
- [ ] Invalid password returns 401
- [ ] Empty username returns 400
- [ ] Empty password returns 400
- [ ] SQL injection attempt is handled safely

---

### 1.2 Refresh Token

| Field | Value |
|-------|-------|
| Endpoint | `POST /auth/refresh` |
| Auth Required | No |
| Content-Type | `application/json` |

**Request Body:**
```json
{
  "refreshToken": "string"
}
```

**Test Cases:**
- [ ] Valid refresh token returns new access_token
- [ ] Expired refresh token returns 401
- [ ] Invalid refresh token returns 401
- [ ] Missing refresh token returns 400

---

### 1.3 Logout

| Field | Value |
|-------|-------|
| Endpoint | `POST /auth/logout` |
| Auth Required | Yes (Bearer Token) |

**Test Cases:**
- [ ] Valid token successfully logs out (200)
- [ ] Invalid/expired token returns 401
- [ ] Token is invalidated after logout

---

### 1.4 Get Current User

| Field | Value |
|-------|-------|
| Endpoint | `GET /auth/me` |
| Auth Required | Yes (Bearer Token) |

**Test Cases:**
- [ ] Returns current user info with valid token
- [ ] Returns 401 with invalid token
- [ ] Returns 401 with expired token

---

## 2. Word APIs

**Base Path:** `/api/word`

### 2.1 Fuzzy Search Words

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/fuzzy` |
| Auth Required | No |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| query | string | Yes | Search query |
| page | integer | No | Page number (0-indexed) |
| size | integer | No | Page size |

**Test Cases:**
- [ ] Returns matching words with variants
- [ ] Empty query returns empty results or validation error
- [ ] Pagination works correctly
- [ ] Special characters in query are handled
- [ ] Case-insensitive search works

---

### 2.2 Search Words

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/search` |
| Auth Required | No |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| query | string | Yes | Search query |
| page | integer | No | Page number |
| size | integer | No | Page size |

**Test Cases:**
- [ ] Returns matching words by name
- [ ] Pagination works correctly
- [ ] No results returns empty array (not error)

---

### 2.3 Get Word by ID

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/{wordId}` |
| Auth Required | No |

**Test Cases:**
- [ ] Valid ID returns word object
- [ ] Invalid ID returns 404
- [ ] Non-numeric ID returns 400

---

### 2.4 Get Word with Details by ID

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/{wordId}/details` |
| Auth Required | No |

**Test Cases:**
- [ ] Returns word with all related details (paraphrases, examples, pronunciations)
- [ ] Invalid ID returns 404

---

### 2.5 Get Word by Name

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/name/{wordName}` |
| Auth Required | No |

**Test Cases:**
- [ ] Returns word by exact name match
- [ ] URL-encoded names work correctly (e.g., spaces, special chars)
- [ ] Non-existent word returns 404

---

### 2.6 Get Word with Pronunciation

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/name/{wordName}/pronunciation` |
| Auth Required | No |

**Test Cases:**
- [ ] Returns word with pronunciation data
- [ ] Word without pronunciation returns word with empty pronunciation field

---

### 2.7 Check Word Exists

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/exists/{wordName}` |
| Auth Required | No |

**Test Cases:**
- [ ] Returns `true` for existing word
- [ ] Returns `false` for non-existing word

---

### 2.8 Autocomplete Words

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/autocomplete` |
| Auth Required | No |

**Query Parameters:**
| Parameter | Type | Required |
|-----------|------|----------|
| prefix | string | Yes |

**Test Cases:**
- [ ] Returns word suggestions starting with prefix
- [ ] Empty prefix returns validation error or empty list
- [ ] Results are sorted by relevance

---

### 2.9 Delete Word

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/word/{wordId}` |
| Auth Required | Yes (Bearer Token) |

**Test Cases:**
- [ ] Successfully deletes word with valid token
- [ ] Returns 401 without authentication
- [ ] Returns 404 for non-existent word
- [ ] Cascades delete to related entities (or returns error if has dependencies)

---

### 2.10 Evict Word Cache

| Field | Value |
|-------|-------|
| Endpoint | `POST /api/word/cache/evict/{wordName}` |
| Auth Required | No |

**Test Cases:**
- [ ] Successfully evicts cache for word
- [ ] Non-existent word is handled gracefully

---

## 3. Paraphrase APIs

**Base Path:** `/api/word/paraphrase`

### 3.1 Get Paraphrase by ID

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/paraphrase/{paraphraseId}` |
| Auth Required | No |

**Test Cases:**
- [ ] Returns paraphrase object
- [ ] Invalid ID returns 404

---

### 3.2 Get Paraphrases by Word ID

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/paraphrase/word/{wordId}` |
| Auth Required | No |

**Test Cases:**
- [ ] Returns all paraphrases for word
- [ ] Invalid word ID returns 404 or empty array

---

### 3.3 Get Paraphrases by Character

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/paraphrase/character/{characterId}` |
| Auth Required | No |

**Test Cases:**
- [ ] Returns paraphrases matching word character/type

---

### 3.4 Get Paraphrases (Paginated)

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/paraphrase/` |
| Auth Required | No |

**Query Parameters:**
| Parameter | Type | Required |
|-----------|------|----------|
| page | integer | Yes |
| size | integer | Yes |

**Test Cases:**
- [ ] Pagination works correctly
- [ ] Returns total count in response

---

### 3.5 Update Paraphrase

| Field | Value |
|-------|-------|
| Endpoint | `PUT /api/word/paraphrase/{paraphraseId}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Successfully updates paraphrase meaning
- [ ] Returns 401 without auth
- [ ] Returns 404 for non-existent ID

---

### 3.6 Delete Paraphrase

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/word/paraphrase/{paraphraseId}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Successfully deletes paraphrase
- [ ] Returns 401 without auth

---

## 4. Word Star List APIs

**Base Path:** `/api/word/star/list`

### 4.1 Get User's Star Lists

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/star/list/` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Returns all star lists for authenticated user
- [ ] Returns empty array for new user
- [ ] Returns 401 without auth

---

### 4.2 Get Star List by ID

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/star/list/{listId}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Returns star list details
- [ ] Returns 403 if user doesn't own the list
- [ ] Returns 404 for non-existent list

---

### 4.3 Get List Items

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/star/list/{listId}/items` |
| Auth Required | Yes |

**Query Parameters:**
| Parameter | Type | Required |
|-----------|------|----------|
| page | integer | Yes |
| size | integer | Yes |

**Test Cases:**
- [ ] Returns paginated word items in list
- [ ] Empty list returns empty array

---

### 4.4 Create Star List

| Field | Value |
|-------|-------|
| Endpoint | `POST /api/word/star/list/` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Successfully creates new list
- [ ] Duplicate name handling (allow or reject)
- [ ] Returns created list with ID

---

### 4.5 Update Star List

| Field | Value |
|-------|-------|
| Endpoint | `PUT /api/word/star/list/{listId}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Successfully updates list name/description
- [ ] Returns 403 if not owner

---

### 4.6 Delete Star List

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/word/star/list/{listId}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Successfully deletes list and all associations
- [ ] Returns 403 if not owner

---

### 4.7 Add Word to List

| Field | Value |
|-------|-------|
| Endpoint | `POST /api/word/star/list/{listId}/word/{wordId}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Successfully adds word to list
- [ ] Duplicate word in list is handled (ignore or error)
- [ ] Invalid word ID returns 404

---

### 4.8 Remove Word from List

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/word/star/list/{listId}/word/{wordId}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Successfully removes word from list
- [ ] Removing non-existent association is handled

---

## 5. Example Star List APIs

**Base Path:** `/api/word/example/star/list`

### 5.1 Get User's Example Star Lists

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/example/star/list/` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Returns all example star lists for user

---

### 5.2 Get Example Star List by ID

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/example/star/list/{listId}` |
| Auth Required | Yes |

---

### 5.3 Get Example List Items

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/example/star/list/{listId}/items` |
| Auth Required | Yes |

**Query Parameters:** `page`, `size`

---

### 5.4 Create Example Star List

| Field | Value |
|-------|-------|
| Endpoint | `POST /api/word/example/star/list/` |
| Auth Required | Yes |

---

### 5.5 Update Example Star List

| Field | Value |
|-------|-------|
| Endpoint | `PUT /api/word/example/star/list/{listId}` |
| Auth Required | Yes |

---

### 5.6 Delete Example Star List

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/word/example/star/list/{listId}` |
| Auth Required | Yes |

---

### 5.7 Add Example to List

| Field | Value |
|-------|-------|
| Endpoint | `POST /api/word/example/star/list/{listId}/example/{exampleId}` |
| Auth Required | Yes |

---

### 5.8 Remove Example from List

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/word/example/star/list/{listId}/example/{exampleId}` |
| Auth Required | Yes |

---

## 6. Paraphrase Star List APIs

**Base Path:** `/api/word/paraphrase/star/list`

### 6.1 Get Paraphrase Star Lists

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/paraphrase/star/list/` |
| Auth Required | Yes |

---

### 6.2 Get List Items

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/paraphrase/star/list/{listId}/items` |
| Auth Required | Yes |

**Query Parameters:** `page`, `size`

---

### 6.3 Get Review List Items

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/paraphrase/star/list/{listId}/review-items` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Returns items due for review based on spaced repetition

---

### 6.4 Get Remember List Items

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/paraphrase/star/list/{listId}/remember-items` |
| Auth Required | Yes |

---

### 6.5 Add Paraphrase to Star List

| Field | Value |
|-------|-------|
| Endpoint | `POST /api/word/paraphrase/star/list/{listId}/paraphrase/{paraphraseId}` |
| Auth Required | Yes |

---

### 6.6 Remove Paraphrase from Star List

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/word/paraphrase/star/list/{listId}/paraphrase/{paraphraseId}` |
| Auth Required | Yes |

---

### 6.7 Create Paraphrase Star List

| Field | Value |
|-------|-------|
| Endpoint | `POST /api/word/paraphrase/star/list/` |
| Auth Required | Yes |

---

### 6.8 Update Paraphrase Star List

| Field | Value |
|-------|-------|
| Endpoint | `PUT /api/word/paraphrase/star/list/{listId}` |
| Auth Required | Yes |

---

### 6.9 Delete Paraphrase Star List

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/word/paraphrase/star/list/{listId}` |
| Auth Required | Yes |

---

### 6.10 Mark as Remembered

| Field | Value |
|-------|-------|
| Endpoint | `PUT /api/word/paraphrase/star/list/{listId}/paraphrase/{paraphraseId}/remember` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Updates review status/schedule

---

### 6.11 Keep in Mind

| Field | Value |
|-------|-------|
| Endpoint | `PUT /api/word/paraphrase/star/list/{listId}/paraphrase/{paraphraseId}/keep-in-mind` |
| Auth Required | Yes |

---

### 6.12 Mark as Forgotten

| Field | Value |
|-------|-------|
| Endpoint | `PUT /api/word/paraphrase/star/list/{listId}/paraphrase/{paraphraseId}/forget` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Resets review schedule

---

## 7. Review APIs

**Base Path:** `/api/word/review`

### 7.1 Get Review Counter by Type

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/review/counter/{type}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Returns counter for specific review type

---

### 7.2 Increase Review Counter

| Field | Value |
|-------|-------|
| Endpoint | `PUT /api/word/review/counter/increase/{type}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Increments counter by 1
- [ ] Returns updated counter value

---

### 7.3 Get All Review Counters

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/review/counter/all` |
| Auth Required | Yes |

---

### 7.4 Get Review Breakpoint

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/review/breakpoint/{listId}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Returns last reviewed page number for resuming

---

### 7.5 Create Review Days

| Field | Value |
|-------|-------|
| Endpoint | `POST /api/word/review/create-days` |
| Auth Required | Yes |

---

### 7.6 Download Review Audio

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/review/audio/{sourceId}/{type}` |
| Auth Required | Yes |
| Response Type | `blob` (audio file) |

**Test Cases:**
- [ ] Returns audio file as blob
- [ ] Correct content-type header
- [ ] Non-existent audio returns 404

---

### 7.7 Download Character Audio

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/word/review/audio/character/{characterCode}` |
| Auth Required | Yes |
| Response Type | `blob` |

---

### 7.8 Deprecate Review Audio

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/word/review/audio/{sourceId}` |
| Auth Required | Yes |

---

### 7.9 Regenerate Review Audio

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/word/review/audio/regen/{sourceId}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Triggers audio regeneration async

---

## 8. AI APIs

**Base Path:** `/api/ai`

### 8.1 AI Chat Completion

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/v2/{selectedMode}/{targetLanguage}/{nativeLanguage}/{originalText}` |
| Auth Required | Yes |

**Path Parameters:**
| Parameter | Description |
|-----------|-------------|
| selectedMode | AI prompt mode (e.g., translate, explain) |
| targetLanguage | Target language code |
| nativeLanguage | Native language code |
| originalText | Double URL-encoded text |

**Test Cases:**
- [ ] Returns AI-generated response
- [ ] Handles long text properly
- [ ] Rate limiting works
- [ ] Invalid mode returns 400

---

### 8.2 Get AI Call History

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/history` |
| Auth Required | Yes |

**Query Parameters:**
| Parameter | Type | Required |
|-----------|------|----------|
| current | integer | Yes |
| size | integer | Yes |
| filter | string | No |

---

### 8.3 Archive AI History Item

| Field | Value |
|-------|-------|
| Endpoint | `POST /api/ai/history/{id}/archive` |
| Auth Required | Yes |

---

### 8.4 Toggle AI History Favorite

| Field | Value |
|-------|-------|
| Endpoint | `POST /api/ai/history/{id}/favorite` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Toggles favorite status (on/off)

---

### 8.5 Get AI History Favorites

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/history/favorites` |
| Auth Required | Yes |

**Query Parameters:** `current`, `size`

---

### 8.6 Get Archived AI History

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/history/archived` |
| Auth Required | Yes |

**Query Parameters:** `current`, `size`

---

### 8.7 Get AI History by Mode

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/history/mode/{promptMode}` |
| Auth Required | Yes |

**Query Parameters:** `current`, `size`

---

### 8.8 Delete AI History Item

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/ai/history/{id}` |
| Auth Required | Yes |

---

## 9. YouTube APIs

**Base Path:** `/api/ai/ytb`

### 9.1 Get Channel List

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/ytb/channel/page` |
| Auth Required | Yes |

**Query Parameters:** `current`, `size`

---

### 9.2 Submit Channel

| Field | Value |
|-------|-------|
| Endpoint | `POST /api/ai/ytb/channel` |
| Auth Required | Yes |

**Query Parameters:**
| Parameter | Type | Required |
|-----------|------|----------|
| channelLinkOrName | string | Yes |

**Test Cases:**
- [ ] Accepts YouTube channel URL
- [ ] Accepts channel name
- [ ] Validates channel exists

---

### 9.3 Get Channel Videos

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/ytb/channel/{channelId}/videos` |
| Auth Required | Yes |

**Query Parameters:** `current`, `size`

---

### 9.4 Favorite/Unfavorite Channel

| Field | Value |
|-------|-------|
| Favorite | `POST /api/ai/ytb/channel/{channelId}/favorite` |
| Unfavorite | `DELETE /api/ai/ytb/channel/{channelId}/favorite` |
| Auth Required | Yes |

---

### 9.5 Favorite/Unfavorite Video by ID

| Field | Value |
|-------|-------|
| Favorite | `POST /api/ai/ytb/channel/video/{videoId}/favorite` |
| Unfavorite | `DELETE /api/ai/ytb/channel/video/{videoId}/favorite` |
| Check Status | `GET /api/ai/ytb/channel/video/{videoId}/favorite` |
| Auth Required | Yes |

---

### 9.6 Favorite/Unfavorite Video by URL

| Field | Value |
|-------|-------|
| Favorite | `POST /api/ai/ytb/channel/video/favorite?videoUrl={url}` |
| Unfavorite | `DELETE /api/ai/ytb/channel/video/favorite?videoUrl={url}` |
| Check Status | `GET /api/ai/ytb/channel/video/favorite?videoUrl={url}` |
| Auth Required | Yes |

---

### 9.7 Get Favorite Channels

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/ytb/channel/favorites/channels` |
| Auth Required | Yes |

**Query Parameters:** `current`, `size`

---

### 9.8 Get Favorite Videos

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/ytb/channel/favorites/videos` |
| Auth Required | Yes |

**Query Parameters:** `current`, `size`

---

### 9.9 Download Video Scrolling Subtitles

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/ytb/video/subtitles/scrolling` |
| Auth Required | Yes |

**Query Parameters:**
| Parameter | Type | Required |
|-----------|------|----------|
| url | string (URL-encoded) | Yes |

---

### 9.10 Download Video Translated Subtitles

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/ytb/video/subtitles/translated` |
| Auth Required | Yes |

**Query Parameters:**
| Parameter | Type | Required |
|-----------|------|----------|
| url | string | Yes |
| language | string | No |

---

### 9.11 Delete Video Subtitles

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /api/ai/ytb/video/subtitles` |
| Auth Required | Yes |

**Query Parameters:** `url`, `language` (optional)

---

### 9.12 Download Video

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/ytb/video/download` |
| Auth Required | Yes |
| Response Type | `blob` |

**Query Parameters:** `url`

---

### 9.13 Get Video Title

| Field | Value |
|-------|-------|
| Endpoint | `GET /api/ai/ytb/video/title` |
| Auth Required | Yes |

**Query Parameters:** `url`

---

## 10. Todo APIs

**Base Path:** `/todo`

### 10.1 List Tasks

| Field | Value |
|-------|-------|
| Endpoint | `GET /todo/tasks` |
| Auth Required | Yes |

**Query Parameters:** Various filter params

**Test Cases:**
- [ ] Returns user's tasks
- [ ] Filtering works correctly
- [ ] Sorting works correctly

---

### 10.2 Create Task

| Field | Value |
|-------|-------|
| Endpoint | `POST /todo/tasks` |
| Auth Required | Yes |

**Headers:**
| Header | Description |
|--------|-------------|
| Idempotency-Key | Optional, prevents duplicate creation |

**Test Cases:**
- [ ] Creates task successfully
- [ ] Idempotency key prevents duplicates
- [ ] Validation errors return 400

---

### 10.3 Get Task

| Field | Value |
|-------|-------|
| Endpoint | `GET /todo/tasks/{id}` |
| Auth Required | Yes |

---

### 10.4 Update Task

| Field | Value |
|-------|-------|
| Endpoint | `PATCH /todo/tasks/{id}` |
| Auth Required | Yes |

**Headers:**
| Header | Description |
|--------|-------------|
| If-Match | Optional ETag for optimistic locking |

**Test Cases:**
- [ ] Partial update works
- [ ] ETag mismatch returns 412 Precondition Failed

---

### 10.5 Delete Task

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /todo/tasks/{id}` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Moves task to trash (soft delete)

---

### 10.6 Complete Task

| Field | Value |
|-------|-------|
| Endpoint | `POST /todo/tasks/{id}/complete` |
| Auth Required | Yes |

**Request Body:**
```json
{
  "status": "completed"
}
```

**Headers:** `Idempotency-Key` (optional)

---

### 10.7 Reset Task Status

| Field | Value |
|-------|-------|
| Endpoint | `POST /todo/tasks/{id}/reset-status` |
| Auth Required | Yes |

---

### 10.8 Reset All Task Statuses

| Field | Value |
|-------|-------|
| Endpoint | `POST /todo/tasks/reset-statuses` |
| Auth Required | Yes |

---

### 10.9 Demo Seed

| Field | Value |
|-------|-------|
| Endpoint | `POST /todo/tasks/demo` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Creates sample tasks for new user

---

### 10.10 List Trash

| Field | Value |
|-------|-------|
| Endpoint | `GET /todo/trash` |
| Auth Required | Yes |

---

### 10.11 Clear Trash

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /todo/trash` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Permanently deletes all trashed items

---

### 10.12 Delete Trash Item

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /todo/trash/{id}` |
| Auth Required | Yes |

---

### 10.13 Restore Trash Item

| Field | Value |
|-------|-------|
| Endpoint | `POST /todo/trash/{id}/restore` |
| Auth Required | Yes |

---

### 10.14 Get History

| Field | Value |
|-------|-------|
| Endpoint | `GET /todo/history` |
| Auth Required | Yes |

**Query Parameters:**
| Parameter | Type | Required |
|-----------|------|----------|
| date | string (date) | Yes |

---

### 10.15 Delete History

| Field | Value |
|-------|-------|
| Endpoint | `DELETE /todo/history/{id}` |
| Auth Required | Yes |

---

### 10.16 Get Monthly Analytics

| Field | Value |
|-------|-------|
| Endpoint | `GET /todo/analytics/monthly` |
| Auth Required | Yes |

**Query Parameters:**
| Parameter | Type | Default |
|-----------|------|---------|
| months | integer | 6 |

---

### 10.17 Get Analytics Summary

| Field | Value |
|-------|-------|
| Endpoint | `GET /todo/analytics/summary` |
| Auth Required | Yes |

**Query Parameters:**
| Parameter | Type | Format |
|-----------|------|--------|
| month | string | YYYY-MM |

---

### 10.18 Get Current Ranking

| Field | Value |
|-------|-------|
| Endpoint | `GET /todo/ranking/current` |
| Auth Required | Yes |

---

### 10.19 Get Ranking Definitions

| Field | Value |
|-------|-------|
| Endpoint | `GET /todo/ranking/ranks` |
| Auth Required | Yes |

---

### 10.20 Export Todo

| Field | Value |
|-------|-------|
| Endpoint | `GET /todo/export/todo` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Returns exportable JSON/file of all todos

---

### 10.21 Import Todo

| Field | Value |
|-------|-------|
| Endpoint | `POST /todo/import/todo` |
| Auth Required | Yes |

**Test Cases:**
- [ ] Imports todos from data
- [ ] Handles duplicates appropriately
- [ ] Validates import data format

---

## General Test Case Guidelines

### Authentication Tests (for all protected endpoints)
- [ ] Returns 401 when no token provided
- [ ] Returns 401 when invalid token provided
- [ ] Returns 401 when expired token provided
- [ ] Returns 403 when user lacks permission

### Pagination Tests
- [ ] First page returns correct number of items
- [ ] Last page returns remaining items
- [ ] Out-of-range page returns empty array
- [ ] Total count is accurate
- [ ] Page size limits are enforced

### Input Validation Tests
- [ ] Required fields are validated
- [ ] Data types are validated
- [ ] Length limits are enforced
- [ ] Special characters are handled/sanitized
- [ ] SQL injection is prevented
- [ ] XSS is prevented

### Error Response Format
All error responses should follow a consistent format:
```json
{
  "code": "ERROR_CODE",
  "message": "Human readable message",
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/endpoint"
}
```

### Response Headers
- [ ] `Content-Type` is set correctly
- [ ] CORS headers are present for cross-origin requests
- [ ] `ETag` is provided for cacheable resources
- [ ] `Cache-Control` is set appropriately

---

## Test Data Setup

### Required Test Users
1. **Admin User** - Full permissions
2. **Regular User** - Standard permissions
3. **Read-Only User** - View only permissions

### Required Test Data
1. Sample words with paraphrases and examples
2. Star lists with items
3. AI history records
4. Todo tasks in various states

---

## Running Tests

```bash
# Run all API tests
./gradlew test --tests "*ApiTest*"

# Run specific module tests
./gradlew test --tests "WordApiTest"
./gradlew test --tests "AuthApiTest"

# Run with coverage
./gradlew test jacocoTestReport
```

---

*Document generated based on frontend API requirements.*
*Last updated: 2024*
