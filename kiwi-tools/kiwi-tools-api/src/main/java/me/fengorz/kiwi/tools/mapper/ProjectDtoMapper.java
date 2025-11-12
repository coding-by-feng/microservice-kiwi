package me.fengorz.kiwi.tools.mapper;

import me.fengorz.kiwi.tools.api.project.dto.ProjectPatchRequest;
import me.fengorz.kiwi.tools.model.project.Project;
import me.fengorz.kiwi.tools.model.project.ProjectPhoto;
import me.fengorz.kiwi.tools.model.project.ProjectStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ProjectDtoMapper {
    private ProjectDtoMapper() {}

    public static Project toEntity(me.fengorz.kiwi.tools.api.project.dto.ProjectCreateRequest req) {
        Project p = new Project();
        if (req == null) return p;
        p.setName(req.getName());
        p.setClientName(req.getClientName());
        p.setClientPhone(req.getClientPhone());
        p.setAddress(req.getAddress());
        p.setSalesPerson(req.getSalesPerson());
        p.setInstaller(req.getInstaller());
        p.setTeamMembers(req.getTeamMembers());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setStatus(ProjectStatus.fromInput(req.getStatus()));
        p.setTodayTask(req.getTodayTask());
        p.setProgressNote(req.getProgressNote());
        p.setChangeNote(req.getChangeNote());
        return p;
    }

    public static Map<String, Object> toPatch(me.fengorz.kiwi.tools.api.project.dto.ProjectUpdateRequest req) {
        if (req == null) return Collections.emptyMap();
        Map<String, Object> m = new LinkedHashMap<>();
        applyBasePatch(m, req);
        return m;
    }

    public static Map<String, Object> toPatch(ProjectPatchRequest req) {
        if (req == null) return Collections.emptyMap();
        Map<String, Object> m = new LinkedHashMap<>();
        applyBasePatch(m, req);
        if (req.getArchived() != null) {
            m.put("archived", req.getArchived());
        }
        return m;
    }

    private static void applyBasePatch(Map<String, Object> m, me.fengorz.kiwi.tools.api.project.dto.ProjectBaseRequest req) {
        put(m, "name", req.getName());
        put(m, "clientName", req.getClientName());
        put(m, "clientPhone", req.getClientPhone());
        put(m, "address", req.getAddress());
        put(m, "salesPerson", req.getSalesPerson());
        put(m, "installer", req.getInstaller());
        put(m, "teamMembers", req.getTeamMembers());
        put(m, "startDate", req.getStartDate());
        put(m, "endDate", req.getEndDate());
        put(m, "status", req.getStatus());
        put(m, "todayTask", req.getTodayTask());
        put(m, "progressNote", req.getProgressNote());
        put(m, "changeNote", req.getChangeNote());
    }

    public static me.fengorz.kiwi.tools.api.project.dto.ProjectResponse toResponse(Project p) {
        if (p == null) return null;
        me.fengorz.kiwi.tools.api.project.dto.ProjectResponse r = new me.fengorz.kiwi.tools.api.project.dto.ProjectResponse();
        r.setId(p.getId());
        r.setProjectCode(p.getProjectCode());
        r.setName(p.getName());
        r.setClientName(p.getClientName());
        r.setClientPhone(p.getClientPhone());
        r.setAddress(p.getAddress());
        r.setSalesPerson(p.getSalesPerson());
        r.setInstaller(p.getInstaller());
        r.setTeamMembers(p.getTeamMembers());
        r.setStartDate(p.getStartDate());
        r.setEndDate(p.getEndDate());
        r.setStatus(p.getStatus() == null ? null : p.getStatus().getCode());
        r.setTodayTask(p.getTodayTask());
        r.setProgressNote(p.getProgressNote());
        r.setChangeNote(p.getChangeNote());
        r.setCreatedAt(p.getCreatedAt());
        r.setArchived(p.getArchived());
        // New status timestamp fields
        r.setGlassOrderedAt(p.getGlassOrderedAt());
        r.setDoorsWindowsProducedAt(p.getDoorsWindowsProducedAt());
        r.setDoorsWindowsDeliveredAt(p.getDoorsWindowsDeliveredAt());
        r.setDoorsWindowsInstalledAt(p.getDoorsWindowsInstalledAt());
        r.setFinalPaymentReceivedAt(p.getFinalPaymentReceivedAt());
        return r;
    }

    public static List<me.fengorz.kiwi.tools.api.project.dto.ProjectResponse> toResponseList(List<Project> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(ProjectDtoMapper::toResponse).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static me.fengorz.kiwi.tools.api.project.dto.PageResponse<me.fengorz.kiwi.tools.api.project.dto.ProjectResponse> toPageResponse(Map<String, Object> serviceResult) {
        me.fengorz.kiwi.tools.api.project.dto.PageResponse<me.fengorz.kiwi.tools.api.project.dto.ProjectResponse> page = new me.fengorz.kiwi.tools.api.project.dto.PageResponse<>();
        List<Project> items = (List<Project>) serviceResult.getOrDefault("items", Collections.emptyList());
        page.setItems(toResponseList(items));
        Object p = serviceResult.get("page");
        Object ps = serviceResult.get("pageSize");
        Object t = serviceResult.get("total");
        page.setPage(p instanceof Number ? ((Number) p).intValue() : 1);
        page.setPageSize(ps instanceof Number ? ((Number) ps).intValue() : (items != null ? items.size() : 0));
        page.setTotal(t instanceof Number ? ((Number) t).longValue() : (items != null ? items.size() : 0));
        return page;
    }

    public static me.fengorz.kiwi.tools.api.project.dto.ProjectPhotoResponse toResponse(ProjectPhoto pp) {
        if (pp == null) return null;
        me.fengorz.kiwi.tools.api.project.dto.ProjectPhotoResponse r = new me.fengorz.kiwi.tools.api.project.dto.ProjectPhotoResponse();
        r.setId(pp.getId());
        r.setProjectId(pp.getProjectId());
        r.setDfsFileId(pp.getDfsFileId());
        r.setToken(pp.getToken());
        r.setContentType(pp.getContentType());
        r.setSize(pp.getSize());
        r.setSortOrder(pp.getSortOrder());
        r.setCaption(pp.getCaption());
        r.setCreatedAt(pp.getCreatedAt());
        return r;
    }

    public static List<me.fengorz.kiwi.tools.api.project.dto.ProjectPhotoResponse> toPhotoResponseList(List<ProjectPhoto> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(ProjectDtoMapper::toResponse).collect(Collectors.toList());
    }

    private static void put(Map<String, Object> m, String key, Object value) {
        if (value != null) m.put(key, value);
    }
}
