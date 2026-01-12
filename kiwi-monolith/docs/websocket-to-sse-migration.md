# WebSocket to SSE Migration Guide

This document describes the migration from WebSocket endpoints to Server-Sent Events (SSE) and REST endpoints.

## Why SSE?

SSE (Server-Sent Events) offers several advantages over WebSocket for our use cases:

1. **Simpler Implementation**: SSE is built on standard HTTP, making it easier to implement and debug
2. **Better Proxy/Load Balancer Support**: SSE works seamlessly with most proxies and load balancers
3. **Automatic Reconnection**: Built-in browser support for automatic reconnection
4. **No Custom Protocol**: Uses standard HTTP, easier to monitor and troubleshoot
5. **Lower Overhead**: For server-to-client streaming, SSE has less overhead than WebSocket

## Endpoint Mapping

| Old WebSocket Endpoint | New SSE/REST Endpoint | Method |
|----------------------|----------------------|--------|
| `/api/ai/ws/stream` | `/api/ai/sse/stream` | GET or POST |
| `/api/ai/ws/ytb/subtitle` | `/api/ai/sse/ytb/subtitle` | GET or POST |
| `/api/ai/ws/stt/audio` | `/api/ai/audio/speech-to-text` | POST (multipart) |

---

## 1. AI Streaming Migration

### Old WebSocket Implementation

```javascript
// WebSocket approach (deprecated)
const ws = new WebSocket('wss://api.example.com/api/ai/ws/stream');

ws.onopen = () => {
  ws.send(JSON.stringify({
    prompt: 'Hello world',
    promptMode: 'DIRECTLY_TRANSLATION',
    targetLanguage: 'EN',
    nativeLanguage: 'ZH'
  }));
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  if (data.type === 'chunk') {
    console.log('Chunk:', data.chunk);
  } else if (data.type === 'completed') {
    console.log('Full response:', data.fullResponse);
  }
};
```

### New SSE Implementation (GET)

```javascript
// SSE approach (recommended)
const params = new URLSearchParams({
  prompt: encodeURIComponent('Hello world'),
  promptMode: 'DIRECTLY_TRANSLATION',
  targetLanguage: 'EN',
  nativeLanguage: 'ZH'
});

const eventSource = new EventSource(`/api/ai/sse/stream?${params}`);

eventSource.addEventListener('started', (e) => {
  const data = JSON.parse(e.data);
  console.log('Started:', data.message);
});

eventSource.addEventListener('chunk', (e) => {
  const data = JSON.parse(e.data);
  console.log('Chunk:', data.chunk);
});

eventSource.addEventListener('completed', (e) => {
  const data = JSON.parse(e.data);
  console.log('Full response:', data.fullResponse);
  eventSource.close();
});

eventSource.addEventListener('error', (e) => {
  const data = JSON.parse(e.data);
  console.error('Error:', data.message);
  eventSource.close();
});

eventSource.onerror = (e) => {
  console.error('Connection error');
  eventSource.close();
};
```

### New SSE Implementation (POST - for longer prompts)

```javascript
// For POST requests with longer prompts, use fetch with ReadableStream
async function streamAiPost(request) {
  const response = await fetch('/api/ai/sse/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': '12345' // optional
    },
    body: JSON.stringify({
      prompt: 'Your long prompt here...',
      promptMode: 'DIRECTLY_TRANSLATION',
      targetLanguage: 'EN',
      nativeLanguage: 'ZH'
    })
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const text = decoder.decode(value);
    // Parse SSE events from text
    const lines = text.split('\n');
    for (const line of lines) {
      if (line.startsWith('data:')) {
        const data = JSON.parse(line.slice(5));
        console.log('Event:', data);
      }
    }
  }
}
```

---

## 2. YouTube Subtitle Migration

### Old WebSocket Implementation

```javascript
// WebSocket approach (deprecated)
const ws = new WebSocket('wss://api.example.com/api/ai/ws/ytb/subtitle');

ws.onopen = () => {
  ws.send(JSON.stringify({
    videoUrl: 'https://youtube.com/watch?v=xxx',
    requestType: 'translated',
    language: 'ZH'
  }));
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  if (data.type === 'chunk') {
    console.log('Subtitles:', data.chunk);
  }
};
```

### New SSE Implementation (GET)

