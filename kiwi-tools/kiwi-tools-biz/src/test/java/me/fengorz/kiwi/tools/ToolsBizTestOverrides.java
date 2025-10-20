package me.fengorz.kiwi.tools;

import me.fengorz.kiwi.common.dfs.DfsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
public class ToolsBizTestOverrides {

    @Bean(name = "kiwiAccessDeniedHandler")
    @Primary
    public AccessDeniedHandler kiwiAccessDeniedHandlerOverride() {
        // Minimal no-op handler to bypass JAXB-heavy OAuth2 handler
        return (request, response, ex) -> response.setStatus(403);
    }

    @Bean(name = "redisConnectionFactory")
    public RedisConnectionFactory redisConnectionFactoryOverride() {
        // No actual Redis server required; not used during tests
        return new LettuceConnectionFactory();
    }

    // Provide a primary DFS bean named 'dfsService' to satisfy @Qualifier("dfsService")
    @Bean(name = "dfsService")
    @Primary
    public DfsService dfsServicePrimary() {
        return inMemoryDfs();
    }

    // Override possible ftp DFS bean by name to avoid other primaries
    @Bean(name = "ftpDfsService")
    public DfsService ftpDfsServiceOverride() {
        return inMemoryDfs();
    }

    private DfsService inMemoryDfs() {
        return new DfsService() {
            private final Map<String, byte[]> store = new ConcurrentHashMap<>(); // key: group/path

            @Override
            public String uploadFile(InputStream inputStream, long size, String extName) {
                String ext = (extName == null || extName.isEmpty()) ? "jpg" : extName;
                String path = "test-" + UUID.randomUUID() + "." + ext;
                String fileId = "group1/" + path;
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = inputStream.read(buf)) != -1) {
                        baos.write(buf, 0, n);
                    }
                    store.put(fileId, baos.toByteArray());
                } catch (Exception ignored) {}
                return fileId;
            }

            @Override
            public String uploadFile(InputStream inputStream, long size, String extName, Set<com.github.tobato.fastdfs.domain.fdfs.MetaData> metaDataSet) {
                return uploadFile(inputStream, size, extName);
            }

            @Override
            public void deleteFile(String groupName, String path) {
                store.remove(groupName + "/" + path);
            }

            @Override
            public InputStream downloadStream(String groupName, String path) {
                return new ByteArrayInputStream(downloadFile(groupName, path));
            }

            @Override
            public byte[] downloadFile(String groupName, String path) {
                return store.getOrDefault(groupName + "/" + path, new byte[0]);
            }
        };
    }
}
