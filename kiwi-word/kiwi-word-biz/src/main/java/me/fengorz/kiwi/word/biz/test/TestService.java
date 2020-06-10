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

package me.fengorz.kiwi.word.biz.test;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.biz.mapper.WordMainMapper;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/11/25 10:26 AM
 */
@Service
@RequiredArgsConstructor
public class TestService {

    private final WordMainMapper wordMainMapper;
    private final SubTestService subTestService;

    @Transactional(noRollbackFor = NullPointerException.class, rollbackFor = Exception.class)
    public void testTransactional() throws Exception {
        this.wordMainMapper.insert(new WordMainDO().setWordName("TestService").setIsDel(CommonConstants.FLAG_N));
        // subTestService.testTransactional();
        throw new NullPointerException();
    }

}
