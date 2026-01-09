# YouTube Channel API Documentation

## Base Path

`/api/ai/ytb/channel`

---

## Endpoints

### 1. Get Channel by ID

```http
GET /api/ai/ytb/channel/{channelId}
```

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| `channelId` | Long | path | Yes | Channel ID |

**Response:** `R<YtbChannel>`

---

### 2. Get Channel by Link

```http
GET /api/ai/ytb/channel/link
```

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| `channelLink` | String | query | Yes | YouTube channel link |

**Response:** `R<YtbChannel>`

---

### 3. Get All Channels (Paginated)

```http
GET /api/ai/ytb/channel
```

**Parameters:**
| Name | Type | Location | Required | Default | Description |
|------|------|----------|----------|---------|-------------|
| `current` | Long | query | No | 1 | Page number |
| `size` | Long | query | No | 20 | Items per page |

**Response:** `R<Page<YtbChannel>>`

---

### 4. Get Enabled Channels

```http
GET /api/ai/ytb/channel/enabled
```

**Parameters:** None

**Response:** `R<List<YtbChannel>>`

---

### 5. Get Channel Videos

```http
GET /api/ai/ytb/channel/{channelId}/videos
```

**Parameters:**
| Name | Type | Location | Required | Default | Description |
|------|------|----------|----------|---------|-------------|
| `channelId` | Long | path | Yes | - | Channel ID |
| `current` | Long | query | No | 1 | Page number |
| `size` | Long | query | No | 20 | Items per page |

**Response:** `R<Page<YtbChannelVideo>>`

---

### 6. Create Channel

```http
POST /api/ai/ytb/channel
```

**Authorization:** `ADMIN` role required

**Request Body:** `YtbChannel` (JSON)

```json
{
  "channelLink": "https://www.youtube.com/@example",
  "channelName": "Example Channel",
  "description": "Channel description",
  "enabled": true
}
```

**Response:** `R<YtbChannel>`

---

### 7. Update Channel

```http
PUT /api/ai/ytb/channel/{channelId}
```

**Authorization:** `ADMIN` role required

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| `channelId` | Long | path | Yes | Channel ID |

**Request Body:** `YtbChannel` (JSON)

**Response:** `R<YtbChannel>`

---

### 8. Enable Channel

```http
POST /api/ai/ytb/channel/{channelId}/enable
```

**Authorization:** `ADMIN` role required

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| `channelId` | Long | path | Yes | Channel ID |

**Response:** `R<Void>`

---

### 9. Disable Channel

```http
POST /api/ai/ytb/channel/{channelId}/disable
```

**Authorization:** `ADMIN` role required

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| `channelId` | Long | path | Yes | Channel ID |

**Response:** `R<Void>`

---

### 10. Delete Channel

```http
DELETE /api/ai/ytb/channel/{channelId}
```

**Authorization:** `ADMIN` role required

**Parameters:**
| Name | Type | Location | Required | Description |
|------|------|----------|----------|-------------|
| `channelId` | Long | path | Yes | Channel ID |

**Response:** `R<Void>`

---

## Response Format

All endpoints return a standard response wrapper:

```json
{
  "code": 0,
  "msg": "success",
  "data": { ... }
}
```

### Error Response

```json
{
  "code": 1,
  "msg": "Error message",
  "data": null
}
```

---

## Models

### YtbChannel

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Channel ID |
| `channelLink` | String | YouTube channel URL |
| `channelName` | String | Channel name |
| `description` | String | Channel description |
| `enabled` | Boolean | Whether channel is enabled |
| `createTime` | DateTime | Creation timestamp |
| `updateTime` | DateTime | Last update timestamp |

### YtbChannelVideo

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Video ID |
| `channelId` | Long | Associated channel ID |
| `videoUrl` | String | YouTube video URL |
| `title` | String | Video title |
| `description` | String | Video description |
| `publishedAt` | DateTime | Video publish date |
| `createTime` | DateTime | Creation timestamp |
