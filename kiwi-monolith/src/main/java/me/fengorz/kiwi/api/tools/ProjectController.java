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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.tools.entity.Project;
import me.fengorz.kiwi.domain.tools.entity.ProjectPhoto;
import me.fengorz.kiwi.domain.tools.service.ProjectPhotoService;
import me.fengorz.kiwi.domain.tools.service.ProjectService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Project Controller
 * CRUD and media (photo/video) management for Projects
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tools/project")
@Tag(name = "Projects", description = "CRUD and media management for Projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectPhotoService projectPhotoService;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024; // 5MB

    @GetMapping("/projects")
    @Operation(summary = "List projects with filter/sort/pagination")
    public R<Map<String, Object>> listProjects(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean glass,
            @RequestParam(required = false) Boolean frame,
            @RequestParam(required = false) Boolean purchase,
            @RequestParam(required = false) Boolean transport,
            @RequestParam(required = false) Boolean install,
            @RequestParam(required = false) Boolean repair,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) Boolean includeArchived) {

        Page<Project> pageRequest = new Page<>(page, pageSize);

        Page<Project> projectPage;
        if (includeArchived != null && includeArchived) {
            projectPage = projectService.findAllProjects(pageRequest);
        } else if (archived != null) {
            projectPage = projectService.findByArchived(archived, pageRequest);
        } else {
            projectPage = projectService.findByArchived(false, pageRequest);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", projectPage.getRecords());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", projectPage.getTotal());

        return R.ok(result);
    }

    @GetMapping("/projects/{id}")
    @Operation(summary = "Get project by ID")
    public R<Project> getProject(@PathVariable String id) {
        return projectService.findById(id)
                .map(R::ok)
                .orElse(R.failed("Project not found"));
    }

    @PostMapping(value = "/projects", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new project")
    public ResponseEntity<R<Project>> createProject(@RequestBody Project project) {
        project.setCreatedAt(LocalDate.now());
        project.setArchived(false);
        Project saved = projectService.saveProject(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(R.ok(saved));
    }

    @PutMapping(value = "/projects/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update project fields")
    public R<Project> updateProject(@PathVariable String id, @RequestBody Project project) {
        return projectService.findById(id)
                .map(existing -> {
                    project.setId(id);
                    project.setCreatedAt(existing.getCreatedAt());
                    return R.ok(projectService.saveProject(project));
                })
                .orElse(R.failed("Project not found"));
    }

    @PatchMapping(value = "/projects/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Patch project fields (partial update)")
    public R<Project> patchProject(@PathVariable String id, @RequestBody Map<String, Object> patch) {
        return projectService.findById(id)
                .map(existing -> {
                    if (patch.containsKey("archived")) {
                        existing.setArchived((Boolean) patch.get("archived"));
                    }
                    if (patch.containsKey("name")) {
                        existing.setName((String) patch.get("name"));
                    }
                    if (patch.containsKey("address")) {
                        existing.setAddress((String) patch.get("address"));
                    }
                    // Add more fields as needed
                    return R.ok(projectService.saveProject(existing));
                })
                .orElse(R.failed("Project not found"));
    }

    @PostMapping(value = "/projects/{id}/archive", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Archive or unarchive a project")
    public R<Project> archiveProject(
            @PathVariable String id,
            @RequestParam(required = false) Boolean archived,
            @RequestBody(required = false) Map<String, Boolean> body) {
        Boolean archiveValue = archived;
        if (archiveValue == null && body != null) {
            archiveValue = body.get("archived");
        }
        if (archiveValue == null) {
            archiveValue = true;
        }

        final Boolean finalArchived = archiveValue;
        return projectService.findById(id)
                .map(existing -> {
                    existing.setArchived(finalArchived);
                    return R.ok(projectService.saveProject(existing));
                })
                .orElse(R.failed("Project not found"));
    }

    @DeleteMapping("/projects/{id}")
    @Operation(summary = "Delete project by ID")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        if (projectService.existsById(id)) {
            projectService.deleteProjectById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ---------- Photo APIs ----------

    @PostMapping(value = "/projects/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a project media (photo or video)")
    public R<ProjectPhoto> uploadPhoto(
            @PathVariable("id") String projectId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            HttpServletRequest request) {
        MultipartFile pic = file != null ? file : photo;
        if (pic == null || pic.isEmpty()) {
            return R.failed("File is required");
        }
        try {
            validateImageUpload(pic);
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        // TODO: Implement actual file storage
        ProjectPhoto projectPhoto = new ProjectPhoto();
        projectPhoto.setProjectId(projectId);
        projectPhoto.setOriginalName(pic.getOriginalFilename());
        projectPhoto.setContentType(pic.getContentType());
        projectPhoto.setSize(pic.getSize());
        return R.ok(projectPhotoService.savePhoto(projectPhoto));
    }

    private void validateImageUpload(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF, and WebP images are allowed");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("File size must not exceed 5MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            boolean extensionMatches = switch (contentType.toLowerCase()) {
                case "image/jpeg" -> "jpg".equals(extension) || "jpeg".equals(extension);
                case "image/png" -> "png".equals(extension);
                case "image/gif" -> "gif".equals(extension);
                case "image/webp" -> "webp".equals(extension);
                default -> false;
            };
            if (!extensionMatches) {
                throw new IllegalArgumentException("File extension does not match content type");
            }
        }
    }

    @GetMapping("/projects/{id}/photos")
    @Operation(summary = "List photos by project ID")
    public R<List<ProjectPhoto>> listPhotos(@PathVariable("id") String projectId) {
        return R.ok(projectPhotoService.findByProjectIdOrderByCreatedAtDesc(projectId));
    }

    @GetMapping("/projects/{id}/photo/{token}")
    @Operation(summary = "Download photo stream by token")
    public ResponseEntity<InputStreamResource> downloadPhoto(
            @PathVariable("id") String projectId,
            @PathVariable("token") String token) {
        // TODO: Implement actual file streaming from storage
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @DeleteMapping("/projects/{id}/photos/{photoId}")
    @Operation(summary = "Delete a photo by ID")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable("id") String projectId,
            @PathVariable String photoId) {
        projectPhotoService.deletePhotoById(photoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/projects/{id}/photo/{token}")
    @Operation(summary = "Delete a photo by token")
    public ResponseEntity<Void> deletePhotoByToken(
            @PathVariable("id") String projectId,
            @PathVariable String token) {
        projectPhotoService.findByToken(token).ifPresent(photo -> projectPhotoService.removeById(photo.getId()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/projects/{id}/photos")
    @Operation(summary = "Delete all photos for a project")
    public ResponseEntity<Void> deleteAllPhotos(@PathVariable("id") String projectId) {
        projectPhotoService.deleteByProjectId(projectId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/projects/{id}/stages", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update project stage flags only")
    public R<Project> patchProjectStages(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> stages) {
        return projectService.findById(id)
                .map(existing -> {
                    if (stages.containsKey("glass")) {
                        existing.setGlass(stages.get("glass"));
                    }
                    if (stages.containsKey("frame")) {
                        existing.setFrame(stages.get("frame"));
                    }
                    if (stages.containsKey("purchase")) {
                        existing.setPurchase(stages.get("purchase"));
                    }
                    if (stages.containsKey("transport")) {
                        existing.setTransport(stages.get("transport"));
                    }
                    if (stages.containsKey("install")) {
                        existing.setInstall(stages.get("install"));
                    }
                    if (stages.containsKey("repair")) {
                        existing.setRepair(stages.get("repair"));
                    }
                    return R.ok(projectService.saveProject(existing));
                })
                .orElse(R.failed("Project not found"));
    }
}
