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

package me.fengorz.kiwi.word.biz.test;

import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.biz.mapper.WordMainMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/11/25 10:27 AM
 */
@Service
@AllArgsConstructor
public class SubTestService {

    private final WordMainMapper wordMainMapper;

    @Transactional(rollbackFor = Exception.class)
    public boolean testTransactional() throws Exception {
        this.wordMainMapper.insert(new WordMainDO().setWordName("SubTestService").setIsDel(CommonConstants.FALSE));
        return true;
    }

}
