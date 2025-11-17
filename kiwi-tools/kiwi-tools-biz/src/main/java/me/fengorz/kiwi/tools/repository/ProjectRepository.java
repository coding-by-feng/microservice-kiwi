package me.fengorz.kiwi.tools.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.tools.model.project.Project;
import me.fengorz.kiwi.tools.model.project.ProjectStageStatus;
import me.fengorz.kiwi.tools.repository.mapper.ProjectMapper;
import me.fengorz.kiwi.tools.repository.mapper.ProjectStageStatusMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProjectRepository {

    private final ProjectMapper mapper;
    private final ProjectStageStatusMapper stageMapper;

    public synchronized Project save(Project p) {
        if (p.getId() == null || p.getId().isEmpty()) {
            long id = IdWorker.getId();
            p.setId(String.valueOf(id));
            if (p.getProjectCode() == null || p.getProjectCode().isEmpty()) {
                p.setProjectCode(formatCode(id));
            }
        }
        if (p.getProjectCode() == null || p.getProjectCode().isEmpty()) {
            p.setProjectCode(formatCode(parseLongSafe(p.getId())));
        }
        if (p.getArchived() == null) {
            p.setArchived(false);
        }
        mapper.insert(p);
        // initialize stages row if not exists
        if (p.getStages() == null) {
            ProjectStageStatus s = ProjectStageStatus.empty(p.getId());
            stageMapper.insert(s);
            p.setStages(s);
        } else {
            p.getStages().setProjectId(p.getId());
            upsertStages(p.getStages());
        }
        return p;
    }

    public Optional<Project> findById(String id) {
        Project p = mapper.selectById(id);
        if (p == null) return Optional.empty();
        ProjectStageStatus s = stageMapper.selectById(id);
        p.setStages(s != null ? s : ProjectStageStatus.empty(id));
        return Optional.of(p);
    }

    public Project update(String id, Project p) {
        mapper.updateById(p);
        if (p.getStages() != null) {
            p.getStages().setProjectId(id);
            upsertStages(p.getStages());
        }
        return p;
    }

    public void delete(String id) {
        mapper.deleteById(id);
        stageMapper.deleteById(id);
    }

    public List<Project> findAll() {
        List<Project> list = mapper.selectList(null);
        list.forEach(pr -> {
            ProjectStageStatus s = stageMapper.selectById(pr.getId());
            pr.setStages(s != null ? s : ProjectStageStatus.empty(pr.getId()));
        });
        return list;
    }

    public boolean existsByProjectCode(String code) {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<Project>()
                .eq(Project::getProjectCode, code);
        return mapper.selectCount(qw) > 0;
    }

    public List<Project> search(String q, Boolean stageGlass, Boolean stageFrame, Boolean stagePurchase,
                                Boolean stageTransport, Boolean stageInstall, Boolean stageRepair,
                                LocalDate start, LocalDate end, String sortBy, String sortOrder,
                                int page, int pageSize, Boolean archived, Boolean includeArchived) {
        LambdaQueryWrapper<Project> qw = buildWrapper(q, start, end, archived, includeArchived);
        applySort(qw, sortBy, sortOrder);
        Page<Project> mp = new Page<>(page, pageSize);
        List<Project> records = mapper.selectPage(mp, qw).getRecords();
        records.forEach(pr -> {
            ProjectStageStatus s = stageMapper.selectById(pr.getId());
            pr.setStages(s != null ? s : ProjectStageStatus.empty(pr.getId()));
        });
        if (anyNonNull(stageGlass, stageFrame, stagePurchase, stageTransport, stageInstall, stageRepair)) {
            records.removeIf(pr -> !matchesStages(pr.getStages(), stageGlass, stageFrame, stagePurchase, stageTransport, stageInstall, stageRepair));
        }
        return records;
    }

    public long count(String q, Boolean stageGlass, Boolean stageFrame, Boolean stagePurchase,
                      Boolean stageTransport, Boolean stageInstall, Boolean stageRepair,
                      LocalDate start, LocalDate end, Boolean archived, Boolean includeArchived) {
        LambdaQueryWrapper<Project> qw = buildWrapper(q, start, end, archived, includeArchived);
        long cnt = mapper.selectCount(qw);
        if (anyNonNull(stageGlass, stageFrame, stagePurchase, stageTransport, stageInstall, stageRepair)) {
            // approximate by loading ids then filtering; for performance, a join is recommended later
            List<Project> list = mapper.selectList(qw.last("limit 100000"));
            return list.stream().map(Project::getId).distinct().filter(id -> {
                ProjectStageStatus s = stageMapper.selectById(id);
                return matchesStages(s, stageGlass, stageFrame, stagePurchase, stageTransport, stageInstall, stageRepair);
            }).count();
        }
        return cnt;
    }

    private LambdaQueryWrapper<Project> buildWrapper(String q, LocalDate start, LocalDate end,
                                                     Boolean archived, Boolean includeArchived) {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<>();
        if (q != null && !q.isEmpty()) {
            String t = "%" + q + "%";
            qw.and(w -> w.like(Project::getProjectCode, t)
                    .or().like(Project::getName, t)
                    .or().like(Project::getClientName, t)
                    .or().like(Project::getAddress, t));
        }
        if (start != null || end != null) {
            String s = start != null ? start.toString() : "0000-01-01";
            String e = end != null ? end.toString() : "9999-12-31";
            qw.apply("( (start_date is not null or end_date is not null) and (coalesce(start_date, end_date) <= {0} and coalesce(end_date, start_date) >= {1}) )", e, s);
        }
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
        qw.last("order by " + col + (desc ? " desc" : " asc"));
    }

    private void upsertStages(ProjectStageStatus s) {
        if (s == null || s.getProjectId() == null) return;
        ProjectStageStatus old = stageMapper.selectById(s.getProjectId());
        if (old == null) stageMapper.insert(s); else stageMapper.updateById(s);
    }

    private boolean matchesStages(ProjectStageStatus s, Boolean g, Boolean f, Boolean p, Boolean t, Boolean i, Boolean r) {
        if (s == null) s = ProjectStageStatus.empty(null);
        if (g != null && !g.equals(Boolean.TRUE.equals(s.getGlass()))) return false;
        if (f != null && !f.equals(Boolean.TRUE.equals(s.getFrame()))) return false;
        if (p != null && !p.equals(Boolean.TRUE.equals(s.getPurchase()))) return false;
        if (t != null && !t.equals(Boolean.TRUE.equals(s.getTransport()))) return false;
        if (i != null && !i.equals(Boolean.TRUE.equals(s.getInstall()))) return false;
        if (r != null && !r.equals(Boolean.TRUE.equals(s.getRepair()))) return false;
        return true;
    }

    private boolean anyNonNull(Object... arr) { for (Object o : arr) if (o != null) return true; return false; }

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
}
