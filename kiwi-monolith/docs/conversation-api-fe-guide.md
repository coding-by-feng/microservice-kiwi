# AI Conversation Generator - Frontend API Guide

## Overview

The AI Conversation Generator creates realistic multi-speaker conversations with TTS audio. Users provide a topic, select accent preferences, duration, and speaker count. The backend generates conversation scripts via AI, then produces TTS audio for each message with distinct voices per speaker.

**Key Feature**: Progressive SSE streaming - each message is sent to frontend as soon as its audio is ready, enabling immediate playback while generation continues.

---

## Base URL

```
/api/ai/conversation
```

**Authentication**: All endpoints require JWT token in header:
```
Authorization: Bearer <jwt_token>
```

---

## Endpoints

### 1. Generate Conversation (SSE Stream)

Generate a new conversation with progressive audio streaming.

**Endpoint**: `POST /api/ai/conversation/generate/stream`

**Content-Type**: `application/json`

**Accept**: `text/event-stream`

**Request Body**:
```json
{
  "prompt": "Two friends discussing their favorite travel destinations",
  "accent": "UK",
  "duration": "FIVE_MINUTES",
  "speakerCount": 2
}
```

**Parameters**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `prompt` | string | Yes | Topic/scenario description for the conversation |
| `accent` | enum | Yes | English accent: `US`, `UK`, `AU`, `IN` |
| `duration` | enum | Yes | Target duration: `TWO_MINUTES`, `FIVE_MINUTES`, `TEN_MINUTES` |
| `speakerCount` | integer | Yes | Number of speakers: 2, 3, or 4 |

**Response**: Server-Sent Events (SSE) stream

---

### SSE Event Types

#### 1. `metadata` - Conversation info (sent first)

```json
{
  "eventType": "metadata",
  "data": {
    "conversationId": 12345,
    "topic": "Travel Destinations Discussion",
    "speakers": [
      {
        "id": 101,
        "speakerIndex": 0,
        "name": "Sarah",
        "voice": "alloy",
        "avatarEmoji": "👩"
      },
      {
        "id": 102,
        "speakerIndex": 1,
        "name": "Mike",
        "voice": "echo",
        "avatarEmoji": "👨"
      }
    ],
    "totalMessageCount": 12
  },
  "timestamp": 1704067200000
}
```

#### 2. `message` - Individual message with audio (sent progressively)

```json
{
  "eventType": "message",
  "data": {
    "id": 1001,
    "speakerId": 101,
    "speakerName": "Sarah",
    "sequence": 1,
    "text": "Hey Mike! Have you decided where to go for your vacation this summer?",
    "emoji": "✈️",
    "audioStatus": "READY",
    "audioUrl": "group1/M00/00/01/message_1001.mp3",
    "audioDurationMs": 4200
  },
  "timestamp": 1704067205000
}
```

#### 3. `progress` - Generation progress update

```json
{
  "eventType": "progress",
  "data": {
    "completed": 5,
    "total": 12,
    "percentage": 42
  },
  "timestamp": 1704067210000
}
```

#### 4. `error` - Error during generation (non-fatal, continues with other messages)

```json
{
  "eventType": "error",
  "data": {
    "message": "Failed to generate audio for message 3",
    "code": "TTS_ERROR"
  },
  "timestamp": 1704067215000
}
```

#### 5. `complete` - Generation finished

```json
{
  "eventType": "complete",
  "data": {
    "conversationId": 12345,
    "totalMessages": 12,
    "totalAudioDurationMs": 285000,
    "generationTimeMs": 45230
  },
  "timestamp": 1704067250000
}
```

---

### 2. Get Conversation by ID

Retrieve a saved conversation with all messages.

**Endpoint**: `GET /api/ai/conversation/{id}`

