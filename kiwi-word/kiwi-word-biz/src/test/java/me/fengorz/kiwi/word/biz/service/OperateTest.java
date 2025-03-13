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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.vo.detail.WordQueryVO;
import me.fengorz.kiwi.word.biz.WordBizApplication;
import me.fengorz.kiwi.word.biz.controller.WordMainController;
import me.fengorz.kiwi.word.biz.service.base.WordMainService;
import me.fengorz.kiwi.word.biz.service.operate.CleanerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@ActiveProfiles({EnvConstants.TEST})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled
public class OperateTest {

    @Autowired
    private WordMainController wordMainController;
    @Autowired
    private CleanerService cleanerService;
    @Autowired
    private WordMainService wordMainService;

    @Test
    void queryWord() {
        R<IPage<WordQueryVO>> result = wordMainController.queryWord("accede to sth");
        List<WordQueryVO> records = result.getData().getRecords();
        Assertions.assertTrue(records.size() > 0);
        Assertions.assertEquals(records.get(0).getWordName(), "accede to sth");
    }

    @Test
    @Disabled
    void evictAll() {
        WordMainDO tuesday = wordMainService.getOne(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, "Tuesday"));
        cleanerService.evictAll(tuesday, "Tuesday");
    }
}