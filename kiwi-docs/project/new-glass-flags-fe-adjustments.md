# DEPRECATED – See project-stages-fe-adjustments.md

This document is superseded by `project-stages-fe-adjustments.md`, which describes the new stages (glass/frame/purchase/transport/install/repair) with remarks. The old glassOrdered/glassManufactured guidance below is retained for historical context only.

# Project Module: New Glass Flags (glassOrdered, glassManufactured)

Date: 2025-11-08
Author: Backend update summary for FE team
Status: Ready for frontend implementation

## 1. Overview
Two new boolean fields have been added to the Project entity to track window glass workflow status:
- `glassOrdered`  (Has the glass been ordered?)
- `glassManufactured` (Has the glass been manufactured / produced?)

They are now fully supported across the REST API CRUD endpoints and included in the Excel export. PDF export currently does NOT show these fields (see section 9). Both fields default to `false` at creation if omitted. Null values coming from legacy rows should be treated as `false` in the UI.

## 2. API Surface Changes
### 2.1 Response Payload (`GET /rangi_windows/api/projects` and `GET /rangi_windows/api/projects/{id}`)
Each project JSON object now includes:
```json
{
  "glassOrdered": false,
  "glassManufactured": false
}
```
Values are booleans.

### 2.2 Create / Update / Patch Requests
- POST `/projects`: You may optionally send `glassOrdered` and/or `glassManufactured`. If omitted, they default to `false`.
- PUT `/projects/{id}` and PATCH `/projects/{id}`: You can include either or both flags. Omitted fields are left unchanged.
- POST `/projects/{id}/archive` remains unchanged (only archives/unarchives).

### 2.3 Filtering & Sorting
Backend currently does NOT expose dedicated query params for these two flags. If FE needs filtering (e.g., show all projects where glassOrdered=false), you have two options:
1. Local filtering after fetching the page. (Simplest; okay for small datasets.)
2. Request backend enhancement to add query params: `glassOrdered`, `glassManufactured` (future work). Please raise a ticket if required.

### 2.4 Excel Export
Endpoint: `GET /rangi_windows/api/export/excel`
New columns appended at the end (after "已归档"):
16. 玻璃已下单 (glassOrdered)
17. 玻璃已生产 (glassManufactured)
Values: "是" (true) / "否" (false or null).

### 2.5 Null Handling
If a legacy record has `null` for either field, API response will return `null` or it may serialize as missing depending on Jackson config (currently appears as JSON `null`). Treat both `null` and `false` identically (display unchecked / "否"). When updating from UI, always send explicit booleans to normalize data.

## 3. FE Data Model Updates
Update your TypeScript / frontend model interface:
```ts
export interface Project {
  id: string;
  projectCode: string;
  name: string;
  clientName?: string;
  address?: string;
  salesPerson?: string;
  installer?: string;
  teamMembers?: string;
  startDate?: string; // YYYY-MM-DD
  endDate?: string;   // YYYY-MM-DD
  status?: 'not_started' | 'in_progress' | 'completed';
  todayTask?: string;
  progressNote?: string;
  createdAt?: string; // ISO 8601
  archived?: boolean;
  glassOrdered?: boolean;        // NEW
  glassManufactured?: boolean;   // NEW
}
```
For forms, set defaults: `{ glassOrdered: false, glassManufactured: false }`.

## 4. UI Adjustments
### 4.1 List / Table View
Add two small boolean indicators (e.g., checkmarks or colored badges):
- Column labels: "玻璃下单" / "玻璃生产" (shortened) or tooltips with full meaning.
- Consider grouping under a single column "玻璃" with value like "下单: 是 / 生产: 否" if horizontal space is limited.

### 4.2 Detail / Edit Form
Add two toggle controls (checkboxes or switches):
- Label: "玻璃已下单" (checkbox) – help text: "勾选表示已向供应商下单".
- Label: "玻璃已生产" (checkbox) – help text: "勾选表示生产完成并可安装".
Disable `glassManufactured` checkbox until `glassOrdered` is true (optional UX rule; confirm with product). If so, ensure PUT/PATCH logic enforces ordering sequence (currently backend does NOT validate sequence).

