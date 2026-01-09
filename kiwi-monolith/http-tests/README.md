# Kiwi Monolith HTTP Tests

This directory contains HTTP test files for testing the Kiwi Monolith API endpoints using IntelliJ IDEA's HTTP Client or VS Code's REST Client extension.

## Prerequisites

1. Start the Kiwi Monolith application:
   ```bash
   cd kiwi-monolith
   DB_USERNAME=root DB_PASSWORD=kiwi123 REDIS_HOST=localhost REDIS_PASSWORD=kiwi123 mvn spring-boot:run
   ```

2. Ensure the required services are running:
   - MySQL database
   - Redis server
   - Elasticsearch (optional)

## File Structure

| File | Description |
|------|-------------|
| `http-client.env.json` | Environment configuration (dev/prod) |
| `01-auth.http` | Authentication endpoints |
| `02-upms.http` | User Permission Management |
| `03-word-main.http` | Word Main Controller |
| `04-word-fetch-queue.http` | Fetch Queue operations |
| `05-word-pronunciation.http` | Pronunciation endpoints |
| `06-word-paraphrase.http` | Paraphrase & Star List |
| `07-word-star-list.http` | Word Star List |
| `08-word-review.http` | Word Review & TTS |
| `09-ai-assistant.http` | AI Translation/Grammar |
| `10-ai-history.http` | AI Call History |
| `11-youtube-video.http` | YouTube Video & Subtitles |
| `12-youtube-channel.http` | YouTube Channel management |
| `13-tools-project.http` | Project Management |
| `14-tools-export.http` | Export (Excel/PDF) |
| `15-tools-todo.http` | Todo Task management |
| `16-flow.http` | Flowable workflow |
| `17-grammar.http` | Grammar resources |
| `18-example-star-list.http` | Example Star List |

## Usage

### IntelliJ IDEA

1. Open any `.http` file
2. Select environment from the dropdown (dev/prod)
3. Click the green play button next to any request
4. View response in the Response panel

### VS Code (REST Client Extension)

1. Install the "REST Client" extension
2. Open any `.http` file
3. Click "Send Request" above each request
4. View response in a new tab

### Authentication Flow

1. First, run the **Login** request in `01-auth.http`
2. The auth token is automatically saved to `{{authToken}}`
3. Subsequent requests will use this token

## Environment Variables

The `http-client.env.json` file defines:

```json
{
  "dev": {
    "baseUrl": "http://localhost:8080",
    "authToken": "{{$auth.token}}"
  }
}
```

## Common Response Format

Success:
```json
{
  "code": 0,
  "msg": "Success",
  "data": { ... }
}
```

Error:
```json
{
  "code": 1,
  "msg": "Error message",
  "data": null
}
```

## Tips

1. **Run authentication first** - Most endpoints require a valid auth token
2. **Check IDs** - Replace placeholder IDs (1, 2, etc.) with actual IDs from your database
3. **URL encoding** - Special characters in URLs should be encoded (e.g., spaces as `%20`)
4. **YouTube tests** - Replace the test video URL with an actual YouTube video
5. **Admin endpoints** - Some endpoints require ADMIN role

## Testing Order Recommendation

1. `01-auth.http` - Login first
2. `02-upms.http` - Verify user permissions
3. `03-word-main.http` - Test word queries
4. `07-word-star-list.http` - Create star lists
5. `09-ai-assistant.http` - Test AI features
6. `11-youtube-video.http` - Test subtitle features

## Troubleshooting

### 401 Unauthorized
- Run the login request first
- Check if the token has expired

### 403 Forbidden
- The endpoint requires ADMIN role
- Login with an admin account

### 404 Not Found
- Check if the resource ID exists
- Verify the endpoint path is correct

### 500 Internal Server Error
- Check application logs
- Verify database connection
