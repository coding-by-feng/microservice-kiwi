package me.fengorz.kiwi.tools.service;

import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.tools.exception.ToolsException;
import me.fengorz.kiwi.tools.model.ProjectPhoto;
import me.fengorz.kiwi.tools.repository.ProjectPhotoRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ProjectPhotoService {
    private final ProjectPhotoRepository repo;
    private final StorageService storageService;
    private final DfsService dfsService;

    public ProjectPhotoService(ProjectPhotoRepository repo, StorageService storageService, @Qualifier("dfsService") DfsService dfsService) {
        this.repo = repo;
        this.storageService = storageService;
        this.dfsService = dfsService;
    }

    public ProjectPhoto upload(String projectId, MultipartFile file, HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ToolsException(HttpStatus.BAD_REQUEST, "validation_error", "file is required");
        }
        String ctype = file.getContentType();
        if (ctype == null || !ctype.toLowerCase().startsWith("image/")) {
            throw new ToolsException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type", "Only image/* supported");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
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
