/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */
package me.fengorz.kiwi.word.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.fengorz.kiwi.common.api.constant.MapperConstant;
import me.fengorz.kiwi.word.api.dto.mapper.in.SelectStarListItemDTO;
import me.fengorz.kiwi.word.api.entity.WordExampleStarListDO;
import me.fengorz.kiwi.word.api.vo.star.ExampleStarItemVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author codingByFeng
 * @date 2019-12-08 23:27:12
 */
public interface WordExampleStarListMapper extends BaseMapper<WordExampleStarListDO> {

    IPage<ExampleStarItemVO> selectListItems(Page<?> page, Integer listId);

}