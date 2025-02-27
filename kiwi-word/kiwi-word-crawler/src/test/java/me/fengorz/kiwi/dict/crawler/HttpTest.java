/*
 *
 * Copyright [2019~2025] [codingByFeng]
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

package me.fengorz.kiwi.dict.crawler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;

/**
 * @Author Kason Zhan @Date 2019/11/4 4:37 PM
 */
public class HttpTest {

    public static void main(String[] args) {
        // String voiceFileUrl =
        // URLUtil.decode("https://dictionary.cambridge.org/" +
        // "M00/1E/F2/rBAQCV9hmDyAVpIYAAAb7aU0miQ759.ogg");
        String voiceFileUrl = URLUtil.decode("http://kiwidict.com/wordBiz/word/pronunciation/downloadVoice/5858984");
        long voiceSize = HttpUtil.downloadFile(voiceFileUrl,
            FileUtil.file("/Users/zhanshifeng/Documents/myDocument/temp/20201015/test.mp3"));
        System.out.println(voiceFileUrl.substring(voiceFileUrl.lastIndexOf("/")));
    }
}
