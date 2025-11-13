package me.fengorz.kason.tools.service;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.dfs.DfsService;
import me.fengorz.kason.tools.config.ToolsProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@Slf4j
@Service
public class StorageService {
    private final ToolsProperties props;
    private final DfsService dfsService;

    public StorageService(ToolsProperties props, DfsService dfsService) {
        this.props = props;
        this.dfsService = dfsService;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(props.getUploadDir()));
    }

    /**
     * Upload a photo to DFS and return a public API URL that can be used to download it.
     * The returned URL is of the form: {base}/api/projects/{projectId}/photo/{token}
     * where token is a URL-safe base64 encoding of the DFS fileId (e.g. group/path).
     */
    public String storeProjectPhoto(String projectId, MultipartFile file, HttpServletRequest request) throws IOException {
        String original = file.getOriginalFilename();
        String ext = inferExt(file, original);
        String dfsFileId;
        try {
            dfsFileId = dfsService.uploadFile(file.getInputStream(), file.getSize(), ext);
        } catch (Exception e) {
            throw new IOException("DFS upload failed", e);
        }
        String token = encodeToken(dfsFileId);
        String base = computeBaseUrl(request);
        return base + "/api/projects/" + projectId + "/photo/" + token;
    }

    /** Upload raw image to DFS and return the internal fileId (e.g. group/path). */
    public String uploadAndGetFileId(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        String ext = inferExt(file, original);
        try {
            log.info("Uploading file to DFS, original name: {}, inferred ext: {}, size: {}", original, ext, file.getSize());
            return dfsService.uploadFile(file.getInputStream(), file.getSize(), ext);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IOException("DFS upload failed", e);
        }
    }

    /** Create a URL-safe token from DFS fileId. */
    public String tokenFromFileId(String fileId) {
        return encodeToken(fileId);
    }

    /** Build a download URL for given project and token using request base URL. */
    public String buildDownloadUrl(HttpServletRequest request, String projectId, String token) {
        String base = computeBaseUrl(request);
        return base + "/api/projects/" + projectId + "/photo/" + token;
    }

    /**
     * Open an InputStream to the photo stored in DFS using our token format.
     */
    public InputStream openPhotoStreamByToken(String token) throws IOException {
        String fileId = decodeToken(token);
        String group;
        String path;
        int idx = fileId.indexOf('/');
        if (idx > 0) {
            group = fileId.substring(0, idx);
            path = fileId.substring(idx + 1);
        } else {
            // Fallback: whole as path, default group as "group1" (common default)
            group = "group1";
            path = fileId;
        }
        try {
            return dfsService.downloadStream(group, path);
        } catch (Exception e) {
            throw new IOException("Failed to download from DFS", e);
        }
    }

    /** Infer a content type for the photo using the token (by extension) or fall back to octet-stream. */
    public String inferContentTypeFromToken(String token) {
        try {
            String fileId = decodeToken(token);
            String namePart = fileId;
            int lastSlash = namePart.lastIndexOf('/') ;
            if (lastSlash >= 0) namePart = namePart.substring(lastSlash + 1);
            String mime = URLConnection.guessContentTypeFromName(namePart);
            if (mime != null) return mime;
        } catch (Exception ignored) {}
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String computeBaseUrl(HttpServletRequest request) {
        String base = props.getPublicBaseUrl();
        if (base == null || base.isEmpty()) {
            String scheme = request.getHeader("X-Forwarded-Proto");
            if (scheme == null || scheme.isEmpty()) scheme = request.getScheme();
            String host = request.getHeader("X-Forwarded-Host");
            if (host == null || host.isEmpty()) host = request.getServerName() + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
            base = scheme + "://" + host;
        }
        return base;
    }

    private String inferExt(MultipartFile file, String original) {
        String ext = null;
        if (original != null) {
            int dot = original.lastIndexOf('.') ;
            if (dot >= 0 && dot < original.length() - 1) {
                ext = original.substring(dot + 1);
            }
        }
        if (ext == null || ext.isEmpty()) {
            String ctype = file.getContentType();
            if (ctype != null && ctype.startsWith("image/")) {
                ext = ctype.substring("image/".length());
            }
        }
        if (ext == null || ext.isEmpty()) ext = "jpg"; // sensible default
        return ext;
    }

    private String encodeToken(String fileId) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(fileId.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeToken(String token) {
        return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
    }
}
