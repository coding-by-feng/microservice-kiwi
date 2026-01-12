# Rangi Windows API Migration Guide

This document describes the API path changes for the Rangi Windows (Project Management) module and provides guidance for frontend adaptation.

## API Path Changes

### Base Path Migration

| Old Path | New Path | Auth Required |
|----------|----------|---------------|
| `/rangi_windows/api` | `/api/tools/project` | Yes |
| `/rangi_windows/api/export` | `/rangi_windows/api/export` (unchanged) | No |

### Authentication

- **Legacy Path**: `/rangi_windows/api/**` - Public (no auth required)
- **New Path**: `/api/tools/project/**` - Requires Bearer token authentication

> **Recommendation**: Use the legacy path `/rangi_windows/api/**` for public access, or the new path `/api/tools/project/**` for authenticated access.

---

## Endpoint Mapping

### Projects CRUD

| Method | Legacy Endpoint (Public) | New Endpoint (Auth Required) |
|--------|--------------------------|------------------------------|
| GET | `/rangi_windows/api/projects` | `/api/tools/project/projects` |
| GET | `/rangi_windows/api/projects/{id}` | `/api/tools/project/projects/{id}` |
| POST | `/rangi_windows/api/projects` | `/api/tools/project/projects` |
| PUT | `/rangi_windows/api/projects/{id}` | `/api/tools/project/projects/{id}` |
| PATCH | `/rangi_windows/api/projects/{id}` | `/api/tools/project/projects/{id}` |
| DELETE | `/rangi_windows/api/projects/{id}` | `/api/tools/project/projects/{id}` |

### Project Stages

| Method | Legacy Endpoint (Public) | New Endpoint (Auth Required) |
|--------|--------------------------|------------------------------|
| PATCH | `/rangi_windows/api/projects/{id}/stages` | `/api/tools/project/projects/{id}/stages` |
| POST | `/rangi_windows/api/projects/{id}/archive` | `/api/tools/project/projects/{id}/archive` |

### Project Photos

| Method | Legacy Endpoint (Public) | New Endpoint (Auth Required) |
|--------|--------------------------|------------------------------|
| GET | `/rangi_windows/api/projects/{id}/photos` | `/api/tools/project/projects/{id}/photos` |
| POST | `/rangi_windows/api/projects/{id}/photo` | `/api/tools/project/projects/{id}/photo` |
| GET | `/rangi_windows/api/projects/{id}/photo/{token}` | `/api/tools/project/projects/{id}/photo/{token}` |
| DELETE | `/rangi_windows/api/projects/{id}/photos/{photoId}` | `/api/tools/project/projects/{id}/photos/{photoId}` |
| DELETE | `/rangi_windows/api/projects/{id}/photo/{token}` | `/api/tools/project/projects/{id}/photo/{token}` |
| DELETE | `/rangi_windows/api/projects/{id}/photos` | `/api/tools/project/projects/{id}/photos` |

### Export (Public - No Auth Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/rangi_windows/api/export/excel` | Export projects to Excel |

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| start | String | No | Start date filter (YYYY-MM-DD) |
| end | String | No | End date filter (YYYY-MM-DD) |
| archived | Boolean | No | Filter by archived status |

**Example:**
```
GET /rangi_windows/api/export/excel?start=2024-01-01&end=2024-12-31&archived=false
```

**Response:** Downloads an Excel file (.xlsx) with project data.

---

## Frontend Migration Steps

### Option 1: Keep Using Legacy Path (Public Access)

If you don't need authentication, continue using the legacy path:

```typescript
const API_BASE = '/rangi_windows/api';

export const projectApi = {
  list: (params?: ProjectListParams) =>
    fetch(`${API_BASE}/projects?${new URLSearchParams(params)}`),

  get: (id: string) =>
    fetch(`${API_BASE}/projects/${id}`),

  create: (data: ProjectCreateRequest) =>
    fetch(`${API_BASE}/projects`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),

  exportExcel: (params?: ExportParams) =>
    window.open(`${API_BASE}/export/excel?${new URLSearchParams(params)}`),
};
```

### Option 2: Use New Path (With Authentication)

If you need authenticated access:

