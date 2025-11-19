# Kiwi Auth Frontend Integration API

Last Updated: 2025-11-19
Status: ACTIVE
Owner: Auth Service / FE Platform Team

## 1. Overview
The Kiwi authentication system now supports two parallel login methods:
1. Google SSO (existing)
2. Native username/password (reintroduced via `/oauth/username-password/login`)

Both methods generate a Kiwi OAuth2 access token stored in Redis and expose identical validation endpoints. Tokens include an `auth_method` field in their additional information:
- `auth_method=google_sso` (Google flow)
- `auth_method=standard` (username/password flow)

Frontend can unify handling post-login by relying on the common token response shape.

## 2. Environment Base URLs
Replace host/port with your environment values.
```
DEV:    http://localhost:3001
STAGING: https://staging-auth.kiwi.example
PROD:    https://auth.kiwi.example
```

## 3. Supported Flows
### 3.1 Username / Password Flow
```
[FE Login Form] --> POST /oauth/username-password/login --> [Kiwi Auth] --> access_token & refresh_token
```
### 3.2 Google SSO Flow (Authorization Code)
```
[FE] GET /oauth/google/authorize -> redirect to Google
[User Grants Access]
Google redirects -> /oauth/google/callback -> Kiwi issues system token -> redirects FE with token query param
```
### 3.3 Google Direct Token Flow
FE obtains a Google access token client-side and POSTs to `/oauth/google/login` for server-side token creation.

## 4. Endpoint Summary
| Method | Endpoint | Purpose | Notes |
|--------|----------|---------|-------|
| POST | `/oauth/username-password/login` | Native login | JSON or form data |
| GET | `/oauth/google/authorize` | Start Google OAuth | Returns authorization URL |
| GET | `/oauth/google/callback` | OAuth redirect handler | Issues system token then redirects |
| POST | `/oauth/google/login` | Direct Google token exchange | Accepts Google access token |
| POST | `/oauth/google/refresh` | Refresh Google access token | Uses Google refresh token |
| POST | `/oauth/google/logout` | Logout Google + system token | Requires `systemToken` param |
| POST | `/oauth/check_token` | Detailed token validation | Returns enriched info incl. Google cache |
| GET | `/oauth/token_info` | Lightweight token info | No Google validation side-effects |
| POST | `/oauth/validate_token` | Boolean validity check | Includes Google token validity for SSO |
| DELETE | `/oauth/logout` | Standard logout | Uses `Authorization: Bearer` header |

## 5. Data Contracts
### 5.1 Standard Success Wrapper
```
{
  "code": 0,
  "msg": "login success",
  "data": { ... }
}
```
Failure:
```
{
  "code": 1,
  "msg": "invalid username or password"
}
```
(`R.success` / `R.failed` conventions)

### 5.2 Token Response (Username/Password)
```
{
  "access_token": "<uuid>",
  "token_type": "Bearer",
  "expires_in": 2592000,       // ~30 days
  "scope": ["read","write","profile"],
  "refresh_token": "<uuid>"
}
```
Additional details retrievable via `/oauth/check_token`:
```
{
  "active": true,
  "client_id": "password-client",
  "scope": ["read","write","profile"],
  "user_id": <number>,
  "user_name": "<username>",
  "dept_id": <number>,
  "auth_method": "standard",
  "is_admin": <boolean>
}
```

### 5.3 Token Response (Google SSO)
Same shape + additional Google info via `/oauth/check_token`:
```
{
  "auth_method": "google_sso",
  "google_user_id": "<google id>",
  "google_email": "user@example.com",
  "google_name": "Display Name",
  "google_picture": "https://...",
  "google_token_expires_in": 3487,
  "google_token_expired": false,
  "google_token_expiring_soon": false
}
```

## 6. Endpoint Details
### 6.1 POST /oauth/username-password/login
Accepts JSON or form.
JSON Example:
```
POST /oauth/username-password/login
Content-Type: application/json
{
  "username": "alice",
  "password": "p@ssw0rd"
}
```
Form Example:
```
POST /oauth/username-password/login
Content-Type: application/x-www-form-urlencoded
username=alice&password=p%40ssw0rd
```
Responses: See 5.2.
Errors:
- 400 wrapper: missing fields
- 401 wrapper: invalid credentials

### 6.2 GET /oauth/google/authorize
Returns JSON `{ authorizationUrl, state }` for redirect.
```
GET /oauth/google/authorize?state=xyz123
```

### 6.3 GET /oauth/google/callback
Internal redirect handler; FE receives redirect to `homePage?active=search&token=<systemToken>&user=<encodedName>`.
Errors appended as `error=` query param.

### 6.4 POST /oauth/google/login
```
POST /oauth/google/login
Content-Type: application/json
{
  "accessToken": "<google_access_token>"
}
```
Returns system token (same shape as username/password) with `auth_method=google_sso`.

