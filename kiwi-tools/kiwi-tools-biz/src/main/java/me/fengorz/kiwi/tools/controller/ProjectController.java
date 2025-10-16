package me.fengorz.kiwi.tools.controller;

import me.fengorz.kiwi.tools.exception.ToolsException;
import me.fengorz.kiwi.tools.model.Project;
import me.fengorz.kiwi.tools.service.ProjectService;
import me.fengorz.kiwi.tools.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * REST controller for Project entity.
 *
 * Mirrors the frontend local API with the same function signatures and field names (camelCase).
 * Base path: /api
 * Auth: optional Bearer token via Authorization header (no enforcement here; gateway can enforce later).
 * Content types:
 *  - JSON for CRUD
 *  - multipart/form-data for photo upload (field name: "photo")
 *
 * Upload constraints:
 *  - Only image/* content types
 *  - Max size ~5MB (configurable via Spring's multipart settings)
 */
@RestController
@RequestMapping("/api")
public class ProjectController {

    private final ProjectService service;
    private final StorageService storageService;

    public ProjectController(ProjectService service, StorageService storageService) {
        this.service = service;
        this.storageService = storageService;
    }

    /**
     * GET /api/projects
     *
     * Supports free-text query (q), status filter, date window (start/end), pagination (page/pageSize),
     * and sorting (sortBy/sortOrder). Returns a page envelope with items, page, pageSize, and total.
     */
    // GET /api/projects
    @GetMapping("/projects")
    public Map<String, Object> listProjects(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) Boolean includeArchived
    ) {
        return service.list(q, status, start, end, page, pageSize, sortBy, sortOrder, archived, includeArchived);
    }

    /**
     * GET /api/projects/{id}
     * Returns 404 if the project is not found.
     */
    // GET /api/projects/:id
    @GetMapping("/projects/{id}")
    public Project getProject(@PathVariable String id) {
        return service.get(id);
    }

    /**
     * POST /api/projects
     * Creates a new project. Server generates id, projectCode, createdAt. Validation errors return 400.
     */
    // POST /api/projects
    @PostMapping(value = "/projects", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Project> createProject(@RequestBody Map<String, Object> body) {
        Project in = new Project();
        in.setName(str(body.get("name")));
        in.setClientName(str(body.get("clientName")));
        in.setClientPhone(str(body.get("clientPhone")));
        in.setAddress(str(body.get("address")));
        in.setSalesPerson(str(body.get("salesPerson")));
        in.setInstaller(str(body.get("installer")));
        in.setTeamMembers(str(body.get("teamMembers")));
        in.setStartDate(str(body.get("startDate")));
        in.setEndDate(str(body.get("endDate")));
        in.setStatus(str(body.get("status")));
        in.setTodayTask(str(body.get("todayTask")));
        in.setProgressNote(str(body.get("progressNote")));
        Project created = service.create(in);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/projects/{id}
     * Partial update: accepts any subset of fields (except id/projectCode/createdAt) and re-validates the entity.
     */
    // PUT /api/projects/:id
    @PutMapping(value = "/projects/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Project updateProject(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return service.update(id, body);
    }

    /**
     * PATCH /api/projects/{id} (partial update; supports { "archived": true|false })
     */
    // PATCH /api/projects/:id (partial update; supports { "archived": true|false })
    @PatchMapping(value = "/projects/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Project patchProject(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return service.update(id, body);
    }

    /**
     * POST /api/projects/{id}/archive (default archived=true, idempotent)
     */
    // POST /api/projects/:id/archive (default archived=true, idempotent)
    @PostMapping(value = "/projects/{id}/archive", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Project archiveProject(@PathVariable String id,
                                  @RequestParam(value = "archived", required = false) Boolean archived,
                                  @RequestBody(required = false) Map<String, Object> body) {
        if (archived == null && body != null && body.containsKey("archived")) {
            Object v = body.get("archived");
            archived = (v instanceof Boolean) ? (Boolean) v : ("true".equalsIgnoreCase(String.valueOf(v)) || "1".equals(String.valueOf(v)));
        }
        return service.archive(id, archived);
    }

    /**
     * DELETE /api/projects/:id
     */
    // DELETE /api/projects/:id
    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable String id) {
        service.delete(id);
    }

    /**
     * POST /api/projects/{id}/photo
     * Upload a project image, store it, update photoUrl, and return the updated project.
     * Returns 415 for unsupported media types and 413 if payload too large.
     */
    // POST /api/projects/:id/photo
    @PostMapping(value = "/projects/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Project uploadPhoto(@PathVariable String id,
                               @RequestPart(value = "file", required = false) MultipartFile file,
                               @RequestPart(value = "photo", required = false) MultipartFile photo,
                               HttpServletRequest request) throws IOException {
        MultipartFile pic = file != null ? file : photo;
        if (pic == null || pic.isEmpty()) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "file is required");
        }
        String ctype = pic.getContentType();
        if (ctype == null || !ctype.toLowerCase().startsWith("image/")) {
            throw new ToolsException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type", "Only image/* supported");
        }
        if (pic.getSize() > 5 * 1024 * 1024) {
            throw new ToolsException(HttpStatus.PAYLOAD_TOO_LARGE, "payload_too_large", "File too large");
        }
        String url = storageService.storeProjectPhoto(id, pic, request);
        return service.setPhotoUrl(id, url);
    }

    private String str(Object v) { return v == null ? null : String.valueOf(v); }
}
