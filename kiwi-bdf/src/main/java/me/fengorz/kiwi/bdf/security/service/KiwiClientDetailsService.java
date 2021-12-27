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

package me.fengorz.kiwi.bdf.security.service;

import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.sdk.constant.CacheConstants;
import me.fengorz.kiwi.common.sdk.constant.SecurityConstants;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;

import javax.sql.DataSource;

/**
 * @Author zhanshifeng
 */
public class KiwiClientDetailsService extends JdbcClientDetailsService {

    public KiwiClientDetailsService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    @KiwiCacheKeyPrefix(CacheConstants.CLIENT_DETAILS)
    @Cacheable(value = SecurityConstants.CLIENT_DETAILS_KEY, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
        unless = "#result == null")
    public ClientDetails loadClientByClientId(@KiwiCacheKey String clientId) throws InvalidClientException {
        return super.loadClientByClientId(clientId);
    }
}
