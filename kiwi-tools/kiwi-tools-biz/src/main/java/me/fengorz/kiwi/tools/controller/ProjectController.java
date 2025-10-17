package me.fengorz.kiwi.tools.controller;

import io.swagger.annotations.*;
import me.fengorz.kiwi.tools.api.dto.*;
import me.fengorz.kiwi.tools.api.mapper.ProjectDtoMapper;
import me.fengorz.kiwi.tools.exception.ToolsException;
import me.fengorz.kiwi.tools.model.Project;
import me.fengorz.kiwi.tools.model.ProjectPhoto;
import me.fengorz.kiwi.tools.service.ProjectPhotoService;
import me.fengorz.kiwi.tools.service.ProjectService;
import me.fengorz.kiwi.tools.service.StorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
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
@Api(tags = "Projects", description = "CRUD and photo management for Projects")
public class ProjectController {

    private final ProjectService service;
    private final ProjectPhotoService photoService;
    private final StorageService storageService;

    public ProjectController(ProjectService service, ProjectPhotoService photoService, StorageService storageService) {
        this.service = service;
        this.photoService = photoService;
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
    @ApiOperation(value = "List projects with filter/sort/pagination", notes = "Returns a page envelope: { items, page, pageSize, total }")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "q", value = "Free-text search across name/address", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "status", value = "Status code filter", allowableValues = "not_started,in_progress,completed", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "start", value = "Start date (YYYY-MM-DD)", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "end", value = "End date (YYYY-MM-DD)", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "Page number (1-based)", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "Page size", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "sortBy", value = "Sort field (e.g., created_at, start_date)", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "sortOrder", value = "asc|desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "archived", value = "If true/false, filter by archived flag (ignored when includeArchived=true). If null, defaults to active only.", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "includeArchived", value = "If true, return only archived projects.", dataType = "boolean", paramType = "query")
    })
    public PageResponse<ProjectResponse> listProjects(
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
        Map<String, Object> res = service.list(q, status, start, end, page, pageSize, sortBy, sortOrder, archived, includeArchived);
        return ProjectDtoMapper.toPageResponse(res);
    }

    /**
     * GET /api/projects/{id}
     * Returns 404 if the project is not found.
     */
    // GET /api/projects/:id
    @GetMapping("/projects/{id}")
    @ApiOperation(value = "Get project by id")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found")
    })
    public ProjectResponse getProject(@ApiParam(value = "Project id", required = true) @PathVariable String id) {
        Project p = service.get(id);
        return ProjectDtoMapper.toResponse(p);
    }

    /**
     * POST /api/projects
     * Creates a new project. Server generates id, projectCode, createdAt. Validation errors return 400.
     */
    // POST /api/projects
    @PostMapping(value = "/projects", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a new project")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Created"),
            @ApiResponse(code = 400, message = "Validation error")
    })
    public ResponseEntity<ProjectResponse> createProject(@ApiParam(value = "Project fields", required = true) @RequestBody ProjectCreateRequest body) {
        Project created = service.create(ProjectDtoMapper.toEntity(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectDtoMapper.toResponse(created));
    }

    /**
     * PUT /api/projects/{id}
     * Partial update: accepts any subset of fields (except id/projectCode/createdAt) and re-validates the entity.
     */
    // PUT /api/projects/:id
    @PutMapping(value = "/projects/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Replace project fields (partial semantics)")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, message = "Validation error"),
        @ApiResponse(code = 404, message = "Not Found")
    })
    public ProjectResponse updateProject(@ApiParam(value = "Project id", required = true) @PathVariable String id,
                                 @ApiParam(value = "Partial fields to update") @RequestBody ProjectUpdateRequest body) {
        Project updated = service.update(id, ProjectDtoMapper.toPatch(body));
        return ProjectDtoMapper.toResponse(updated);
    }

    /**
     * PATCH /api/projects/{id} (partial update; supports { "archived": true|false })
     */
    // PATCH /api/projects/:id (partial update; supports { "archived": true|false })
    @PatchMapping(value = "/projects/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Patch project fields (partial update)")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, message = "Validation error"),
        @ApiResponse(code = 404, message = "Not Found")
    })
    public ProjectResponse patchProject(@ApiParam(value = "Project id", required = true) @PathVariable String id,
                                @ApiParam(value = "Fields to patch (e.g., archived)") @RequestBody ProjectPatchRequest body) {
        Project updated = service.update(id, ProjectDtoMapper.toPatch(body));
        return ProjectDtoMapper.toResponse(updated);
    }

    /**
     * POST /api/projects/{id}/archive (default archived=true, idempotent)
     */
    // POST /api/projects/:id/archive (default archived=true, idempotent)
    @PostMapping(value = "/projects/{id}/archive", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Archive or unarchive a project", notes = "If request param 'archived' is null, the body {archived:true|false} is used. Defaults to true if both absent.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "archived", value = "true to archive, false to unarchive", dataType = "boolean", paramType = "query")
    })
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")
    })
    public ProjectResponse archiveProject(@ApiParam(value = "Project id", required = true) @PathVariable String id,
                                  @RequestParam(value = "archived", required = false) Boolean archived,
                                  @ApiParam(value = "Optional body {archived:true|false}") @RequestBody(required = false) ProjectArchiveRequest body) {
        if (archived == null && body != null) {
            archived = body.getArchived();
        }
        Project updated = service.archive(id, archived);
        return ProjectDtoMapper.toResponse(updated);
    }

    /**
     * DELETE /api/projects/:id
     */
    // DELETE /api/projects/:id
    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Delete project by id")
    @ApiResponses({
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 404, message = "Not Found")
    })
    public void deleteProject(@ApiParam(value = "Project id", required = true) @PathVariable String id) {
        service.delete(id);
    }

    // ---------- Photo APIs ----------

    /**
     * Upload a photo to a project, persist metadata, and return the ProjectPhoto record.
     * Accepts multipart/form-data with field name "file" or "photo".
     */
    // POST /api/projects/:id/photo
    @PostMapping(value = "/projects/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "Upload a project photo", notes = "Use form field 'file' or 'photo'. Only image/* is allowed.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Image file", dataType = "__file", paramType = "form", required = false),
        @ApiImplicitParam(name = "photo", value = "Alternate image field", dataType = "__file", paramType = "form", required = false)
    })
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, message = "Validation error")
    })
    public ProjectPhotoResponse uploadPhoto(@ApiParam(value = "Project id", required = true) @PathVariable("id") String projectId,
                                    @RequestPart(value = "file", required = false) MultipartFile file,
                                    @RequestPart(value = "photo", required = false) MultipartFile photo,
                                    HttpServletRequest request) throws IOException {
        MultipartFile pic = file != null ? file : photo;
        if (pic == null || pic.isEmpty()) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "file is required");
        }
        ProjectPhoto saved = photoService.upload(projectId, pic, request);
        return ProjectDtoMapper.toResponse(saved);
    }

    /** List photos for a project (ordered). */
    @GetMapping("/projects/{id}/photos")
    @ApiOperation(value = "List photos by project id")
    public List<ProjectPhotoResponse> listPhotos(@ApiParam(value = "Project id", required = true) @PathVariable("id") String projectId) {
        return ProjectDtoMapper.toPhotoResponseList(photoService.list(projectId));
    }

    /** Stream download by token. */
    @GetMapping("/projects/{id}/photo/{token}")
    @ApiOperation(value = "Download photo stream by token", produces = "application/octet-stream")
    public ResponseEntity<InputStreamResource> downloadPhoto(@ApiParam(value = "Project id", required = true) @PathVariable("id") String projectId,
                                                             @ApiParam(value = "Photo token", required = true) @PathVariable("token") String token) throws IOException {
        InputStreamResource body = new InputStreamResource(storageService.openPhotoStreamByToken(token));
        String contentType = storageService.inferContentTypeFromToken(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000, public")
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }

    /** Delete a photo by id. */
    @DeleteMapping("/projects/{id}/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Delete a photo by id")
    @ApiResponses({
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 404, message = "Not Found")
    })
    public void deletePhoto(@ApiParam(value = "Project id", required = true) @PathVariable("id") String projectId,
                            @ApiParam(value = "Photo id", required = true) @PathVariable String photoId) {
        photoService.delete(photoId);
    }

    /** Delete a photo by token (idempotent). */
    @DeleteMapping("/projects/{id}/photo/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Delete a photo by token", notes = "Idempotent: returns 204 even if token not found")
    public void deletePhotoByToken(@ApiParam(value = "Project id", required = true) @PathVariable("id") String projectId,
                                   @ApiParam(value = "Photo token", required = true) @PathVariable String token) {
        photoService.deleteByToken(projectId, token);
    }

    /** Delete all photos for a project (idempotent). */
    @DeleteMapping("/projects/{id}/photos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Delete all photos for a project", notes = "Idempotent: deletes all and returns 204")
    public void deleteAllPhotos(@ApiParam(value = "Project id", required = true) @PathVariable("id") String projectId) {
        photoService.deleteAll(projectId);
    }
}
