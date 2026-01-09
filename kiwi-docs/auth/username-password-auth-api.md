# Kiwi Username/Password Authentication API

Last Updated: 2026-01-07
Status: ACTIVE
Owner: Auth Service / FE Platform Team

## 1. Overview

This document describes the API endpoints for user registration and username/password authentication in the Kiwi monolith application. These endpoints use the same token system as Google OAuth, ensuring unified token management across all authentication methods.

## 2. Base URL

```
Development: http://localhost:8088
Staging:     https://staging-api.kiwi.example
Production:  https://api.kiwi.example
```

## 3. Authentication Flow

### 3.1 Registration Flow
```
[FE Registration Form] --> POST /auth/oauth/register --> [Success Response]
                                                               |
                                                               v
                                                    [Redirect to Login]
```

### 3.2 Login Flow
```
[FE Login Form] --> POST /auth/oauth/login --> [Token Response]
                                              |
                                              v
                                    [Store token in memory/storage]
                                              |
                                              v
                                    [Use token for API requests]
```

### 3.3 Token Usage
```
[API Request] --> Authorization: Bearer <access_token> --> [Protected Resource]
```

## 4. API Endpoints

### 4.1 User Registration

**Endpoint:** `POST /auth/oauth/register`

**Description:** Register a new user account.

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "username": "string (required, unique)",
  "password": "string (required, min 6 characters)",
  "email": "string (required, unique, valid email format)",
  "realName": "string (optional)",
  "phone": "string (optional)",
  "avatar": "string (optional, URL)",
  "deptId": "integer (optional, default: 1)"
}
```

**Success Response (200 OK):**
```json
{
  "code": 0,
  "msg": "Success",
  "data": {
    "userId": 123,
    "username": "johndoe",
    "email": "john@example.com",
    "message": "Registration successful. Please login."
  },
  "success": true,
  "failed": false
}
```

**Error Responses:**

| Code | Message | Description |
|------|---------|-------------|
| 1 | Username is required | Missing username field |
| 1 | Password is required | Missing password field |
| 1 | Email is required | Missing email field |
| 1 | Password must be at least 6 characters | Password too short |
| 1 | Username already exists | Username is taken |
| 1 | Email already exists | Email is already registered |

**Example Error Response:**
```json
{
  "code": 1,
  "msg": "Username already exists",
  "data": null,
  "success": false,
  "failed": true
}
```

---

### 4.2 User Login

**Endpoint:** `POST /auth/oauth/login`

**Description:** Authenticate user and receive access token.

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

**Alternative:** Form URL Encoded
```
Content-Type: application/x-www-form-urlencoded

username=johndoe&password=mypassword
```

**Success Response (200 OK):**
```json
{
  "code": 0,
  "msg": "Success",
  "data": {
    "access_token": "550e8400-e29b-41d4-a716-446655440000",
    "token_type": "Bearer",
    "expires_in": 2592000,
    "scope": ["read", "write", "profile"],
    "refresh_token": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "userInfo": {
      "userId": 123,
      "username": "johndoe",
      "email": "john@example.com",
      "realName": "John Doe",
      "avatar": "https://example.com/avatar.jpg"
    }
  },
  "success": true,
  "failed": false
}
```

**Error Responses:**

| Code | Message | Description |
|------|---------|-------------|
| 1 | Username and password are required | Missing credentials |
| 1 | Invalid username or password | Wrong credentials |

---

### 4.3 Get Current User Info

**Endpoint:** `GET /auth/oauth/me`

**Description:** Get the current authenticated user's information.

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Success Response (200 OK):**
```json
{
  "code": 0,
  "msg": "Success",
  "data": {
    "userId": 123,
    "username": "johndoe",
    "email": "john@example.com",
    "realName": "John Doe",
    "avatar": "https://example.com/avatar.jpg",
    "deptId": 1,
    "isAdmin": false,
    "authMethod": "standard"
  },
  "success": true,
  "failed": false
}
```

**Note:** `authMethod` will be `"standard"` for username/password login or `"google_sso"` for Google OAuth login.

**Error Responses:**

| Code | Message | Description |
|------|---------|-------------|
| 1 | Authorization header is required | Missing token |
| 1 | Invalid token | Token not found |
| 1 | Token expired | Token has expired |

---

### 4.4 Logout

**Endpoint:** `POST /auth/oauth/logout`

**Description:** Logout and invalidate the access token.

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Success Response (200 OK):**
```json
{
  "code": 0,
  "msg": "Success",
  "data": true,
  "success": true,
  "failed": false
}
```

---

### 4.5 Refresh Token

**Endpoint:** `POST /auth/oauth/refresh`

**Description:** Refresh the access token using a refresh token.

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "refreshToken": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
}
```

**Success Response (200 OK):**
```json
{
  "code": 0,
  "msg": "Success",
  "data": {
    "access_token": "new-access-token-uuid",
    "token_type": "Bearer",
    "expires_in": 2592000,
    "scope": ["read", "write", "profile"],
    "refresh_token": "new-refresh-token-uuid"
  },
  "success": true,
  "failed": false
}
```

**Error Responses:**

| Code | Message | Description |
|------|---------|-------------|
| 1 | Refresh token is required | Missing refresh token |
| 1 | Invalid or expired refresh token | Token invalid/expired |

---

### 4.6 Check Username Availability

**Endpoint:** `GET /auth/oauth/check-username`

**Description:** Check if a username is available for registration.

**Query Parameters:**
```
username: string (required)
```

