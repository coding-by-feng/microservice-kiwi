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
package me.fengorz.kiwi.word.biz.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.bdf.core.service.ISeqService;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.constant.MapperConstant;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.entity.FetchQueueDO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.biz.service.base.IWordFetchQueueService;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 单词主表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:32:07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/")
@Slf4j
public class TestTempController extends BaseController {

    private final IWordFetchQueueService wordFetchQueueService;
    private final ISeqService seqService;
    // @Value("${me.fengorz.file.vocabulary.word.list.path}")
    private String tmp;

    @GetMapping("/readTxt")
    public R readTxt() throws Exception {
        List<String> words = this.readWords();
        for (String word : words) {
            FetchQueueDO one = wordFetchQueueService
                .getOne(new LambdaQueryWrapper<FetchQueueDO>().eq(FetchQueueDO::getWordName, word));
            FetchQueueDO queue = null;
            if (one != null) {
                if (WordCrawlerConstants.STATUS_ALL_SUCCESS == one.getFetchStatus()) {
                    continue;
                } else {
                    queue = one;
                    queue.setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH);
                    queue.setWordName(word.trim());
                    queue.setFetchPriority(100);
                    queue.setIsLock(GlobalConstants.FLAG_YES);
                    wordFetchQueueService.updateById(queue);
                }
            } else {
                queue = new FetchQueueDO();
                queue.setQueueId(seqService.genIntSequence(MapperConstant.T_INS_SEQUENCE));
                queue.setFetchStatus(WordCrawlerConstants.STATUS_TO_FETCH);
                queue.setWordName(word.trim());
                queue.setFetchPriority(100);
                queue.setIsLock(GlobalConstants.FLAG_YES);
                wordFetchQueueService.save(queue);
            }
            log.info(word + "insert success!");
        }
        return R.success();
    }

    @PostMapping("/testEdit")
    public R testEdit(@RequestBody WordMainDO wordMainDO) {
        return R.success();
    }

    public List<String> readWords() {
        log.info("=================>this.tmp=" + this.tmp);
        List<String> wordList = new ArrayList<>();
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null; // 用于包装InputStreamReader,提高处理性能。因为BufferedReader有缓冲的，而InputStreamReader没有。
        try {
            fis = new FileInputStream(this.tmp + "/vocabulary.txt");// FileInputStream
            // fis = new FileInputStream("/root/tmp/vocabulary.txt");// FileInputStream
            // 从文件系统中的某个文件中获取字节
            isr = new InputStreamReader(fis);// InputStreamReader 是字节流通向字符流的桥梁,
            br = new BufferedReader(isr);// 从字符输入流中读取文件中的内容,封装了一个new InputStreamReader的对象
            String line = null;
            int i = 1;
            while ((line = br.readLine()) != null) {
                if (StrUtil.isBlank(line)) {
                    continue;
                }
                wordList.add(line);
            }
        } catch (FileNotFoundException e) {
            System.out.println("找不到指定文件");
        } catch (IOException e) {
            System.out.println("读取文件失败");
        } finally {
            try {
                br.close();
                isr.close();
                fis.close();
                // 关闭的时候最好按照先后顺序关闭最后开的先关闭所以先关s,再关n,最后关m
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return wordList;
    }
}
