package me.fengorz.kiwi.tools.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.tools.model.Project;
import me.fengorz.kiwi.tools.model.ProjectStatus;
import me.fengorz.kiwi.tools.repository.mapper.ProjectMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProjectRepository {

    private final ProjectMapper mapper;

    public synchronized Project save(Project p) {
        if (p.getId() == null || p.getId().isEmpty()) {
            long id = IdWorker.getId();
            p.setId(String.valueOf(id));
            if (p.getProjectCode() == null || p.getProjectCode().isEmpty()) {
                p.setProjectCode(formatCode(id));
            }
        }
        if (p.getProjectCode() == null || p.getProjectCode().isEmpty()) {
            // fallback if caller clears code
            p.setProjectCode(formatCode(parseLongSafe(p.getId())));
        }
        if (p.getArchived() == null) {
            p.setArchived(false);
        }
        mapper.insert(p);
        return p;
    }

    public Optional<Project> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public Project update(String id, Project p) {
        mapper.updateById(p);
        return p;
    }

    public void delete(String id) {
        mapper.deleteById(id);
    }

    public List<Project> findAll() {
        return mapper.selectList(null);
    }

    public boolean existsByProjectCode(String code) {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<Project>()
                .eq(Project::getProjectCode, code);
        return mapper.selectCount(qw) > 0;
    }

    public List<Project> search(String q, ProjectStatus status, LocalDate start, LocalDate end,
                                String sortBy, String sortOrder, int page, int pageSize,
                                Boolean archived, Boolean includeArchived) {
        LambdaQueryWrapper<Project> qw = buildWrapper(q, status, start, end, archived, includeArchived);
        applySort(qw, sortBy, sortOrder);
        Page<Project> mp = new Page<>(page, pageSize);
        return mapper.selectPage(mp, qw).getRecords();
    }

    public List<Project> search(String q, ProjectStatus status, LocalDate start, LocalDate end,
                                String sortBy, String sortOrder, int page, int pageSize) {
        return search(q, status, start, end, sortBy, sortOrder, page, pageSize, false, false);
    }

    public long count(String q, ProjectStatus status, LocalDate start, LocalDate end,
                      Boolean archived, Boolean includeArchived) {
        LambdaQueryWrapper<Project> qw = buildWrapper(q, status, start, end, archived, includeArchived);
        return mapper.selectCount(qw);
    }

    public long count(String q, ProjectStatus status, LocalDate start, LocalDate end) {
        return count(q, status, start, end, false, false);
    }

    private LambdaQueryWrapper<Project> buildWrapper(String q, ProjectStatus status, LocalDate start, LocalDate end,
                                                     Boolean archived, Boolean includeArchived) {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<>();
        if (q != null && !q.isEmpty()) {
            String t = "%" + q + "%";
            qw.and(w -> w.like(Project::getProjectCode, t)
                    .or().like(Project::getName, t)
                    .or().like(Project::getClientName, t)
                    .or().like(Project::getAddress, t));
        }
        if (status != null) {
            qw.eq(Project::getStatus, status);
        }
        if (start != null || end != null) {
            String s = start != null ? start.toString() : "0000-01-01";
            String e = end != null ? end.toString() : "9999-12-31";
            // overlap logic on ISO date strings using DB functions
            // relies on snake_case column names start_date/end_date
            qw.apply("( (start_date is not null or end_date is not null) " +
                     "and (coalesce(start_date, end_date) <= {0} and coalesce(end_date, start_date) >= {1}) )", e, s);
        }
        // Archived filter semantics:
        // - includeArchived=true => only archived
        // - else => archived flag equals provided 'archived' param (default false)
        if (Boolean.TRUE.equals(includeArchived)) {
            qw.eq(Project::getArchived, true);
        } else {
            boolean a = archived != null ? archived : false;
            qw.eq(Project::getArchived, a);
        }
        return qw;
    }

    private void applySort(LambdaQueryWrapper<Project> qw, String sortBy, String sortOrder) {
        boolean desc = "desc".equalsIgnoreCase(sortOrder);
        String col = toSnake(sortBy == null || sortBy.isEmpty() ? "created_at" : sortBy);
        // created_at, start_date, end_date, project_code
        qw.last("order by " + col + (desc ? " desc" : " asc"));
    }

    private String formatCode(long id) {
        if (id < 1000) return String.format("P-%03d", id);
        return "P-" + id;
    }

    private long parseLongSafe(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0L; }
    }

    private String toSnake(String camel) {
        if (camel == null || camel.isEmpty()) return camel;
        StringBuilder sb = new StringBuilder();
        for (char c : camel.toCharArray()) {
            if (Character.isUpperCase(c)) sb.append('_').append(Character.toLowerCase(c));
            else sb.append(c);
        }
        return sb.toString();
    }

    // kept to satisfy comparatorFor reference if used elsewhere
    private Comparator<Project> comparatorFor(String sortBy) {
        if ("startDate".equals(sortBy)) return Comparator.comparing(Project::getStartDate, Comparator.nullsLast(String::compareTo));
        if ("endDate".equals(sortBy)) return Comparator.comparing(Project::getEndDate, Comparator.nullsLast(String::compareTo));
        if ("projectCode".equals(sortBy)) return Comparator.comparing(Project::getProjectCode, Comparator.nullsLast(String::compareTo));
        return Comparator.comparing(Project::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
    }
}
