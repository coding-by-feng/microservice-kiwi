package me.fengorz.kiwi.tools.service;

import me.fengorz.kiwi.tools.exception.ToolsException;
import me.fengorz.kiwi.tools.model.project.Project;
import me.fengorz.kiwi.tools.model.project.ProjectStageStatus;
import me.fengorz.kiwi.tools.repository.ProjectRepository;
import me.fengorz.kiwi.tools.util.DateUtil;
import me.fengorz.kiwi.tools.util.StringUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.hutool.core.map.MapUtil.getBool;
import static cn.hutool.core.map.MapUtil.getStr;

/**
 * ProjectService encapsulates core business logic for Project CRUD and listing.
 * Responsibilities:
 * - Validate incoming data (lengths, control characters, date formats/order).
 * - Apply partial updates (PATCH-like behavior for PUT body with partial fields).
 * - Enforce pagination, sorting, and date-overlap filtering (delegates to repository for search).
 * - Generate server fields (createdAt).
 *
 * Notes:
 * - sortBy expects snake_case keys: created_at (default), start_date, end_date, project_code.
 */
@Service
public class ProjectService {
    private final ProjectRepository repo;

    public ProjectService(ProjectRepository repo) {
        this.repo = repo;
    }

    /**
     * List projects with optional free-text search, stage filters, date-overlap filter,
     * pagination, and sorting.
     */
    public Map<String, Object> list(String q,
                                    Boolean glass, Boolean frame, Boolean purchase, Boolean transport, Boolean install, Boolean repair,
                                    String start, String end,
                                    Integer page, Integer pageSize, String sortBy, String sortOrder,
                                    Boolean archived, Boolean includeArchived) {
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 50 : Math.min(pageSize, 200);
        String sb = (sortBy == null || sortBy.isEmpty()) ? "created_at" : sortBy;
        String so = (sortOrder == null || sortOrder.isEmpty()) ? "desc" : sortOrder;
        LocalDate s = start != null && !start.isEmpty() ? LocalDate.parse(start) : null;
        LocalDate e = end != null && !end.isEmpty() ? LocalDate.parse(end) : null;
        List<Project> items = repo.search(q, glass, frame, purchase, transport, install, repair, s, e, sb, so, p, ps, archived, includeArchived);
        long total = repo.count(q, glass, frame, purchase, transport, install, repair, s, e, archived, includeArchived);
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        resp.put("page", p);
        resp.put("pageSize", ps);
        resp.put("total", total);
        return resp;
    }

    /** Fetch a project by id or throw 404 if missing. */
    public Project get(String id) {
        return repo.findById(id).orElseThrow(() -> new ToolsException(HttpStatus.NOT_FOUND, "not_found", "Project not found"));
    }

    /** Create a new project after validating fields. */
    public Project create(Project in) {
        validateProject(in, true);
        in.setCreatedAt(LocalDateTime.now());
        // stages are initialized in repository if null
        return repo.save(in);
    }

    /** Update a project with a partial body. Only keys present in the map are applied. */
    public Project update(String id, Map<String, Object> patch) {
        Project existing = get(id);
        applyPatch(existing, patch);
        validateProject(existing, false);
        repo.update(id, existing);
        return existing;
    }

    /** Delete a project by id (404 if not found). */
    public void delete(String id) {
        get(id);
        repo.delete(id);
    }

    /** Archive/unarchive a project. Default target=true when null. */
    public Project archive(String id, Boolean archived) {
        Project p = get(id);
        boolean target = archived == null || archived;
        if (Boolean.valueOf(target).equals(p.getArchived())) {
            return p;
        }
        p.setArchived(target);
        repo.update(id, p);
        return p;
    }