```typescript
const API_BASE = '/api/tools/project';

const apiClient = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const projectApi = {
  list: (params?: ProjectListParams) =>
    apiClient.get('/projects', { params }),

  get: (id: string) =>
    apiClient.get(`/projects/${id}`),

  create: (data: ProjectCreateRequest) =>
    apiClient.post('/projects', data),

  // Note: Export uses legacy path (no auth needed)
  exportExcel: (params?: ExportParams) =>
    window.open(`/rangi_windows/api/export/excel?${new URLSearchParams(params)}`),
};
```

---

## Export Excel Example

```typescript
interface ExportParams {
  start?: string;    // YYYY-MM-DD
  end?: string;      // YYYY-MM-DD
  archived?: string; // 'true' or 'false'
}

// Simple download link
function downloadExcel(params?: ExportParams) {
  const queryString = params ? new URLSearchParams(params as any).toString() : '';
  const url = `/rangi_windows/api/export/excel${queryString ? '?' + queryString : ''}`;
  window.open(url, '_blank');
}

// Or using anchor element for filename control
function downloadExcelWithName(params?: ExportParams) {
  const queryString = params ? new URLSearchParams(params as any).toString() : '';
  const url = `/rangi_windows/api/export/excel${queryString ? '?' + queryString : ''}`;

  const link = document.createElement('a');
  link.href = url;
  link.download = `projects_${new Date().toISOString().split('T')[0]}.xlsx`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

// Usage examples
downloadExcel(); // All projects
downloadExcel({ archived: 'false' }); // Active projects only
downloadExcel({ start: '2024-01-01', end: '2024-12-31' }); // Date range
downloadExcel({ start: '2024-01-01', end: '2024-12-31', archived: 'false' }); // Combined
```

---

## Response Format

All endpoints return responses in this format:

```typescript
interface ApiResponse<T> {
  code: number;      // 0 = success, non-zero = error
  msg: string;       // Message (usually "success" or error description)
  data: T;           // Response payload
  success: boolean;  // true if code === 0
  failed: boolean;   // true if code !== 0
}
```

### List Projects Response

```typescript
interface ProjectListResponse {
  items: Project[];
  page: number;
  pageSize: number;
  total: number;
}
```

---

## TypeScript Interfaces

```typescript
interface Project {
  id: string;
  projectCode: string;
  name: string;
  address: string;
  contactName: string;
  contactPhone: string;
  notes: string;
  startDate: string;  // YYYY-MM-DD
  endDate: string;    // YYYY-MM-DD
  glass: boolean;
  frame: boolean;
  purchase: boolean;
  transport: boolean;
  install: boolean;
  repair: boolean;
  archived: boolean;
  createdAt: string;  // YYYY-MM-DD
  updatedAt: string;  // ISO datetime string
}

interface ProjectPhoto {
  id: string;
  projectId: string;
  token: string;
  originalName: string;
  contentType: string;
  size: number;
  storagePath: string;
  createdAt: string;
}

interface ProjectListParams {
  q?: string;
  glass?: boolean;
  frame?: boolean;
  purchase?: boolean;
  transport?: boolean;
  install?: boolean;
  repair?: boolean;
  start?: string;       // YYYY-MM-DD
  end?: string;         // YYYY-MM-DD
  page?: number;        // Default: 1
  pageSize?: number;    // Default: 20
  sortBy?: string;      // Default: 'createdAt'
  sortOrder?: 'asc' | 'desc'; // Default: 'desc'
  archived?: boolean;
  includeArchived?: boolean;
}

interface ProjectCreateRequest {
  name: string;
  projectCode?: string;
  address?: string;
  contactName?: string;
  contactPhone?: string;
  notes?: string;
  startDate?: string;
  endDate?: string;
  glass?: boolean;
  frame?: boolean;
  purchase?: boolean;
  transport?: boolean;
  install?: boolean;
  repair?: boolean;
}

interface StageUpdateRequest {
  glass?: boolean;
  frame?: boolean;
  purchase?: boolean;
  transport?: boolean;
  install?: boolean;
  repair?: boolean;
}

interface ExportParams {
  start?: string;
  end?: string;
  archived?: string;
}
```

---

## Complete API Service Example

