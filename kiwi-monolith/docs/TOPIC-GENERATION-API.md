# Topic Generation API Enhancement

## Overview

The conversation topic generation API now supports custom prompts. Users can provide a basic topic idea, and the AI will refine it into a structured, engaging conversation topic with suggested parameters.

## Endpoint

```
POST /api/ai/conversation/topic/random
```

## Request

### Headers
```
Authorization: Bearer <token>
Content-Type: application/json
```

### Body

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `prompt` | string | No | null | Custom topic idea to refine. If provided, generates topic based on this input. |
| `category` | string | No | `LIFESTYLE` | Topic category (ignored when `prompt` is provided) |
| `difficulty` | string | No | `INTERMEDIATE` | Difficulty level for the conversation |
| `language` | string | No | `en` | Target language code |

### Category Options
- `LIFESTYLE` - Daily life topics
- `TRAVEL` - Travel and tourism
- `FOOD` - Food and dining
- `WORK` - Professional and business
- `HOBBIES` - Hobbies and interests
- `HEALTH` - Health and wellness

### Difficulty Options
- `BEGINNER` - Simple vocabulary and short sentences
- `INTERMEDIATE` - Moderate complexity
- `ADVANCED` - Complex topics and vocabulary

## Usage Examples

### 1. Random Topic Generation (Existing Behavior)

Generate a random topic based on category and difficulty:

```json
{
  "category": "TRAVEL",
  "difficulty": "INTERMEDIATE",
  "language": "en"
}
```

### 2. Custom Prompt Topic Generation (New Feature)

Provide a basic idea and let AI refine it:

```json
{
  "prompt": "ordering coffee at a cafe",
  "difficulty": "BEGINNER",
  "language": "en"
}
```

The AI will expand this into a detailed scenario like:
> "You're at a cozy neighborhood cafe during the morning rush. Practice ordering your favorite coffee drink, asking about the menu options, and making small talk with the barista while waiting for your order."

### 3. More Custom Prompt Examples

**Restaurant scenario:**
```json
{
  "prompt": "complaining about cold food at restaurant",
  "difficulty": "INTERMEDIATE"
}
```

**Job interview:**
```json
{
  "prompt": "job interview for software developer",
  "difficulty": "ADVANCED"
}
```

**Shopping:**
```json
{
  "prompt": "returning a defective product",
  "difficulty": "INTERMEDIATE"
}
```

## Response

```json
{
  "code": 0,
  "msg": "Success",
  "data": {
    "topic": "A detailed, engaging conversation scenario...",
    "category": "LIFESTYLE",
    "difficulty": "INTERMEDIATE",
    "suggestedSpeakerCount": 2,
    "suggestedDuration": "FIVE_MINUTES",
    "keywords": ["keyword1", "keyword2", "keyword3", "keyword4"]
  }
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `topic` | string | The refined conversation topic/scenario |
| `category` | string | Topic category |
| `difficulty` | string | Difficulty level |
| `suggestedSpeakerCount` | number | Recommended number of speakers (2-4) |
| `suggestedDuration` | string | Suggested conversation length |
| `keywords` | array | Related vocabulary keywords for the topic |

### Duration Options
- `TWO_MINUTES` - Quick, simple exchanges
- `FIVE_MINUTES` - Standard conversation length
- `TEN_MINUTES` - Extended discussions

## Frontend Implementation Notes

### UI Suggestions

1. **Input Field**: Add a text input for custom topic ideas
   - Placeholder: "Enter a topic idea (e.g., 'ordering food at a restaurant')"
   - Optional field - can be left empty for random generation

2. **Quick Suggestions**: Provide clickable suggestion chips
   - "ordering coffee"
   - "asking for directions"
   - "making a hotel reservation"
   - "job interview"

3. **Category Selector**: Hide or disable when custom prompt is entered (category is ignored)

4. **Difficulty Selector**: Keep visible - applies to both random and custom generation

### Example UI Flow

```
┌─────────────────────────────────────────────────────┐
│  Generate Conversation Topic                        │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Topic Idea (optional):                             │
│  ┌─────────────────────────────────────────────┐   │
│  │ ordering coffee at a cafe                    │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  Quick suggestions:                                 │
│  [ordering food] [asking directions] [hotel] [job] │
│                                                     │
│  Difficulty:  ○ Beginner  ● Intermediate  ○ Advanced│
│                                                     │
│              [ Generate Topic ]                     │
│                                                     │
└─────────────────────────────────────────────────────┘
```

## Error Handling

| Code | Message | Cause |
|------|---------|-------|
| 401 | Unauthorized | Missing or invalid token |
| 500 | Failed to generate conversation topic | AI service error |

## Changelog

- **v1.1.0** - Added `prompt` field for custom topic generation
- **v1.0.0** - Initial release with random topic generation
