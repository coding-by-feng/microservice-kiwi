# Private Notes Feature - Frontend Development Guide

## Overview

The Private Notes feature allows users to create and manage personal notes organized by categories. Each note can have AI-generated audio (TTS) and images for enhanced learning and review.

**Key Features:**
- Note categories with customizable colors and icons
- Note items with card-based display
- Previous/Next navigation with loop support (for flashcard-style review)
- AI-generated audio with multiple accents (US, UK, AU, IN)
- AI-generated images via Gemini Imagen
- All data is user-private (only owner can access)

---

## Data Structures

### NotesCategoryVO

```typescript
interface NotesCategoryVO {
  id: number;
  name: string;
  description: string | null;
  color: string | null;        // e.g., "#FF5733"
  icon: string | null;         // e.g., "star", "heart"
  sortOrder: number;
  itemCount: number;           // Number of items in this category
  createTime: string;          // ISO datetime
  updateTime: string;          // ISO datetime
}
```

### NotesItemVO

```typescript
interface NotesItemVO {
  id: number;
  categoryId: number;
  categoryName: string | null;
  content: string;
  displayOrder: number;

  // Image fields
  imagePrompt: string | null;
  imageStyle: string | null;
  imageUrl: string | null;
  imageStatus: MediaStatus;

  // Audio fields
  audioUrl: string | null;
  audioAccent: AccentType | null;
  audioVoice: string | null;
  audioDurationMs: number;
  audioStatus: MediaStatus;

  // Navigation flags (for card navigation)
  hasPrevious: boolean;
  hasNext: boolean;

  createTime: string;
  updateTime: string;
}

type MediaStatus = 'NONE' | 'PENDING' | 'GENERATING' | 'READY' | 'FAILED';
type AccentType = 'US' | 'UK' | 'AU' | 'IN';
type VoiceType = 'alloy' | 'echo' | 'fable' | 'onyx' | 'nova' | 'shimmer' | 'coral' | 'sage';
```

### Request DTOs

```typescript
interface NotesCategoryRequest {
  name: string;                // Required, max 100 chars
  description?: string;        // Optional, max 500 chars
  color?: string;
  icon?: string;
  sortOrder?: number;
}

interface NotesItemRequest {
  categoryId: number;          // Required
  content: string;             // Required
  displayOrder?: number;       // Optional, auto-assigned if not provided
  imagePrompt?: string;
  imageStyle?: string;
}

interface NotesAudioRequest {
  noteItemId: number;          // Required
  accent?: AccentType;         // Default: 'US'
  voice?: VoiceType;           // Default: 'alloy'
}

interface NotesImageRequest {
  noteItemId: number;          // Required
  customPrompt?: string;       // Additional context for image
  style?: string;              // e.g., "watercolor", "minimalist"
}
```

### API Response Wrapper

```typescript
interface ApiResponse<T> {
  code: number;      // 0 = success, 1 = error
  msg: string;       // "Success" or error message
  data: T | null;
}
```

---

## API Endpoints

### Base URL
```
Development: http://localhost:8088
Production: https://your-api-domain.com
```

### Authentication
All endpoints require Bearer token authentication:
```
Authorization: Bearer {access_token}
```

---

## Category APIs

### List Categories
```http
GET /api/notes/category/list
```

**Response:**
```json
{
  "code": 0,
  "msg": "Success",
  "data": [
    {
      "id": 1,
      "name": "Most Important",
      "description": "Key principles for daily life",
      "color": "#FF5733",
      "icon": "star",
      "sortOrder": 1,
      "itemCount": 42,
      "createTime": "2024-01-15T10:30:00",
      "updateTime": "2024-01-15T10:30:00"
    }
  ]
}
```

### Get Category
```http
GET /api/notes/category/{id}
```

### Create Category
```http
POST /api/notes/category
Content-Type: application/json

{
  "name": "Physical Health",
  "description": "Health and wellness reminders",
  "color": "#33FF57",
  "icon": "heart",
  "sortOrder": 2
}
```

### Update Category
```http
PUT /api/notes/category/{id}
Content-Type: application/json

{
  "name": "Updated Name",
  "description": "Updated description",
  "color": "#FF0000"
}
```

### Delete Category
```http
DELETE /api/notes/category/{id}
```

---

## Note Item APIs

### List Items by Category
```http
GET /api/notes/item/list/{categoryId}
```

