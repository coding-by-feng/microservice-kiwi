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

package me.fengorz.kiwi.word.api.common;

/**
 * @Description word服务静态变量
 * @Author zhanshifeng
 * @Date 2020/5/17 11:19 PM
 */
public class WordConstants {

    public static final String KIWI_WORD_BIZ = "kiwi-word-biz";

    public static final String CACHE_NAMES = "kiwi";

    public interface CACHE_KEY_PREFIX_OPERATE {

        String CLASS = "operate";
        String METHOD_WORD_NAME = "word_name";
        String METHOD_PARAPHRASE_ID = "paraphrase_id";

    }

    public interface CACHE_KEY_PREFIX_WORD_MAIN {

        String CLASS = "word_main";
        String METHOD_ID = "id";
        String METHOD_NAME = "name";

    }

    public interface CACHE_KEY_PREFIX_WORD_VARIANT {
        String CLASS = "word_variant";
        String METHOD_VARIANT_NAME = "name";
        String METHOD_ID_NAME = "id_name";
    }

    public static final int VARIANT_TYPE_UNKNOWN = 0;

}