### 6.5 POST /oauth/google/refresh
```
POST /oauth/google/refresh
{
  "refreshToken": "<google_refresh_token>"
}
```
Returns refreshed Google token fields and updates cache.

### 6.6 POST /oauth/google/logout
```
POST /oauth/google/logout?systemToken=<system_access_token>
```
Invalidates system + Google tokens.

### 6.7 POST /oauth/check_token
```
POST /oauth/check_token
Content-Type: application/x-www-form-urlencoded
token=<system_access_token>
```
Returns enriched map. Use for FE session hydration.

### 6.8 GET /oauth/token_info
Lightweight introspection.
```
GET /oauth/token_info?token=<system_access_token>
```

### 6.9 POST /oauth/validate_token
Returns `{ code:0, data:true/false }` validity boolean.

### 6.10 DELETE /oauth/logout
```
DELETE /oauth/logout
Authorization: Bearer <system_access_token>
```
Idempotent.

## 7. Token Lifetimes
| Token Type | Lifetime | Notes |
|------------|----------|-------|
| Access Token (system) | 30 days | Random UUID stored in Redis |
| Refresh Token (system) | 90 days | Expiring token object |
| Google Access Token | Google-defined (≈1h) | Cached; validated periodically |
| Google Refresh Token | Long-lived | Only if provided by Google |

## 8. Frontend Integration Guidelines
1. Preferred storage: httpOnly secure cookie for access token if using server-side rendering; otherwise memory + refresh logic.
2. Include `Authorization: Bearer <access_token>` header on resource requests.
3. On app bootstrap:
   - Read token from storage.
   - Call `/oauth/check_token` for full context (roles, google info).
   - If `google_token_expiring_soon` attempt `/oauth/google/refresh` (if you have refresh token).
4. Distinguish login method: inspect `auth_method`.
5. On 401 from resource server: attempt silent re-auth (if refresh token available) else show login.
6. Avoid sending token via query params except during initial redirect.

### 8.1 Unified Login Pseudocode
```js
async function loginUsernamePassword(u, p) {
  const res = await fetch('/oauth/username-password/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: u, password: p })
  });
  const data = await res.json();
  if (data.code !== 0) throw new Error(data.msg);
  return data.data; // { access_token, refresh_token, ... }
}

async function hydrateSession(token) {
  const form = new URLSearchParams({ token });
  const res = await fetch('/oauth/check_token', { method: 'POST', body: form });
  const data = await res.json();
  if (data.code !== 0) throw new Error(data.msg);
  return data.data; // includes auth_method
}
```

### 8.2 Error Handling Pattern
- Display `msg` from failed response.
- Implement exponential backoff for transient network issues (NOT for credential errors).

## 9. Security Recommendations
| Concern | Recommendation |
|---------|----------------|
| Token Theft | Prefer not storing tokens in localStorage; use memory or secure cookies. |
| CSRF | For cookie-based auth, use double-submit or SameSite=strict. |
| XSS | Sanitize user-provided data; tokens must never be injected into DOM as text. |
| Refresh Logic | Gracefully degrade if refresh fails; force re-login. |
| Logout | Always call `/oauth/logout` client-side and clear local state. |

## 10. Migration Notes
Legacy FE expecting only Google tokens should now:
1. Add a username/password form.
2. After any login, treat tokens uniformly.
3. Read `auth_method` to adjust UI (e.g., show "Link Google" button for standard users).
4. Existing Google flow endpoints unchanged.

## 11. Edge Cases
| Case | Handling |
|------|----------|
| Missing fields on login | Immediate `code=1` failure; show inline validation. |
| Expired access token | 401 on resource; FE triggers refresh or re-login. |
| Google token expired but system token valid | `/oauth/check_token` returns `google_token_expired=true`; prompt re-auth Google if feature requires Google-specific data. |
| Stolen token logout attempt | Idempotent; returns success even if already invalidated. |

## 12. Testing Cheatsheet
```bash
# Username/password login
curl -X POST http://localhost:3001/oauth/username-password/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"p@ssw0rd"}'

# Validate token
curl -X POST http://localhost:3001/oauth/check_token -d 'token=<ACCESS>'

# Google authorize
curl -X GET http://localhost:3001/oauth/google/authorize

# Logout
curl -X DELETE http://localhost:3001/oauth/logout -H 'Authorization: Bearer <ACCESS>'
```

## 13. Future Enhancements
- Add rate limiting headers for login endpoints.
- Implement refresh for standard tokens (currently manual new login or password grant via /oauth/token if enabled).
- Add endpoints for password change & account linking Google.<br>

## 14. Change Log
| Date | Change | Author |
|------|--------|--------|
| 2025-11-19 | Initial unified FE auth API doc | System

---
End of document.

