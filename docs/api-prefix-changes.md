# API Path Prefix Changes

All AI-related endpoints have been updated to include the `/api` prefix for consistency with the proxy configuration.

## Summary

| Component | Old Path | New Path |
|-----------|----------|----------|
| AI Controller | `/ai/*` | `/api/ai/*` |
| AI V2 Controller | `/ai/v2/*` | `/api/ai/v2/*` |
| YouTube Video Controller | `/ai/ytb/video/*` | `/api/ai/ytb/video/*` |
| YouTube Channel Controller | `/ai/ytb/channel/*` | `/api/ai/ytb/channel/*` |
| WebSocket Endpoints | `/ai/ws/*` | `/api/ai/ws/*` |

---

## REST API Endpoints

### AI Controller (`/api/ai`)

| Method | Old Path | New Path |
|--------|----------|----------|
| GET | `/ai/directly-translation/{language}/{originalText}` | `/api/ai/directly-translation/{language}/{originalText}` |
| GET | `/ai/translation-and-explanation/{language}/{originalText}` | `/api/ai/translation-and-explanation/{language}/{originalText}` |
| GET | `/ai/grammar-explanation/{language}/{originalText}` | `/api/ai/grammar-explanation/{language}/{originalText}` |
| GET | `/ai/grammar-correction/{language}/{originalText}` | `/api/ai/grammar-correction/{language}/{originalText}` |
| GET | `/ai/vocabulary-explanation/{language}/{originalText}` | `/api/ai/vocabulary-explanation/{language}/{originalText}` |
| GET | `/ai/synonym/{language}/{originalText}` | `/api/ai/synonym/{language}/{originalText}` |
| GET | `/ai/antonym/{language}/{originalText}` | `/api/ai/antonym/{language}/{originalText}` |
| GET | `/ai/history` | `/api/ai/history` |
| PUT | `/ai/history/{id}/archive` | `/api/ai/history/{id}/archive` |
| DELETE | `/ai/history/{id}` | `/api/ai/history/{id}` |
| PUT | `/ai/history/{id}/favorite` | `/api/ai/history/{id}/favorite` |

### AI V2 Controller (`/api/ai/v2`)

| Method | Old Path | New Path |
|--------|----------|----------|
| GET | `/ai/v2/directly-translation/{target-lang}/{native-lang}/{originalText}` | `/api/ai/v2/directly-translation/{target-lang}/{native-lang}/{originalText}` |
| GET | `/ai/v2/translation-and-explanation/{target-lang}/{native-lang}/{originalText}` | `/api/ai/v2/translation-and-explanation/{target-lang}/{native-lang}/{originalText}` |
| GET | `/ai/v2/grammar-explanation/{target-lang}/{native-lang}/{originalText}` | `/api/ai/v2/grammar-explanation/{target-lang}/{native-lang}/{originalText}` |
| GET | `/ai/v2/grammar-correction/{target-lang}/{native-lang}/{originalText}` | `/api/ai/v2/grammar-correction/{target-lang}/{native-lang}/{originalText}` |
| GET | `/ai/v2/vocabulary-explanation/{target-lang}/{native-lang}/{originalText}` | `/api/ai/v2/vocabulary-explanation/{target-lang}/{native-lang}/{originalText}` |
| GET | `/ai/v2/synonym/{target-lang}/{native-lang}/{originalText}` | `/api/ai/v2/synonym/{target-lang}/{native-lang}/{originalText}` |
| GET | `/ai/v2/antonym/{target-lang}/{native-lang}/{originalText}` | `/api/ai/v2/antonym/{target-lang}/{native-lang}/{originalText}` |
| GET | `/ai/v2/vocabulary-association/{target-lang}/{native-lang}/{originalText}` | `/api/ai/v2/vocabulary-association/{target-lang}/{native-lang}/{originalText}` |
| GET | `/ai/v2/phrases-association/{target-lang}/{native-lang}/{originalText}` | `/api/ai/v2/phrases-association/{target-lang}/{native-lang}/{originalText}` |

### YouTube Video Controller (`/api/ai/ytb/video`)

| Method | Old Path | New Path |
|--------|----------|----------|
| GET | `/ai/ytb/video/download` | `/api/ai/ytb/video/download` |
| GET | `/ai/ytb/video/subtitles/translated/download` | `/api/ai/ytb/video/subtitles/translated/download` |
| GET | `/ai/ytb/video/subtitles/scrolling` | `/api/ai/ytb/video/subtitles/scrolling` |
| GET | `/ai/ytb/video/subtitles/translated` | `/api/ai/ytb/video/subtitles/translated` |
| GET | `/ai/ytb/video/subtitles/translated/stream` | `/api/ai/ytb/video/subtitles/translated/stream` |
| DELETE | `/ai/ytb/video/subtitles` | `/api/ai/ytb/video/subtitles` |
| GET | `/ai/ytb/video/title` | `/api/ai/ytb/video/title` |

