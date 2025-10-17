package me.fengorz.kiwi.common.dfs.ftp;

import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.dfs.DfsConstants;
import me.fengorz.kiwi.common.dfs.DfsService;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
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
import java.util.Set;
import java.util.UUID;

@Service("ftpDfsService")
@ConditionalOnProperty(name = "storage.backend", havingValue = "ftp")
@Slf4j
@RequiredArgsConstructor
public class FtpDfsServiceImpl implements DfsService {

    private static final String GROUP = "ftp";

    private final FtpProperties props;

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName) throws DfsOperateException {
        return uploadFile(inputStream, size, extName, null);
    }

    @Override
    public String uploadFile(InputStream inputStream, long size, String extName, Set<MetaData> metaDataSet)
        throws DfsOperateException {
        FTPClient client = new FTPClient();
        try {
            configure(client);
            connectAndLogin(client);
            String path = buildPath(extName);
            ensureDirectories(client, path);
            client.setFileType(FTP.BINARY_FILE_TYPE);
            boolean ok = client.storeFile(path, inputStream);
            if (!ok) {
                throw new IOException("FTP storeFile failed: " + client.getReplyString());
            }
            // Return as group/path, compatible with existing token scheme
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return GROUP + "/" + path;
        } catch (Exception e) {
            log.error(DfsConstants.UPLOAD_FILE_EXCEPTION, e);
            throw new DfsOperateException(DfsConstants.UPLOAD_FILE_EXCEPTION, e);
        } finally {
            logoutQuietly(client);
        }
    }

    @Override
    public void deleteFile(String groupName, String path) throws DfsOperateDeleteException {
        FTPClient client = new FTPClient();
        try {
            if (path.startsWith(groupName + "/")) {
                path = path.split(groupName + "/")[1];
            }
            configure(client);
            connectAndLogin(client);
            if (!path.startsWith("/")) path = "/" + path;
            boolean ok = client.deleteFile(path);
            if (!ok) {
                throw new IOException("FTP delete failed: " + client.getReplyString());
            }
            log.info("delete file success，group：{}，path：{}", groupName, path);
        } catch (Exception e) {
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
        try {
            if (path.startsWith(groupName + "/")) {
                path = path.split(groupName + "/")[1];
            }
            configure(client);
            connectAndLogin(client);
            if (!path.startsWith("/")) path = "/" + path;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean ok = client.retrieveFile(path, baos);
            if (!ok) {
                throw new IOException("FTP retrieve failed: " + client.getReplyString());
            }
            return baos.toByteArray();
        } catch (Exception e) {
            log.error(DfsConstants.DOWNLOAD_FILE_EXCEPTION, e);
            throw new DfsOperateException(DfsConstants.DOWNLOAD_FILE_EXCEPTION, e);
        } finally {
            logoutQuietly(client);
        }
    }

    private void configure(FTPClient client) {
        client.setControlEncoding(StandardCharsets.UTF_8.name());
        client.setConnectTimeout(props.getConnectTimeout());
        client.setDataTimeout(props.getDataTimeout());
    }

    private void connectAndLogin(FTPClient client) throws IOException {
        client.connect(props.getHost(), props.getPort());
        String user = props.getUsername();
        if (user == null || user.trim().isEmpty()) {
            user = "anonymous";
        }
        String pass = props.getPassword();
        if (pass == null) {
            pass = ""; // allow passwordless
        }
        if (!client.login(user, pass)) {
            throw new IOException("FTP login failed: " + client.getReplyString());
        }
        if (props.isPassiveMode()) {
            client.enterLocalPassiveMode();
        } else {
            client.enterLocalActiveMode();
        }
        client.setFileType(FTP.BINARY_FILE_TYPE);
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
            client.makeDirectory(cur);
        }
    }

    private void logoutQuietly(FTPClient client) {
        try {
            if (client.isConnected()) {
                try { client.logout(); } catch (Exception ignored) {}
                try { client.disconnect(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
        }
    }
}
