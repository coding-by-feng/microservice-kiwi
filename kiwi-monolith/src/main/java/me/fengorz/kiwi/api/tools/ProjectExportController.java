/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.api.tools;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.domain.tools.entity.Project;
import me.fengorz.kiwi.domain.tools.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Project Export Controller
 * Export projects to Excel format
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/rangi_windows/api/export")
@Tag(name = "Project Export", description = "Export projects to Excel")
public class ProjectExportController {

    private final ProjectService projectService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping("/excel")
    @Operation(summary = "Export projects to Excel")
    public void exportToExcel(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) Boolean archived,
            HttpServletResponse response) throws IOException {

        // Build query
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();

        // Date range filter
        if (start != null && !start.isEmpty()) {
            LocalDate startDate = LocalDate.parse(start, DATE_FORMATTER);
            queryWrapper.ge(Project::getCreatedAt, startDate);
        }
        if (end != null && !end.isEmpty()) {
            LocalDate endDate = LocalDate.parse(end, DATE_FORMATTER);
            queryWrapper.le(Project::getCreatedAt, endDate);
        }

        // Archived filter
        if (archived != null) {
            queryWrapper.eq(Project::getArchived, archived);
        }

        // Order by created date desc
        queryWrapper.orderByDesc(Project::getCreatedAt);

        // Fetch projects
        List<Project> projects = projectService.list(queryWrapper);

        // Generate filename
        String filename = "projects_" + LocalDate.now().format(DATE_FORMATTER) + ".xlsx";

        // Set response headers
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        // Create Excel writer
        try (ExcelWriter writer = ExcelUtil.getWriter(true)) {
            // Set column headers
            writer.addHeaderAlias("projectCode", "Project Code");
            writer.addHeaderAlias("name", "Project Name");
            writer.addHeaderAlias("address", "Address");
            writer.addHeaderAlias("contactName", "Contact Name");
            writer.addHeaderAlias("contactPhone", "Contact Phone");
            writer.addHeaderAlias("notes", "Notes");
            writer.addHeaderAlias("startDate", "Start Date");
            writer.addHeaderAlias("endDate", "End Date");
            writer.addHeaderAlias("glass", "Glass");
            writer.addHeaderAlias("frame", "Frame");
            writer.addHeaderAlias("purchase", "Purchase");
            writer.addHeaderAlias("transport", "Transport");
            writer.addHeaderAlias("install", "Install");
            writer.addHeaderAlias("repair", "Repair");
            writer.addHeaderAlias("archived", "Archived");
            writer.addHeaderAlias("createdAt", "Created At");

            // Only write specified columns
            writer.setOnlyAlias(true);

            // Write data
            writer.write(projects, true);

            // Auto-size columns
            writer.autoSizeColumnAll();

            // Flush to response
            writer.flush(response.getOutputStream(), true);
        }

        log.info("Exported {} projects to Excel", projects.size());
    }
}
