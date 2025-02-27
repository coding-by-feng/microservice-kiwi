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

package me.fengorz.kiwi.common.sdk.util;

import lombok.experimental.UtilityClass;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.ResourceNotFoundException;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

/**
 * @Author Kason Zhan @Date 2020/1/14 4:48 PM
 */
@UtilityClass
public class CommonUtils {

    public static String translateBooleanToStr(boolean flag) {
        if (flag) {
            return GlobalConstants.FLAG_Y;
        } else {
            return GlobalConstants.FLAG_N;
        }
    }


    public static String getResourcePath() {
        return Optional.ofNullable(CommonUtils.class.getResource(File.separator))
                .orElseThrow(ResourceNotFoundException::new).getPath();
    }

    public static String getResourceFile(String pathAndFile) {
        return Optional.ofNullable(CommonUtils.class.getResource(File.separator))
                .orElseThrow(ResourceNotFoundException::new).getFile() + pathAndFile;
    }

    public static InputStream getResourceFileInputStream(String pathAndFile) {
        return CommonUtils.class.getClassLoader().getResourceAsStream(pathAndFile);
    }
}
