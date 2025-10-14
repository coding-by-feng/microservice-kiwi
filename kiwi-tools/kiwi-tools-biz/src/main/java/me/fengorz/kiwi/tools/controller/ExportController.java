package me.fengorz.kiwi.tools.controller;

import me.fengorz.kiwi.tools.model.Project;
import me.fengorz.kiwi.tools.service.ExportService;
import me.fengorz.kiwi.tools.service.ProjectService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
public class ExportController {
    private final ProjectService projectService;
    private final ExportService exportService;

    public ExportController(ProjectService projectService, ExportService exportService) {
        this.projectService = projectService;
        this.exportService = exportService;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(required = false) String start,
                                              @RequestParam(required = false) String end) throws Exception {
        Map<String, Object> res = projectService.list(null, null, start, end, 1, 10000, "startDate", "asc");
        @SuppressWarnings("unchecked")
        List<Project> items = (List<Project>) res.get("items");
        byte[] data = exportService.toExcel(items);
        String fileName = String.format("施工安排表_%s_%s.xlsx", nvl(start, "all"), nvl(end, "all"));
        String disposition = "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false) String start,
                                            @RequestParam(required = false) String end) throws Exception {
        Map<String, Object> res = projectService.list(null, null, start, end, 1, 10000, "startDate", "asc");
        @SuppressWarnings("unchecked")
        List<Project> items = (List<Project>) res.get("items");
        byte[] data = exportService.toPdf(items);
        String fileName = String.format("施工安排表_%s_%s.pdf", nvl(start, "all"), nvl(end, "all"));
        String disposition = "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    private String nvl(String s, String d) { return s == null || s.isEmpty() ? d : s; }
}
