package com;/*
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

import com.vocabulary.enhancer.word.biz.config.TransactionalTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @Description 测试一下事务注解的一些特性
 * @Author zhanshifeng
 * @Date 2019/11/24 9:24 PM
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TransactionalTestConfig.class)
@Slf4j
public class TransactionalTest {

    // @Autowired
    // private TestService testService;

    // @Test
    public void test() {

        // log.debug(EnhancedLogUtils.getClassName() + CommonConstants.DOT + EnhancedLogUtils.getMethodName());
        // testService.testTransactional();

    }

}
