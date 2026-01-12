# Review Audio Mode API Documentation

## Overview

This document describes the API for the **Review Audio Mode** feature, which enables audio-based vocabulary review using Text-to-Speech (TTS) technology. Frontend applications can use these APIs to generate and play audio for paraphrases in user's star lists.

## Base URL

```
/api/word/review
```

## Authentication

All endpoints (except audio download) require authentication via JWT token in the `Authorization` header:

```
Authorization: Bearer <jwt_token>
```

---

## Audio Generation Endpoints

### 1. Generate Audio for Star List

Generates audio files for all paraphrases in a user's star list. This is the primary endpoint for batch audio generation.

**Endpoint:** `POST /audio/generate/list/{listId}`

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| listId | Integer | Path | Yes | The star list ID |

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "enabled": true,
    "totalRequested": 50,
    "successCount": 45,
    "failedCount": 2,
    "skippedCount": 3,
    "remainingCount": 0,
    "maxGenerationCount": 100,
    "successIds": [101, 102, 103, ...],
    "failedIds": [104, 105],
    "skippedIds": [106, 107, 108]
  }
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| enabled | Boolean | Whether audio generation is enabled on the server |
| totalRequested | Integer | Total paraphrases in the list |
| successCount | Integer | Number of successfully generated audio files |
| failedCount | Integer | Number of failed generations |
| skippedCount | Integer | Number skipped (audio already exists) |
| remainingCount | Integer | Number not processed (exceeds max limit) |
| maxGenerationCount | Integer | Maximum allowed per request (default: 100) |
| successIds | Array | List of successfully generated paraphrase IDs |
| failedIds | Array | List of failed paraphrase IDs |
| skippedIds | Array | List of skipped paraphrase IDs |

**Example Request:**
```javascript
const response = await fetch('/api/word/review/audio/generate/list/123', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
const result = await response.json();
```

---

### 2. Generate Audio for Review Items Only

Generates audio only for paraphrases that haven't been marked as "remembered" yet. Useful for preparing audio for the current review session.

**Endpoint:** `POST /audio/generate/review-items/{listId}`

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| listId | Integer | Path | Yes | The star list ID |

**Response:** Same as "Generate Audio for Star List"

---

### 3. Generate Audio for Single Paraphrase

Generates audio for a single paraphrase. Useful for on-demand generation.

**Endpoint:** `POST /audio/generate/paraphrase/{paraphraseId}`

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| paraphraseId | Integer | Path | Yes | The paraphrase ID |

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": true
}
```

---

## Audio Playback Endpoints

### 4. Check if Audio Exists

Check whether audio has been generated for a paraphrase.

**Endpoint:** `GET /audio/exists/{paraphraseId}`

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| paraphraseId | Integer | Path | Yes | The paraphrase ID |

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": true
}
```

---

### 5. Download/Stream Audio

Download or stream the audio file for a paraphrase. This endpoint returns the raw audio data.

**Endpoint:** `GET /audio/download/{paraphraseId}`

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| paraphraseId | Integer | Path | Yes | The paraphrase ID |

**Response:**
- **Content-Type:** `audio/mpeg`
- **Body:** Raw MP3 audio data

**HTTP Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Audio found and returned |
| 404 | Audio not found for this paraphrase |
| 500 | Server error |

**Example Usage (HTML5 Audio):**
```html
<audio id="reviewAudio" controls>
  <source src="/api/word/review/audio/download/12345" type="audio/mpeg">
</audio>
```

**Example Usage (JavaScript):**
```javascript
const audio = new Audio(`/api/word/review/audio/download/${paraphraseId}`);
audio.play();
```

---

## Audio Management Endpoints

### 6. Delete Audio

Delete the audio file for a paraphrase.

**Endpoint:** `DELETE /audio/{paraphraseId}`

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| paraphraseId | Integer | Path | Yes | The paraphrase ID |

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": true
}
```

---

### 7. Regenerate Audio

Delete existing audio and generate a new one. Useful when the paraphrase content has been updated.

**Endpoint:** `POST /audio/regenerate/{paraphraseId}`

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| paraphraseId | Integer | Path | Yes | The paraphrase ID |

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": true
}
```

---

### 8. Get Audio Configuration

Get the current audio generation configuration/limits.

**Endpoint:** `GET /audio/config`

**Response:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "enabled": true,
    "maxGenerationCount": 100,
    "contentMode": "both",
    "asyncGeneration": true
  }
}
```

**Configuration Fields:**
| Field | Type | Description |
|-------|------|-------------|
| enabled | Boolean | Whether audio generation is enabled |
| maxGenerationCount | Integer | Max audio files per generation request |
| contentMode | String | Audio content: "english", "chinese", or "both" |
| asyncGeneration | Boolean | Whether generation is async |

---

## Frontend Implementation Guide

### Audio Review Mode Flow

```
┌─────────────────┐
│  User enters    │
│  Review Mode    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Check if audio  │◄──── GET /audio/config
│ mode is enabled │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Generate audio  │◄──── POST /audio/generate/review-items/{listId}
│ for review items│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Display review │
│  card with audio│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  User clicks    │
│  play button    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Play audio     │◄──── GET /audio/download/{paraphraseId}
│                 │
└─────────────────┘
```

### React Component Example

```jsx
import React, { useState, useRef, useEffect } from 'react';