**Response**:
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 12345,
    "topic": "Travel Destinations Discussion",
    "accent": "UK",
    "durationMinutes": 5,
    "status": "COMPLETED",
    "speakers": [
      {
        "id": 101,
        "speakerIndex": 0,
        "name": "Sarah",
        "voice": "alloy",
        "avatarEmoji": "👩"
      }
    ],
    "messages": [
      {
        "id": 1001,
        "speakerId": 101,
        "speakerName": "Sarah",
        "sequence": 1,
        "text": "Hey Mike! Have you decided where to go for your vacation?",
        "emoji": "✈️",
        "audioStatus": "READY",
        "audioUrl": "group1/M00/00/01/message_1001.mp3",
        "audioDurationMs": 4200
      }
    ],
    "createTime": "2024-01-01T10:00:00"
  }
}
```

---

### 3. List User's Conversations

Get all conversations for the authenticated user.

**Endpoint**: `GET /api/ai/conversation/list`

**Response**:
```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 12345,
      "topic": "Travel Destinations Discussion",
      "accent": "UK",
      "durationMinutes": 5,
      "status": "COMPLETED",
      "createTime": "2024-01-01T10:00:00"
    },
    {
      "id": 12344,
      "topic": "Coffee Shop Conversation",
      "accent": "US",
      "durationMinutes": 2,
      "status": "COMPLETED",
      "createTime": "2023-12-31T15:30:00"
    }
  ]
}
```

---

### 4. Delete Conversation

Soft delete a conversation.

**Endpoint**: `DELETE /api/ai/conversation/{id}`

**Response**:
```json
{
  "code": 0,
  "msg": "success",
  "data": null
}
```

---

### 5. Stream Audio File

Stream audio for a specific message.

**Endpoint**: `GET /api/ai/conversation/{conversationId}/audio/{messageId}`

**Response**: Audio stream (`audio/mpeg`)

**Headers**:
```
Content-Type: audio/mpeg
Content-Disposition: inline; filename="message_1001.mp3"
```

---

### 6. Get Configuration

Get conversation generator configuration.

**Endpoint**: `GET /api/ai/conversation/config`

**Response**:
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "enabled": true,
    "maxDailyGenerations": 10,
    "maxSpeakers": 4,
    "minSpeakers": 2
  }
}
```

---

## Frontend Implementation Guide

### 1. SSE Connection with EventSource

```typescript
interface ConversationRequest {
  prompt: string;
  accent: 'US' | 'UK' | 'AU' | 'IN';
  duration: 'TWO_MINUTES' | 'FIVE_MINUTES' | 'TEN_MINUTES';
  speakerCount: number;
}

interface Speaker {
  id: number;
  speakerIndex: number;
  name: string;
  voice: string;
  avatarEmoji: string;
}

interface Message {
  id: number;
  speakerId: number;
  speakerName: string;
  sequence: number;
  text: string;
  emoji: string;
  audioStatus: 'PENDING' | 'GENERATING' | 'READY' | 'FAILED';
  audioUrl: string;
  audioDurationMs: number;
}

async function generateConversation(
  request: ConversationRequest,
  callbacks: {
    onMetadata: (data: { conversationId: number; topic: string; speakers: Speaker[]; totalMessageCount: number }) => void;
    onMessage: (message: Message) => void;
    onProgress: (data: { completed: number; total: number; percentage: number }) => void;
    onComplete: (data: { conversationId: number; totalMessages: number; totalAudioDurationMs: number }) => void;
    onError: (error: { message: string; code: string }) => void;
  }
) {
  const token = getAuthToken(); // Get JWT token from your auth system

  // Use fetch with ReadableStream for SSE with POST
  const response = await fetch('/api/ai/conversation/generate/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'Accept': 'text/event-stream'
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const reader = response.body?.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader!.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      if (line.startsWith('event:')) {
        const eventType = line.substring(6).trim();
        continue;
      }
      if (line.startsWith('data:')) {
        const jsonStr = line.substring(5).trim();
        if (jsonStr) {
          const event = JSON.parse(jsonStr);
          switch (event.eventType) {
            case 'metadata':
              callbacks.onMetadata(event.data);
              break;
            case 'message':
              callbacks.onMessage(event.data);
              break;
            case 'progress':
              callbacks.onProgress(event.data);
              break;
            case 'complete':
              callbacks.onComplete(event.data);
              break;
            case 'error':
              callbacks.onError(event.data);
              break;
          }
        }
      }
    }
  }
}
```

### 2. React Component Example

