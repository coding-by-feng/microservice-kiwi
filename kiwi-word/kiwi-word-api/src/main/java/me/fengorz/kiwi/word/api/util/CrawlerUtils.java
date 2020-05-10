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

package me.fengorz.kiwi.word.api.util;

import cn.hutool.core.util.StrUtil;
import me.fengorz.kiwi.common.api.constant.CommonConstants;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/11/4 4:51 PM
 */
public class CrawlerUtils {

    public static String getGroupName(String uploadResult) {
        if (StrUtil.isNotBlank(uploadResult)) {
            return uploadResult.substring(0, uploadResult.indexOf("/"));
        }
        return CommonConstants.EMPTY;
    }

    public static String getUploadVoiceFilePath(String uploadResult) {
        if (StrUtil.isNotBlank(uploadResult)) {
            return uploadResult.substring(uploadResult.indexOf("/") + 1, uploadResult.length());
        }
        return CommonConstants.EMPTY;
    }

    public static String getVoiceFileName(String url) {
        return url.substring(url.lastIndexOf("/"), url.length());
    }

}