**Example:** `GET /auth/oauth/check-username?username=johndoe`

**Success Response (200 OK):**
```json
{
  "code": 0,
  "msg": "Success",
  "data": true,
  "success": true,
  "failed": false
}
```

**Note:** `data` is `true` if username is available, `false` if already taken.

---

### 4.7 Check Email Availability

**Endpoint:** `GET /auth/oauth/check-email`

**Description:** Check if an email is available for registration.

**Query Parameters:**
```
email: string (required)
```

**Example:** `GET /auth/oauth/check-email?email=john@example.com`

**Success Response (200 OK):**
```json
{
  "code": 0,
  "msg": "Success",
  "data": true,
  "success": true,
  "failed": false
}
```

**Note:** `data` is `true` if email is available, `false` if already registered.

---

## 5. Token Information

| Token Type | Lifetime | Storage Recommendation |
|------------|----------|------------------------|
| Access Token | 30 days | Memory or secure storage |
| Refresh Token | 90 days | Secure storage only |

## 6. Frontend Integration Guide

### 6.1 Registration Implementation

```javascript
async function register(userData) {
  const response = await fetch('/auth/oauth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      username: userData.username,
      password: userData.password,
      email: userData.email,
      realName: userData.realName // optional
    })
  });

  const data = await response.json();

  if (data.code === 0) {
    // Registration successful - redirect to login
    return { success: true, data: data.data };
  } else {
    // Registration failed - show error
    return { success: false, error: data.msg };
  }
}
```

### 6.2 Login Implementation

```javascript
async function login(username, password) {
  const response = await fetch('/auth/oauth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username, password })
  });

  const data = await response.json();

  if (data.code === 0) {
    // Store tokens
    const { access_token, refresh_token, userInfo } = data.data;

    // Store in memory or secure storage
    sessionStorage.setItem('access_token', access_token);
    sessionStorage.setItem('refresh_token', refresh_token);

    return { success: true, user: userInfo };
  } else {
    return { success: false, error: data.msg };
  }
}
```

### 6.3 Making Authenticated Requests

```javascript
async function fetchWithAuth(url, options = {}) {
  const token = sessionStorage.getItem('access_token');

  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${token}`
    }
  });

  // Handle 401 - try refresh token
  if (response.status === 401) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      // Retry with new token
      return fetchWithAuth(url, options);
    } else {
      // Redirect to login
      window.location.href = '/login';
    }
  }

  return response.json();
}
```

### 6.4 Token Refresh Implementation

```javascript
async function refreshAccessToken() {
  const refreshToken = sessionStorage.getItem('refresh_token');

  if (!refreshToken) {
    return false;
  }

  const response = await fetch('/auth/oauth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ refreshToken })
  });

  const data = await response.json();

  if (data.code === 0) {
    sessionStorage.setItem('access_token', data.data.access_token);
    sessionStorage.setItem('refresh_token', data.data.refresh_token);
    return true;
  }

  return false;
}
```

### 6.5 Logout Implementation

```javascript
async function logout() {
  const token = sessionStorage.getItem('access_token');

  if (token) {
    await fetch('/auth/oauth/logout', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
  }

  // Clear local storage
  sessionStorage.removeItem('access_token');
  sessionStorage.removeItem('refresh_token');

  // Redirect to login
  window.location.href = '/login';
}
```

### 6.6 Real-time Username/Email Validation

```javascript
// Debounced username check
let usernameCheckTimeout;

function onUsernameChange(username) {
  clearTimeout(usernameCheckTimeout);

  usernameCheckTimeout = setTimeout(async () => {
    const response = await fetch(`/auth/oauth/check-username?username=${encodeURIComponent(username)}`);
    const data = await response.json();

    if (data.code === 0) {
      if (data.data) {
        showSuccess('Username is available');
      } else {
        showError('Username is already taken');
      }
    }
  }, 500); // 500ms debounce
}
```

## 7. Security Recommendations

| Concern | Recommendation |
|---------|----------------|
| Token Storage | Use `sessionStorage` for access token, avoid `localStorage` for sensitive data |
| HTTPS | Always use HTTPS in production |
| Password | Never log or store plain-text passwords |
| XSS | Sanitize all user inputs |
| CSRF | Not required for Bearer token auth (stateless) |

## 8. Error Handling

All API responses follow this structure:

```typescript
interface ApiResponse<T> {
  code: number;      // 0 = success, 1 = failure
  msg: string;       // Human-readable message
  data: T | null;    // Response data or null on error
  success: boolean;  // true if code === 0
  failed: boolean;   // true if code !== 0
}
```

## 9. Testing with cURL

```bash
# Register
curl -X POST http://localhost:8088/auth/oauth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser","password":"password123","email":"test@example.com"}'

# Login
curl -X POST http://localhost:8088/auth/oauth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser","password":"password123"}'

# Get current user (replace <TOKEN> with actual token)
curl -X GET http://localhost:8088/auth/oauth/me \
  -H 'Authorization: Bearer <TOKEN>'

# Logout
curl -X POST http://localhost:8088/auth/oauth/logout \
  -H 'Authorization: Bearer <TOKEN>'

# Check username availability
curl -X GET 'http://localhost:8088/auth/oauth/check-username?username=testuser'

# Check email availability
curl -X GET 'http://localhost:8088/auth/oauth/check-email?email=test@example.com'
```

## 10. Changelog

| Date | Change | Author |
|------|--------|--------|
| 2026-01-07 | Initial API documentation | System |

---
End of document.