### 4.3 Badges / Status Integration (Optional)
You may add conditional badges near the project status summary:
- If `glassOrdered && !glassManufactured`: show badge "玻璃生产中".
- If `glassManufactured`: show badge "玻璃已完成".

### 4.4 Bulk Edit (Optional Future)
If the UI supports bulk operations, allow selecting multiple projects and toggling these flags. Use PATCH endpoint individually per id (no bulk endpoint exists yet). Consider throttling / queueing for large selections.

## 5. State Transitions & UX Suggestions
Recommended sequence: Ordered -> Manufactured.
Backend currently allows setting `glassManufactured=true` while `glassOrdered=false`. If FE wants to prevent this, enforce client-side rule and show validation error.

## 6. Caching / Local Store Impact
Invalidate any cached project list after toggling these flags since they affect export output and potential filtering. Ensure optimistic UI updates reflect changes immediately.

## 7. Error Handling
Expect standard validation errors (400) unrelated to these booleans (they are optional and always valid). No special error messages. Null is treated like false in export (renders "否").

## 8. Testing Guidelines (FE)
Create/update tests:
- Rendering: table shows columns and correct labels.
- Form submit: toggling flags updates project row.
- Export integration: after toggling a flag and exporting, downloaded Excel row shows "是" for true.
- Sequence rule (if implemented): manufacturing cannot be enabled before ordering.

Sample assertion after export (pseudo):
```js
expect(excelRow['玻璃已下单']).toBe('是');
expect(excelRow['玻璃已生产']).toBe('否');
```

## 9. PDF Export (Current Limitation)
PDF export does NOT include the glass flags. If FE / stakeholders need them:
- Option A: Append two more columns (space is tight; may need layout adjustments).
- Option B: Add them into the detail line: e.g., `任务: ... | 备注: ... | 玻璃: 下单/生产完成`.
Raise a ticket if required.

## 10. Performance Considerations
Adding two booleans is negligible. Excel export column expansion is minimal. No pagination change; be aware that export currently fetches up to 10,000 items (`pageSize=10000`). If FE anticipates >10k rows often, request streaming export refactor.

## 11. Summary of Required FE Actions (Checklist)
- [ ] Update Project interface with two new fields.
- [ ] Default form model to `false` for both flags on create.
- [ ] Add form controls (checkboxes / switches) for the two flags.
- [ ] Add columns (or combined column) in list/table view.
- [ ] Adjust cell rendering: show "是" for true, "否" for false/null.
- [ ] Ensure PATCH/PUT includes changed flags only.
- [ ] Update any export parsing logic expecting column positions (columns shifted after index 15).
- [ ] Add unit/integration tests covering display & export.
- [ ] (Optional) Implement sequencing rule (manufactured implies ordered).
- [ ] (Optional) Add filtering UI or raise backend filter request.

## 12. Column Index Reference (Excel)
```
0: 项目ID
1: 项目编号
2: 项目名称
3: 客户
4: 地址
5: 销售
6: 安装
7: 团队成员
8: 开始日期
9: 结束日期
10: 状态
11: 今日任务
12: 进度备注
13: 创建时间
14: 已归档
15: 玻璃已下单  (NEW)
16: 玻璃已生产  (NEW)
```

## 13. Backend Reference
- DTO Mapper already maps both fields in all directions.
- Excel export updated (unit test: `ExportServiceTest.toExcel_includesGlassFlagsColumns`).
- No database migration doc here: Field names in table likely `glass_ordered`, `glass_manufactured` (confirm via schema).

## 14. Questions / Follow-ups
If you need:
- Backend filtering/query params.
- PDF enhancement.
- Validation sequencing.
- Bulk update endpoint.
Open a ticket with priority and proposed UX.

---
For any clarification, tag backend maintainers or refer to `ExportService` and `ProjectDtoMapper` implementations.
