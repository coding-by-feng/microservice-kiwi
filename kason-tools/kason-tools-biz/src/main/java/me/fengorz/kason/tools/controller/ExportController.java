package me.fengorz.kason.tools.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import me.fengorz.kason.tools.model.project.Project;
import me.fengorz.kason.tools.service.ExportService;
import me.fengorz.kason.tools.service.ProjectService;
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
@RequestMapping("/rangi_windows/api/export")
@Api(tags = "Export", description = "Export project schedules as Excel or PDF")
public class ExportController {
    private final ProjectService projectService;
    private final ExportService exportService;

    public ExportController(ProjectService projectService, ExportService exportService) {
        this.projectService = projectService;
        this.exportService = exportService;
    }

    @GetMapping("/excel")
    @ApiOperation(value = "Export projects to Excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "start", value = "Start date (YYYY-MM-DD)", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "end", value = "End date (YYYY-MM-DD)", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "archived", value = "If true/false, filter by archived flag. If null, defaults to active only.", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "includeArchived", value = "If true, include archived and active.", dataType = "boolean", paramType = "query")
    })
    public ResponseEntity<byte[]> exportExcel(@RequestParam(required = false) String start,
                                              @RequestParam(required = false) String end,
                                              @RequestParam(required = false) Boolean archived,
                                              @RequestParam(required = false) Boolean includeArchived) throws Exception {
        Map<String, Object> res = projectService.list(null, null, start, end, 1, 10000, "start_date", "asc", archived, includeArchived);
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
    @ApiOperation(value = "Export projects to PDF", produces = MediaType.APPLICATION_PDF_VALUE)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "start", value = "Start date (YYYY-MM-DD)", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "end", value = "End date (YYYY-MM-DD)", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "archived", value = "If true/false, filter by archived flag. If null, defaults to active only.", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "includeArchived", value = "If true, include archived and active.", dataType = "boolean", paramType = "query")
    })
    public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false) String start,
                                            @RequestParam(required = false) String end,
                                            @RequestParam(required = false) Boolean archived,
                                            @RequestParam(required = false) Boolean includeArchived) throws Exception {
        Map<String, Object> res = projectService.list(null, null, start, end, 1, 10000, "start_date", "asc", archived, includeArchived);
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