```tsx
import React, { useState, useRef } from 'react';

interface Speaker {
  id: number;
  name: string;
  avatarEmoji: string;
}

interface ChatMessage {
  id: number;
  speakerId: number;
  speakerName: string;
  text: string;
  emoji: string;
  audioUrl: string;
  audioDurationMs: number;
}

const ConversationGenerator: React.FC = () => {
  const [prompt, setPrompt] = useState('');
  const [accent, setAccent] = useState<'US' | 'UK' | 'AU' | 'IN'>('UK');
  const [duration, setDuration] = useState<'TWO_MINUTES' | 'FIVE_MINUTES' | 'TEN_MINUTES'>('FIVE_MINUTES');
  const [speakerCount, setSpeakerCount] = useState(2);

  const [isGenerating, setIsGenerating] = useState(false);
  const [progress, setProgress] = useState(0);
  const [topic, setTopic] = useState('');
  const [speakers, setSpeakers] = useState<Speaker[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [currentPlayingId, setCurrentPlayingId] = useState<number | null>(null);

  const audioRef = useRef<HTMLAudioElement>(null);
  const conversationIdRef = useRef<number | null>(null);

  const handleGenerate = async () => {
    setIsGenerating(true);
    setProgress(0);
    setMessages([]);
    setSpeakers([]);
    setTopic('');

    try {
      await generateConversation(
        { prompt, accent, duration, speakerCount },
        {
          onMetadata: (data) => {
            conversationIdRef.current = data.conversationId;
            setTopic(data.topic);
            setSpeakers(data.speakers);
          },
          onMessage: (msg) => {
            setMessages(prev => [...prev, msg]);
          },
          onProgress: (data) => {
            setProgress(data.percentage);
          },
          onComplete: (data) => {
            setIsGenerating(false);
            console.log(`Generation complete: ${data.totalMessages} messages, ${data.totalAudioDurationMs}ms total audio`);
          },
          onError: (error) => {
            console.error('Generation error:', error.message);
          }
        }
      );
    } catch (error) {
      console.error('Failed to generate conversation:', error);
      setIsGenerating(false);
    }
  };

  const playAudio = (messageId: number, audioUrl: string) => {
    const fullUrl = `/api/ai/conversation/${conversationIdRef.current}/audio/${messageId}`;
    if (audioRef.current) {
      audioRef.current.src = fullUrl;
      audioRef.current.play();
      setCurrentPlayingId(messageId);
    }
  };

  const getSpeakerColor = (speakerId: number): string => {
    const colors = ['#4A90D9', '#E74C3C', '#27AE60', '#9B59B6'];
    const index = speakers.findIndex(s => s.id === speakerId);
    return colors[index % colors.length];
  };

  return (
    <div className="conversation-generator">
      {/* Input Form */}
      <div className="form-section">
        <textarea
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          placeholder="Describe the conversation topic..."
          disabled={isGenerating}
        />

        <div className="options">
          <select value={accent} onChange={(e) => setAccent(e.target.value as any)} disabled={isGenerating}>
            <option value="US">American English</option>
            <option value="UK">British English</option>
            <option value="AU">Australian English</option>
            <option value="IN">Indian English</option>
          </select>

          <select value={duration} onChange={(e) => setDuration(e.target.value as any)} disabled={isGenerating}>
            <option value="TWO_MINUTES">~2 minutes</option>
            <option value="FIVE_MINUTES">~5 minutes</option>
            <option value="TEN_MINUTES">~10 minutes</option>
          </select>

          <select value={speakerCount} onChange={(e) => setSpeakerCount(Number(e.target.value))} disabled={isGenerating}>
            <option value={2}>2 speakers</option>
            <option value={3}>3 speakers</option>
            <option value={4}>4 speakers</option>
          </select>
        </div>

        <button onClick={handleGenerate} disabled={isGenerating || !prompt.trim()}>
          {isGenerating ? `Generating... ${progress}%` : 'Generate Conversation'}
        </button>
      </div>

      {/* Progress Bar */}
      {isGenerating && (
        <div className="progress-bar">
          <div className="progress-fill" style={{ width: `${progress}%` }} />
        </div>
      )}

      {/* Topic Header */}
      {topic && (
        <div className="topic-header">
          <h2>{topic}</h2>
          <div className="speakers-list">
            {speakers.map(speaker => (
              <span key={speaker.id} className="speaker-badge">
                {speaker.avatarEmoji} {speaker.name}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Chat Messages */}
      <div className="chat-container">
        {messages.map((msg) => (
          <div
            key={msg.id}
            className="chat-message"
            style={{ borderLeftColor: getSpeakerColor(msg.speakerId) }}
          >
            <div className="message-header">
              <span className="speaker-name">{msg.speakerName}</span>
              <span className="message-emoji">{msg.emoji}</span>
            </div>
            <p className="message-text">{msg.text}</p>
            <div className="message-footer">
              <button
                className={`play-btn ${currentPlayingId === msg.id ? 'playing' : ''}`}
                onClick={() => playAudio(msg.id, msg.audioUrl)}
              >
                {currentPlayingId === msg.id ? '⏸️ Playing' : '▶️ Play'}
              </button>
              <span className="duration">{(msg.audioDurationMs / 1000).toFixed(1)}s</span>
            </div>
          </div>
        ))}
      </div>

      {/* Hidden Audio Element */}
      <audio
        ref={audioRef}
        onEnded={() => setCurrentPlayingId(null)}
        onError={() => setCurrentPlayingId(null)}
      />
    </div>
  );
};

export default ConversationGenerator;
```

