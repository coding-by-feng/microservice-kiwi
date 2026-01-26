# Notes Image Styles - Frontend Enhancement Guide

## Overview

This guide describes the enhanced image generation feature for Private Notes, which allows users to select from predefined artistic styles when generating AI images for their notes.

**Key Enhancement:**
- Predefined image styles configured on the backend
- Style selection UI with descriptions and previews
- Consistent, high-quality image generation with curated prompts

---

## Data Structures

### ImageStyleVO

```typescript
interface ImageStyleVO {
  id: string;           // Unique style identifier (e.g., "ghibli", "cyberpunk")
  name: string;         // Display name (e.g., "Studio Ghibli")
  description: string;  // Brief description for UI tooltips
  previewUrl: string | null;  // Optional preview image URL
  sortOrder: number;    // Display order in the style picker
}
```

### Updated NotesImageRequest

```typescript
interface NotesImageRequest {
  noteItemId: number;   // Required - the note item to generate image for
  style?: string;       // Optional - the style ID from the available styles
  customPrompt?: string; // Optional - additional context (deprecated, use style instead)
}
```

---

## New API Endpoint

### List Available Image Styles

```http
GET /api/notes/item/image/styles
```

**Authentication:** Required (Bearer token)

**Response:**
```json
{
  "code": 0,
  "msg": "Success",
  "data": [
    {
      "id": "ghibli",
      "name": "Studio Ghibli",
      "description": "Hand-drawn animation inspired by Hayao Miyazaki with vibrant watercolor textures",
      "previewUrl": null,
      "sortOrder": 1
    },
    {
      "id": "cyberpunk",
      "name": "Cyberpunk",
      "description": "Futuristic metropolis at night with neon lights and rain-slicked streets",
      "previewUrl": null,
      "sortOrder": 2
    },
    {
      "id": "oil-painting",
      "name": "Classical Oil Painting",
      "description": "Renaissance-inspired fine art with rich colors and dramatic lighting",
      "previewUrl": null,
      "sortOrder": 3
    },
    {
      "id": "pixar",
      "name": "Pixar 3D Animation",
      "description": "Modern 3D animated movie style with polished rendering and expressive characters",
      "previewUrl": null,
      "sortOrder": 4
    },
    {
      "id": "graphic-novel",
      "name": "Graphic Novel",
      "description": "Sin City inspired black and white illustration with stark contrast",
      "previewUrl": null,
      "sortOrder": 5
    },
    {
      "id": "minimalist",
      "name": "Minimalist Vector",
      "description": "Clean flat design with bold lines and limited colors",
      "previewUrl": null,
      "sortOrder": 6
    }
  ]
}
```

---

## Image Generation with Style

### Generate Image API (Updated)

```http
POST /api/notes/item/image/generate
Content-Type: application/json
Authorization: Bearer {access_token}

{
  "noteItemId": 1,
  "style": "ghibli"
}
```

**How the Prompt is Built:**

The backend uses a structured prompt format that prioritizes content alignment while applying artistic style:

```
Create an image that visually represents and captures the essence of this concept:

"{note content (first 300 chars)}"

The image should clearly convey the meaning, emotion, and message of this text through visual storytelling.

Artistic style to apply: {full style prompt from configuration}

Important: The subject matter and core message must be the primary focus. Use the artistic style to enhance the visual presentation without overshadowing the content's meaning.
```

**Example:**

For a note with content "Focus on your breathing and stay present in the moment" and style "ghibli":

```
Create an image that visually represents and captures the essence of this concept:

"Focus on your breathing and stay present in the moment"

The image should clearly convey the meaning, emotion, and message of this text through visual storytelling.

Artistic style to apply: Studio Ghibli style, hand-drawn animation, inspired by Hayao Miyazaki, vibrant watercolor textures, lush greenery, nostalgic atmosphere, high aesthetic, detailed scenery, soft sunlight, 8k resolution.

Important: The subject matter and core message must be the primary focus. Use the artistic style to enhance the visual presentation without overshadowing the content's meaning.
```

This format ensures the AI:
1. **Prioritizes the content** - The note's meaning is explicitly emphasized
2. **Uses visual storytelling** - Translates text concepts into imagery
3. **Applies style secondarily** - Style enhances rather than dominates

---

## Available Styles

| Style ID | Name | Description |
|----------|------|-------------|
| `ghibli` | Studio Ghibli | Hand-drawn animation with vibrant watercolor textures, Miyazaki-inspired |
| `cyberpunk` | Cyberpunk | Futuristic metropolis, neon lights, Blade Runner inspired |
| `oil-painting` | Classical Oil Painting | Renaissance style, Rembrandt-inspired with dramatic lighting |
| `pixar` | Pixar 3D Animation | Modern CGI style, Disney/Pixar quality rendering |
| `graphic-novel` | Graphic Novel | Sin City inspired, stark black and white with high contrast |
| `minimalist` | Minimalist Vector | Clean flat design, geometric shapes, modern aesthetic |

