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
package me.fengorz.kiwi.common.dfs;

import io.minio.*;
import io.minio.errors.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.exception.ServiceException;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO DFS Service Implementation
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.backend", havingValue = "minio")
public class MinioDfsServiceImpl implements DfsService {

    private static final String GROUP = "minio";
    private final MinioProperties props;
    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(props.getConnectTimeout(), TimeUnit.MILLISECONDS)
                    .readTimeout(props.getReadTimeout(), TimeUnit.MILLISECONDS)
                    .writeTimeout(props.getWriteTimeout(), TimeUnit.MILLISECONDS)
                    .build();

            minioClient = MinioClient.builder()
                    .endpoint(props.getEndpoint())
                    .credentials(props.getAccessKey(), props.getSecretKey())
                    .httpClient(httpClient)
                    .build();

            // Ensure bucket exists
            ensureBucketExists();
            log.info("MinIO client initialized | endpoint={} | bucket={}", props.getEndpoint(), props.getBucket());
        } catch (Exception e) {
            log.error("Failed to initialize MinIO client", e);
            throw new ServiceException("Failed to initialize MinIO client: " + e.getMessage());
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(props.getBucket())
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(props.getBucket())
                    .build());
            log.info("Created MinIO bucket: {}", props.getBucket());
        }
    }

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName) {
        return uploadFile(inputStream, size, extName, null);
    }

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName, Set<Map<String, String>> metaDataSet) {
        long start = System.currentTimeMillis();
        String objectName = buildObjectName(extName);
        try {
            log.debug("MinIO upload start | size={}, extName={}, objectName={}", size, extName, objectName);

            String contentType = getContentType(extName);

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());

            long cost = System.currentTimeMillis() - start;
            String fullPath = GROUP + "/" + props.getBucket() + "/" + objectName;
            log.info("MinIO upload success | path={} | size={} | cost={}ms", fullPath, size, cost);
            return fullPath;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("MinIO upload failed | objectName={} | cost={}ms", objectName, cost);
            log.error(DfsConstants.UPLOAD_FILE_EXCEPTION, e);
            throw new ServiceException(DfsConstants.UPLOAD_FILE_EXCEPTION, e);
        }
    }

    @Override
    public void deleteFile(String groupName, String path) {
        long start = System.currentTimeMillis();
        String objectName = extractObjectName(path);
        try {
            log.debug("MinIO delete start | objectName={}", objectName);

            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .build());

            long cost = System.currentTimeMillis() - start;
            log.info("MinIO delete success | objectName={} | cost={}ms", objectName, cost);
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("MinIO delete failed | objectName={} | cost={}ms", objectName, cost);
            log.error(DfsConstants.DELETE_FILE_EXCEPTION, e);
            throw new ServiceException(DfsConstants.DELETE_FILE_EXCEPTION, e);
        }
    }

    @Override
    public InputStream downloadStream(String groupName, String path) {
        try {
            String objectName = extractObjectName(path);
            log.debug("MinIO download stream | objectName={}", objectName);

            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error(DfsConstants.DOWNLOAD_STREAM_FILE_EXCEPTION, e);
            throw new ServiceException(DfsConstants.DOWNLOAD_STREAM_FILE_EXCEPTION, e);
        }
    }

    @Override
    public byte[] downloadFile(String groupName, String path) {
        long start = System.currentTimeMillis();
        String objectName = extractObjectName(path);
        try {
            log.debug("MinIO download start | objectName={}", objectName);

            try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .build())) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = stream.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                byte[] data = baos.toByteArray();
                long cost = System.currentTimeMillis() - start;
                log.info("MinIO download success | objectName={} | bytes={} | cost={}ms", objectName, data.length, cost);
                return data;
            }
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("MinIO download failed | objectName={} | cost={}ms", objectName, cost);
            log.error(DfsConstants.DOWNLOAD_FILE_EXCEPTION, e);
            throw new ServiceException(DfsConstants.DOWNLOAD_FILE_EXCEPTION, e);
        }
    }

    private String buildObjectName(String extName) {
        String dateDir = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String filename = UUID.randomUUID().toString().replace("-", "");
        if (extName != null && !extName.isEmpty()) {
            filename = filename + "." + extName;
        }
        return dateDir + "/" + filename;
    }

    private String extractObjectName(String path) {
        // Path format: minio/bucket/yyyy/MM/dd/filename.ext
        // We need to extract: yyyy/MM/dd/filename.ext
        if (path.startsWith(GROUP + "/")) {
            path = path.substring(GROUP.length() + 1);
        }
        if (path.startsWith(props.getBucket() + "/")) {
            path = path.substring(props.getBucket().length() + 1);
        }
        return path;
    }

    private String getContentType(String extName) {
        if (extName == null) return "application/octet-stream";
        return switch (extName.toLowerCase()) {
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "json" -> "application/json";
            case "txt" -> "text/plain";
            case "html" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            default -> "application/octet-stream";
        };
    }
}
