package me.fengorz.kiwi.tools.service;

import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.tools.config.ToolsProperties;
import me.fengorz.kiwi.tools.exception.ToolsException;
import me.fengorz.kiwi.tools.model.project.ProjectPhoto;
import me.fengorz.kiwi.tools.repository.ProjectPhotoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectPhotoService {
    private final ProjectPhotoRepository repo;
    private final StorageService storageService;
    private final DfsService dfsService;
    private final ToolsProperties toolsProperties;

    public ProjectPhotoService(ProjectPhotoRepository repo, StorageService storageService, DfsService dfsService, ToolsProperties toolsProperties) {
        this.repo = repo;
        this.storageService = storageService;
        this.dfsService = dfsService;
        this.toolsProperties = toolsProperties;
    }

    public ProjectPhoto upload(String projectId, MultipartFile file, HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "file is required");
        }
        String ctype = file.getContentType();
        if (ctype == null) {
            throw new ToolsException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type", "Missing content type");
        }
        String lower = ctype.toLowerCase();
        boolean isImage = lower.startsWith("image/");
        boolean isVideo = lower.startsWith("video/");
        if (!isImage && !isVideo) {
            throw new ToolsException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type", "Only image/* or video/* supported");
        }
        DataSize max = isImage ? toolsProperties.getPhotoMaxSize() : toolsProperties.getVideoMaxSize();
        if (max != null && file.getSize() > max.toBytes()) {
            throw new ToolsException(HttpStatus.PAYLOAD_TOO_LARGE, "payload_too_large", "File too large");
        }
        String fileId = storageService.uploadAndGetFileId(file);
        String token = storageService.tokenFromFileId(fileId);
        ProjectPhoto photo = new ProjectPhoto();
        photo.setProjectId(projectId);
        photo.setDfsFileId(fileId);
        photo.setToken(token);
        photo.setContentType(ctype);
        photo.setSize(file.getSize());
        return repo.save(photo);
    }

    /** Upload multiple files at once and return saved photo records */
    public List<ProjectPhoto> uploadMany(String projectId, List<MultipartFile> files, HttpServletRequest request) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "files are required");
        }
        List<ProjectPhoto> saved = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue; // skip empties
            saved.add(upload(projectId, f, request));
        }
        return saved;
    }

    public List<ProjectPhoto> list(String projectId) {
        return repo.findByProjectId(projectId);
    }

    public InputStream openStreamByToken(String token) throws IOException {
        return storageService.openPhotoStreamByToken(token);
    }

    public void delete(String photoId) {
        repo.findById(photoId).ifPresent(p -> {
            String fid = p.getDfsFileId();
            int idx = fid.indexOf('/');
            String group = idx > 0 ? fid.substring(0, idx) : "group1";
            String path = idx > 0 ? fid.substring(idx + 1) : fid;
            try {
                dfsService.deleteFile(group, path);
            } catch (Exception ignored) {}
            repo.deleteById(photoId);
        });
    }

    public void deleteByToken(String projectId, String token) {
        repo.findByProjectId(projectId).stream()
            .filter(p -> token.equals(p.getToken()))
            .findFirst()
            .ifPresent(p -> delete(p.getId()));
    }

    public void deleteAll(String projectId) {
        repo.findByProjectId(projectId).forEach(p -> delete(p.getId()));
    }
}
