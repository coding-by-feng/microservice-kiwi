# Conversation API - Emoji Fields Removal

## Overview

The emoji fields have been removed from the AI conversation feature. The frontend needs to update accordingly to remove emoji-related UI elements and data handling.

## API Changes

### 1. Speaker Object

**Before:**
```json
{
  "id": 1,
  "speakerIndex": 0,
  "name": "Alice",
  "voice": "alloy",
  "avatarEmoji": "👩"
}
```

**After:**
```json
{
  "id": 1,
  "speakerIndex": 0,
  "name": "Alice",
  "voice": "alloy"
}
```

**Removed field:** `avatarEmoji`

### 2. Message Object

**Before:**
```json
{
  "id": 1,
  "speakerId": 1,
  "speakerName": "Alice",
  "sequence": 1,
  "text": "Hello, how are you?",
  "emoji": "👋",
  "audioStatus": "READY",
  "audioUrl": "minio/kiwi/conversation/...",
  "audioDurationMs": 2500
}
```

**After:**
```json
{
  "id": 1,
  "speakerId": 1,
  "speakerName": "Alice",
  "sequence": 1,
  "text": "Hello, how are you?",
  "audioStatus": "READY",
  "audioUrl": "minio/kiwi/conversation/...",
  "audioDurationMs": 2500
}
```

**Removed field:** `emoji`

## Frontend Changes Required

### 1. Type Definitions

Update TypeScript interfaces:

```typescript
// Before
interface Speaker {
  id: number;
  speakerIndex: number;
  name: string;
  voice: string;
  avatarEmoji: string;  // Remove this
}

interface Message {
  id: number;
  speakerId: number;
  speakerName: string;
  sequence: number;
  text: string;
  emoji: string;  // Remove this
  audioStatus: string;
  audioUrl: string;
  audioDurationMs: number;
}

// After
interface Speaker {
  id: number;
  speakerIndex: number;
  name: string;
  voice: string;
}

interface Message {
  id: number;
  speakerId: number;
  speakerName: string;
  sequence: number;
  text: string;
  audioStatus: string;
  audioUrl: string;
  audioDurationMs: number;
}
```

### 2. UI Components

Remove or replace the following UI elements:

1. **Speaker Avatar Emoji Display**
   - Remove emoji avatar display next to speaker names
   - Consider using initials or colored circles as speaker indicators instead

2. **Message Emoji Display**
   - Remove emoji display next to messages
   - Remove any emoji-related styling or animations

### 3. SSE Event Handling

The `metadata` event payload no longer includes `avatarEmoji` in speakers:

```typescript
// SSE metadata event
interface MetadataPayload {
  conversationId: number;
  topic: string;
  speakers: Speaker[];  // No avatarEmoji field
  totalMessageCount: number;
}
```

The `message` event payload no longer includes `emoji`:

```typescript
// SSE message event
interface MessagePayload {
  id: number;
  speakerId: number;
  speakerName: string;
  sequence: number;
  text: string;
  // emoji field removed
  audioStatus: string;
  audioUrl: string;
  audioDurationMs: number;
}
```

## Alternative Speaker Identification

Since `avatarEmoji` is removed, consider these alternatives for speaker identification:

1. **Color-coded speakers** - Assign a unique color to each speaker based on `speakerIndex`
2. **Initials** - Use the first letter of the speaker's `name`
3. **Numbered badges** - Use `speakerIndex + 1` as a badge number

Example implementation:

```typescript
const speakerColors = ['#4F46E5', '#10B981', '#F59E0B', '#EF4444'];

function getSpeakerColor(speakerIndex: number): string {
  return speakerColors[speakerIndex % speakerColors.length];
}

function getSpeakerInitial(name: string): string {
  return name.charAt(0).toUpperCase();
}
```

## Speaker Name Consistency

The AI now generates **character/role names** as speaker names for role-play scenarios. This means:

- For a sales pitch practice, speaker names will be like "Liam" (salesperson) and "Mrs. Henderson" (customer)
- The speaker `name` field will match exactly how characters address each other in the conversation
- No more mismatch between display names and in-dialogue names

**Example:**
```json
{
  "speakers": [
    {"index": 0, "name": "Liam", "personality": "Eager salesperson practicing his pitch"},
    {"index": 1, "name": "Mrs. Henderson", "personality": "Potential customer browsing for windows"}
  ],
  "messages": [
    {"speakerIndex": 0, "text": "Good morning, Mrs. Henderson! Looking for new windows today?"},
    {"speakerIndex": 1, "text": "Yes, Liam. I'm interested in energy-efficient options."}
  ]
}
```

## Testing Checklist

- [ ] Conversation generation works without emoji fields
- [ ] Speaker list displays correctly without avatar emoji
- [ ] Message list displays correctly without message emoji
- [ ] SSE streaming handles updated payload structure
- [ ] No console errors related to missing emoji fields
- [ ] Existing conversations load correctly (backward compatible)
- [ ] Speaker names match the names used in conversation text
