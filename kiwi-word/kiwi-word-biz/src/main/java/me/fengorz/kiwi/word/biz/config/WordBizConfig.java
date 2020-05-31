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

package me.fengorz.kiwi.word.biz.config;

import me.fengorz.kiwi.bdf.core.config.ScanConfig;
import me.fengorz.kiwi.common.fastdfs.config.DfsConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @Author zhanshifeng
 * @Date 2019/10/30 3:45 PM
 */
@Configuration
// @MapperScan("me.fengorz.kiwi.word.biz.mapper")
// @ComponentScan("me.fengorz.kiwi.word.biz")
@Import({ScanConfig.class, DfsConfig.class})
public class WordBizConfig {

}
