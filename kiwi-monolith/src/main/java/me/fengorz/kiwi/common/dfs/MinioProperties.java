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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO Properties Configuration
 *
 * @author codingByFeng
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO server endpoint URL
     */
    private String endpoint = "http://localhost:9001";

    /**
     * Access key (username)
     */
    private String accessKey;

    /**
     * Secret key (password)
     */
    private String secretKey;

    /**
     * Default bucket name
     */
    private String bucket = "kiwi";

    /**
     * Connection timeout in milliseconds
     */
    private int connectTimeout = 10000;

    /**
     * Read timeout in milliseconds
     */
    private int readTimeout = 30000;

    /**
     * Write timeout in milliseconds
     */
    private int writeTimeout = 60000;
}
