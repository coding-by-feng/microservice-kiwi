package me.fengorz.kiwi.common.dfs.ftp;

import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import me.fengorz.kiwi.common.dfs.DfsConstants;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.backend", havingValue = "ftp")
public class FtpDfsServiceImpl implements me.fengorz.kiwi.common.dfs.DfsService {

    private static final Logger log = LoggerFactory.getLogger(FtpDfsServiceImpl.class);
    private static final String GROUP = "ftp";
    private final FtpProperties props;

    public FtpDfsServiceImpl(FtpProperties props) { this.props = props; }

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName) throws DfsOperateException {
        return uploadFile(inputStream, size, extName, null);
    }

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName, Set<MetaData> metaDataSet)
        throws DfsOperateException {
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
            // Return as group/path, compatible with existing token scheme
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            long cost = System.currentTimeMillis() - start;
            log.info("FTP upload success | path={}/{} | size={} | cost={}ms", GROUP, path, size, cost);
            return GROUP + "/" + path;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("FTP upload failed | path={} | cost={}ms", path, cost);
            logFailureContext(client, "upload", GROUP, path, start, e);
            log.error(DfsConstants.UPLOAD_FILE_EXCEPTION, e);
            throw new DfsOperateException(DfsConstants.UPLOAD_FILE_EXCEPTION, e);
        } finally {
            logoutQuietly(client);
        }
    }

    @Override
    public void deleteFile(String groupName, String path) throws DfsOperateDeleteException {
        FTPClient client = new FTPClient();
        long start = System.currentTimeMillis();
        String normalized = path;
        try {
            String original = path;
            if (path.startsWith(groupName + "/")) {
                path = path.split(groupName + "/")[1];
            }
            configure(client);
            connectAndLogin(client);
            if (!path.startsWith("/")) path = "/" + path;
            normalized = path;
            log.debug("FTP delete start | group={} | originalPath={} | normalizedPath={}", groupName, original, path);
            boolean ok = client.deleteFile(path);
            logReply(client, "deleteFile(" + path + ")");
            if (!ok) {
                throw new IOException("FTP delete failed: " + client.getReplyString());
            }
            log.info("delete file success，group：{}，path：{}", groupName, path);
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("FTP delete failed | group={} | path={} | cost={}ms", groupName, normalized, cost);
            logFailureContext(client, "delete", groupName, normalized, start, e);
            log.error(DfsConstants.DELETE_FILE_EXCEPTION, e);
            throw new DfsOperateDeleteException(DfsConstants.DELETE_FILE_EXCEPTION);
        } finally {
            logoutQuietly(client);
        }
    }

    @Override
    public InputStream downloadStream(String groupName, String path) throws DfsOperateException {
        try {
            byte[] bytes = downloadFile(groupName, path);
            return new ByteArrayInputStream(bytes);
        } catch (Exception e) {
            log.error(DfsConstants.DOWNLOAD_STREAM_FILE_EXCEPTION, e);
            throw new DfsOperateException(DfsConstants.DOWNLOAD_STREAM_FILE_EXCEPTION);
        }
    }

    @Override
    public byte[] downloadFile(String groupName, String path) throws DfsOperateException {
        FTPClient client = new FTPClient();
        long start = System.currentTimeMillis();
        String normalized = path;
        try {
            String original = path;
            if (path.startsWith(groupName + "/")) {
                path = path.split(groupName + "/")[1];
            }
            configure(client);
            connectAndLogin(client);
            if (!path.startsWith("/")) path = "/" + path;
            normalized = path;
            log.debug("FTP download start | group={} | originalPath={} | normalizedPath={}", groupName, original, normalized);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean ok = client.retrieveFile(path, baos);
            logReply(client, "retrieveFile(" + path + ")");
            if (!ok) {
                throw new IOException("FTP retrieve failed: " + client.getReplyString());
            }
            byte[] data = baos.toByteArray();
            long cost = System.currentTimeMillis() - start;
            log.info("FTP download success | path={}/{} | bytes={} | cost={}ms", groupName, normalized.startsWith("/") ? normalized.substring(1) : normalized, data.length, cost);
            return data;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("FTP download failed | path={} | cost={}ms", normalized, cost);
            logFailureContext(client, "download", groupName, normalized, start, e);
            log.error(DfsConstants.DOWNLOAD_FILE_EXCEPTION, e);
            throw new DfsOperateException(DfsConstants.DOWNLOAD_FILE_EXCEPTION, e);
        } finally {
            logoutQuietly(client);
        }
    }

    private void configure(FTPClient client) {
        client.setControlEncoding(StandardCharsets.UTF_8.name());
        client.setConnectTimeout(props.getConnectTimeout());
        // Deprecated: client.setDataTimeout(props.getDataTimeout()); -> use socket SO timeout instead
        try {
            client.setSoTimeout(props.getDataTimeout());
        } catch (Exception ex) {
            log.debug("FTP setSoTimeout failed, dataTimeout={} ex={}", props.getDataTimeout(), ex.toString());
        }
        if (log.isDebugEnabled()) {
            log.debug("FTP client configured | encoding={} | connectTimeout={} | soTimeout={}ms", StandardCharsets.UTF_8, props.getConnectTimeout(), props.getDataTimeout());
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
            pass = ""; // allow passwordless
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
                String[] lines = client.getReplyStrings();
                if (lines != null && lines.length > 1) {
                    for (String line : lines) {
                        log.trace("FTP {} | srv> {}", action, line);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("FTP logReply suppressed: {}", ex.toString());
        }
    }

    private void logFailureContext(FTPClient client, String action, String group, String path, long start, Exception e) {
        try {
            long cost = System.currentTimeMillis() - start;
            String normalized = path == null ? "null" : (path.startsWith("/") ? path.substring(1) : path);
            int replyCode = -1;
            String replyStr = null;
            try {
                replyCode = client.getReplyCode();
                replyStr = client.getReplyString();
            } catch (Exception ignored) {}
            boolean connected = false;
            String remote = null;
            try {
                connected = client.isConnected();
                if (connected && client.getRemoteAddress() != null) {
                    remote = client.getRemoteAddress().getHostAddress() + ":" + client.getRemotePort();
                }
            } catch (Exception ignored) {}
            String exType = e != null ? e.getClass().getSimpleName() : "null";
            String exMsg = e != null && e.getMessage() != null ? e.getMessage() : "null";
            if (exMsg.length() > 256) exMsg = exMsg.substring(0,256) + "...";
            log.warn("FTP {} context | group={} | path={} | connected={} | remote={} | passiveMode={} | replyCode={} | reply={} | cost={}ms | exType={} | exMsg={}",
                action, group, normalized, connected, remote, props.isPassiveMode(), replyCode, replyStr != null ? replyStr.trim() : "null", cost, exType, exMsg);
        } catch (Exception ignore) {
            // best-effort logging only
        }
    }

    private String mask(String v) {
        if (v == null) return "null";
        if (v.length() <= 2) return "***";
        return v.charAt(0) + "***" + v.charAt(v.length() - 1);
    }
}
