package me.fengorz.kiwi.common.dfs.ftp;

import me.fengorz.kiwi.common.dfs.DfsService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link FtpDfsServiceImpl} using MockFtpServer.
 */
@SpringBootTest(classes = {FtpDfsServiceImplIT.TestConfig.class})
@ActiveProfiles("test")
public class FtpDfsServiceImplIT {

    private static final Logger log = LoggerFactory.getLogger(FtpDfsServiceImplIT.class);

    @Configuration
    static class TestConfig {
        @Bean
        public FtpProperties ftpProperties(Environment env) {
            FtpProperties p = new FtpProperties();
            p.setHost(env.getProperty("ftp.host", "127.0.0.1"));
            p.setPort(Integer.parseInt(env.getProperty("ftp.port", "21")));
            p.setUsername(env.getProperty("ftp.username", "anonymous"));
            p.setPassword(env.getProperty("ftp.password", ""));
            p.setBaseDir(env.getProperty("ftp.baseDir", "/"));
            p.setPassiveMode(Boolean.parseBoolean(env.getProperty("ftp.passiveMode", "true")));
            p.setConnectTimeout(Integer.parseInt(env.getProperty("ftp.connectTimeout", "15000")));
            p.setDataTimeout(Integer.parseInt(env.getProperty("ftp.dataTimeout", "30000")));
            return p;
        }
        @Bean
        public DfsService dfsService(FtpProperties props) {
            return new FtpDfsServiceImpl(props);
        }
    }

    @Autowired
    private DfsService dfsService;
    @Autowired
    private FtpProperties ftpProperties;

    private static FakeFtpServer fakeFtpServer;

    @BeforeAll
    static void setUpServer() {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.setServerControlPort(2121); // use a test port

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data"));
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.addUserAccount(new UserAccount("testuser", "testpass", "/"));
        fakeFtpServer.start();
    }

    @AfterAll
    static void tearDownServer() {
        if (fakeFtpServer != null) {
            fakeFtpServer.stop();
        }
    }

    @Test
    void uploadDownloadDelete_roundTrip() throws Exception {
        log.info("Using ftp properties: host={} port={} user={} baseDir={} passiveMode={}",
                ftpProperties.getHost(), ftpProperties.getPort(), ftpProperties.getUsername(), ftpProperties.getBaseDir(), ftpProperties.isPassiveMode());

        assertEquals("127.0.0.1", ftpProperties.getHost());
        assertEquals(2121, ftpProperties.getPort());

        String content = "Hello FTP " + UUID.randomUUID();
        byte[] data = content.getBytes();
        String storedPath;
        try (InputStream is = new ByteArrayInputStream(data)) {
            storedPath = dfsService.uploadFile(is, data.length, "txt");
        }
        assertNotNull(storedPath, "storedPath should not be null after upload");
        assertTrue(storedPath.startsWith("ftp/"), "storedPath should start with ftp/");

        String group = storedPath.split("/")[0];
        String relative = storedPath.substring(group.length() + 1);

        byte[] downloaded = dfsService.downloadFile(group, relative);
        assertArrayEquals(data, downloaded, "Downloaded data should match uploaded content");

        dfsService.deleteFile(group, relative);

        Exception ex = assertThrows(Exception.class, () -> dfsService.downloadFile(group, relative));
        assertNotNull(ex);
    }
}
