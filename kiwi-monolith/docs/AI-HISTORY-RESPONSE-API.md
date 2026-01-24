# AI Call History - Response Storage & Regenerate API

## Overview

AI responses are now stored with each history record. You can retrieve past responses without re-calling the AI API, and regenerate responses when needed.

---

## Changes to Existing APIs

### GET `/api/ai/history` - List History

**Response now includes `result` field (AI response):**

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 123,
        "userId": 1,
        "promptMode": "directly-translation",
        "language": "en",
        "originalText": "Hello world",
        "result": "The stored AI response text...",
        "favorite": false,
        "archived": false,
        "createTime": "2025-01-25T10:30:00"
      }
    ]
  }
}
```

> **Note:** `result` was previously always `null`. Existing history records will still have `result: null`.

---

### GET `/api/ai/history/{id}` - Get Single History

**Response now includes `aiResponse` field:**

```json
{
  "code": 0,
  "data": {
    "id": 123,
    "userId": 1,
    "aiUrl": null,
    "prompt": "Hello world",
    "promptMode": "directly-translation",
    "targetLanguage": "en",
    "nativeLanguage": "zh",
    "aiResponse": "The stored AI response...",
    "isDelete": false,
    "isArchive": false,
    "isFavorite": false,
    "createTime": "2025-01-25T10:30:00",
    "updateTime": "2025-01-25T10:30:05"
  }
}
```

---

### Streaming Endpoints

**`GET /api/ai/sse/stream`** and **`POST /api/ai/sse/stream`**

No change to request/response format. AI responses are now **automatically saved** after streaming completes when `X-User-Id` header is provided.

---

## New API: Regenerate Response

### `POST /api/ai/sse/regenerate/{historyId}`

Re-calls the AI API with the same prompt/settings from the history record and updates the stored response.

#### Request

| Field | Value |
|-------|-------|
| Method | `POST` |
| Path | `/api/ai/sse/regenerate/{historyId}` |
| Auth | `Authorization: Bearer <token>` (required) |
| Response Type | `text/event-stream` (SSE) |

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `historyId` | Long | The ID of the history record to regenerate |

#### SSE Events

Same format as `/api/ai/sse/stream` endpoint:

```
event: started
data: {"type":"started","message":"AI streaming started","request":{...}}

event: chunk
data: {"type":"chunk","chunk":"Hello","request":{...}}

event: chunk
data: {"type":"chunk","chunk":" world","request":{...}}

event: completed
data: {"type":"completed","message":"AI streaming completed","fullResponse":"Hello world","processingDuration":1234,"request":{...}}
```

#### Error Events

```
event: error
data: {"type":"error","message":"Error description","errorCode":"ERROR_CODE","request":{...}}
```

#### Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `NOT_FOUND` | 200 (SSE) | History record doesn't exist |
| `UNAUTHORIZED` | 200 (SSE) | User doesn't own this history record |
| `VALIDATION_ERROR` | 200 (SSE) | Invalid prompt mode or language in history |
| `STREAMING_ERROR` | 200 (SSE) | AI streaming failed |

---

## Frontend Integration Examples

### JavaScript - Using Fetch API

```javascript
async function regenerateResponse(historyId, token) {
  const response = await fetch(`/api/ai/sse/regenerate/${historyId}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'text/event-stream'
    }
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let fullResponse = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const text = decoder.decode(value);
    const lines = text.split('\n');

    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = JSON.parse(line.slice(6));

        switch (data.type) {
          case 'started':
            console.log('Streaming started');
            break;
          case 'chunk':
            fullResponse += data.chunk;
            // Update UI with chunk
            break;
          case 'completed':
            console.log('Completed:', data.fullResponse);
            console.log('Duration:', data.processingDuration, 'ms');
            break;
          case 'error':
            console.error('Error:', data.errorCode, data.message);
            break;
        }
      }
    }
  }

  return fullResponse;
}
```

### JavaScript - Using EventSource (if CORS allows)

```javascript
function regenerateWithEventSource(historyId) {
  // Note: EventSource doesn't support custom headers
  // You may need to pass token via query parameter or use fetch instead
  const eventSource = new EventSource(`/api/ai/sse/regenerate/${historyId}`);

  eventSource.addEventListener('started', (e) => {
    console.log('Started:', JSON.parse(e.data));
  });

  eventSource.addEventListener('chunk', (e) => {
    const data = JSON.parse(e.data);
    console.log('Chunk:', data.chunk);
  });

  eventSource.addEventListener('completed', (e) => {
    const data = JSON.parse(e.data);
    console.log('Completed:', data.fullResponse);
    eventSource.close();
  });

  eventSource.addEventListener('error', (e) => {
    if (e.data) {
      const data = JSON.parse(e.data);
      console.error('Error:', data.errorCode, data.message);
    }
    eventSource.close();
  });

  return eventSource;
}
```

### React Hook Example

```typescript
import { useState, useCallback } from 'react';

interface UseRegenerateOptions {
  onChunk?: (chunk: string) => void;
  onComplete?: (fullResponse: string, duration: number) => void;
  onError?: (errorCode: string, message: string) => void;
}

function useRegenerate(options: UseRegenerateOptions = {}) {
  const [isLoading, setIsLoading] = useState(false);
  const [response, setResponse] = useState('');

  const regenerate = useCallback(async (historyId: number, token: string) => {
    setIsLoading(true);
    setResponse('');

    try {
      const res = await fetch(`/api/ai/sse/regenerate/${historyId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Accept': 'text/event-stream'
        }
      });

      const reader = res.body!.getReader();
      const decoder = new TextDecoder();
      let accumulated = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const text = decoder.decode(value);
        const lines = text.split('\n');

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = JSON.parse(line.slice(6));

            if (data.type === 'chunk') {
              accumulated += data.chunk;
              setResponse(accumulated);
              options.onChunk?.(data.chunk);
            } else if (data.type === 'completed') {
              options.onComplete?.(data.fullResponse, data.processingDuration);
            } else if (data.type === 'error') {
              options.onError?.(data.errorCode, data.message);
            }
          }
        }
      }
    } finally {
      setIsLoading(false);
    }
  }, [options]);

  return { regenerate, isLoading, response };
}
```

---

## UI Integration Notes

1. **Display Cached Responses**: Show the `result` field from history list immediately without loading state.

2. **Regenerate Button**: Add a "Regenerate" or "Refresh" button on history items that triggers the regenerate API.

3. **Loading State**: During regeneration, show the streaming UI (same as new AI calls) with chunks appearing in real-time.

4. **After Completion**: The history record is automatically updated in the database. Refresh the local state to reflect the new response.

5. **Handle Null Responses**: Existing history records created before this feature will have `result: null`. Display appropriately (e.g., "Response not available - click Regenerate").

6. **Error Handling**: Handle `UNAUTHORIZED` (user doesn't own record) and `NOT_FOUND` (record deleted) gracefully.

---

## Database Migration

The `ai_response` column is added via Liquibase changelog. For dev environment where Liquibase is disabled, run manually:

```sql
ALTER TABLE ai_call_history ADD COLUMN ai_response MEDIUMTEXT;
```
