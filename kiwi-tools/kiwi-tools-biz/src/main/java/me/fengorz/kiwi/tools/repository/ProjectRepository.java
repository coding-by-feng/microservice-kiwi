package me.fengorz.kiwi.tools.repository;

import me.fengorz.kiwi.tools.model.Project;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ProjectRepository {
    private final Map<String, Project> store = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(100);

    public synchronized Project save(Project p) {
        if (p.getId() == null) {
            long id = idGen.incrementAndGet();
            p.setId(String.valueOf(id));
            p.setProjectCode(formatCode(id));
        }
        store.put(p.getId(), p);
        return p;
    }

    public Optional<Project> findById(String id) { return Optional.ofNullable(store.get(id)); }

    public Project update(String id, Project p) { store.put(id, p); return p; }

    public List<Project> findAll() { return new ArrayList<>(store.values()); }

    private String formatCode(long id) {
        if (id < 1000) return String.format("P-%03d", id);
        return "P-" + id;
    }

    public boolean existsByProjectCode(String code) {
        for (Project v : store.values()) {
            if (code.equals(v.getProjectCode())) return true;
        }
        return false;
    }

    public List<Project> search(String q, String status, LocalDate start, LocalDate end,
                                String sortBy, String sortOrder, int page, int pageSize) {
        Comparator<Project> comparator = comparatorFor(sortBy);
        if ("desc".equalsIgnoreCase(sortOrder)) comparator = comparator.reversed();
        return filter(q, status, start, end)
                .stream()
                .sorted(comparator)
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    public long count(String q, String status, LocalDate start, LocalDate end) {
        return filter(q, status, start, end).size();
    }

    private List<Project> filter(String q, String status, LocalDate start, LocalDate end) {
        return store.values().stream().filter(p -> {
            boolean match = true;
            if (q != null && !q.isEmpty()) {
                String t = q.toLowerCase();
                match &= contains(p.getProjectCode(), t) || contains(p.getName(), t)
                        || contains(p.getClientName(), t) || contains(p.getAddress(), t);
            }
            if (status != null && !status.isEmpty()) {
                match &= status.equals(p.getStatus());
            }
            if (start != null || end != null) {
                LocalDate ps = safeParse(p.getStartDate());
                LocalDate pe = safeParse(p.getEndDate());
                // overlap logic: [ps, pe] overlaps [start, end]
                LocalDate s = start != null ? start : LocalDate.MIN;
                LocalDate e = end != null ? end : LocalDate.MAX;
                if (ps == null && pe == null) {
                    // if no dates, exclude from date-filtered queries
                    match &= false;
                } else {
                    LocalDate a = ps != null ? ps : pe;
                    LocalDate b = pe != null ? pe : ps;
                    if (a != null && b != null) {
                        match &= !a.isAfter(e) && !b.isBefore(s);
                    } else {
                        LocalDate single = a != null ? a : b;
                        match &= !single.isBefore(s) && !single.isAfter(e);
                    }
                }
            }
            return match;
        }).collect(Collectors.toList());
    }

    private boolean contains(String field, String token) {
        return field != null && field.toLowerCase().contains(token);
    }

    private LocalDate safeParse(String s) {
        try {
            if (s == null || s.isEmpty()) return null;
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Comparator<Project> comparatorFor(String sortBy) {
        if ("startDate".equals(sortBy)) return Comparator.comparing(Project::getStartDate, Comparator.nullsLast(String::compareTo));
        if ("endDate".equals(sortBy)) return Comparator.comparing(Project::getEndDate, Comparator.nullsLast(String::compareTo));
        if ("projectCode".equals(sortBy)) return Comparator.comparing(Project::getProjectCode, Comparator.nullsLast(String::compareTo));
        return Comparator.comparing(Project::getCreatedAt, Comparator.nullsLast(String::compareTo));
    }
}