**Response:**
```json
{
  "code": 0,
  "msg": "Success",
  "data": [
    {
      "id": 1,
      "categoryId": 1,
      "content": "Challenge yourself to endure for at least 10 seconds...",
      "displayOrder": 1,
      "imageUrl": null,
      "imageStatus": "NONE",
      "audioUrl": "conversation-audio/note_1.mp3",
      "audioAccent": "UK",
      "audioVoice": "alloy",
      "audioDurationMs": 8500,
      "audioStatus": "READY",
      "hasPrevious": false,
      "hasNext": true,
      "createTime": "2024-01-15T10:30:00",
      "updateTime": "2024-01-15T10:30:00"
    }
  ]
}
```

### Get Item
```http
GET /api/notes/item/{id}
```

### Create Item
```http
POST /api/notes/item
Content-Type: application/json

{
  "categoryId": 1,
  "content": "Note content here...",
  "imageStyle": "minimalist illustration"
}
```

### Update Item
```http
PUT /api/notes/item/{id}
Content-Type: application/json

{
  "categoryId": 1,
  "content": "Updated content..."
}
```

### Delete Item
```http
DELETE /api/notes/item/{id}
```

---

## Navigation APIs (For Card Loop)

### Get Next Item
Returns the next item in display order. **Loops to first item** if current is the last.

```http
GET /api/notes/item/{id}/next
```

### Get Previous Item
Returns the previous item in display order. **Loops to last item** if current is the first.

```http
GET /api/notes/item/{id}/previous
```

### Reorder Items
Reorder items by providing an array of item IDs in the desired order.

```http
PUT /api/notes/item/reorder/{categoryId}
Content-Type: application/json

[3, 1, 2, 5, 4]
```

---

## Audio APIs

### Generate Audio (TTS)
Generates audio for a note item using OpenAI TTS with selectable accent and voice.

```http
POST /api/notes/item/audio/generate
Content-Type: application/json

{
  "noteItemId": 1,
  "accent": "UK",
  "voice": "alloy"
}
```

**Available Accents:**
| Accent | Description |
|--------|-------------|
| `US` | American English (default) |
| `UK` | British English |
| `AU` | Australian English |
| `IN` | Indian English |

**Available Voices:**
| Voice | Description |
|-------|-------------|
| `alloy` | Neutral, balanced |
| `echo` | Deeper, authoritative |
| `fable` | Warm, storytelling |
| `onyx` | Deep, mature |
| `nova` | Friendly, conversational |
| `shimmer` | Soft, gentle |
| `coral` | Clear, articulate |
| `sage` | Calm, measured |

**Response:** Returns updated `NotesItemVO` with `audioStatus: "READY"` and `audioUrl`.

### Stream Audio
Stream the audio file for playback.

```http
GET /api/notes/item/{id}/audio/stream?access_token={token}
```

**Response:** `audio/mpeg` binary stream

> **IMPORTANT:** Browser `<audio>` elements don't send Authorization headers automatically.
> You MUST append `?access_token={token}` to the URL for authentication.

**Usage in HTML:**
```html
<!-- Token must be appended as query parameter -->
<audio controls>
  <source src="/api/notes/item/1/audio/stream?access_token=YOUR_TOKEN" type="audio/mpeg">
</audio>
```

**Usage in JavaScript/React:**
```javascript
// Always include access_token in the URL
const audioUrl = `${API_BASE}/api/notes/item/${itemId}/audio/stream?access_token=${token}`;
const audio = new Audio(audioUrl);
audio.play();
```

**React Example:**
```tsx
const AudioPlayer: React.FC<{ itemId: number; token: string }> = ({ itemId, token }) => {
  const audioUrl = `/api/notes/item/${itemId}/audio/stream?access_token=${encodeURIComponent(token)}`;
  return <audio controls src={audioUrl} />;
};
```

### Delete Audio
```http
DELETE /api/notes/item/{id}/audio
```

---

## Image APIs

### Generate Image
Generates an image using Gemini Imagen API based on note content.

```http
POST /api/notes/item/image/generate
Content-Type: application/json

{
  "noteItemId": 1,
  "customPrompt": "A person staying focused and determined",
  "style": "minimalist illustration with warm colors"
}
```

**Image Prompt Format:**
The final prompt sent to Gemini is constructed as:
```
Image description: {note content (first 200 chars)}. Additional context: {customPrompt}. Image style: {style}
```

**Response:** Returns updated `NotesItemVO` with `imageStatus: "READY"` and `imageUrl`.

### Stream Image
```http
GET /api/notes/item/{id}/image/stream?access_token={token}
```

**Response:** `image/png` binary stream

> **IMPORTANT:** Browser `<img>` elements don't send Authorization headers automatically.
> You MUST append `?access_token={token}` to the URL for authentication.

**Usage:**
```html
<!-- Token must be appended as query parameter -->
<img src="/api/notes/item/1/image/stream?access_token=YOUR_TOKEN" alt="Note illustration">
```

