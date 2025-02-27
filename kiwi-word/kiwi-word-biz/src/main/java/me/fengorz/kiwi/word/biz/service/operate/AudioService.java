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

package me.fengorz.kiwi.word.biz.service.operate;

import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/7/13 20:29
 */
public interface AudioService {

    String generateVoice(String text, int type) throws DfsOperateException, TtsException;

    String generateEnglishVoice(String englishText) throws DfsOperateException, TtsException;

    String generateChineseVoice(String chineseText) throws DfsOperateException, TtsException;

    String generateVoiceUseBaiduTts(String chineseText) throws DfsOperateException, TtsException;
}
