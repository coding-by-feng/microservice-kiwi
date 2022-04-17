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

package me.fengorz.kiwi.generator.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.generator.mapper.SysGeneratorMapper;
import me.fengorz.kiwi.generator.service.SysGeneratorService;

/**
 * @Author zhanshifeng @Date 2019-09-16 16:51
 */
@Service
@RequiredArgsConstructor
public class SysGeneratorServiceImpl implements SysGeneratorService {
    private final SysGeneratorMapper sysGeneratorMapper;

    @Override
    public Map<String, String> queryTable(String tableName) {
        return sysGeneratorMapper.queryTable(tableName);
    }

    @Override
    public List<Map<String, String>> queryColumns(String tableName) {
        return sysGeneratorMapper.queryColumns(tableName);
    }
}