```typescript
import axios, { AxiosInstance } from 'axios';

class ProjectApiService {
  private client: AxiosInstance;
  private legacyBase = '/rangi_windows/api';

  constructor(baseURL: string, getToken?: () => string | null) {
    this.client = axios.create({ baseURL });
    if (getToken) {
      this.client.interceptors.request.use((config) => {
        const token = getToken();
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      });
    }
  }

  // Projects CRUD
  async listProjects(params?: ProjectListParams) {
    const res = await this.client.get<ApiResponse<ProjectListResponse>>('/projects', { params });
    return res.data.data;
  }

  async getProject(id: string) {
    const res = await this.client.get<ApiResponse<Project>>(`/projects/${id}`);
    return res.data.data;
  }

  async createProject(data: ProjectCreateRequest) {
    const res = await this.client.post<ApiResponse<Project>>('/projects', data);
    return res.data.data;
  }

  async updateProject(id: string, data: Partial<Project>) {
    const res = await this.client.put<ApiResponse<Project>>(`/projects/${id}`, data);
    return res.data.data;
  }

  async patchProject(id: string, data: Partial<Project>) {
    const res = await this.client.patch<ApiResponse<Project>>(`/projects/${id}`, data);
    return res.data.data;
  }

  async deleteProject(id: string) {
    await this.client.delete(`/projects/${id}`);
  }

  async archiveProject(id: string, archived: boolean = true) {
    const res = await this.client.post<ApiResponse<Project>>(
      `/projects/${id}/archive`,
      { archived }
    );
    return res.data.data;
  }

  async updateStages(id: string, stages: StageUpdateRequest) {
    const res = await this.client.patch<ApiResponse<Project>>(
      `/projects/${id}/stages`,
      stages
    );
    return res.data.data;
  }

  // Photos
  async listPhotos(projectId: string) {
    const res = await this.client.get<ApiResponse<ProjectPhoto[]>>(
      `/projects/${projectId}/photos`
    );
    return res.data.data;
  }

  async uploadPhoto(projectId: string, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    const res = await this.client.post<ApiResponse<ProjectPhoto>>(
      `/projects/${projectId}/photo`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return res.data.data;
  }

  async deletePhoto(projectId: string, photoId: string) {
    await this.client.delete(`/projects/${projectId}/photos/${photoId}`);
  }

  // Export (uses legacy path - no auth required)
  exportExcel(params?: ExportParams) {
    const queryString = params ? new URLSearchParams(params as any).toString() : '';
    const url = `${this.legacyBase}/export/excel${queryString ? '?' + queryString : ''}`;
    window.open(url, '_blank');
  }
}

// Usage with auth (new path)
const authProjectApi = new ProjectApiService(
  '/api/tools/project',
  () => localStorage.getItem('authToken')
);

// Usage without auth (legacy path)
const publicProjectApi = new ProjectApiService('/rangi_windows/api');

export { authProjectApi, publicProjectApi };
```

---

## Migration Checklist

### If using legacy path (public access):
- [ ] Endpoints remain at `/rangi_windows/api/*`
- [ ] No authentication changes needed
- [ ] Export endpoint: `/rangi_windows/api/export/excel`

### If migrating to new path (authenticated):
- [ ] Update API base URL from `/rangi_windows/api` to `/api/tools/project`
- [ ] Add authentication token to all API requests
- [ ] Update all endpoint paths in API service
- [ ] Keep export at legacy path `/rangi_windows/api/export/excel` (no auth)
- [ ] Test all CRUD operations with new endpoints
- [ ] Test photo upload/download with authentication
- [ ] Update error handling for new response format

---

## Excel Export Columns

The exported Excel file contains the following columns:

| Column | Description |
|--------|-------------|
| Project Code | Unique project identifier |
| Project Name | Name of the project |
| Address | Project location |
| Contact Name | Client/contact name |
| Contact Phone | Contact phone number |
| Notes | Additional notes |
| Start Date | Project start date |
| End Date | Project end date |
| Glass | Glass stage completed |
| Frame | Frame stage completed |
| Purchase | Purchase stage completed |
| Transport | Transport stage completed |
| Install | Install stage completed |
| Repair | Repair stage completed |
| Archived | Whether project is archived |
| Created At | Project creation date |