**React Example:**
```tsx
const NoteImage: React.FC<{ itemId: number; token: string }> = ({ itemId, token }) => {
  const imageUrl = `/api/notes/item/${itemId}/image/stream?access_token=${encodeURIComponent(token)}`;
  return <img src={imageUrl} alt="Note illustration" />;
};
```

### Delete Image
```http
DELETE /api/notes/item/{id}/image
```

---

## Frontend Implementation Guide

### 1. Category List Page

```tsx
// Fetch categories
const fetchCategories = async () => {
  const response = await fetch('/api/notes/category/list', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const result = await response.json();
  return result.data;
};

// Display with item count
categories.map(cat => (
  <CategoryCard
    key={cat.id}
    name={cat.name}
    itemCount={cat.itemCount}
    color={cat.color}
    icon={cat.icon}
    onClick={() => navigate(`/notes/${cat.id}`)}
  />
));
```

### 2. Note Card Component

```tsx
interface NoteCardProps {
  item: NotesItemVO;
  token: string;  // Auth token for media URLs
  onPrevious: () => void;
  onNext: () => void;
  onGenerateAudio: (accent: AccentType, voice: VoiceType) => void;
  onGenerateImage: (style: string) => void;
}

const NoteCard: React.FC<NoteCardProps> = ({ item, token, onPrevious, onNext, ... }) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);

  // Build media URLs with auth token (required for browser media elements)
  const imageUrl = item.imageUrl
    ? `/api/notes/item/${item.id}/image/stream?access_token=${encodeURIComponent(token)}`
    : null;
  const audioUrl = item.audioStatus === 'READY'
    ? `/api/notes/item/${item.id}/audio/stream?access_token=${encodeURIComponent(token)}`
    : null;

  return (
    <div className="note-card">
      {/* Image - token in URL */}
      {imageUrl && (
        <img src={imageUrl} alt="Note illustration" />
      )}

      {/* Content */}
      <p>{item.content}</p>

      {/* Audio Player - token in URL */}
      {audioUrl && (
        <audio ref={audioRef} src={audioUrl} controls />
      )}

      {/* Navigation */}
      <div className="navigation">
        <button onClick={onPrevious} disabled={!item.hasPrevious}>Previous</button>
        <button onClick={onNext} disabled={!item.hasNext}>Next</button>
      </div>
    </div>
  );
};
```

### 3. Loop Playback Feature

```tsx
const NotesPlayer: React.FC<{ categoryId: number; token: string }> = ({ categoryId, token }) => {
  const [items, setItems] = useState<NotesItemVO[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isLooping, setIsLooping] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);

  // Load items
  useEffect(() => {
    fetchItems(categoryId).then(setItems);
  }, [categoryId]);

  // Handle audio end - move to next item
  const handleAudioEnded = () => {
    if (isLooping) {
      const nextIndex = (currentIndex + 1) % items.length;
      setCurrentIndex(nextIndex);
    }
  };

  // Auto-play when current item changes
  useEffect(() => {
    if (isLooping && audioRef.current) {
      audioRef.current.play();
    }
  }, [currentIndex, isLooping]);

  const currentItem = items[currentIndex];

  // Build audio URL with token (REQUIRED for browser audio element)
  const audioUrl = currentItem?.id
    ? `/api/notes/item/${currentItem.id}/audio/stream?access_token=${encodeURIComponent(token)}`
    : '';

  return (
    <div className="notes-player">
      <NoteCard item={currentItem} token={token} />

      <audio
        ref={audioRef}
        src={audioUrl}
        onEnded={handleAudioEnded}
      />

      <div className="controls">
        <button onClick={() => setIsLooping(!isLooping)}>
          {isLooping ? 'Stop Loop' : 'Start Loop'}
        </button>
        <select onChange={(e) => generateAudioForAll(e.target.value as AccentType)}>
          <option value="US">US Accent</option>
          <option value="UK">UK Accent</option>
          <option value="AU">Australian Accent</option>
          <option value="IN">Indian Accent</option>
        </select>
      </div>
    </div>
  );
};
```

### 4. Audio Generation with Accent Selection