---

## Frontend Implementation

### 1. Fetch and Cache Styles

```typescript
// api/notes.ts
export const fetchImageStyles = async (token: string): Promise<ImageStyleVO[]> => {
  const response = await fetch('/api/notes/item/image/styles', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const result = await response.json();
  if (result.code !== 0) throw new Error(result.msg);
  return result.data;
};
```

### 2. Style Selector Component

```tsx
import React, { useState, useEffect } from 'react';

interface StyleSelectorProps {
  token: string;
  selectedStyleId: string | null;
  onStyleSelect: (styleId: string) => void;
}

const StyleSelector: React.FC<StyleSelectorProps> = ({
  token,
  selectedStyleId,
  onStyleSelect
}) => {
  const [styles, setStyles] = useState<ImageStyleVO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchImageStyles(token)
      .then(setStyles)
      .finally(() => setLoading(false));
  }, [token]);

  if (loading) return <div className="loading">Loading styles...</div>;

  return (
    <div className="style-selector">
      <h4>Select Image Style</h4>
      <div className="style-grid">
        {styles.map(style => (
          <div
            key={style.id}
            className={`style-card ${selectedStyleId === style.id ? 'selected' : ''}`}
            onClick={() => onStyleSelect(style.id)}
          >
            {style.previewUrl && (
              <img
                src={style.previewUrl}
                alt={style.name}
                className="style-preview"
              />
            )}
            <div className="style-info">
              <h5>{style.name}</h5>
              <p>{style.description}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
```

### 3. Image Generator with Style Selection

```tsx
import React, { useState } from 'react';

interface ImageGeneratorProps {
  noteItemId: number;
  token: string;
  onImageGenerated: (updatedItem: NotesItemVO) => void;
}

const ImageGenerator: React.FC<ImageGeneratorProps> = ({
  noteItemId,
  token,
  onImageGenerated
}) => {
  const [selectedStyle, setSelectedStyle] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleGenerate = async () => {
    if (!selectedStyle) {
      setError('Please select a style');
      return;
    }

    setIsGenerating(true);
    setError(null);

    try {
      const response = await fetch('/api/notes/item/image/generate', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          noteItemId,
          style: selectedStyle
        })
      });

      const result = await response.json();

      if (result.code === 0) {
        onImageGenerated(result.data);
      } else {
        setError(result.msg);
      }
    } catch (err) {
      setError('Failed to generate image');
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="image-generator">
      <StyleSelector
        token={token}
        selectedStyleId={selectedStyle}
        onStyleSelect={setSelectedStyle}
      />

      {error && <div className="error-message">{error}</div>}

      <button
        onClick={handleGenerate}
        disabled={isGenerating || !selectedStyle}
        className="generate-button"
      >
        {isGenerating ? 'Generating...' : 'Generate Image'}
      </button>
    </div>
  );
};
```

### 4. Complete Note Card with Image Style

```tsx
interface NoteCardWithImageProps {
  item: NotesItemVO;
  token: string;
  onRefresh: () => void;
}

const NoteCardWithImage: React.FC<NoteCardWithImageProps> = ({
  item,
  token,
  onRefresh
}) => {
  const [showGenerator, setShowGenerator] = useState(false);

  // Build image URL with auth token
  const imageUrl = item.imageStatus === 'READY' && item.id
    ? `/api/notes/item/${item.id}/image/stream?access_token=${encodeURIComponent(token)}`
    : null;

  const handleImageGenerated = (updatedItem: NotesItemVO) => {
    setShowGenerator(false);
    onRefresh();
  };

  return (
    <div className="note-card">
      {/* Note Content */}
      <div className="note-content">
        <p>{item.content}</p>
      </div>

      {/* Image Section */}
      <div className="note-image-section">
        {imageUrl ? (
          <div className="image-display">
            <img src={imageUrl} alt="Note illustration" />
            {item.imageStyle && (
              <span className="style-badge">{item.imageStyle}</span>
            )}
            <button onClick={() => setShowGenerator(true)}>
              Regenerate
            </button>
          </div>
        ) : (
          <div className="no-image">
            {item.imageStatus === 'GENERATING' ? (
              <div className="generating">
                <span className="spinner" />
                Generating image...
              </div>
            ) : item.imageStatus === 'FAILED' ? (
              <div className="failed">
                <span>Generation failed</span>
                <button onClick={() => setShowGenerator(true)}>
                  Retry
                </button>
              </div>
            ) : (
              <button onClick={() => setShowGenerator(true)}>
                Generate Image
              </button>
            )}
          </div>
        )}
      </div>

      {/* Style Selector Modal */}
      {showGenerator && (
        <div className="modal-overlay">
          <div className="modal-content">
            <button
              className="close-button"
              onClick={() => setShowGenerator(false)}
            >
              ×
            </button>
            <ImageGenerator
              noteItemId={item.id}
              token={token}
              onImageGenerated={handleImageGenerated}
            />
          </div>
        </div>
      )}
    </div>
  );
};
```

