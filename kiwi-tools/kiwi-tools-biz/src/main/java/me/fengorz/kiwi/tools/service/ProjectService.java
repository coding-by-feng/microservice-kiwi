package me.fengorz.kiwi.tools.service;

import me.fengorz.kiwi.tools.config.ToolsProperties;
import me.fengorz.kiwi.tools.exception.AppException;
import me.fengorz.kiwi.tools.model.Project;
import me.fengorz.kiwi.tools.repository.ProjectRepository;
import me.fengorz.kiwi.tools.util.DateUtil;
import me.fengorz.kiwi.tools.util.StringUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProjectService {
    private final ToolsProperties props;
    private ProjectRepository repo;

    public ProjectService(ToolsProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        this.repo = new ProjectRepository();
    }

    public Map<String, Object> list(String q, String status, String start, String end,
                                    Integer page, Integer pageSize, String sortBy, String sortOrder) {
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 50 : Math.min(pageSize, 200);
        String sb = (sortBy == null || sortBy.isEmpty()) ? "createdAt" : sortBy;
        String so = (sortOrder == null || sortOrder.isEmpty()) ? "desc" : sortOrder;
        LocalDate s = start != null && !start.isEmpty() ? LocalDate.parse(start) : null;
        LocalDate e = end != null && !end.isEmpty() ? LocalDate.parse(end) : null;
        List<Project> items = repo.search(q, status, s, e, sb, so, p, ps);
        long total = repo.count(q, status, s, e);
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        resp.put("page", p);
        resp.put("pageSize", ps);
        resp.put("total", total);
        return resp;
    }

    public Project get(String id) {
        return repo.findById(id).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "not_found", "Project not found"));
    }

    public Project create(Project in) {
        validateProject(in, true);
        in.setCreatedAt(DateUtil.nowIso());
        in.setPhotoUrl(in.getPhotoUrl() == null ? "" : in.getPhotoUrl());
        return repo.save(in);
    }

    public Project update(String id, Map<String, Object> patch) {
        Project existing = get(id);
        applyPatch(existing, patch);
        validateProject(existing, false);
        repo.update(id, existing);
        return existing;
    }

    private void applyPatch(Project p, Map<String, Object> m) {
        if (m.containsKey("name")) p.setName(getStr(m, "name"));
        if (m.containsKey("clientName")) p.setClientName(getStr(m, "clientName"));
        if (m.containsKey("clientPhone")) p.setClientPhone(getStr(m, "clientPhone"));
        if (m.containsKey("address")) p.setAddress(getStr(m, "address"));
        if (m.containsKey("salesPerson")) p.setSalesPerson(getStr(m, "salesPerson"));
        if (m.containsKey("installer")) p.setInstaller(getStr(m, "installer"));
        if (m.containsKey("teamMembers")) p.setTeamMembers(getStr(m, "teamMembers"));
        if (m.containsKey("startDate")) p.setStartDate(getStr(m, "startDate"));
        if (m.containsKey("endDate")) p.setEndDate(getStr(m, "endDate"));
        if (m.containsKey("status")) p.setStatus(getStr(m, "status"));
        if (m.containsKey("todayTask")) p.setTodayTask(getStr(m, "todayTask"));
        if (m.containsKey("progressNote")) p.setProgressNote(getStr(m, "progressNote"));
        if (m.containsKey("photoUrl")) p.setPhotoUrl(Optional.ofNullable(getStr(m, "photoUrl")).orElse(""));
    }

    private String getStr(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }

    private void validateProject(Project p, boolean creating) {
        // trim
        p.setName(trim(p.getName()));
        p.setClientName(trim(p.getClientName()));
        p.setClientPhone(trim(p.getClientPhone()));
        p.setAddress(trim(p.getAddress()));
        p.setSalesPerson(trim(p.getSalesPerson()));
        p.setInstaller(trim(p.getInstaller()));
        p.setTeamMembers(p.getTeamMembers()); // free text kept raw
        p.setTodayTask(p.getTodayTask());
        p.setProgressNote(p.getProgressNote());

        if (p.getName() == null || p.getName().length() < 1 || p.getName().length() > 100)
            throw new AppException(HttpStatus.BAD_REQUEST, "validation_error", "name is required (1-100)");
        if (hasCtl(p.getName()) || hasCtl(p.getClientName()) || hasCtl(p.getClientPhone()) || hasCtl(p.getAddress())
                || hasCtl(p.getSalesPerson()) || hasCtl(p.getInstaller()))
            throw new AppException(HttpStatus.BAD_REQUEST, "validation_error", "contains control characters");
        if (p.getClientPhone() != null && p.getClientPhone().length() > 30)
            throw new AppException(HttpStatus.BAD_REQUEST, "validation_error", "clientPhone too long");
        if (p.getAddress() != null && p.getAddress().length() > 200)
            throw new AppException(HttpStatus.BAD_REQUEST, "validation_error", "address too long");
        if (p.getSalesPerson() != null && p.getSalesPerson().length() > 100)
            throw new AppException(HttpStatus.BAD_REQUEST, "validation_error", "salesPerson too long");
        if (p.getInstaller() != null && p.getInstaller().length() > 100)
            throw new AppException(HttpStatus.BAD_REQUEST, "validation_error", "installer too long");

        // dates
        String sd = trim(p.getStartDate());
        String ed = trim(p.getEndDate());
        if (sd != null && !DateUtil.isValidDate(sd))
            throw new AppException(HttpStatus.BAD_REQUEST, "validation_error", "startDate invalid format");
        if (ed != null && !DateUtil.isValidDate(ed))
            throw new AppException(HttpStatus.BAD_REQUEST, "validation_error", "endDate invalid format");
        if (sd != null && ed != null) {
            if (DateUtil.parseDate(ed).isBefore(DateUtil.parseDate(sd)))
                throw new AppException(HttpStatus.BAD_REQUEST, "validation_error", "endDate must be >= startDate");
        }

        // status default
        if (p.getStatus() == null || p.getStatus().isEmpty()) p.setStatus("未开始");
        else if (!props.getAllowedStatuses().contains(p.getStatus()))
            throw new AppException(HttpStatus.BAD_REQUEST, "validation_error", "invalid status");

        p.setStartDate(sd);
        p.setEndDate(ed);
    }

    private String trim(String s) { return StringUtil.trimToNull(s); }
    private boolean hasCtl(String s) { return StringUtil.hasControlChars(s); }

    public Project setPhotoUrl(String id, String url) {
        Project p = get(id);
        p.setPhotoUrl(url == null ? "" : url);
        repo.update(id, p);
        return p;
    }
}