### YouTube Channel Controller (`/api/ai/ytb/channel`)

| Method | Old Path | New Path |
|--------|----------|----------|
| POST | `/ai/ytb/channel` | `/api/ai/ytb/channel` |
| POST | `/ai/ytb/channel/id/{channelId}/favorite` | `/api/ai/ytb/channel/id/{channelId}/favorite` |
| DELETE | `/ai/ytb/channel/id/{channelId}/favorite` | `/api/ai/ytb/channel/id/{channelId}/favorite` |
| POST | `/ai/ytb/channel/video/{videoId}/favorite` | `/api/ai/ytb/channel/video/{videoId}/favorite` |
| DELETE | `/ai/ytb/channel/video/{videoId}/favorite` | `/api/ai/ytb/channel/video/{videoId}/favorite` |
| GET | `/ai/ytb/channel/video/{videoId}/favorite` | `/api/ai/ytb/channel/video/{videoId}/favorite` |
| POST | `/ai/ytb/channel/video/favorite` | `/api/ai/ytb/channel/video/favorite` |
| DELETE | `/ai/ytb/channel/video/favorite` | `/api/ai/ytb/channel/video/favorite` |
| GET | `/ai/ytb/channel/video/favorite` | `/api/ai/ytb/channel/video/favorite` |
| GET | `/ai/ytb/channel/favorites/channels` | `/api/ai/ytb/channel/favorites/channels` |
| GET | `/ai/ytb/channel/favorites/videos` | `/api/ai/ytb/channel/favorites/videos` |
| GET | `/ai/ytb/channel/page` | `/api/ai/ytb/channel/page` |
| GET | `/ai/ytb/channel/id/{channelId}/videos` | `/api/ai/ytb/channel/id/{channelId}/videos` |

---

## WebSocket Endpoints

| Purpose | Old URL | New URL |
|---------|---------|---------|
| AI Streaming | `ws://host/ai/ws/stream` | `ws://host/api/ai/ws/stream` |
| YouTube Subtitle | `ws://host/ai/ws/ytb/subtitle` | `ws://host/api/ai/ws/ytb/subtitle` |
| Speech-to-Text Audio | `ws://host/ai/ws/stt/audio` | `ws://host/api/ai/ws/stt/audio` |

### WebSocket Connection Example

```javascript
// Old
const ws = new WebSocket('ws://localhost:8080/ai/ws/stream?access_token=YOUR_TOKEN');

// New (Monolith - port 8088 direct, or port 8080 via proxy)
const ws = new WebSocket('ws://localhost:8088/api/ai/ws/stream?access_token=YOUR_TOKEN');
// or through proxy:
const ws = new WebSocket('ws://localhost:8080/api/ai/ws/stream?access_token=YOUR_TOKEN');
```

### AI Streaming Request Format

```javascript
// Connect to WebSocket
const ws = new WebSocket('ws://localhost:8088/api/ai/ws/stream?access_token=YOUR_TOKEN');

ws.onopen = () => {
  // Send request
  ws.send(JSON.stringify({
    promptMode: "DIRECTLY_TRANSLATION",  // or GRAMMAR_EXPLANATION, etc.
    prompt: "hello world",
    targetLanguage: "ZH_CN",
    nativeLanguage: "EN"
  }));
};

ws.onmessage = (event) => {
  const response = JSON.parse(event.data);
  console.log('Type:', response.type);  // connected, started, chunk, completed, error
  if (response.type === 'chunk') {
    console.log('Chunk:', response.chunk);
  }
  if (response.type === 'completed') {
    console.log('Full response:', response.fullResponse);
  }
};
```

### YouTube Subtitle Request Format

```javascript
const ws = new WebSocket('ws://localhost:8088/api/ai/ws/ytb/subtitle?access_token=YOUR_TOKEN');

ws.onopen = () => {
  ws.send(JSON.stringify({
    videoUrl: "https://www.youtube.com/watch?v=VIDEO_ID",
    language: "ZH_CN",       // target language for translation
    requestType: "translated" // or "scrolling" for non-translated
  }));
};
```

---

## Frontend Migration Guide

### Quick Find & Replace

In your frontend codebase, perform the following replacements:

```
/ai/ws/stream        → /api/ai/ws/stream
/ai/ws/ytb/subtitle  → /api/ai/ws/ytb/subtitle
/ai/ws/stt/audio     → /api/ai/ws/stt/audio
/ai/v2/              → /api/ai/v2/
/ai/ytb/video/       → /api/ai/ytb/video/
/ai/ytb/channel/     → /api/ai/ytb/channel/
/ai/                 → /api/ai/
```

**Note:** Be careful with the order of replacements to avoid double-prefixing. The more specific paths should be replaced first, or use exact matching.

### If Using a Base URL Constant

If your frontend uses a base URL constant for API calls, you may only need to update the base configuration:

```javascript
// If you have something like:
const AI_API_BASE = '/ai';

// Change to:
const AI_API_BASE = '/api/ai';
```