    /** Apply a JSON-like patch into an existing Project. Missing keys are ignored. */
    private void applyPatch(Project p, Map<String, Object> m) {
        if (m.containsKey("name")) p.setName(getStr(m, "name"));
        if (m.containsKey("clientName")) p.setClientName(getStr(m, "clientName"));
        if (m.containsKey("client_name")) p.setClientName(getStr(m, "client_name"));
        if (m.containsKey("address")) p.setAddress(getStr(m, "address"));
        if (m.containsKey("salesPerson")) p.setSalesPerson(getStr(m, "salesPerson"));
        if (m.containsKey("sales_person")) p.setSalesPerson(getStr(m, "sales_person"));
        if (m.containsKey("installer")) p.setInstaller(getStr(m, "installer"));
        if (m.containsKey("teamMembers")) p.setTeamMembers(getStr(m, "teamMembers"));
        if (m.containsKey("team_members")) p.setTeamMembers(getStr(m, "team_members"));
        if (m.containsKey("startDate")) p.setStartDate(getStr(m, "startDate"));
        if (m.containsKey("start_date")) p.setStartDate(getStr(m, "start_date"));
        if (m.containsKey("endDate")) p.setEndDate(getStr(m, "endDate"));
        if (m.containsKey("end_date")) p.setEndDate(getStr(m, "end_date"));
        if (m.containsKey("todayTask")) p.setTodayTask(getStr(m, "todayTask"));
        if (m.containsKey("today_task")) p.setTodayTask(getStr(m, "today_task"));
        if (m.containsKey("progressNote")) p.setProgressNote(getStr(m, "progressNote"));
        if (m.containsKey("progress_note")) p.setProgressNote(getStr(m, "progress_note"));
        if (m.containsKey("changeNote")) p.setChangeNote(getStr(m, "changeNote"));
        if (m.containsKey("change_note")) p.setChangeNote(getStr(m, "change_note"));
        if (m.containsKey("archived")) {
            Boolean a = getBool(m, "archived");
            if (a != null) p.setArchived(a);
        }
        // stages comes as ProjectStagesDto (DTO). Convert and attach to entity, repository will upsert.
        Object stages = m.get("stages");
        if (stages instanceof me.fengorz.kiwi.tools.api.project.dto.ProjectStagesDto) {
            me.fengorz.kiwi.tools.api.project.dto.ProjectStagesDto dto = (me.fengorz.kiwi.tools.api.project.dto.ProjectStagesDto) stages;
            ProjectStageStatus s = p.getStages() != null ? p.getStages() : new ProjectStageStatus();
            s.setProjectId(p.getId());
            if (dto.getGlass() != null) s.setGlass(dto.getGlass());
            if (dto.getGlassRemark() != null) s.setGlassRemark(dto.getGlassRemark());
            if (dto.getFrame() != null) s.setFrame(dto.getFrame());
            if (dto.getFrameRemark() != null) s.setFrameRemark(dto.getFrameRemark());
            if (dto.getPurchase() != null) s.setPurchase(dto.getPurchase());
            if (dto.getPurchaseRemark() != null) s.setPurchaseRemark(dto.getPurchaseRemark());
            if (dto.getTransport() != null) s.setTransport(dto.getTransport());
            if (dto.getTransportRemark() != null) s.setTransportRemark(dto.getTransportRemark());
            if (dto.getInstall() != null) s.setInstall(dto.getInstall());
            if (dto.getInstallRemark() != null) s.setInstallRemark(dto.getInstallRemark());
            if (dto.getRepair() != null) s.setRepair(dto.getRepair());
            if (dto.getRepairRemark() != null) s.setRepairRemark(dto.getRepairRemark());
            p.setStages(s);
        }
    }

    /** Validate user-provided fields according to the spec. */
    private void validateProject(Project p, boolean creating) {
        // trim
        p.setName(trim(p.getName()));
        p.setClientName(trim(p.getClientName()));
        p.setAddress(trim(p.getAddress()));
        p.setSalesPerson(trim(p.getSalesPerson()));
        p.setInstaller(trim(p.getInstaller()));
        p.setTeamMembers(p.getTeamMembers()); // free text kept raw
        p.setTodayTask(p.getTodayTask());
        p.setProgressNote(p.getProgressNote());

        if (p.getName() == null || p.getName().isEmpty() || p.getName().length() > 100)
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "name is required (1-100)");
        if (hasCtl(p.getName()) || hasCtl(p.getClientName()) || hasCtl(p.getAddress())
                || hasCtl(p.getSalesPerson()) || hasCtl(p.getInstaller()))
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "contains control characters");
        if (p.getAddress() != null && p.getAddress().length() > 200)
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "address too long");
        if (p.getSalesPerson() != null && p.getSalesPerson().length() > 100)
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "salesPerson too long");
        if (p.getInstaller() != null && p.getInstaller().length() > 100)
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "installer too long");

        // dates
        String sd = trim(p.getStartDate());
        String ed = trim(p.getEndDate());
        if (sd != null && !DateUtil.isValidDate(sd))
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "startDate invalid format");
        if (ed != null && !DateUtil.isValidDate(ed))
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "endDate invalid format");
        if (sd != null && ed != null) {
            if (DateUtil.parseDate(ed).isBefore(DateUtil.parseDate(sd)))
                throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "endDate must be >= startDate (" + sd + " - " + ed + ")");
        }

        p.setStartDate(sd);
        p.setEndDate(ed);
    }

    private String trim(String s) { return StringUtil.trimToNull(s); }
    private boolean hasCtl(String s) { return StringUtil.hasControlChars(s); }
}