### 3. CSS Styles (Reference)

```css
.conversation-generator {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.form-section textarea {
  width: 100%;
  height: 100px;
  margin-bottom: 15px;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  resize: vertical;
}

.options {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.options select {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
}

button {
  width: 100%;
  padding: 14px;
  background: #4A90D9;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
}

button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.progress-bar {
  height: 4px;
  background: #eee;
  border-radius: 2px;
  margin: 20px 0;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4A90D9, #27AE60);
  transition: width 0.3s ease;
}

.topic-header {
  text-align: center;
  margin: 30px 0;
}

.speakers-list {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 10px;
}

.speaker-badge {
  padding: 6px 12px;
  background: #f5f5f5;
  border-radius: 20px;
  font-size: 14px;
}

.chat-container {
  margin-top: 20px;
}

.chat-message {
  padding: 15px;
  margin-bottom: 15px;
  background: #f9f9f9;
  border-radius: 12px;
  border-left: 4px solid;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.speaker-name {
  font-weight: 600;
  color: #333;
}

.message-text {
  margin: 0 0 10px 0;
  line-height: 1.5;
}

.message-footer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.play-btn {
  width: auto;
  padding: 6px 14px;
  font-size: 13px;
}

.play-btn.playing {
  background: #27AE60;
}

.duration {
  font-size: 12px;
  color: #888;
}
```

---

## Error Codes

| Code | Description |
|------|-------------|
| `TTS_ERROR` | Failed to generate TTS audio for a message |
| `GENERATION_ERROR` | General generation failure (script or overall process) |

---

## Status Values

**Conversation Status**:
- `PENDING` - Created, not yet started
- `GENERATING_SCRIPT` - AI generating conversation script
- `GENERATING_AUDIO` - TTS generating audio for messages
- `COMPLETED` - All processing complete
- `FAILED` - Generation failed

**Audio Status**:
- `PENDING` - Audio not yet generated
- `GENERATING` - TTS in progress
- `READY` - Audio available for playback
- `FAILED` - Audio generation failed

---

## Best Practices

1. **Progressive Playback**: Start playing first message audio while others are still generating
2. **Error Handling**: Individual message TTS failures don't stop the entire generation - handle gracefully
3. **Loading States**: Show skeleton/placeholder for total messages based on `totalMessageCount` from metadata
4. **Reconnection**: If SSE connection drops, use conversation ID to fetch completed state via `GET /{id}`
5. **Audio Preloading**: Consider preloading next message audio while current plays
6. **Accessibility**: Provide text alongside audio for users who prefer reading

---

## Rate Limits

- Maximum 10 conversations per user per day (configurable)
- SSE connection timeout: 10 minutes (600,000ms)
- Recommended delay between TTS generations: 200ms (handled server-side)