```javascript
// SSE approach (recommended)
const params = new URLSearchParams({
  videoUrl: 'https://youtube.com/watch?v=xxx',
  requestType: 'translated',
  language: 'ZH'
});

const eventSource = new EventSource(`/api/ai/sse/ytb/subtitle?${params}`);

eventSource.addEventListener('started', (e) => {
  const data = JSON.parse(e.data);
  console.log('Started:', data.message);
});

eventSource.addEventListener('progress', (e) => {
  const data = JSON.parse(e.data);
  console.log(`Progress: ${data.currentStep}/${data.totalSteps} - ${data.currentStepDescription}`);
});

eventSource.addEventListener('chunk', (e) => {
  const data = JSON.parse(e.data);
  console.log('Subtitles:', data.chunk);
});

eventSource.addEventListener('completed', (e) => {
  const data = JSON.parse(e.data);
  console.log('Completed in:', data.processingDuration, 'ms');
  eventSource.close();
});

eventSource.addEventListener('error', (e) => {
  const data = JSON.parse(e.data);
  console.error('Error:', data.message);
  eventSource.close();
});
```

---

## 3. Audio Speech-to-Text Migration

### Old WebSocket Implementation

```javascript
// WebSocket approach (deprecated) - binary audio
const ws = new WebSocket('wss://api.example.com/api/ai/ws/stt/audio');

ws.binaryType = 'arraybuffer';

ws.onopen = () => {
  // Send audio as binary
  ws.send(audioBlob);
};

ws.onmessage = (event) => {
  console.log('Transcript:', event.data);
};
```

### New REST Implementation (Multipart Form)

```javascript
// REST approach (recommended) - file upload
async function speechToText(audioFile) {
  const formData = new FormData();
  formData.append('file', audioFile);
  formData.append('language', 'en'); // optional

  const response = await fetch('/api/ai/audio/speech-to-text', {
    method: 'POST',
    body: formData
  });

  const result = await response.json();
  if (result.code === 0) {
    console.log('Transcript:', result.data);
  } else {
    console.error('Error:', result.msg);
  }
}

// Example with audio recording
navigator.mediaDevices.getUserMedia({ audio: true })
  .then(stream => {
    const mediaRecorder = new MediaRecorder(stream);
    const chunks = [];

    mediaRecorder.ondataavailable = (e) => chunks.push(e.data);

    mediaRecorder.onstop = () => {
      const audioBlob = new Blob(chunks, { type: 'audio/webm' });
      speechToText(audioBlob);
    };

    // Start recording
    mediaRecorder.start();

    // Stop after 5 seconds
    setTimeout(() => mediaRecorder.stop(), 5000);
  });
```

### New REST Implementation (Raw Bytes)

```javascript
// For raw audio bytes
async function speechToTextRaw(audioBytes) {
  const response = await fetch('/api/ai/audio/speech-to-text/raw?language=en', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/octet-stream'
    },
    body: audioBytes
  });

  const result = await response.json();
  return result.data;
}
```

---

## Event Types Reference

### AI Streaming Events

| Event Name | Description | Payload |
|------------|-------------|---------|
| `started` | Streaming started | `{ type, message, request, timestamp }` |
| `chunk` | Content chunk received | `{ type, chunk, request, timestamp }` |
| `completed` | Streaming completed | `{ type, message, fullResponse, processingDuration, timestamp }` |
| `error` | Error occurred | `{ type, message, errorCode, request, timestamp }` |

### YouTube Subtitle Events

| Event Name | Description | Payload |
|------------|-------------|---------|
| `started` | Processing started | `{ type, message, originalRequest, currentStep, totalSteps }` |
| `progress` | Progress update | `{ type, currentStep, totalSteps, currentStepDescription }` |
| `chunk` | Subtitle content | `{ type, chunk, originalRequest }` |
| `completed` | Processing completed | `{ type, message, fullContent, processingDuration }` |
| `error` | Error occurred | `{ type, message, errorCode }` |

---

## Error Handling

### SSE Connection Errors

```javascript
eventSource.onerror = (event) => {
  if (eventSource.readyState === EventSource.CLOSED) {
    console.log('Connection was closed');
  } else {
    console.log('Connection error, will auto-retry');
  }
};
```

### Graceful Shutdown

```javascript
// Always close the EventSource when done
eventSource.close();
```

---

## Headers

For SSE endpoints that require authentication, include the user ID header:

```javascript
// Note: EventSource doesn't support custom headers directly
// Use query parameters or cookies for authentication

// Alternative: Use fetch with credentials
const response = await fetch('/api/ai/sse/stream?prompt=...', {
  headers: {
    'X-User-Id': '12345'
  }
});
```

---

## Backward Compatibility

The old WebSocket endpoints remain available but are marked as deprecated. They will be removed in a future release. Please migrate to the new SSE/REST endpoints as soon as possible.