const ReviewAudioPlayer = ({ paraphraseId, listId }) => {
  const [hasAudio, setHasAudio] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const audioRef = useRef(null);

  // Check if audio exists on mount
  useEffect(() => {
    checkAudioExists();
  }, [paraphraseId]);

  const checkAudioExists = async () => {
    try {
      const response = await fetch(`/api/word/review/audio/exists/${paraphraseId}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      });
      const result = await response.json();
      setHasAudio(result.data);
    } catch (error) {
      console.error('Failed to check audio:', error);
    }
  };

  const generateAudio = async () => {
    setIsLoading(true);
    try {
      const response = await fetch(`/api/word/review/audio/generate/paraphrase/${paraphraseId}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${getToken()}` }
      });
      const result = await response.json();
      if (result.data) {
        setHasAudio(true);
      }
    } catch (error) {
      console.error('Failed to generate audio:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const playAudio = () => {
    if (audioRef.current) {
      audioRef.current.play();
      setIsPlaying(true);
    }
  };

  const pauseAudio = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      setIsPlaying(false);
    }
  };

  return (
    <div className="audio-player">
      {hasAudio ? (
        <>
          <audio
            ref={audioRef}
            src={`/api/word/review/audio/download/${paraphraseId}`}
            onEnded={() => setIsPlaying(false)}
          />
          <button onClick={isPlaying ? pauseAudio : playAudio}>
            {isPlaying ? '⏸️ Pause' : '▶️ Play'}
          </button>
        </>
      ) : (
        <button onClick={generateAudio} disabled={isLoading}>
          {isLoading ? '⏳ Generating...' : '🔊 Generate Audio'}
        </button>
      )}
    </div>
  );
};

export default ReviewAudioPlayer;
```

### Vue Component Example

```vue
<template>
  <div class="audio-player">
    <template v-if="hasAudio">
      <audio
        ref="audioRef"
        :src="audioUrl"
        @ended="isPlaying = false"
      />
      <button @click="togglePlay">
        {{ isPlaying ? '⏸️ Pause' : '▶️ Play' }}
      </button>
    </template>
    <template v-else>
      <button @click="generateAudio" :disabled="isLoading">
        {{ isLoading ? '⏳ Generating...' : '🔊 Generate Audio' }}
      </button>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';

const props = defineProps({
  paraphraseId: { type: Number, required: true }
});

const hasAudio = ref(false);
const isPlaying = ref(false);
const isLoading = ref(false);
const audioRef = ref(null);

const audioUrl = computed(() =>
  `/api/word/review/audio/download/${props.paraphraseId}`
);

onMounted(() => {
  checkAudioExists();
});

async function checkAudioExists() {
  try {
    const response = await fetch(`/api/word/review/audio/exists/${props.paraphraseId}`);
    const result = await response.json();
    hasAudio.value = result.data;
  } catch (error) {
    console.error('Failed to check audio:', error);
  }
}

async function generateAudio() {
  isLoading.value = true;
  try {
    const response = await fetch(
      `/api/word/review/audio/generate/paraphrase/${props.paraphraseId}`,
      { method: 'POST' }
    );
    const result = await response.json();
    if (result.data) {
      hasAudio.value = true;
    }
  } catch (error) {
    console.error('Failed to generate audio:', error);
  } finally {
    isLoading.value = false;
  }
}

function togglePlay() {
  if (audioRef.value) {
    if (isPlaying.value) {
      audioRef.value.pause();
    } else {
      audioRef.value.play();
    }
    isPlaying.value = !isPlaying.value;
  }
}
</script>
```

---

## Batch Generation Best Practices

### 1. Pre-generate Audio Before Review Session

When the user selects a star list for review, pre-generate audio for all items:

```javascript
async function prepareReviewSession(listId) {
  // Show loading indicator
  showLoading('Preparing audio for review...');

  try {
    const response = await fetch(`/api/word/review/audio/generate/review-items/${listId}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    });

    const result = await response.json();

    if (result.data.remainingCount > 0) {
      // There are more items than the max limit allows
      showWarning(`Generated audio for ${result.data.successCount} items. ${result.data.remainingCount} items remaining.`);
    }

    if (result.data.failedCount > 0) {
      showWarning(`Failed to generate audio for ${result.data.failedCount} items.`);
    }

    return result.data;
  } finally {
    hideLoading();
  }
}
```

### 2. Handle Rate Limiting

The backend automatically adds delays between API calls. However, if you're generating audio one at a time, add a small delay:

```javascript
async function generateMultipleAudios(paraphraseIds) {
  for (const id of paraphraseIds) {
    await generateAudio(id);
    await sleep(200); // 200ms delay
  }
}
```

### 3. Progressive Loading

For lists with many items, show progress:

```javascript
async function generateWithProgress(listId, onProgress) {
  const result = await fetch(`/api/word/review/audio/generate/list/${listId}`, {
    method: 'POST'
  });

  const data = await result.json();
  const total = data.data.totalRequested;
  const processed = data.data.successCount + data.data.failedCount + data.data.skippedCount;

  onProgress({
    progress: (processed / total) * 100,
    message: `Generated ${data.data.successCount} of ${total} audio files`
  });

  return data.data;
}
```

---

## Audio Content Modes

The server can generate audio with different content:

| Mode | Description | Audio Contains |
|------|-------------|----------------|
| `english` | English only | Paraphrase English text |
| `chinese` | Chinese only | Chinese meaning |
| `both` | Both languages | English text followed by Chinese meaning |

The content mode is configured on the server. Use `GET /audio/config` to check the current mode.

---

## Error Handling

### Common Error Responses

```json
{
  "code": 401,
  "msg": "User not authenticated",
  "data": null
}
```

```json
{
  "code": 404,
  "msg": "Audio not found",
  "data": null
}
```

```json
{
  "code": 500,
  "msg": "Failed to generate audio",
  "data": null
}
```

### Frontend Error Handling

```javascript
async function handleAudioRequest(url, options = {}) {
  try {
    const response = await fetch(url, options);

    if (!response.ok) {
      if (response.status === 401) {
        // Redirect to login
        router.push('/login');
        return null;
      }
      if (response.status === 404) {
        // Audio not found - might need generation
        return { notFound: true };
      }
      throw new Error(`HTTP ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Audio request failed:', error);
    showError('Failed to process audio request');
    return null;
  }
}
```

---

## Performance Considerations

1. **Audio files are cached on the server** - Once generated, audio is stored and reused
2. **Skipped items don't count against the limit** - If audio already exists, it won't be regenerated
3. **Use batch generation** - Generating audio for a whole list is more efficient than one at a time
4. **Pre-generate during idle time** - Generate audio when the user adds items to their star list
5. **Background scheduler** - The server can automatically generate audio in the background (see below)

---

## Background Audio Generation (Scheduler)

The backend includes a configurable scheduled job that automatically generates audio for recently added paraphrases. This runs in the background without requiring any frontend interaction.

### How It Works

1. The scheduler runs at a configurable time (default: 3:00 AM daily)
2. It finds paraphrases added within a configurable lookback period (default: 30 days)
3. It generates audio for paraphrases that don't have audio yet
4. The number of audio files generated per run is limited (default: 500)

### Server Configuration (YAML)

```yaml
kiwi:
  review:
    audio:
      enabled: true
      max-generation-count: 100        # Max per API request
      storage-path: /path/to/audio     # Where audio files are stored
      async-generation: true           # Async processing
      content-mode: both               # "english", "chinese", or "both"
      api-call-delay-ms: 200           # Delay between TTS API calls
      scheduler:
        enabled: false                 # Enable/disable scheduler
        cron: "0 0 3 * * *"           # Cron expression (3:00 AM daily)
        max-per-run: 500              # Max audio files per scheduled run
        lookback-days: 30             # Process paraphrases from last N days
```

### Scheduler Configuration Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `scheduler.enabled` | Boolean | `false` | Enable/disable the background scheduler |
| `scheduler.cron` | String | `0 0 3 * * *` | Cron expression for when to run |
| `scheduler.max-per-run` | Integer | `500` | Maximum audio files to generate per run |
| `scheduler.lookback-days` | Integer | `30` | Only process paraphrases added in the last N days |

### Benefits for Frontend

When the scheduler is enabled:
- Users may find that audio is already available when they start a review session
- Reduces wait time during review as audio is pre-generated
- The frontend can check if audio exists (`GET /audio/exists/{id}`) and skip generation if already available

### Frontend Strategy with Scheduler

```javascript
async function startReviewWithSchedulerAwareness(listId) {
  // First, check config to see current state
  const configResponse = await fetch('/api/word/review/audio/config');
  const config = await configResponse.json();

  if (!config.data.enabled) {
    console.log('Audio generation is disabled');
    return;
  }

  // Generate audio for review items
  // The backend will skip items that already have audio (from scheduler)
  const result = await fetch(`/api/word/review/audio/generate/review-items/${listId}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });

  const data = await result.json();

  // skippedCount shows how many already had audio (likely from scheduler)
  console.log(`Skipped ${data.data.skippedCount} items (already have audio)`);
  console.log(`Generated ${data.data.successCount} new audio files`);
}
```

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-01-11 | Initial release with OpenAI TTS support |

---

## Support

For questions or issues, contact the backend team or create an issue in the project repository.
