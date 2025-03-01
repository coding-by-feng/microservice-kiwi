/*
 *
 * Copyright [2019~2025] [codingByFeng]
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

package me.fengorz.kiwi.bdf.core.config;

import me.fengorz.kiwi.bdf.core.mapper.SeqMapper;
import me.fengorz.kiwi.bdf.core.service.SeqService;
import me.fengorz.kiwi.common.sdk.config.UtilsBeanConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.*;

/**
 * @Description 排除DB的配置类
 * @Author Kason Zhan
 * @Date 2022/4/24 23:07
 */
@Configuration
@ImportAutoConfiguration(
    exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@ComponentScan(basePackages = {"me.fengorz.kiwi.bdf.**"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {SeqService.class, SeqMapper.class})})
@Import({UtilsBeanConfiguration.class})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ConditionalOnProperty(value = "my.config.exclude-db", havingValue = "true")
public class CoreExcludeDBConfig {}
