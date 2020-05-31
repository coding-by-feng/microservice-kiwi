/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
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

import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kiwi.word.api.entity.WordParaphraseDO;
import me.fengorz.kiwi.word.api.vo.detail.WordParaphraseVO;

import java.util.List;

/**
 * 单词释义表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:39:48
 */
public interface IWordParaphraseService extends IService<WordParaphraseDO> {

    Integer countById(Integer id);

    List<WordParaphraseVO> selectParaphraseAndIsCollect(Integer characterId, Integer currentUserId);

}
