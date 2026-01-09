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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.exception.ServiceException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * FTP DFS Service Implementation
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.backend", havingValue = "ftp")
public class FtpDfsServiceImpl implements DfsService {

    private static final String GROUP = "ftp";
    private final FtpProperties props;

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName) {
        return uploadFile(inputStream, size, extName, null);
    }

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName, Set<Map<String, String>> metaDataSet) {
        FTPClient client = new FTPClient();
        long start = System.currentTimeMillis();
        String path = null;
        try {
            log.debug("FTP upload start | size={}, extName={}", size, extName);
            configure(client);
            connectAndLogin(client);
            path = buildPath(extName);
            log.debug("FTP upload target path resolved: {}", path);
            ensureDirectories(client, path);
            client.setFileType(FTP.BINARY_FILE_TYPE);
            boolean ok = client.storeFile(path, inputStream);
            logReply(client, "storeFile(" + path + ")");
            if (!ok) {
                throw new IOException("FTP storeFile failed: " + client.getReplyString());
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            long cost = System.currentTimeMillis() - start;
            log.info("FTP upload success | path={}/{} | size={} | cost={}ms", GROUP, path, size, cost);
            return GROUP + "/" + path;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("FTP upload failed | path={} | cost={}ms", path, cost);
            log.error(DfsConstants.UPLOAD_FILE_EXCEPTION, e);
            throw new ServiceException(DfsConstants.UPLOAD_FILE_EXCEPTION, e);
        } finally {
            logoutQuietly(client);
        }
    }

    @Override
    public void deleteFile(String groupName, String path) {
        FTPClient client = new FTPClient();
        long start = System.currentTimeMillis();
        String normalized = path;
        try {
            if (path.startsWith(groupName + "/")) {
                path = path.split(groupName + "/")[1];
            }
            configure(client);
            connectAndLogin(client);
            if (!path.startsWith("/")) path = "/" + path;
            normalized = path;
            log.debug("FTP delete start | group={} | normalizedPath={}", groupName, path);
            boolean ok = client.deleteFile(path);
            logReply(client, "deleteFile(" + path + ")");
            if (!ok) {
                throw new IOException("FTP delete failed: " + client.getReplyString());
            }
            log.info("delete file success, group: {}, path: {}", groupName, path);
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("FTP delete failed | group={} | path={} | cost={}ms", groupName, normalized, cost);
            log.error(DfsConstants.DELETE_FILE_EXCEPTION, e);
            throw new ServiceException(DfsConstants.DELETE_FILE_EXCEPTION, e);
        } finally {
            logoutQuietly(client);
        }
    }

    @Override
    public InputStream downloadStream(String groupName, String path) {
        try {
            byte[] bytes = downloadFile(groupName, path);
            return new ByteArrayInputStream(bytes);
        } catch (Exception e) {
            log.error(DfsConstants.DOWNLOAD_STREAM_FILE_EXCEPTION, e);
            throw new ServiceException(DfsConstants.DOWNLOAD_STREAM_FILE_EXCEPTION, e);
        }
    }

    @Override
    public byte[] downloadFile(String groupName, String path) {
        FTPClient client = new FTPClient();
        long start = System.currentTimeMillis();
        String normalized = path;
        try {
            if (path.startsWith(groupName + "/")) {
                path = path.split(groupName + "/")[1];
            }
            configure(client);
            connectAndLogin(client);
            if (!path.startsWith("/")) path = "/" + path;
            normalized = path;
            log.debug("FTP download start | group={} | normalizedPath={}", groupName, normalized);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean ok = client.retrieveFile(path, baos);
            logReply(client, "retrieveFile(" + path + ")");
            if (!ok) {
                throw new IOException("FTP retrieve failed: " + client.getReplyString());
            }
            byte[] data = baos.toByteArray();
            long cost = System.currentTimeMillis() - start;
            log.info("FTP download success | path={}/{} | bytes={} | cost={}ms", groupName,
                normalized.startsWith("/") ? normalized.substring(1) : normalized, data.length, cost);
            return data;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("FTP download failed | path={} | cost={}ms", normalized, cost);
            log.error(DfsConstants.DOWNLOAD_FILE_EXCEPTION, e);
            throw new ServiceException(DfsConstants.DOWNLOAD_FILE_EXCEPTION, e);
        } finally {
            logoutQuietly(client);
        }
    }

    private void configure(FTPClient client) {
        client.setControlEncoding(StandardCharsets.UTF_8.name());
        client.setConnectTimeout(props.getConnectTimeout());
        try {
            client.setSoTimeout(props.getDataTimeout());
        } catch (Exception ex) {
            log.debug("FTP setSoTimeout failed, dataTimeout={} ex={}", props.getDataTimeout(), ex.toString());
        }
        if (log.isDebugEnabled()) {
            log.debug("FTP client configured | encoding={} | connectTimeout={} | soTimeout={}ms",
                StandardCharsets.UTF_8, props.getConnectTimeout(), props.getDataTimeout());
        }
    }

    private void connectAndLogin(FTPClient client) throws IOException {
        log.debug("FTP connecting to {}:{} (connectTimeout={}ms)", props.getHost(), props.getPort(), props.getConnectTimeout());
        client.connect(props.getHost(), props.getPort());
        logReply(client, "connect");
        String user = props.getUsername();
        if (user == null || user.trim().isEmpty()) {
            user = "anonymous";
        }
        String pass = props.getPassword();
        if (pass == null) {
            pass = "";
        }
        log.debug("FTP logging in | user={}", mask(user));
        if (!client.login(user, pass)) {
            logReply(client, "login");
            throw new IOException("FTP login failed: " + client.getReplyString());
        }
        logReply(client, "login");
        if (props.isPassiveMode()) {
            client.enterLocalPassiveMode();
            log.debug("FTP mode: passive");
        } else {
            client.enterLocalActiveMode();
            log.debug("FTP mode: active");
        }
        boolean setTypeOk = client.setFileType(FTP.BINARY_FILE_TYPE);
        logReply(client, "setFileType(BINARY) result=" + setTypeOk);
    }

    private String buildPath(String extName) {
        String dateDir = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String filename = UUID.randomUUID().toString().replace("-", "");
        if (extName != null && !extName.isEmpty()) {
            filename = filename + "." + extName;
        }
        String base = props.getBaseDir();
        if (base == null || base.isEmpty()) base = "/";
        if (!base.startsWith("/")) base = "/" + base;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/" + dateDir + "/" + filename;
    }

    private void ensureDirectories(FTPClient client, String fullPath) throws IOException {
        int lastSlash = fullPath.lastIndexOf('/');
        if (lastSlash <= 0) return;
        String dir = fullPath.substring(0, lastSlash);
        String[] parts = dir.split("/");
        String cur = "";
        for (String p : parts) {
            if (p.isEmpty()) continue;
            cur += "/" + p;
            boolean made = client.makeDirectory(cur);
            logReply(client, "makeDirectory(" + cur + ") result=" + made);
        }
    }

    private void logoutQuietly(FTPClient client) {
        try {
            if (client.isConnected()) {
                try { client.logout(); logReply(client, "logout"); } catch (Exception ignored) {}
                try { client.disconnect(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
        }
    }

    private void logReply(FTPClient client, String action) {
        try {
            int code = client.getReplyCode();
            String reply = client.getReplyString();
            if (code >= 400) {
                log.warn("FTP {} | replyCode={} | reply={}", action, code, reply != null ? reply.trim() : "null");
            } else if (log.isDebugEnabled()) {
                log.debug("FTP {} | replyCode={} | reply={}", action, code, reply != null ? reply.trim() : "null");
            }
        } catch (Exception ex) {
            log.debug("FTP logReply suppressed: {}", ex.toString());
        }
    }

    private String mask(String v) {
        if (v == null) return "null";
        if (v.length() <= 2) return "***";
        return v.charAt(0) + "***" + v.charAt(v.length() - 1);
    }
}
