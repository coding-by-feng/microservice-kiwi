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
package me.fengorz.kiwi.word.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.entity.WordExampleStarListDO;
import me.fengorz.kiwi.word.api.vo.star.ExampleStarItemVO;

/**
 * @author codingByFeng
 * @date 2019-12-08 23:27:12
 */
public interface IWordExampleStarListService extends IService<WordExampleStarListDO> {

    Integer countById(Integer id);

    R getCurrentUserList(Page page, Integer userId);

    R updateListByUser(WordExampleStarListDO entity, Integer id, Integer userId);

    IPage<ExampleStarItemVO> getListItems(Page page, Integer listId);
}