```tsx
const AudioControls: React.FC<{ itemId: number }> = ({ itemId }) => {
  const [accent, setAccent] = useState<AccentType>('US');
  const [voice, setVoice] = useState<VoiceType>('alloy');
  const [isGenerating, setIsGenerating] = useState(false);

  const generateAudio = async () => {
    setIsGenerating(true);
    try {
      const response = await fetch('/api/notes/item/audio/generate', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ noteItemId: itemId, accent, voice })
      });
      const result = await response.json();
      if (result.code === 0) {
        // Audio ready - update UI
      }
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="audio-controls">
      <select value={accent} onChange={e => setAccent(e.target.value as AccentType)}>
        <option value="US">US English</option>
        <option value="UK">UK English</option>
        <option value="AU">Australian</option>
        <option value="IN">Indian</option>
      </select>

      <select value={voice} onChange={e => setVoice(e.target.value as VoiceType)}>
        <option value="alloy">Alloy (Neutral)</option>
        <option value="nova">Nova (Friendly)</option>
        <option value="echo">Echo (Deep)</option>
        <option value="shimmer">Shimmer (Soft)</option>
      </select>

      <button onClick={generateAudio} disabled={isGenerating}>
        {isGenerating ? 'Generating...' : 'Generate Audio'}
      </button>
    </div>
  );
};
```

### 5. Image Generation

```tsx
const ImageGenerator: React.FC<{ itemId: number }> = ({ itemId }) => {
  const [style, setStyle] = useState('minimalist illustration');
  const [customPrompt, setCustomPrompt] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);

  const generateImage = async () => {
    setIsGenerating(true);
    try {
      const response = await fetch('/api/notes/item/image/generate', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ noteItemId: itemId, customPrompt, style })
      });
      const result = await response.json();
      // Handle result
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="image-generator">
      <select value={style} onChange={e => setStyle(e.target.value)}>
        <option value="minimalist illustration">Minimalist</option>
        <option value="watercolor painting">Watercolor</option>
        <option value="photorealistic">Photorealistic</option>
        <option value="cartoon style">Cartoon</option>
        <option value="abstract art">Abstract</option>
      </select>

      <input
        type="text"
        placeholder="Additional context (optional)"
        value={customPrompt}
        onChange={e => setCustomPrompt(e.target.value)}
      />

      <button onClick={generateImage} disabled={isGenerating}>
        {isGenerating ? 'Generating...' : 'Generate Image'}
      </button>
    </div>
  );
};
```

---

## Status Handling

### MediaStatus States

| Status | Description | UI Action |
|--------|-------------|-----------|
| `NONE` | No media generated | Show "Generate" button |
| `PENDING` | Queued for generation | Show "Pending..." |
| `GENERATING` | Currently generating | Show spinner/progress |
| `READY` | Media available | Show player/image |
| `FAILED` | Generation failed | Show error + retry button |

```tsx
const MediaStatusIndicator: React.FC<{ status: MediaStatus }> = ({ status }) => {
  switch (status) {
    case 'NONE':
      return <button>Generate</button>;
    case 'PENDING':
      return <span className="pending">Pending...</span>;
    case 'GENERATING':
      return <Spinner />;
    case 'READY':
      return <span className="ready">Ready</span>;
    case 'FAILED':
      return <button className="error">Retry</button>;
  }
};
```

---

## Error Handling

```typescript
const handleApiError = (response: ApiResponse<any>) => {
  if (response.code !== 0) {
    switch (response.msg) {
      case 'Access denied':
        // User doesn't own this resource
        showToast('You do not have access to this note');
        break;
      case 'Category not found':
      case 'Note item not found':
        showToast('Item not found');
        break;
      case 'Audio is already being generated':
        showToast('Please wait for current generation to complete');
        break;
      default:
        showToast(response.msg);
    }
  }
};
```

---

## Recommended UI Flow

1. **Categories Page**
   - List all categories with item counts
   - Click category to view items

2. **Notes List View**
   - Display items in a list or grid
   - Quick actions: play audio, view image

3. **Card View (Flashcard Mode)**
   - Single card display with navigation
   - Previous/Next buttons (loops automatically)
   - Audio player with accent selector
   - Image display with generation option

4. **Loop Playback Mode**
   - Auto-advance through cards
   - Play audio for each card
   - Configurable pause between cards
   - Shuffle option (client-side)

---

## Important: Media Stream Authentication

**Browser media elements (`<audio>`, `<img>`) don't send Authorization headers.**

When streaming audio or images, you MUST append `?access_token={token}` to the URL:

```typescript
// WRONG - Will get 403 Forbidden
const badUrl = `/api/notes/item/${id}/audio/stream`;

// CORRECT - Include access_token
const goodUrl = `/api/notes/item/${id}/audio/stream?access_token=${encodeURIComponent(token)}`;
```

This is because:
1. The `Authorization: Bearer {token}` header works for API calls via `fetch()`
2. But HTML elements like `<audio src="...">` make requests without custom headers
3. The backend supports `?access_token=` query parameter as a fallback for these cases

---

## Notes

- All timestamps are in ISO 8601 format
- Audio files are MP3 format
- Images are PNG format
- Maximum content length: TEXT (65,535 chars)
- Maximum category name: 100 chars
- **Media stream URLs require `?access_token=` parameter**