---

## CSS Styling Example

```css
/* Style Selector */
.style-selector {
  padding: 1rem;
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.style-card {
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.style-card:hover {
  border-color: #4a90d9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.style-card.selected {
  border-color: #4a90d9;
  background: #f0f7ff;
}

.style-preview {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
  margin-bottom: 0.5rem;
}

.style-info h5 {
  margin: 0 0 0.25rem 0;
  font-size: 0.9rem;
  font-weight: 600;
}

.style-info p {
  margin: 0;
  font-size: 0.75rem;
  color: #666;
  line-height: 1.3;
}

/* Generate Button */
.generate-button {
  width: 100%;
  padding: 0.75rem 1.5rem;
  margin-top: 1rem;
  background: #4a90d9;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease;
}

.generate-button:hover:not(:disabled) {
  background: #3a7bc8;
}

.generate-button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* Style Badge on Image */
.style-badge {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  text-transform: capitalize;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  max-width: 600px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
}

.close-button {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
}
```

---

## Usage Flow

### User Experience

1. **View Note** - User views a note item in card or list view
2. **Generate Image** - User clicks "Generate Image" button
3. **Select Style** - Modal shows available styles with descriptions
4. **Preview Style** - User hovers over styles to read descriptions
5. **Choose Style** - User clicks on preferred style (highlighted selection)
6. **Confirm Generation** - User clicks "Generate Image" button
7. **Wait** - UI shows "Generating..." spinner
8. **View Result** - Generated image displays with style badge

### Regeneration

If user wants a different style:
1. Click "Regenerate" on existing image
2. Select new style
3. Generate replaces old image

---

## Error Handling

```typescript
const handleApiError = (response: ApiResponse<any>) => {
  if (response.code !== 0) {
    switch (response.msg) {
      case 'Access denied':
        showToast('You do not have access to this note');
        break;
      case 'Note item not found':
        showToast('Note not found');
        break;
      case 'Image is already being generated':
        showToast('Please wait for current generation to complete');
        break;
      case 'Gemini image generation not configured':
        showToast('Image generation is temporarily unavailable');
        break;
      default:
        showToast(response.msg || 'Failed to generate image');
    }
  }
};
```

---

## Caching Recommendations

### Style List Caching

Styles are configured on the backend and rarely change. Cache them:

```typescript
// Use React Query or SWR
const { data: styles } = useQuery(
  ['imageStyles'],
  () => fetchImageStyles(token),
  {
    staleTime: 24 * 60 * 60 * 1000, // 24 hours
    cacheTime: 7 * 24 * 60 * 60 * 1000, // 7 days
  }
);
```

### Local Storage Fallback

```typescript
const STYLES_CACHE_KEY = 'notes_image_styles';
const STYLES_CACHE_TTL = 24 * 60 * 60 * 1000; // 24 hours

const getCachedStyles = (): ImageStyleVO[] | null => {
  const cached = localStorage.getItem(STYLES_CACHE_KEY);
  if (!cached) return null;

  const { styles, timestamp } = JSON.parse(cached);
  if (Date.now() - timestamp > STYLES_CACHE_TTL) {
    localStorage.removeItem(STYLES_CACHE_KEY);
    return null;
  }
  return styles;
};

const setCachedStyles = (styles: ImageStyleVO[]) => {
  localStorage.setItem(STYLES_CACHE_KEY, JSON.stringify({
    styles,
    timestamp: Date.now()
  }));
};
```

---

## Migration Notes

### Breaking Changes

- The `customPrompt` field in `NotesImageRequest` is now deprecated
- Use `style` field with a style ID instead

### Backward Compatibility

- Existing images generated with old `style` strings will continue to work
- The API still accepts raw style strings as fallback
- If a style ID is not found, the raw string is used as the style prompt

---

## API Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/notes/item/image/styles` | GET | List available image styles |
| `/api/notes/item/image/generate` | POST | Generate image with selected style |
| `/api/notes/item/{id}/image/stream` | GET | Stream generated image |
| `/api/notes/item/{id}/image` | DELETE | Delete generated image |

---

## Notes

- All image generation uses **Gemini Imagen API** (imagen-4.0-generate-001)
- Generated images are **PNG format**, **1024x1024 resolution**
- Images are stored in **MinIO** and streamed on demand
- **Authentication required** for all endpoints
- Image URLs require `?access_token=` query parameter for browser `<img>` elements
