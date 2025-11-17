# Project Module: Stage Flags with Remarks (glass/frame/purchase/transport/install/repair)

Date: 2025-11-17
Author: Backend update summary for FE team
Status: Ready for frontend implementation
Base path: /rangi_windows/api

Overview
- Project no longer uses a single string status. It now has a one-to-one "stages" object holding six boolean flags with per-stage remarks:
  - glass (玻璃) + glassRemark
  - frame (框架) + frameRemark
  - purchase (采购) + purchaseRemark
  - transport (运输) + transportRemark
  - install (安装) + installRemark
  - repair (维修) + repairRemark
- All CRUD APIs return and accept the stages object. A dedicated endpoint exists to patch stages only.
- List API supports filtering by the six boolean flags.
- Exports: Excel shows both flags and remarks; PDF appends remarks to the detail line.

1) Response shape
Returned by GET /projects and GET /projects/{id}
{
  "id": "123",
  "projectCode": "P-001",
  "name": "客厅窗户",
  "clientName": "张三",
  "address": "上海市…",
  "salesPerson": "Alice",
  "installer": "Bob",
  "teamMembers": "Tom, Jerry",
  "startDate": "2025-10-01",
  "endDate": "2025-10-10",
  "todayTask": "复尺",
  "progressNote": "玻璃到货",
  "changeNote": "增加纱窗",
  "createdAt": "2025-10-01T12:30:00",
  "archived": false,
  "stages": {
    "glass": true,
    "glassRemark": "超白钢化",
    "frame": true,
    "frameRemark": "黑色氧化",
    "purchase": true,
    "purchaseRemark": "配件齐全",
    "transport": false,
    "transportRemark": null,
    "install": false,
    "installRemark": null,
    "repair": false,
    "repairRemark": null
  }
}
Notes
- Any missing remark should be treated as empty string in UI.
- Any missing boolean should be treated as false in UI (backend normally returns all properties).

2) Create a project
POST /rangi_windows/api/projects
- Body accepts stages; omitting stages will default all flags=false and remarks empty.
- Partial stages also allowed (only provided fields are set).

Example body
{
  "name": "阳台门",
  "clientName": "李四",
  "startDate": "2025-11-20",
  "endDate": "2025-11-22",
  "todayTask": "下单",
  "progressNote": "",
  "changeNote": "",
  "stages": {
    "glass": true,
    "glassRemark": "LOW-E",
    "purchase": true,
    "purchaseRemark": "五金齐全"
  }
}

3) Update a project (partial)
PUT /rangi_windows/api/projects/{id}
PATCH /rangi_windows/api/projects/{id}
- Send only fields to change. For stages, send any subset; omitted flags/remarks are not modified.

Example: toggle install true with remark
{
  "stages": { "install": true, "installRemark": "周五安装" }
}

4) Update stages only (new)
PATCH /rangi_windows/api/projects/{id}/stages
- Body is the stages object (any subset of 12 fields – 6 flags + 6 remarks).
- Response is the full Project object.

Example body
{ "transport": true, "transportRemark": "货到仓库" }

5) List projects with filters
GET /rangi_windows/api/projects
Query params
- q: string – free-text across code/name/client/address
- start, end: string (YYYY-MM-DD) – overlap filter
- page, pageSize: number; sortBy; sortOrder
- archived: boolean; includeArchived: boolean
- Stage filters (booleans; optional): glass, frame, purchase, transport, install, repair
Notes
- There are no query params for remarks; if needed, filter client-side or request backend enhancement.

6) Export
Excel – GET /rangi_windows/api/export/excel
- Columns (excerpt around stages):
  - 玻璃, 玻璃备注, 框架, 框架备注, 采购, 采购备注, 运输, 运输备注, 安装阶段, 安装备注, 维修, 维修备注
- Boolean values appear as "是"/"否". Remarks are plain text.

PDF – GET /rangi_windows/api/export/pdf
- Columns remain compact (flags only). Remarks are appended in the detail line as: 阶段备注: 玻璃: ...; 框架: ...; … (only non-empty shown).

7) FE model suggestions
TypeScript interface snippet
export interface ProjectStages {
  glass?: boolean;        glassRemark?: string;
  frame?: boolean;        frameRemark?: string;
  purchase?: boolean;     purchaseRemark?: string;
  transport?: boolean;    transportRemark?: string;
  install?: boolean;      installRemark?: string;
  repair?: boolean;       repairRemark?: string;
}
export interface Project {
  id: string; projectCode: string; name: string;
  // ...other fields...
  stages?: ProjectStages;
}
Defaults
- On create: initialize stages to all false + empty remarks.
- In UI: show unchecked/"否" for falsy, and blank remarks when undefined.

8) UI adjustments
- List/Table: display six flags (or group visually by stage); add tooltips showing remarks when present.
- Detail: for each stage, show a toggle and a remark input (textarea or inline). Save both via PATCH /projects/{id}/stages.
- Forms: remember partial update semantics – send only changed fields; keep other stage fields untouched.

9) Error handling & behavior
- Standard 400/404 applies; no special errors for stages/remarks.
- Remarks are limited to 255 chars server-side; consider client-side limit and trimming.
- Sequence rules are not enforced server-side (e.g., install can be true while transport=false). Enforce in UI if desired.

10) Quick reference (endpoints)
- GET    /projects                      – supports stage boolean filters
- GET    /projects/{id}
- POST   /projects                      – accepts optional stages
- PUT    /projects/{id}                 – accepts optional stages (partial semantics)
- PATCH  /projects/{id}                 – accepts optional stages (partial semantics)
- PATCH  /projects/{id}/stages          – update stages only
- POST   /projects/{id}/archive         – unchanged
- Media  (photo/video)                  – unchanged
- Export /export/excel, /export/pdf     – updated columns/details as above

11) Migration checklist (FE)
- [ ] Replace legacy single status with stages UI.
- [ ] Add per-stage remark inputs and persistence.
- [ ] Update list filters to use stage booleans.
- [ ] Adapt FE models and serializers to include stages and remarks.
- [ ] Update Excel/PDF expectations in any FE tests.
- [ ] Remove obsolete code relying on status or previous glass flags docs.

Notes
- This supersedes prior glass-only flags docs. See deprecation note in `new-glass-flags-fe-adjustments.md`.

