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

package me.fengorz.kiwi.upms.config;

import me.fengorz.kiwi.bdf.config.CoreConfig;
import me.fengorz.kiwi.bdf.config.SecurityConfig;
import me.fengorz.kiwi.common.cache.redis.config.CacheConfig;
import me.fengorz.kiwi.common.db.config.DbConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @Author Kason Zhan @Date 2019-09-20 09:28
 */
@Configuration
@Import({CoreConfig.class, DbConfig.class, CacheConfig.class, SecurityConfig.class})
@ComponentScan("me.fengorz.kiwi.upms.**")
public class UpmsConfig {
}
