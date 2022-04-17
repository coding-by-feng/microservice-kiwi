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

package me.fengorz.kiwi.common.es.service;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.baomidou.mybatisplus.annotation.IdType;

public interface GenerateMappingService<T, U extends ElasticsearchRepository> {
    /**
     * 根据相应的ID生成对应的ES实体
     *
     * @param id
     * @param idType
     * @return
     */
    T generate(Integer id, IdType idType);
}
