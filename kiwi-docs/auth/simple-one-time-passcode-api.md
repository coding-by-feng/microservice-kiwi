# Simple One-Time Passcode API

This document describes the simple 6-digit passcode verification API exposed by the Auth service. It is intended for verifying eligible users before granting access to important resources.

- Service: `kiwi-auth`
- Endpoint base path: `/oauth/one-time`
- Config source: Spring Cloud Config (via Config Server)

## Overview

The Auth service exposes an endpoint that validates a 6-digit numeric passcode. The passcode is configured centrally in the Config Server and injected via the `bootstrap.yml` settings. On success, the endpoint returns HTTP 200 with an empty body. On failure, it returns an appropriate error status and a short message.

The endpoint is allowed anonymously by the existing security configuration because it is mounted under `/oauth/**`.

## Endpoint

- Method: `POST`
- Path: `/oauth/one-time/verify-code`
- Content-Type: `application/json` (preferred) or query param

### Request

Either JSON body or query parameter can be used:

- JSON body:

```json
{
  "code": "123456"
}
```

- Or as a query parameter:

```
POST /oauth/one-time/verify-code?code=123456
```

Notes:
- `code` must be exactly 6 digits (`0-9`).

### Responses

- 200 OK
  - The passcode is valid. Empty body.
- 400 Bad Request
  - Code missing: `"code is required"`
  - Invalid format: `"code must be 6 digits"`
- 401 Unauthorized
  - Mismatch: `"invalid code"`
- 503 Service Unavailable
  - Passcode not configured: `"passcode not configured"`

## Configuration

The passcode is read from Spring configuration under the following key and is hot-refreshable via Spring Cloud Config:

```
kiwi.auth.simple.passcode
```

Example Config Server YAML (e.g., `kiwi-auth.yml` in the config repository):

```yaml
kiwi:
  auth:
    simple:
      # 6-digit numeric passcode
      passcode: "123456"
```

This is bound to the `SimpleAuthProperties` bean in the auth service and can be updated at runtime (with `/actuator/refresh` if manual refresh is needed).

## Security Considerations

- The comparison uses constant-time hashing to reduce timing-attack leakage.
- Keep the passcode short-lived and rotate regularly via the Config Server.
- Consider rate-limiting this endpoint at the gateway or via a filter if exposed publicly.

## cURL Examples

- JSON body:

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  http://kiwi-auth:3001/oauth/one-time/verify-code \
  -d '{"code":"123456"}'
```

- Query parameter:

```bash
curl -i -X POST \
  'http://kiwi-auth:3001/oauth/one-time/verify-code?code=123456'
```

## Implementation Pointers

- Controller: `SimpleAuthController` (`/oauth/one-time/verify-code`)
- Properties: `SimpleAuthProperties` (prefix `kiwi.auth.simple`)
- Security config: Requests under `/oauth/**` are permitted without authentication in `WebSecurityConfigurer`.

