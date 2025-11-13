/*
 *
 * Copyright [2019~2025] [zhanshifeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kason.bdf.security.service;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.sdk.annotation.cache.KasonCacheKey;
import me.fengorz.kason.common.sdk.annotation.cache.KasonCacheKeyPrefix;
import me.fengorz.kason.common.sdk.constant.CacheConstants;
import me.fengorz.kason.common.sdk.constant.SecurityConstants;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;

import javax.sql.DataSource;

/**
 * @Author Kason Zhan
 */
@Slf4j
public class KasonClientDetailsService extends JdbcClientDetailsService {

    public KasonClientDetailsService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    @KasonCacheKeyPrefix(CacheConstants.CLIENT_DETAILS)
    @Cacheable(value = SecurityConstants.CLIENT_DETAILS_KEY, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
        unless = "#result == null")
    public ClientDetails loadClientByClientId(@KasonCacheKey String clientId) throws InvalidClientException {
        try {
            return super.loadClientByClientId(clientId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
