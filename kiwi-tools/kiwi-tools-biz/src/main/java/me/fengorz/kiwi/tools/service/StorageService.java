package me.fengorz.kiwi.tools.service;

import me.fengorz.kiwi.tools.config.ToolsProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StorageService {
    private final ToolsProperties props;

    public StorageService(ToolsProperties props) { this.props = props; }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(props.getUploadDir()));
    }

    public String storeProjectPhoto(String projectId, MultipartFile file, HttpServletRequest request) throws IOException {
        String original = file.getOriginalFilename();
        String safeName = original == null ? "photo" : original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dir = Paths.get(props.getUploadDir(), "projects", projectId);
        Files.createDirectories(dir);
        Path dest = dir.resolve(safeName);
        file.transferTo(dest.toFile());
        String base = props.getPublicBaseUrl();
        if (base == null || base.isEmpty()) {
            String scheme = request.getHeader("X-Forwarded-Proto");
            if (scheme == null || scheme.isEmpty()) scheme = request.getScheme();
            String host = request.getHeader("X-Forwarded-Host");
            if (host == null || host.isEmpty()) host = request.getServerName() + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
            base = scheme + "://" + host;
        }
        String rel = "/uploads/projects/" + projectId + "/" + safeName;
        return base + rel;
    }
}

