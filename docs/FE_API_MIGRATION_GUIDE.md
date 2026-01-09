# Frontend API Migration Guide

## Overview

The backend has been migrated from a microservice architecture to a monolith. While legacy paths are currently supported via a rewrite filter, the frontend should migrate to the new API paths for better performance and long-term compatibility.

## Path Mapping Changes

### Word Service
| Legacy Path (Old) | New Path |
|-------------------|----------|
| `/wordBiz/word/main/...` | `/api/word/main/...` |
| `/wordBiz/word/pronunciation/...` | `/api/word/pronunciation/...` |
| `/wordBiz/word/fetch/...` | `/api/word/fetch/...` |
| `/wordBiz/word/star/list/...` | `/api/word/star/list/...` |
| `/wordBiz/word/example/star/list/...` | `/api/word/example/star/list/...` |
| `/wordBiz/word/review/...` | `/api/word/review/...` |
| `/wordBiz/word/paraphrase/...` | `/api/word/paraphrase/...` |

### UPMS Service
| Legacy Path (Old) | New Path |
|-------------------|----------|
| `/upmsBiz/sys/user/...` | `/api/sys/user/...` |
| `/upmsBiz/log/...` | `/api/log/...` |

### Tools Service
| Legacy Path (Old) | New Path |
|-------------------|----------|
| `/toolsBiz/rangi_windows/api/...` | `/api/rangi_windows/...` |

## Migration Steps

### 1. Update API Base URL Configuration

If you have a centralized API configuration, update the base paths:

```javascript
// Before
const API_CONFIG = {
  wordService: '/wordBiz/word',
  upmsService: '/upmsBiz',
  toolsService: '/toolsBiz/rangi_windows'
};

// After
const API_CONFIG = {
  wordService: '/api/word',
  upmsService: '/api',
  toolsService: '/api/rangi_windows'
};
```

### 2. Search and Replace Patterns

Run these search/replace operations across your codebase:

| Search | Replace |
|--------|---------|
| `/wordBiz/word/` | `/api/word/` |
| `/wordBiz/` | `/api/word/` |
| `/upmsBiz/sys/` | `/api/sys/` |
| `/upmsBiz/log/` | `/api/log/` |
| `/upmsBiz/` | `/api/` |
| `/toolsBiz/rangi_windows/` | `/api/rangi_windows/` |
| `/toolsBiz/` | `/api/` |

### 3. Example Endpoint Updates

```javascript
// Word query - Before
fetch('/wordBiz/word/main/query/gate/test?current=0&size=10', { method: 'POST' });

// Word query - After
fetch('/api/word/main/query/gate/test?current=0&size=10', { method: 'POST' });

// User info - Before
fetch('/upmsBiz/sys/user/info');

// User info - After
fetch('/api/sys/user/info');
```

### 4. Remove Gateway Routing Logic

If your frontend has logic to route to different microservice URLs, this can be simplified:

```javascript
// Before - routing to different services
const getServiceUrl = (service) => {
  switch(service) {
    case 'word': return `${gatewayUrl}/wordBiz`;
    case 'upms': return `${gatewayUrl}/upmsBiz`;
    case 'tools': return `${gatewayUrl}/toolsBiz`;
  }
};

// After - single monolith URL
const apiBaseUrl = '/api';
```

## Backward Compatibility

The backend currently supports legacy paths via `LegacyPathRewriteFilter`. This filter automatically rewrites:
- `/wordBiz/...` → `/api/word/...`
- `/upmsBiz/...` → `/api/...`
- `/toolsBiz/...` → `/api/...`

**Note:** This backward compatibility layer will be removed in a future release. Please migrate to the new paths.

## Testing Checklist

After migration, verify these key functionalities:

- [ ] Word search/query works
- [ ] Word pronunciation download works
- [ ] User authentication/login works
- [ ] Star list operations work
- [ ] Review functionality works
- [ ] Tools/Rangi Windows integration works

## Questions?

Contact the backend team if you encounter any issues during migration.
