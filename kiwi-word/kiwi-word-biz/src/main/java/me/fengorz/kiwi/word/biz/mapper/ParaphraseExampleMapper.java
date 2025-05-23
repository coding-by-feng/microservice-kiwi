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
package me.fengorz.kiwi.word.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.fengorz.kiwi.common.db.MapperConstant;
import me.fengorz.kiwi.word.api.dto.mapper.in.SelectEntityIsCollectDTO;
import me.fengorz.kiwi.word.api.entity.ParaphraseExampleDO;
import me.fengorz.kiwi.word.api.vo.ParaphraseExampleVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 单词例句表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:40:38
 */
public interface ParaphraseExampleMapper extends BaseMapper<ParaphraseExampleDO> {

    List<ParaphraseExampleVO>
        selectExampleAndIsCollect(@Param(MapperConstant.QUERY_PARAMS) SelectEntityIsCollectDTO dto);
}
