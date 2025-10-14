package me.fengorz.kiwi.tools.controller;

import me.fengorz.kiwi.tools.exception.AppException;
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

@RestController
@RequestMapping("/api")
public class ProjectController {

    private final ProjectService service;
    private final StorageService storageService;

    public ProjectController(ProjectService service, StorageService storageService) {
        this.service = service;
        this.storageService = storageService;
    }

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
            @RequestParam(required = false) String sortOrder
    ) {
        return service.list(q, status, start, end, page, pageSize, sortBy, sortOrder);
    }

    // GET /api/projects/:id
    @GetMapping("/projects/{id}")
    public Project getProject(@PathVariable String id) {
        return service.get(id);
    }

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

    // PUT /api/projects/:id
    @PutMapping(value = "/projects/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Project updateProject(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return service.update(id, body);
    }

    // POST /api/projects/:id/photo
    @PostMapping(value = "/projects/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Project uploadPhoto(@PathVariable String id, @RequestPart("photo") MultipartFile photo, HttpServletRequest request) throws IOException {
        if (photo == null || photo.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "validation_error", "photo is required");
        }
        String ctype = photo.getContentType();
        if (ctype == null || !ctype.toLowerCase().startsWith("image/")) {
            throw new AppException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type", "Only image/* supported");
        }
        if (photo.getSize() > 5 * 1024 * 1024) {
            throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE, "payload_too_large", "File too large");
        }
        String url = storageService.storeProjectPhoto(id, photo, request);
        return service.setPhotoUrl(id, url);
    }

    private String str(Object v) { return v == null ? null : String.valueOf(v); }
}
